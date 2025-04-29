package com.artemis.the.gr8.playerstats.core.commands;

import com.artemis.the.gr8.playerstats.core.config.ApprovedStat;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.enums.StandardMessage;
import com.artemis.the.gr8.playerstats.core.msg.OutputManager;
import com.artemis.the.gr8.playerstats.core.utils.EnumHandler;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class StatAdminCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "playerstats.admin";
    private static OutputManager outputManager;
    private static ConfigHandler config;
    private static EnumHandler enumHandler;

    public StatAdminCommand() {
        StatAdminCommand.outputManager = OutputManager.getInstance();
        StatAdminCommand.config = ConfigHandler.getInstance();
        StatAdminCommand.enumHandler = EnumHandler.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.MISSING_PERMISSION);
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "addapproved" ->
                handleAddApproved(sender, subArgs);
            case "removeapproved" ->
                handleRemoveApproved(sender, subArgs);
            case "listapproved" ->
                handleListApproved(sender);
            default ->
                sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        // Replace with proper message construction later
        sender.sendMessage("PlayerStats Admin Help:");
        sender.sendMessage("/statadmin addapproved <alias> <display_name> <statistic> [sub_statistic]");
        sender.sendMessage("/statadmin removeapproved <alias>");
        sender.sendMessage("/statadmin listapproved");
    }

    private void handleAddApproved(CommandSender sender, String[] args) {
        // Usage: <alias> <"Display Name"> <StatisticName> [SubStatName]
        // Require quotes around display name
        if (args.length < 3 || !args[1].startsWith("\"")) {
            sender.sendMessage("Usage: /statadmin addapproved <alias> \"<Display Name>\" <StatisticName> [SubStatName]");
            sender.sendMessage("Note: Display Name must be enclosed in double quotes.");
            return;
        }

        String alias = args[0].toLowerCase();

        // Extract display name from quotes
        StringBuilder displayNameBuilder = new StringBuilder();
        int i = 1;
        boolean foundEndQuote = false;
        while (i < args.length) {
            String part = args[i];
            if (i == 1) {
                part = part.substring(1); // Remove leading quote
            }
            if (part.endsWith("\"")) {
                displayNameBuilder.append(part.substring(0, part.length() - 1));
                foundEndQuote = true;
                i++; // Move index past the display name args
                break;
            } else {
                displayNameBuilder.append(part).append(" ");
            }
            i++;
        }

        if (!foundEndQuote) {
            sender.sendMessage("Usage: /statadmin addapproved <alias> \"<Display Name>\" <StatisticName> [SubStatName]");
            sender.sendMessage("Error: Display Name must end with a double quote.");
            return;
        }
        String displayName = displayNameBuilder.toString().trim();

        // Check if there are enough remaining args for stat and optional sub-stat
        if (args.length <= i) {
            sender.sendMessage("Usage: /statadmin addapproved <alias> \"<Display Name>\" <StatisticName> [SubStatName]");
            sender.sendMessage("Error: Missing StatisticName.");
            return;
        }

        String statName = args[i];
        String subStatName = (args.length > i + 1) ? args[i + 1] : null;

        // Validation
        if (!alias.matches("[a-z0-9_]+")) {
            sender.sendMessage("Invalid alias. Use lowercase letters, numbers, and underscores only.");
            return;
        }

        Statistic statistic = enumHandler.getStatEnum(statName);
        if (statistic == null) {
            sender.sendMessage("Invalid Statistic name: " + statName);
            sender.sendMessage("See: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Statistic.html"); // Link might change
            return;
        }

        Statistic.Type type = statistic.getType();
        Object subStatEnum = null;
        Material material = null;
        EntityType entityType = null;

        if (type != Statistic.Type.UNTYPED) {
            if (subStatName == null) {
                sender.sendMessage("Statistic type " + type + " requires a sub-statistic (Material or EntityType). Example: STONE or ZOMBIE");
                return;
                // Alternative: Allow null sub-stat to mean "total" like in defaults?
                // For admin command, let's be explicit for now.
                // sender.sendMessage("Warning: No sub-statistic provided for type " + type + ". Assuming total count.");
                // subStatEnum = null;
            } else {
                switch (type) {
                    case BLOCK -> {
                        material = enumHandler.getBlockEnum(subStatName);
                        if (material == null) {
                            sender.sendMessage("Invalid Material (for BLOCK type): " + subStatName);
                            return;
                        }
                        subStatEnum = material;
                    }
                    case ITEM -> {
                        material = enumHandler.getItemEnum(subStatName);
                        if (material == null) {
                            sender.sendMessage("Invalid Material (for ITEM type): " + subStatName);
                            return;
                        }
                        subStatEnum = material;
                    }
                    case ENTITY -> {
                        entityType = enumHandler.getEntityEnum(subStatName);
                        if (entityType == null) {
                            sender.sendMessage("Invalid EntityType: " + subStatName);
                            return;
                        }
                        subStatEnum = entityType;
                    }
                }
            }
        } else if (subStatName != null) {
            sender.sendMessage("Statistic type UNTYPED does not accept a sub-statistic ('" + subStatName + "' provided).");
            return;
        }

        // Save to config
        boolean saved = config.addApprovedStat(alias, displayName, statistic, type, subStatEnum);

        if (saved) {
            sender.sendMessage("Successfully added/updated approved stat '" + alias + "'!");
        } else {
            sender.sendMessage("Error saving configuration. Check server console for details.");
        }
    }

    private void handleRemoveApproved(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Usage: /statadmin removeapproved <alias>");
            return;
        }
        String alias = args[0].toLowerCase();

        if (config.getApprovedStat(alias) == null) {
            sender.sendMessage("Approved stat with alias '" + alias + "' not found.");
            return;
        }

        boolean removed = config.removeApprovedStat(alias);
        if (removed) {
            sender.sendMessage("Successfully removed approved stat '" + alias + "'!");
        } else {
            sender.sendMessage("Error removing approved stat from configuration. Check server console.");
        }
    }

    private void handleListApproved(CommandSender sender) {
        Set<String> aliases = config.getApprovedAliases();
        if (aliases.isEmpty()) {
            sender.sendMessage("There are no approved stats configured.");
            return;
        }

        sender.sendMessage("Approved Stats (/top): [" + aliases.size() + "]");
        for (String alias : aliases) {
            ApprovedStat stat = config.getApprovedStat(alias);
            if (stat != null) {
                String sub = stat.material() != null ? " (" + stat.material().name() + ")"
                        : stat.entityType() != null ? " (" + stat.entityType().name() + ")" : "";
                sender.sendMessage("- " + alias + ": \"" + stat.displayName() + "\" [" + stat.statistic().name() + sub + "]");
            } else {
                sender.sendMessage("- " + alias + ": [Error retrieving details]"); // Should not happen if alias list is correct
            }
        }
    }

    @Override
    public @Nullable
    List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();
        String currentArg = args[args.length - 1].toLowerCase();

        if (args.length == 1) { // Subcommand
            completions.addAll(List.of("addapproved", "removeapproved", "listapproved"));
        } else if (args.length > 1) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("removeapproved") && args.length == 2) { // Alias to remove
                completions.addAll(config.getApprovedAliases());
            } // Tab complete for addapproved: <alias> <"Display Name"> <StatisticName> [SubStatName]
            else if (subCommand.equals("addapproved")) {
                if (args.length == 4) { // StatisticName
                    completions.addAll(enumHandler.getStatNames());
                } else if (args.length == 5) { // SubStatName (optional)
                    String statName = args[3];
                    Statistic statistic = enumHandler.getStatEnum(statName);
                    if (statistic != null) {
                        Statistic.Type type = statistic.getType();
                        switch (type) {
                            case BLOCK ->
                                completions.addAll(enumHandler.getBlockNames());
                            case ITEM ->
                                completions.addAll(enumHandler.getItemNames());
                            case ENTITY ->
                                completions.addAll(enumHandler.getEntityNames());
                        }
                    }
                }
            }
        }

        // Filter based on current input
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .collect(Collectors.toList());
    }
}
