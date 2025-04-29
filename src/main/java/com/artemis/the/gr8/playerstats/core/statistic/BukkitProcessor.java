package com.artemis.the.gr8.playerstats.core.statistic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstats.api.enums.Target;
import com.artemis.the.gr8.playerstats.core.Main;
import com.artemis.the.gr8.playerstats.core.config.ApprovedStat;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.config.StatType;
import com.artemis.the.gr8.playerstats.core.enums.StandardMessage;
import com.artemis.the.gr8.playerstats.core.msg.MessageBuilder;
import com.artemis.the.gr8.playerstats.core.msg.OutputManager;
import com.artemis.the.gr8.playerstats.core.msg.TopCommandFormatter;
import com.artemis.the.gr8.playerstats.core.msg.components.ComponentFactory;
import com.artemis.the.gr8.playerstats.core.msg.msgutils.FormattingFunction;
import com.artemis.the.gr8.playerstats.core.multithreading.ThreadManager;
import com.artemis.the.gr8.playerstats.core.sharing.ShareManager;
import com.artemis.the.gr8.playerstats.core.utils.MyLogger;
import com.artemis.the.gr8.playerstats.core.utils.OfflinePlayerHandler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

final class BukkitProcessor extends RequestProcessor {

    private final OutputManager outputManager;
    private final ShareManager shareManager;
    private final OfflinePlayerHandler offlinePlayerHandler;
    private final ThreadManager threadManager;
    private final ConfigHandler config;

    public BukkitProcessor(OutputManager outputManager) {
        this.outputManager = outputManager;
        this.config = ConfigHandler.getInstance();

        shareManager = ShareManager.getInstance();
        offlinePlayerHandler = OfflinePlayerHandler.getInstance();
        threadManager = new ThreadManager(Main.getPluginInstance());
    }

    @Override
    public void processPlayerRequest(StatRequest<?> playerStatRequest) {
        MyLogger.logLowLevelTask("Processing player stat request...", System.currentTimeMillis());
        CommandSender sender = playerStatRequest.getSettings().getCommandSender();
        int stat = getPlayerStat(playerStatRequest.getSettings());

        if (stat == -1) {
            MyLogger.logLowLevelMsg("getPlayerStat returned -1, aborting processing for player request.");
            MyLogger.actionFinished();
            return;
        }

        FormattingFunction formattingFunction = outputManager.formatPlayerStat(playerStatRequest.getSettings(), stat);

        TextComponent formattedResult = formatAndStoreIfNeeded(sender, formattingFunction);
        outputManager.sendToCommandSender(sender, formattedResult);
        MyLogger.actionFinished();
    }

    @Override
    public void processServerRequest(StatRequest<?> serverStatRequest) {
        MyLogger.logLowLevelTask("Processing server stat request...", System.currentTimeMillis());
        CommandSender sender = serverStatRequest.getSettings().getCommandSender();

        threadManager.startStatCalculation(serverStatRequest, (request, rawResult) -> {
            long totalServerStat = rawResult.values().parallelStream().mapToLong(Integer::longValue).sum();

            FormattingFunction formattingFunction = outputManager.formatServerStat(request.getSettings(), totalServerStat);
            TextComponent formattedResult = formatAndStoreIfNeeded(sender, formattingFunction);

            outputManager.sendToCommandSender(sender, formattedResult);
            MyLogger.actionFinished();
        });
    }

    @Override
    public void processTopRequest(StatRequest<?> topStatRequest) {
        MyLogger.logLowLevelTask("Processing top stat request...", System.currentTimeMillis());
        CommandSender sender = topStatRequest.getSettings().getCommandSender();

        threadManager.startStatCalculation(topStatRequest, (request, rawResult) -> {
            StatRequest.Settings settings = request.getSettings();
            ApprovedStat approvedStat = settings.getApprovedStat();

            if (rawResult == null || rawResult.isEmpty()) {
                MyLogger.logLowLevelMsg("Stat calculation returned empty or null result map.");
                rawResult = new ConcurrentHashMap<>();
            }

            String senderName = sender.getName();
            int senderStatValue = rawResult.getOrDefault(senderName, 0);

            LinkedHashMap<String, Integer> sortedStats = rawResult.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            int senderRank = 0;
            int currentRank = 0;
            for (String playerName : sortedStats.keySet()) {
                currentRank++;
                if (playerName.equalsIgnoreCase(senderName)) {
                    senderRank = currentRank;
                    break;
                }
            }

            LinkedHashMap<String, Integer> topStats = sortedStats.entrySet().stream()
                    .limit(settings.getTopListSize())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            ComponentFactory senderFactory = outputManager.getMessageBuilderForSender(sender).getComponentFactory();
            TextComponent formattedComponent;

            if (approvedStat != null) {
                TopCommandFormatter formatter = new TopCommandFormatter(topStats, senderRank, senderStatValue, approvedStat, settings.getTopListSize(), senderName, senderFactory);
                formattedComponent = formatter.format();
            } else {
                MessageBuilder builder = outputManager.getMessageBuilderForSender(sender);
                Statistic legacyStat = settings.getStatistic();
                String legacySubStatName = settings.getSubStatEntryName();

                if (legacyStat == null) {
                    MyLogger.logWarning("Cannot format legacy top request: Statistic is null in settings!");
                    outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
                    MyLogger.actionFinished();
                    return;
                }

                TextComponent title = builder.getTopStatTitle(settings.getTopListSize(), legacyStat, legacySubStatName);

                TextComponent list = formatLegacyTopList(builder, topStats, legacyStat);

                TextComponent.Builder totalMsg = Component.text();
                if (ConfigHandler.getInstance().useEnters(Target.TOP, false) && !list.children().isEmpty()) {
                    totalMsg.append(Component.newline());
                }
                totalMsg.append(title);

                if (list.children().isEmpty()) {
                    totalMsg.append(Component.newline());
                    totalMsg.append(senderFactory.messageAccent().content("(no results found)"));
                } else {
                    totalMsg.append(list);
                }
                formattedComponent = totalMsg.build();
            }

            FormattingFunction formattingFunction = new FormattingFunction((shareCode, sharer) -> {
                if (shareCode != null) {
                    return formattedComponent.append(Component.space()).append(senderFactory.shareButton(shareCode));
                } else if (sharer != null) {
                    Component sharerNameComponent = senderFactory.sharerName(sharer.getName());
                    return formattedComponent.append(Component.newline()).append(senderFactory.sharedByMessage(sharerNameComponent));
                } else {
                    return formattedComponent;
                }
            });

            TextComponent finalComponentToSend = formatAndStoreIfNeeded(sender, formattingFunction);
            outputManager.sendToCommandSender(sender, finalComponentToSend);
            MyLogger.actionFinished();
        });
    }

    private TextComponent formatLegacyTopList(MessageBuilder builder, LinkedHashMap<String, Integer> topStats, Statistic statistic) {
        TextComponent.Builder listBuilder = Component.text();
        int rank = 0;
        for (Map.Entry<String, Integer> entry : topStats.entrySet()) {
            rank++;
            TextComponent line = builder.formatTopStatLine(rank, entry.getKey(), entry.getValue().longValue(), statistic);
            listBuilder.append(Component.newline()).append(line);
        }
        return listBuilder.build();
    }

    private int getPlayerStat(@NotNull StatRequest.Settings requestSettings) {
        OfflinePlayer player;
        String playerName = requestSettings.getPlayerName();
        CommandSender sender = requestSettings.getCommandSender();

        if (playerName == null) {
            MyLogger.logWarning("Player name is null in getPlayerStat!");
            outputManager.sendFeedbackMsg(sender, StandardMessage.MISSING_PLAYER_NAME);
            return -1;
        }

        if (offlinePlayerHandler.isExcludedPlayer(playerName)
                && !config.allowPlayerLookupsForExcludedPlayers()) {
            MyLogger.logLowLevelMsg("Attempted lookup for excluded player: " + playerName);
            outputManager.sendFeedbackMsg(sender, StandardMessage.PLAYER_IS_EXCLUDED);
            return -1;
        }

        player = offlinePlayerHandler.getIncludedOfflinePlayer(playerName);
        if (player == null) {
            MyLogger.logWarning("Could not find included offline player: " + playerName + " (or they are excluded and lookups are disallowed)");
            outputManager.sendFeedbackMsg(sender, StandardMessage.PLAYER_NOT_FOUND);
            return -1;
        }

        ApprovedStat approvedStat = requestSettings.getApprovedStat();

        if (approvedStat != null) {
            if (approvedStat.getStatType() != StatType.BUKKIT) {
                MyLogger.logWarning("getPlayerStat called with non-BUKKIT ApprovedStat: " + approvedStat.alias() + ". Returning 0.");
                outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
                return -1;
            }

            int totalValue = 0;
            for (ApprovedStat.StatComponent component : approvedStat.getBukkitComponents()) {
                try {
                    totalValue += getStatComponentValue(player, component);
                } catch (IllegalArgumentException e) {
                    MyLogger.logWarning("IllegalArgumentException for stat component '" + component + "' for player " + player.getName() + ": " + e.getMessage());
                    outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
                    return -1;
                } catch (NullPointerException npe) {
                    MyLogger.logLowLevelMsg("NPE caught getting Bukkit stat component '" + component + "' for player " + player.getName() + " in getPlayerStat. Assuming 0.");
                } catch (Exception e) {
                    MyLogger.logWarning("Exception caught getting Bukkit stat component '" + component + "' for player " + player.getName() + " in getPlayerStat. Please check the full stack trace below:");
                    e.printStackTrace();
                    outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
                    return -1;
                }
            }
            return totalValue;
        } else {
            MyLogger.logWarning("Executing getPlayerStat with legacy settings - ApprovedStat was null!");
            if (requestSettings.getStatistic() == null) {
                outputManager.sendFeedbackMsg(sender, StandardMessage.MISSING_STAT_NAME);
                return -1;
            }
            try {
                return switch (requestSettings.getStatistic().getType()) {
                    case UNTYPED ->
                        player.getStatistic(requestSettings.getStatistic());
                    case ENTITY ->
                        player.getStatistic(requestSettings.getStatistic(), requestSettings.getEntity());
                    case BLOCK ->
                        player.getStatistic(requestSettings.getStatistic(), requestSettings.getBlock());
                    case ITEM ->
                        player.getStatistic(requestSettings.getStatistic(), requestSettings.getItem());
                };
            } catch (IllegalArgumentException e) {
                MyLogger.logWarning("IllegalArgumentException getting legacy stat for player " + player.getName() + ": " + e.getMessage());
                outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
                return -1;
            } catch (NullPointerException npe) {
                MyLogger.logLowLevelMsg("NPE caught getting legacy stat for player " + player.getName() + ". Assuming 0.");
                return 0;
            } catch (Exception e) {
                MyLogger.logWarning("Exception caught getting legacy stat for player " + player.getName() + ". Please check the full stack trace below:");
                e.printStackTrace();
                outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
                return -1;
            }
        }
    }

    private int getStatComponentValue(@NotNull OfflinePlayer player, @NotNull ApprovedStat.StatComponent component) throws IllegalArgumentException, NullPointerException {
        return switch (component.type()) {
            case UNTYPED ->
                player.getStatistic(component.statistic());
            case BLOCK ->
                player.getStatistic(component.statistic(), component.material());
            case ITEM ->
                player.getStatistic(component.statistic(), component.material());
            case ENTITY ->
                player.getStatistic(component.statistic(), component.entityType());
        };
    }

    private TextComponent formatAndStoreIfNeeded(CommandSender sender, FormattingFunction formattingFunction) {
        if (outputShouldBeStored(sender)) {
            int shareCode = shareManager.saveStatResult(sender.getName(), formattingFunction.getResultWithSharerName(sender));
            return formattingFunction.getResultWithShareButton(shareCode);
        }
        return formattingFunction.getDefaultResult();
    }

    private boolean outputShouldBeStored(CommandSender sender) {
        return !(sender instanceof ConsoleCommandSender)
                && shareManager.isEnabled()
                && shareManager.senderHasPermission(sender);
    }
}
