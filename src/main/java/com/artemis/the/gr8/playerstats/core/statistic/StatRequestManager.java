package com.artemis.the.gr8.playerstats.core.statistic;

import java.util.LinkedHashMap;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.artemis.the.gr8.playerstats.api.RequestGenerator;
import com.artemis.the.gr8.playerstats.api.StatManager;
import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstats.api.StatResult;
import com.artemis.the.gr8.playerstats.core.Main;
import com.artemis.the.gr8.playerstats.core.msg.OutputManager;
import com.artemis.the.gr8.playerstats.core.utils.OfflinePlayerHandler;
import com.artemis.the.gr8.playerstats.core.utils.Reloadable;

/**
 * Turns user input into a {@link StatRequest} that can be executed to get
 * statistic data.
 */
public final class StatRequestManager implements StatManager, Reloadable {

    private static RequestProcessor processor;
    private final OfflinePlayerHandler offlinePlayerHandler;

    public StatRequestManager() {
        offlinePlayerHandler = OfflinePlayerHandler.getInstance();
        processor = getProcessor();
        Main.registerReloadable(this);
    }

    @Override
    public void reload() {
        processor = getProcessor();
    }

    private @NotNull
    RequestProcessor getProcessor() {
        OutputManager outputManager = OutputManager.getInstance();
        return new BukkitProcessor(outputManager);
    }

    /**
     * Returns the RequestProcessor responsible for calculations.
     */
    public static RequestProcessor getBukkitProcessor() {
        // Ensure processor is initialized if called before constructor/reload
        if (processor == null) {
            processor = new BukkitProcessor(OutputManager.getInstance());
        }
        return processor;
    }

    /**
     * @deprecated Use the individual execute[Type]Request methods.
     */
    @Deprecated
    public static void execute(@NotNull StatRequest<?> request) { // Return void
        switch (request.getSettings().getTarget()) {

            case PLAYER ->
                processor.processPlayerRequest(request); // Don't return
            case SERVER ->
                processor.processServerRequest(request); // Don't return
            case TOP ->
                processor.processTopRequest(request); // Don't return
        };
    }

    @Override
    public boolean isExcludedPlayer(String playerName) {
        return offlinePlayerHandler.isExcludedPlayer(playerName);
    }

    @Contract("_ -> new")
    @Override
    public @NotNull
    RequestGenerator<Integer> createPlayerStatRequest(String playerName) {
        return new PlayerStatRequest(playerName);
    }

    @Override
    public @NotNull
    StatResult<Integer> executePlayerStatRequest(@NotNull StatRequest<Integer> request) {
        // This method still expects StatResult, but the processor returns void.
        // The StatManager API needs rethinking or BukkitProcessor needs adapting.
        // Temporary Fix: Process and return a dummy/empty result.
        processor.processPlayerRequest(request);
        // TODO: Adapt API or processor - how should results be retrieved synchronously now?
        return new StatResult<>(0, null, null); // Dummy result
    }

    @Contract(" -> new")
    @Override
    public @NotNull
    RequestGenerator<Long> createServerStatRequest() {
        return new ServerStatRequest();
    }

    @Override
    public @NotNull
    StatResult<Long> executeServerStatRequest(@NotNull StatRequest<Long> request) {
        // This method still expects StatResult, but the processor returns void.
        processor.processServerRequest(request);
        // TODO: Adapt API or processor
        return new StatResult<>(0L, null, null); // Dummy result
    }

    @Contract("_ -> new")
    @Override
    public @NotNull
    RequestGenerator<LinkedHashMap<String, Integer>> createTopStatRequest(int topListSize) {
        return new TopStatRequest(topListSize);

    }

    @Override
    public @NotNull
    RequestGenerator<LinkedHashMap<String, Integer>> createTotalTopStatRequest() {
        int playerCount = offlinePlayerHandler.getIncludedPlayerCount();
        return createTopStatRequest(playerCount);

    }

    @Override
    public @NotNull
    StatResult<LinkedHashMap<String, Integer>> executeTopRequest(@NotNull StatRequest<LinkedHashMap<String, Integer>> request) {
        // This method still expects StatResult, but the processor returns void.
        processor.processTopRequest(request);
        // TODO: Adapt API or processor
        // Ensure correct instantiation of LinkedHashMap for the dummy result
        return new StatResult<>(new LinkedHashMap<String, Integer>(), null, null);
    }
}
