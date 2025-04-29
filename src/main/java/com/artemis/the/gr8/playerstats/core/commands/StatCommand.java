package com.artemis.the.gr8.playerstats.core.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.artemis.the.gr8.playerstats.api.RequestGenerator;
import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstats.api.enums.Target;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.enums.StandardMessage;
import com.artemis.the.gr8.playerstats.core.msg.OutputManager;
import com.artemis.the.gr8.playerstats.core.multithreading.ThreadManager;
import com.artemis.the.gr8.playerstats.core.statistic.PlayerStatRequest;
import com.artemis.the.gr8.playerstats.core.statistic.RequestProcessor;
import com.artemis.the.gr8.playerstats.core.statistic.ServerStatRequest;
import com.artemis.the.gr8.playerstats.core.statistic.StatRequestManager;
import com.artemis.the.gr8.playerstats.core.statistic.TopStatRequest;
import com.artemis.the.gr8.playerstats.core.utils.EnumHandler;
import com.artemis.the.gr8.playerstats.core.utils.OfflinePlayerHandler;

public final class StatCommand implements CommandExecutor {

    private static final Pattern pattern = Pattern.compile("top|server|me|player");

    private static OutputManager outputManager;
    private final ConfigHandler config;
    private final EnumHandler enumHandler;
    private final OfflinePlayerHandler offlinePlayerHandler;

    public StatCommand(ThreadManager threadManager) {
        outputManager = OutputManager.getInstance();
        config = ConfigHandler.getInstance();
        enumHandler = EnumHandler.getInstance();
        offlinePlayerHandler = OfflinePlayerHandler.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0
                || args[0].equalsIgnoreCase("help")
                || args[0].equalsIgnoreCase("info")) {
            outputManager.sendHelp(sender);
        } else if (args[0].equalsIgnoreCase("examples")
                || args[0].equalsIgnoreCase("example")) {
            outputManager.sendExamples(sender);
        } else {
            ArgProcessor processor = new ArgProcessor(sender, args);
            if (processor.request != null && processor.request.isValid()) {
                RequestProcessor requestProcessor = StatRequestManager.getBukkitProcessor();
                switch (processor.request.getSettings().getTarget()) {
                    case PLAYER ->
                        requestProcessor.processPlayerRequest(processor.request);
                    case SERVER ->
                        requestProcessor.processServerRequest(processor.request);
                    case TOP ->
                        requestProcessor.processTopRequest(processor.request);
                }
            } else {
                sendFeedback(sender, processor);
            }
        }
        return true;
    }

    /**
     * Analyzes the provided args and sends an appropriate feedback message to
     * the CommandSender that called the stat command. The following is checked:
     * <ul>
     * <li>Is a <code>statistic</code> set?
     * <li>Is a <code>subStatEntry</code> needed, and if so, is a corresponding
     * Material/EntityType present?
     * <li>If the <code>target</code> is Player, is a valid
     * <code>playerName</code> provided?
     * </ul>
     *
     * @param sender the CommandSender to send feedback to
     * @param processor the ArgProcessor object that holds the analyzed args
     */
    private void sendFeedback(CommandSender sender, @NotNull ArgProcessor processor) {
        if (processor.statistic == null) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.MISSING_STAT_NAME);
        } else if (processor.target == Target.PLAYER && processor.playerName == null) {
            // This case should technically be caught earlier if playername is needed,
            // but keeping it as a fallback. Check if player name was expected but not found.
            outputManager.sendFeedbackMsg(sender, StandardMessage.MISSING_PLAYER_NAME);
        } else if (processor.target == Target.PLAYER && processor.playerName != null
                && offlinePlayerHandler.isExcludedPlayer(processor.playerName)
                && !config.allowPlayerLookupsForExcludedPlayers()) {
            // Check for excluded player specifically for Player target
            outputManager.sendFeedbackMsg(sender, StandardMessage.PLAYER_IS_EXCLUDED);
        } else {
            // All other error cases likely involve sub-statistics
            Statistic.Type type = processor.statistic.getType();
            String expectedSubStatTypeName = enumHandler.getSubStatTypeName(type); // e.g., "block", "item", "entity"

            // Case 1: Sub-statistic is required, but no potential argument was found.
            if (type != Statistic.Type.UNTYPED && processor.subStatName == null) {
                outputManager.sendFeedbackMsgMissingSubStat(sender, expectedSubStatTypeName);

                // Case 2: Sub-statistic is required, an argument was found, but it's invalid *for this statistic type*.
                // We infer this because ArgProcessor successfully extracted statistic and target,
                // but combineProcessedArgsIntoRequest() failed to create a valid request,
                // likely due to the enumHandler.getXEnum() check failing.
            } else if (type != Statistic.Type.UNTYPED && processor.subStatName != null) {
                // Check if the provided name is even a recognized sub-stat entry at all
                if (!enumHandler.isSubStatEntry(processor.subStatName)) {
                    outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_SUBSTAT_NAME, processor.subStatName);
                } else {
                    // It's a recognized sub-stat, just not the right type for the statistic
                    outputManager.sendFeedbackMsgWrongSubStatType(sender, expectedSubStatTypeName, processor.subStatName, processor.statistic.toString());
                }
                // Case 3: Fallback for unexpected scenarios or untyped stats with errors (shouldn't happen often)
            } else {
                outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_COMMAND_SYNTAX); // Generic fallback
            }
        }
    }

    private final class ArgProcessor {

        private final CommandSender sender;
        private String[] argsToProcess;

        private Statistic statistic;
        private String subStatName;
        private Target target;
        private String playerName;
        private StatRequest<?> request;

        private ArgProcessor(CommandSender sender, String[] args) {
            this.sender = sender;
            this.argsToProcess = args;

            extractStatistic();
            extractSubStatistic();
            extractTarget();
            combineProcessedArgsIntoRequest();
        }

        private void combineProcessedArgsIntoRequest() {
            if (statistic == null
                    || target == Target.PLAYER && playerName == null) {
                return;
            }

            RequestGenerator<?> requestGenerator
                    = switch (target) {
                case PLAYER ->
                    new PlayerStatRequest(sender, playerName);
                case SERVER ->
                    new ServerStatRequest(sender);
                case TOP ->
                    new TopStatRequest(sender, config.getTopListMaxSize());
            };

            switch (statistic.getType()) {
                case UNTYPED ->
                    request = requestGenerator.untyped(statistic);
                case BLOCK -> {
                    Material block = enumHandler.getBlockEnum(subStatName);
                    if (block != null) {
                        request = requestGenerator.blockOrItemType(statistic, block);
                    }
                }
                case ITEM -> {
                    Material item = enumHandler.getItemEnum(subStatName);
                    if (item != null) {
                        request = requestGenerator.blockOrItemType(statistic, item);
                    }
                }
                case ENTITY -> {
                    EntityType entity = enumHandler.getEntityEnum(subStatName);
                    if (entity != null) {
                        request = requestGenerator.entityType(statistic, entity);
                    }
                }
            }
        }

        private void extractTarget() {
            String targetArg = null;
            for (String arg : argsToProcess) {
                Matcher matcher = pattern.matcher(arg);
                if (matcher.find()) {
                    targetArg = matcher.group();
                    switch (targetArg) {
                        case "me" -> {
                            if (sender instanceof Player) {
                                target = Target.PLAYER;
                                playerName = sender.getName();
                            } else {
                                target = Target.SERVER;
                            }
                        }
                        case "player" -> {
                            target = Target.PLAYER;
                            playerName = tryToFindPlayerName(argsToProcess);
                        }
                        case "server" ->
                            target = Target.SERVER;
                        case "top" ->
                            target = Target.TOP;
                    }
                    argsToProcess = removeArg(targetArg);
                    break;
                }
            }

            if (targetArg == null) {
                String playerName = tryToFindPlayerName(argsToProcess);
                if (playerName != null) {
                    target = Target.PLAYER;
                    this.playerName = playerName;
                } else {
                    target = Target.TOP;
                }
            }
        }

        private void extractStatistic() {
            String statName = null;
            for (String arg : argsToProcess) {
                if (enumHandler.isStatistic(arg)) {
                    statName = arg;
                    break;
                }
            }
            if (statName != null) {
                statistic = enumHandler.getStatEnum(statName);
                argsToProcess = removeArg(statName);
            }
        }

        private void extractSubStatistic() {
            if (statistic == null
                    || statistic.getType() == Statistic.Type.UNTYPED
                    || argsToProcess.length == 0) {
                return;
            }

            String subStatName = null;
            List<String> subStats = Arrays.stream(argsToProcess)
                    .filter(enumHandler::isSubStatEntry)
                    .toList();
            if (subStats.isEmpty()) {
                return;
            } else if (subStats.size() == 1) {
                subStatName = subStats.get(0);
            } else {
                for (String arg : subStats) {
                    if (!arg.equalsIgnoreCase("player")) {
                        subStatName = arg;
                        break;
                    }
                }
                if (subStatName == null) {
                    subStatName = "player";
                }
            }
            this.subStatName = subStatName;
            argsToProcess = removeArg(subStatName);
        }

        @Contract(pure = true)
        private @Nullable
        String tryToFindPlayerName(@NotNull String[] args) {
            for (String arg : args) {
                if (offlinePlayerHandler.isIncludedPlayer(arg) || offlinePlayerHandler.isExcludedPlayer(arg)) {
                    return arg;
                }
            }
            return null;
        }

        private String[] removeArg(String argToRemove) {
            ArrayList<String> currentArgs = new ArrayList<>(Arrays.asList(argsToProcess));
            currentArgs.remove(argToRemove);
            return currentArgs.toArray(String[]::new);
        }
    }
}
