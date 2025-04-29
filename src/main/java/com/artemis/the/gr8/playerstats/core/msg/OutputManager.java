package com.artemis.the.gr8.playerstats.core.msg;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstats.api.StatTextFormatter;
import com.artemis.the.gr8.playerstats.core.Main;
import com.artemis.the.gr8.playerstats.core.config.ApprovedStat;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.enums.StandardMessage;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.AVAILABLE_STATS;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.CALCULATING_MSG;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.COMMAND_PLAYER_ONLY;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.EXCLUDE_FAILED;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.INCLUDE_FAILED;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.INTERNAL_ERROR;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.INVALID_COMMAND_SYNTAX;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.INVALID_STAT_MSG;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.INVALID_SUBSTAT_NAME;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.MISSING_PERMISSION;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.MISSING_PLAYER_NAME;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.MISSING_STAT_NAME;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.NO_APPROVED_STATS;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.PLAYER_IS_EXCLUDED;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.PLAYER_NOT_FOUND;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.RELOADED_CONFIG;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.REQUEST_ALREADY_RUNNING;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.RESULTS_ALREADY_SHARED;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.STAT_RESULTS_TOO_OLD;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.STILL_ON_SHARE_COOLDOWN;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.STILL_RELOADING;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.UNKNOWN_ERROR;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.WAIT_A_MINUTE;
import static com.artemis.the.gr8.playerstats.core.enums.StandardMessage.WAIT_A_MOMENT;
import com.artemis.the.gr8.playerstats.core.msg.components.BirthdayComponentFactory;
import com.artemis.the.gr8.playerstats.core.msg.components.BukkitConsoleComponentFactory;
import com.artemis.the.gr8.playerstats.core.msg.components.ComponentFactory;
import com.artemis.the.gr8.playerstats.core.msg.components.ConsoleComponentFactory;
import com.artemis.the.gr8.playerstats.core.msg.components.HalloweenComponentFactory;
import com.artemis.the.gr8.playerstats.core.msg.components.PrideComponentFactory;
import com.artemis.the.gr8.playerstats.core.msg.components.WinterComponentFactory;
import com.artemis.the.gr8.playerstats.core.msg.msgutils.FormattingFunction;
import com.artemis.the.gr8.playerstats.core.utils.Closable;
import com.artemis.the.gr8.playerstats.core.utils.Reloadable;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.TextComponent;

/**
 * This class manages all PlayerStats output. It is the only place where
 * messages are sent. It gets its messages from a {@link MessageBuilder}
 * configured for either a Console or for Players (mainly to deal with the lack
 * of hover-text, and for Bukkit consoles to make up for the lack of
 * hex-colors).
 */
public final class OutputManager implements Reloadable, Closable {

    private static volatile OutputManager instance;
    private static BukkitAudiences adventure;
    private static EnumMap<StandardMessage, Function<MessageBuilder, TextComponent>> standardMessages;

    private final ConfigHandler config;
    private MessageBuilder messageBuilder;
    private MessageBuilder consoleMessageBuilder;

    private OutputManager() {
        adventure = BukkitAudiences.create(Main.getPluginInstance());
        config = ConfigHandler.getInstance();

        getMessageBuilders();
        prepareFunctions();

        Main.registerReloadable(this);
        Main.registerClosable(this);
    }

    public static OutputManager getInstance() {
        OutputManager localVar = instance;
        if (localVar != null) {
            return localVar;
        }

        synchronized (OutputManager.class) {
            if (instance == null) {
                instance = new OutputManager();
            }
            return instance;
        }
    }

    @Override
    public void reload() {
        getMessageBuilders();
    }

    @Override
    public void close() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
    }

    public StatTextFormatter getMainMessageBuilder() {
        return messageBuilder;
    }

    public @NotNull
    String textComponentToString(TextComponent component) {
        return messageBuilder.textComponentToString(component);
    }

    /**
     * @return a TextComponent with the following parts:
     * <br>[player-name]: [number] [stat-name] {sub-stat-name}
     */
    public @NotNull
    FormattingFunction formatPlayerStat(@NotNull StatRequest.Settings requestSettings, int playerStat) {
        return getMessageBuilder(requestSettings.getCommandSender())
                .formattedPlayerStatFunction(playerStat, requestSettings);
    }

    /**
     * @return a TextComponent with the following parts:
     * <br>[Total on] [server-name]: [number] [stat-name] [sub-stat-name]
     */
    public @NotNull
    FormattingFunction formatServerStat(@NotNull StatRequest.Settings requestSettings, long serverStat) {
        return getMessageBuilder(requestSettings.getCommandSender())
                .formattedServerStatFunction(serverStat, requestSettings);
    }

    /**
     * @return a TextComponent with the following parts:
     * <br>[PlayerStats] [Top 10] [stat-name] [sub-stat-name]
     * <br> [1.] [player-name] [number]
     * <br> [2.] [player-name] [number]
     * <br> [3.] etc...
     */
    public @NotNull
    FormattingFunction formatTopStats(@NotNull StatRequest.Settings requestSettings, @NotNull LinkedHashMap<String, Integer> topStats) {
        // Keep original method signature for compatibility with existing /statistic top command
        return formatTopStats(requestSettings, topStats, 0, 0, null);
    }

    /**
     * Overloaded version for the new /top command, including sender rank/value
     * and ApprovedStat.
     */
    public @NotNull
    FormattingFunction formatTopStats(@NotNull StatRequest.Settings requestSettings, @NotNull LinkedHashMap<String, Integer> topStats, int senderRank, int senderStatValue, @Nullable ApprovedStat approvedStat) {
        return getMessageBuilder(requestSettings.getCommandSender())
                .formattedTopStatFunction(topStats, requestSettings, senderRank, senderStatValue, approvedStat);
    }

    public void sendFeedbackMsg(@NotNull CommandSender sender, StandardMessage message) {
        if (message != null) {
            adventure.sender(sender).sendMessage(standardMessages.get(message)
                    .apply(getMessageBuilder(sender)));
        }
    }

    // Overload to handle messages with variable arguments
    public void sendFeedbackMsg(@NotNull CommandSender sender, StandardMessage message, String... args) {
        if (message == null) {
            return;
        }

        MessageBuilder builder = getMessageBuilder(sender);
        TextComponent componentToSend;

        switch (message) {
            case INVALID_SUBSTAT_NAME:
                if (args.length >= 1) {
                    componentToSend = builder.invalidSubStatName(args[0]);
                } else {
                    // Fallback if argument is somehow missing
                    componentToSend = builder.invalidSubStatName("provided name");
                }
                break;
            // Add other cases here if needed in the future
            default:
                // Fallback to the standard message if no special handling is needed
                // or if the enum doesn't match known cases with args
                Function<MessageBuilder, TextComponent> function = standardMessages.get(message);
                if (function != null) {
                    componentToSend = function.apply(builder);
                } else {
                    // Fallback for completely unknown message types
                    componentToSend = builder.unknownError();
                }
                break;
        }
        adventure.sender(sender).sendMessage(componentToSend);
    }

    public void sendFeedbackMsgPlayerExcluded(@NotNull CommandSender sender, String playerName) {
        adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                .excludeSuccess(playerName));
    }

    public void sendFeedbackMsgPlayerIncluded(@NotNull CommandSender sender, String playerName) {
        adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                .includeSuccess(playerName));
    }

    public void sendFeedbackMsgMissingSubStat(@NotNull CommandSender sender, String statType) {
        adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                .missingSubStatName(statType));
    }

    public void sendFeedbackMsgWrongSubStat(@NotNull CommandSender sender, String statType, @Nullable String subStatName) {
        if (subStatName == null) {
            sendFeedbackMsgMissingSubStat(sender, statType);
        } else {
            adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                    .wrongSubStatType(statType, subStatName));
        }
    }

    // New method for more specific wrong sub-stat type feedback
    public void sendFeedbackMsgWrongSubStatType(@NotNull CommandSender sender, String expectedType, @NotNull String actualName, @NotNull String statName) {
        adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                .wrongSubStatType(expectedType, actualName, statName));
    }

    public void sendExamples(@NotNull CommandSender sender) {
        adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                .usageExamples());
    }

    public void sendHelp(@NotNull CommandSender sender) {
        adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                .helpMsg());
    }

    public void sendExcludeInfo(@NotNull CommandSender sender) {
        adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                .excludeInfoMsg());
    }

    public void sendExcludedList(@NotNull CommandSender sender, ArrayList<String> excludedPlayerNames) {
        adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                .excludedList(excludedPlayerNames));
    }

    public void sendToAllPlayers(@NotNull TextComponent component) {
        adventure.players().sendMessage(component);
    }

    public void sendToCommandSender(@NotNull CommandSender sender, @NotNull TextComponent component) {
        adventure.sender(sender).sendMessage(component);
    }

    /**
     * Gets the appropriate MessageBuilder (and underlying ComponentFactory) for
     * the given CommandSender. Needed by BukkitProcessor to pass the correct
     * factory to TopCommandFormatter.
     */
    public @NotNull
    MessageBuilder getMessageBuilderForSender(@NotNull CommandSender sender) {
        return getMessageBuilder(sender);
    }

    private MessageBuilder getMessageBuilder(CommandSender sender) {
        return sender instanceof ConsoleCommandSender ? consoleMessageBuilder : messageBuilder;
    }

    private void getMessageBuilders() {
        messageBuilder = getClientMessageBuilder();
        consoleMessageBuilder = getConsoleMessageBuilder();
    }

    private MessageBuilder getClientMessageBuilder() {
        ComponentFactory festiveFactory = getFestiveFactory();
        if (festiveFactory == null) {
            return MessageBuilder.defaultBuilder();
        }
        return MessageBuilder.fromComponentFactory(festiveFactory);
    }

    private @NotNull
    MessageBuilder getConsoleMessageBuilder() {
        MessageBuilder consoleBuilder;
        if (isBukkit()) {
            consoleBuilder = MessageBuilder.fromComponentFactory(new BukkitConsoleComponentFactory());
        } else {
            consoleBuilder = MessageBuilder.fromComponentFactory(new ConsoleComponentFactory());
        }
        return consoleBuilder;
    }

    private @Nullable
    ComponentFactory getFestiveFactory() {
        if (config.useRainbowMode()) {
            return new PrideComponentFactory();
        } else if (config.useFestiveFormatting()) {
            return switch (LocalDate.now().getMonth()) {
                case JUNE ->
                    new PrideComponentFactory();
                case OCTOBER ->
                    new HalloweenComponentFactory();
                case SEPTEMBER -> {
                    if (LocalDate.now().getDayOfMonth() == 12) {
                        yield new BirthdayComponentFactory();
                    }
                    yield null;
                }
                case DECEMBER ->
                    new WinterComponentFactory();
                default ->
                    null;
            };
        }
        return null;
    }

    private boolean isBukkit() {
        return Bukkit.getName().equalsIgnoreCase("CraftBukkit");
    }

    private void prepareFunctions() {
        standardMessages = new EnumMap<>(StandardMessage.class);

        standardMessages.put(RELOADED_CONFIG, MessageBuilder::reloadedConfig);
        standardMessages.put(STILL_RELOADING, MessageBuilder::stillReloading);
        standardMessages.put(EXCLUDE_FAILED, MessageBuilder::excludeFailed);
        standardMessages.put(INCLUDE_FAILED, MessageBuilder::includeFailed);
        standardMessages.put(MISSING_STAT_NAME, MessageBuilder::missingStatName);
        standardMessages.put(MISSING_PLAYER_NAME, MessageBuilder::missingPlayerName);
        standardMessages.put(PLAYER_IS_EXCLUDED, MessageBuilder::playerIsExcluded);
        standardMessages.put(WAIT_A_MOMENT, MessageBuilder::waitAMoment);
        standardMessages.put(WAIT_A_MINUTE, MessageBuilder::waitAMinute);
        standardMessages.put(REQUEST_ALREADY_RUNNING, MessageBuilder::requestAlreadyRunning);
        standardMessages.put(STILL_ON_SHARE_COOLDOWN, MessageBuilder::stillOnShareCoolDown);
        standardMessages.put(RESULTS_ALREADY_SHARED, MessageBuilder::resultsAlreadyShared);
        standardMessages.put(STAT_RESULTS_TOO_OLD, MessageBuilder::statResultsTooOld);
        standardMessages.put(UNKNOWN_ERROR, MessageBuilder::unknownError);

        // Added mappings for new messages
        standardMessages.put(COMMAND_PLAYER_ONLY, MessageBuilder::commandPlayerOnly);
        standardMessages.put(INVALID_STAT_MSG, MessageBuilder::invalidStatName);
        standardMessages.put(MISSING_PERMISSION, MessageBuilder::missingPermission);
        standardMessages.put(NO_APPROVED_STATS, MessageBuilder::noApprovedStats);
        standardMessages.put(AVAILABLE_STATS, MessageBuilder::availableStats);
        standardMessages.put(PLAYER_NOT_FOUND, MessageBuilder::playerNotFound);
        standardMessages.put(INTERNAL_ERROR, MessageBuilder::internalError);
        standardMessages.put(CALCULATING_MSG, MessageBuilder::calculatingMsg);

        // Added for StatCommand feedback improvements
        standardMessages.put(INVALID_COMMAND_SYNTAX, MessageBuilder::invalidCommandSyntax);
        // INVALID_SUBSTAT_NAME is now handled by the overloaded sendFeedbackMsg method
        // standardMessages.put(INVALID_SUBSTAT_NAME, (mb) -> mb.invalidSubStatName("...")); // Remove this placeholder
    }
}
