package com.artemis.the.gr8.playerstats.core.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.utils.EnumHandler;
import com.artemis.the.gr8.playerstats.core.utils.OfflinePlayerHandler;

public final class TabCompleter implements org.bukkit.command.TabCompleter {

    private final OfflinePlayerHandler offlinePlayerHandler;
    private final EnumHandler enumHandler;
    private final ConfigHandler configHandler;

    private static final List<String> topSubcommands = TopCommand.getSubcommands();
    private static final List<String> distanceModes = TopCommand.getDistanceModes().stream().sorted().toList();
    private static final List<String> killTypes = Arrays.asList("entity", "player", "hostile", "passive");
    private List<String> oreTypes;
    private List<String> craftableItems;
    private List<String> entityTypes;

    private List<String> statCommandTargets;
    private List<String> excludeCommandOptions;

    public TabCompleter() {
        offlinePlayerHandler = OfflinePlayerHandler.getInstance();
        enumHandler = EnumHandler.getInstance();
        configHandler = ConfigHandler.getInstance();
        prepareLists();
    }

    @Override
    public @Nullable
    List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase(Locale.ENGLISH);

        return switch (cmdName) {
            case "statistic" ->
                getStatCommandSuggestions(args);
            case "statisticexclude" ->
                getExcludeCommandSuggestions(args);
            case "top" ->
                getTopCommandSuggestions(args);
            default ->
                null;
        };
    }

    private @Nullable
    List<String> getTopCommandSuggestions(@NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        String currentArg = args[args.length - 1].toLowerCase(Locale.ENGLISH);

        if (args.length == 1) {
            suggestions = topSubcommands;
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase(Locale.ENGLISH);
            switch (subCommand) {
                case "distance_travelled":
                    suggestions = distanceModes;
                    break;
                case "kills":
                    suggestions = killTypes;
                    break;
                case "ores_mined":
                    suggestions = oreTypes;
                    break;
                case "craft":
                    suggestions = craftableItems;
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase(Locale.ENGLISH);
            String killType = args[1].toLowerCase(Locale.ENGLISH);
            if (subCommand.equals("kills") && killType.equals("entity")) {
                suggestions = entityTypes;
            }
        }

        return getDynamicTabSuggestions(suggestions, currentArg);
    }

    private @Nullable
    List<String> getExcludeCommandSuggestions(@NotNull String[] args) {
        if (args.length == 0) {
            return null;
        }

        List<String> tabSuggestions = new ArrayList<>();
        if (args.length == 1) {
            tabSuggestions = excludeCommandOptions;
        } else if (args.length == 2) {
            tabSuggestions = switch (args[0]) {
                case "add" ->
                    offlinePlayerHandler.getIncludedOfflinePlayerNames();
                case "remove" ->
                    offlinePlayerHandler.getExcludedPlayerNames();
                default ->
                    tabSuggestions;
            };
        }
        return getDynamicTabSuggestions(tabSuggestions, args[args.length - 1]);
    }

    private @Nullable
    List<String> getStatCommandSuggestions(@NotNull String[] args) {
        if (args.length == 0) {
            return null;
        }

        List<String> tabSuggestions = new ArrayList<>();
        if (args.length == 1) {
            tabSuggestions = firstStatCommandArgSuggestions();
        } else {
            String previousArg = args[args.length - 2];

            if (enumHandler.isStatistic(previousArg)) {
                Statistic stat = enumHandler.getStatEnum(previousArg);
                if (stat != null) {
                    tabSuggestions = suggestionsAfterFirstStatCommandArg(stat);
                }
            } else if (previousArg.equalsIgnoreCase("player")) {
                if (args.length >= 3 && enumHandler.isEntityStatistic(args[args.length - 3])) {
                    tabSuggestions = statCommandTargets;
                } else {
                    tabSuggestions = offlinePlayerHandler.getIncludedOfflinePlayerNames();
                }
            } else if (enumHandler.isSubStatEntry(previousArg)) {
                tabSuggestions = statCommandTargets;
            }
        }
        return getDynamicTabSuggestions(tabSuggestions, args[args.length - 1]);
    }

    private List<String> getDynamicTabSuggestions(@NotNull List<String> completeList, String currentArg) {
        return completeList.stream()
                .filter(item -> item.toLowerCase(Locale.ENGLISH).contains(currentArg.toLowerCase(Locale.ENGLISH)))
                .collect(Collectors.toList());
    }

    private @NotNull
    List<String> firstStatCommandArgSuggestions() {
        List<String> suggestions = enumHandler.getAllStatNames();
        suggestions.add("examples");
        suggestions.add("info");
        suggestions.add("help");
        return suggestions;
    }

    private List<String> suggestionsAfterFirstStatCommandArg(@NotNull Statistic stat) {
        switch (stat.getType()) {
            case BLOCK -> {
                return enumHandler.getAllBlockNames();
            }
            case ITEM -> {
                if (stat == Statistic.BREAK_ITEM) {
                    return enumHandler.getAllItemsThatCanBreak();
                } else {
                    return enumHandler.getAllItemNames();
                }
            }
            case ENTITY -> {
                return enumHandler.getAllEntitiesThatCanDie();
            }
            default -> {
                return statCommandTargets;
            }
        }
    }

    private void prepareLists() {
        statCommandTargets = List.of("top", "player", "server", "me");
        excludeCommandOptions = List.of("add", "list", "remove", "info");

        craftableItems = enumHandler.getItemNames();
        entityTypes = enumHandler.getEntityNames();

        // Generate ore types dynamically from all registered block names ending with '_ore' or special-case ancient_debris
        oreTypes = enumHandler.getAllBlockNames().stream()
                .filter(name -> name.toLowerCase(Locale.ENGLISH).endsWith("_ore")
                || name.equalsIgnoreCase("ancient_debris"))
                .map(name -> {
                    String lower = name.toLowerCase(Locale.ENGLISH);
                    if (lower.equals("ancient_debris")) {
                        return lower;
                    }
                    // Remove '_ore' suffix
                    return lower.substring(0, lower.length() - "_ore".length());
                })
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
