package com.artemis.the.gr8.playerstats.core.multithreading;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstats.core.Main;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.enums.StandardMessage;
import com.artemis.the.gr8.playerstats.core.msg.OutputManager;
import com.artemis.the.gr8.playerstats.core.utils.MyLogger;
import com.artemis.the.gr8.playerstats.core.utils.OfflinePlayerHandler;

/**
 * The ThreadManager is in charge of the Threads that PlayerStats can utilize.
 * It keeps track of past and currently active Threads, to ensure a Player
 * cannot start multiple Threads at the same time (thereby limiting them to one
 * stat-lookup at a time). It also passes appropriate references along to the
 * {@link ReloadThread}, which handles plugin reloading.
 */
public final class ThreadManager {

    private final static int threshold = 10;
    private int reloadThreadID;

    private final Main main;
    private final ConfigHandler config;
    private static OutputManager outputManager;

    private ReloadThread activatedReloadThread;
    private static long lastRecordedCalcTime;

    private static Plugin plugin;
    private static OfflinePlayerHandler offlinePlayerHandler;

    private static ForkJoinPool commonPool;
    private static ConcurrentHashMap<UUID, StatRequest<?>> activeRequests;
    private static ConcurrentHashMap<String, Long> taskTime;

    private static AtomicInteger activeStatActionCount;

    public ThreadManager(JavaPlugin plugin) {
        this.main = null;
        this.config = ConfigHandler.getInstance();
        outputManager = OutputManager.getInstance();

        reloadThreadID = 0;
        lastRecordedCalcTime = 0;

        ThreadManager.plugin = plugin;
        offlinePlayerHandler = OfflinePlayerHandler.getInstance();

        commonPool = ForkJoinPool.commonPool();
        activeRequests = new ConcurrentHashMap<>();
        taskTime = new ConcurrentHashMap<>();

        activeStatActionCount = new AtomicInteger(0);
    }

    /**
     * Factory method to create a PlayerLoadAction. This is needed by
     * OfflinePlayerHandler.
     */
    public static PlayerLoadAction getPlayerLoadAction(OfflinePlayer[] players, ConcurrentHashMap<String, UUID> mapToFill) {
        MyLogger.actionCreated(players != null ? players.length : 0); // Log action creation
        return new PlayerLoadAction(players, mapToFill);
    }

    /**
     * Prepares the StatAction for execution.
     */
    private static @NotNull
    StatAction prepareAction(StatRequest.Settings requestSettings) {
        long time = System.currentTimeMillis();

        // Get player names first, then map to OfflinePlayer objects
        List<String> playerNames = offlinePlayerHandler.getIncludedOfflinePlayerNames();
        OfflinePlayer[] players = playerNames.parallelStream()
                .map(offlinePlayerHandler::getIncludedOfflinePlayer)
                .filter(player -> player != null)
                .toArray(OfflinePlayer[]::new);

        activeStatActionCount.getAndIncrement();

        MyLogger.logLowLevelTask("Prepared calculation task", time);
        MyLogger.logMediumLevelMsg("Prepared stat calculation task for " + players.length + " players!");

        return new StatAction(players, requestSettings);
    }

    /**
     * Executes the stat calculation task asynchronously and calls the provided
     * callback on the main server thread with the raw result.
     *
     * @param request The StatRequest containing settings and sender info.
     * @param onComplete A BiConsumer callback that accepts the original request
     * and the resulting map.
     */
    public void startStatCalculation(@NotNull StatRequest<?> request, @NotNull BiConsumer<StatRequest<?>, ConcurrentHashMap<String, Integer>> onComplete) {
        CommandSender sender = request.getSettings().getCommandSender();
        UUID uniqueId = sender instanceof OfflinePlayer ? ((OfflinePlayer) sender).getUniqueId() : UUID.randomUUID();

        // Check if this sender already has a request running (if config limits it)
        if (ConfigHandler.getInstance().limitStatRequests() && activeRequests.containsKey(uniqueId)) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.REQUEST_ALREADY_RUNNING);
            return;
        }
        activeRequests.put(uniqueId, request);

        // Prepare the actual calculation task
        final StatAction task = prepareAction(request.getSettings());

        // Run the calculation task asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                ConcurrentHashMap<String, Integer> rawResult = null;
                try {
                    rawResult = commonPool.invoke(task); // Execute the StatAction directly
                } catch (Exception e) {
                    MyLogger.logWarning("Exception during async stat calculation: " + e.getMessage());
                    // Optionally log stack trace: e.printStackTrace();
                } finally {
                    activeRequests.remove(uniqueId);
                    activeStatActionCount.decrementAndGet(); // Decrement counter when task finishes

                    final ConcurrentHashMap<String, Integer> finalRawResult = rawResult != null ? rawResult : new ConcurrentHashMap<>();

                    // Schedule the callback to run on the main thread
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                onComplete.accept(request, finalRawResult);
                            } catch (Exception e) {
                                MyLogger.logWarning("Exception during stat calculation completion callback: " + e.getMessage());
                                // Optionally log stack trace: e.printStackTrace();
                            }
                        }
                    }.runTask(plugin);
                }
                MyLogger.logMediumLevelMsg("Stat calculation task finished! Average execution time (ms) for all requests: " + getAverageRequestTime());
            }
        }.runTaskAsynchronously(plugin);
    }

    public void startReloadThread(CommandSender sender) {
        if (main == null) {
            MyLogger.logWarning("Cannot start reload thread: Main plugin instance not available.");
            outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR); // Or a more specific message
            return;
        }
        if (activatedReloadThread == null || !activatedReloadThread.isAlive()) {
            reloadThreadID += 1;

            activatedReloadThread = new ReloadThread(main, outputManager, reloadThreadID, sender);
            activatedReloadThread.start();
        } else {
            MyLogger.logLowLevelMsg("Another reloadThread is already running! (" + activatedReloadThread.getName() + ")");
        }
    }

    /**
     * Store the duration in milliseconds of the last top-stat-lookup (or of
     * loading the offline-player-list if no look-ups have been done yet).
     */
    public static void recordCalcTime(long time) {
        taskTime.put(String.valueOf(taskTime.mappingCount()), time);
    }

    /**
     * Returns the duration in milliseconds of the last top-stat-lookup (or of
     * loading the offline-player-list if no look-ups have been done yet).
     */
    public static long getLastRecordedCalcTime() {
        return lastRecordedCalcTime;
    }

    public static int getActiveActionCount() {
        return activeStatActionCount.get();
    }

    public static double getAverageRequestTime() {
        // Ensure thread-safety if taskTime can be modified concurrently
        // Using ConcurrentHashMap is good, but the stream operation might need care
        // For simplicity, assuming this read is infrequent enough or acceptable
        java.util.Collection<Long> times = taskTime.values();
        return times.stream().mapToLong(l -> l).average().orElse(0);
    }
}
