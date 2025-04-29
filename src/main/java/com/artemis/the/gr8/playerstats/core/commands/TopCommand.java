package com.artemis.the.gr8.playerstats.core.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.artemis.the.gr8.playerstats.core.config.ApprovedStat;
import com.artemis.the.gr8.playerstats.core.config.ApprovedStat.StatComponent;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.enums.StandardMessage;
import com.artemis.the.gr8.playerstats.core.msg.OutputManager;
import com.artemis.the.gr8.playerstats.core.multithreading.ThreadManager;
import com.artemis.the.gr8.playerstats.core.statistic.RequestProcessor;
import com.artemis.the.gr8.playerstats.core.statistic.StatRequestManager;
import com.artemis.the.gr8.playerstats.core.statistic.TopStatRequest;
import com.artemis.the.gr8.playerstats.core.utils.EnumHandler;
import com.artemis.the.gr8.playerstats.core.utils.MyLogger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public final class TopCommand implements CommandExecutor {

    private static OutputManager outputManager;
    private static ConfigHandler config;
    private static EnumHandler enumHandler;
    private static final List<String> subcommands = Arrays.asList(
            "distance_travelled", "kills", "ores_mined", "mined", "craft", "play_time"
    );

    // Map Mode String -> List<Statistic>
    private static final Map<String, List<Statistic>> distanceModeStats = new HashMap<>();
    private static final List<Statistic> allDistanceStats;

    static {
        // Initialize distance mode mappings
        distanceModeStats.put("walk", List.of(Statistic.WALK_ONE_CM));
        distanceModeStats.put("sprint", List.of(Statistic.SPRINT_ONE_CM));
        distanceModeStats.put("crouch", List.of(Statistic.CROUCH_ONE_CM));
        distanceModeStats.put("air", List.of(Statistic.FLY_ONE_CM)); // Flying
        distanceModeStats.put("elytra", List.of(Statistic.AVIATE_ONE_CM)); // Elytra
        distanceModeStats.put("water", List.of(Statistic.SWIM_ONE_CM, Statistic.WALK_ON_WATER_ONE_CM, Statistic.WALK_UNDER_WATER_ONE_CM));
        distanceModeStats.put("boat", List.of(Statistic.BOAT_ONE_CM));
        distanceModeStats.put("rail", List.of(Statistic.MINECART_ONE_CM));
        distanceModeStats.put("minecart", List.of(Statistic.MINECART_ONE_CM));
        distanceModeStats.put("horse", List.of(Statistic.HORSE_ONE_CM));
        distanceModeStats.put("pig", List.of(Statistic.PIG_ONE_CM));
        distanceModeStats.put("strider", List.of(Statistic.STRIDER_ONE_CM));
        distanceModeStats.put("climb", List.of(Statistic.CLIMB_ONE_CM));
        // "mount" could aggregate horse, pig, strider?
        distanceModeStats.put("mount", List.of(Statistic.HORSE_ONE_CM, Statistic.PIG_ONE_CM, Statistic.STRIDER_ONE_CM));

        // Aggregate all relevant distance stats for the default case
        List<Statistic> tempAllStats = new ArrayList<>();
        tempAllStats.add(Statistic.WALK_ONE_CM);
        tempAllStats.add(Statistic.CROUCH_ONE_CM);
        tempAllStats.add(Statistic.SPRINT_ONE_CM);
        tempAllStats.add(Statistic.SWIM_ONE_CM);
        tempAllStats.add(Statistic.CLIMB_ONE_CM);
        tempAllStats.add(Statistic.FLY_ONE_CM);
        tempAllStats.add(Statistic.AVIATE_ONE_CM);
        tempAllStats.add(Statistic.BOAT_ONE_CM);
        tempAllStats.add(Statistic.MINECART_ONE_CM);
        tempAllStats.add(Statistic.PIG_ONE_CM);
        tempAllStats.add(Statistic.HORSE_ONE_CM);
        tempAllStats.add(Statistic.STRIDER_ONE_CM);
        tempAllStats.add(Statistic.WALK_ON_WATER_ONE_CM);
        tempAllStats.add(Statistic.WALK_UNDER_WATER_ONE_CM);
        allDistanceStats = List.copyOf(tempAllStats);
    }

    public TopCommand(ThreadManager threadManager) {
        TopCommand.outputManager = OutputManager.getInstance();
        TopCommand.config = ConfigHandler.getInstance();
        TopCommand.enumHandler = EnumHandler.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.COMMAND_PLAYER_ONLY);
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ENGLISH);

        switch (subCommand) {
            case "distance_travelled":
                handleDistanceTravelled(sender, args);
                break;
            case "kills":
                handleKills(sender, args);
                break;
            case "ores_mined":
                handleOresMined(sender, args);
                break;
            case "mined":
                handleMined(sender, args);
                break;
            case "craft":
                handleCraft(sender, args);
                break;
            case "play_time", "playtime":
                handlePlaytime(sender, args);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    // New handler method for /top distance_travelled
    private void handleDistanceTravelled(CommandSender sender, String[] args) {
        // Validate number of arguments for distance command
        if (args.length > 2) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_COMMAND_SYNTAX);
            sender.sendMessage(Component.text("Usage: /top distance_travelled [<mode>]").color(NamedTextColor.RED));
            return;
        }
        List<Statistic> statsToQuery;
        String modeArg = null;
        String displayName;

        if (args.length >= 2) {
            modeArg = args[1].toLowerCase(Locale.ENGLISH);
            statsToQuery = distanceModeStats.get(modeArg);

            if (statsToQuery == null) {
                outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_SUBSTAT_NAME, modeArg); // Assuming an appropriate message exists
                // Maybe send available modes?
                sender.sendMessage(Component.text("Invalid mode: " + modeArg + ". Available modes: " + String.join(", ", distanceModeStats.keySet())).color(NamedTextColor.RED));
                return;
            }
            // Capitalize first letter for display name
            displayName = "Distance Travelled (" + modeArg.substring(0, 1).toUpperCase() + modeArg.substring(1) + ")";
        } else { // No mode specified, get total distance
            statsToQuery = allDistanceStats;
            displayName = "Total Distance Travelled";
        }

        // Create StatComponent list
        List<StatComponent> components = new ArrayList<>();
        for (Statistic stat : statsToQuery) {
            if (stat.getType() == Statistic.Type.UNTYPED) {
                components.add(new StatComponent(stat));
            } else {
                // This shouldn't happen for distance stats, but log if it does
                MyLogger.logWarning("Unexpected type for distance statistic: " + stat.name());
            }
        }

        if (components.isEmpty()) {
            // Should not happen if mappings are correct
            outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR); // Or a more specific message
            return;
        }

        // Create ApprovedStat dynamically
        // Use the modeArg or "total" as a pseudo-alias for clarity, though it's not from config
        String pseudoAlias = (modeArg != null) ? "distance_" + modeArg : "distance_total";
        ApprovedStat approvedStat = new ApprovedStat(pseudoAlias, displayName, components);

        // Create and process the request
        TopStatRequest request = new TopStatRequest(sender, config.getTopListMaxSize());
        request.approvedStat(approvedStat);

        if (!request.isValid()) {
            MyLogger.logWarning("Dynamically created TopStatRequest for distance was invalid!");
            outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
            return;
        }

        // Send calculating message
        // TODO: Check if OutputManager has a better way or add one
        outputManager.sendToCommandSender(sender,
                Component.text("Calculating: ").color(NamedTextColor.GRAY)
                        .append(Component.text(displayName).color(NamedTextColor.GOLD)));

        // Delegate processing
        RequestProcessor requestProcessor = StatRequestManager.getBukkitProcessor();
        requestProcessor.processTopRequest(request);
    }

    private void handleKills(CommandSender sender, String[] args) {
        // Validate argument count and filter type for kills command
        if (args.length > 3
                || (args.length == 2 && !List.of("player", "hostile", "passive", "entity").contains(args[1].toLowerCase(Locale.ENGLISH)))
                || (args.length == 3 && !args[1].equalsIgnoreCase("entity"))) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_COMMAND_SYNTAX);
            sender.sendMessage(Component.text("Usage: /top kills [player|hostile|passive] or /top kills entity <type>").color(NamedTextColor.RED));
            return;
        }
        Statistic statToQuery = null;
        EntityType entityToQuery = null;
        String filterType = null; // "player", "hostile", "passive", "entity" (specific)
        String displayName;

        // Determine statistic and filter type
        if (args.length == 1) {
            // Default: /top kills -> All mob kills + player kills?
            // Let's make default MOB_KILLS (all entities except players)
            statToQuery = Statistic.KILL_ENTITY;
            displayName = "Total Kills (Mobs)";
            filterType = "entity"; // All entities except players implicit
        } else {
            filterType = args[1].toLowerCase(Locale.ENGLISH);
            switch (filterType) {
                case "player":
                    statToQuery = Statistic.PLAYER_KILLS;
                    displayName = "Player Kills";
                    break;
                case "hostile":
                    statToQuery = Statistic.KILL_ENTITY;
                    displayName = "Hostile Mob Kills";
                    // EntityType filtering happens during calculation if needed
                    break;
                case "passive":
                    statToQuery = Statistic.KILL_ENTITY;
                    displayName = "Passive Mob Kills";
                    // EntityType filtering happens during calculation if needed
                    break;
                case "entity":
                    statToQuery = Statistic.KILL_ENTITY;
                    if (args.length >= 3) {
                        entityToQuery = enumHandler.getEntityEnum(args[2]);
                        if (entityToQuery == null) {
                            outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_SUBSTAT_NAME, args[2]);
                            // Maybe suggest entity types?
                            sender.sendMessage(Component.text("Invalid entity type: " + args[2]).color(NamedTextColor.RED));
                            return;
                        }
                        displayName = "Kills (" + entityToQuery.name().toLowerCase().replace('_', ' ') + ")";
                    } else {
                        // /top kills entity -> All mob kills
                        displayName = "Total Kills (Mobs)";
                    }
                    break;
                default:
                    outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_COMMAND_SYNTAX); // Or a more specific message for invalid type
                    sender.sendMessage(Component.text("Invalid kill type: " + filterType + ". Use player, hostile, passive, or entity [type].").color(NamedTextColor.RED));
                    return;
            }
        }

        // Create ApprovedStat component
        StatComponent component;
        boolean isTotalRequest = false;

        if (statToQuery == Statistic.PLAYER_KILLS) {
            component = new StatComponent(statToQuery); // UNTYPED, okay
        } else { // KILL_ENTITY
            if (entityToQuery != null) {
                component = new StatComponent(statToQuery, entityToQuery); // ENTITY type with specific entity
            } else {
                // Total or filtered (hostile/passive) KILL_ENTITY request
                // Create a valid dummy component (e.g., using PLAYER)
                component = new StatComponent(statToQuery, EntityType.PLAYER);
                isTotalRequest = true; // Signal that we want the total (filtered in StatAction)
                MyLogger.logLowLevelMsg("Requesting total/filtered entity kills.");
            }
        }

        // Create ApprovedStat dynamically
        String pseudoAlias;
        if (entityToQuery != null) {
            pseudoAlias = entityToQuery.name().toLowerCase() + "_kills";
        } else {
            switch (filterType) {
                case "player" ->
                    pseudoAlias = "player_kills";
                case "hostile" ->
                    pseudoAlias = "hostile_kills";
                case "passive" ->
                    pseudoAlias = "passive_kills";
                default ->
                    pseudoAlias = "mob_kills";
            }
        }
        ApprovedStat approvedStat;
        if (isTotalRequest) {
            approvedStat = new ApprovedStat(pseudoAlias, displayName, component, true); // Use total request constructor
        } else {
            approvedStat = new ApprovedStat(pseudoAlias, displayName, List.of(component)); // Use standard list constructor
        }

        // Create and process the request
        TopStatRequest request = new TopStatRequest(sender, config.getTopListMaxSize());
        request.approvedStat(approvedStat);
        // *** We need to pass the filterType ("hostile"/"passive") to the calculation somehow ***
        // Options: Add field to ApprovedStat? Add field to StatRequestSettings?
        // Let's modify StatRequestSettings for simplicity for now.
        request.getSettings().setKillFilterType(filterType); // Assumes method exists/will be added

        if (!request.isValid()) {
            MyLogger.logWarning("Dynamically created TopStatRequest for kills was invalid!");
            outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
            return;
        }

        // Send calculating message
        outputManager.sendToCommandSender(sender,
                Component.text("Calculating: ").color(NamedTextColor.GRAY)
                        .append(Component.text(displayName).color(NamedTextColor.GOLD)));

        // Delegate processing
        RequestProcessor requestProcessor = StatRequestManager.getBukkitProcessor();
        requestProcessor.processTopRequest(request);
    }

    private void handleOresMined(CommandSender sender, String[] args) {
        if (args.length != 2) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.MISSING_SUBSTAT_NAME, "ore_type"); // Reusing generic message
            // TODO: List available ore types?
            sender.sendMessage(Component.text("Usage: /top ores_mined <ore_type>").color(NamedTextColor.RED));
            return;
        }

        String oreArg = args[1].toLowerCase(Locale.ENGLISH);
        Material oreMaterial = enumHandler.getBlockEnum(oreArg + "_ore"); // Try appending _ore
        Material deepslateOreMaterial = enumHandler.getBlockEnum("deepslate_" + oreArg + "_ore");

        // Special case for quartz (NETHER_QUARTZ_ORE)
        if (oreArg.equalsIgnoreCase("nether_quartz") || oreArg.equalsIgnoreCase("quartz")) {
            oreMaterial = Material.NETHER_QUARTZ_ORE;
            deepslateOreMaterial = null; // No deepslate variant
        } // Special case for nether gold (NETHER_GOLD_ORE)
        else if (oreArg.equalsIgnoreCase("nether_gold")) {
            oreMaterial = Material.NETHER_GOLD_ORE;
            deepslateOreMaterial = null;
        } // Special case for ancient debris
        else if (oreArg.equalsIgnoreCase("ancient_debris")) {
            oreMaterial = Material.ANCIENT_DEBRIS;
            deepslateOreMaterial = null;
        }

        if (oreMaterial == null) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_SUBSTAT_NAME, oreArg); // Invalid ore
            sender.sendMessage(Component.text("Invalid ore type: " + oreArg).color(NamedTextColor.RED));
            // Suggest valid ore types
            List<String> validOres = enumHandler.getAllBlockNames().stream()
                    .filter(name -> name.toLowerCase(Locale.ENGLISH).endsWith("_ore") || name.equalsIgnoreCase("ancient_debris"))
                    .map(name -> {
                        String lower = name.toLowerCase(Locale.ENGLISH);
                        return lower.equals("ancient_debris") ? lower : lower.substring(0, lower.length() - "_ore".length());
                    })
                    .sorted()
                    .collect(Collectors.toList());
            sender.sendMessage(Component.text("Valid ore types: " + String.join(", ", validOres)).color(NamedTextColor.RED));
            return;
        }

        // Create a user-friendly display name for the ore total
        String friendlyOreName = oreArg.replace('_', ' ');
        friendlyOreName = Arrays.stream(friendlyOreName.split(" "))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
        String displayName = friendlyOreName + " Mined";

        // Create StatComponents for both normal and deepslate (if exists)
        List<StatComponent> components = new ArrayList<>();
        components.add(new StatComponent(Statistic.MINE_BLOCK, oreMaterial)); // BLOCK type
        if (deepslateOreMaterial != null) {
            components.add(new StatComponent(Statistic.MINE_BLOCK, deepslateOreMaterial)); // BLOCK type
        }

        // Create ApprovedStat dynamically
        String pseudoAlias = "ores_mined_" + oreArg;
        ApprovedStat approvedStat = new ApprovedStat(pseudoAlias, displayName, components);

        // Create and process the request
        TopStatRequest request = new TopStatRequest(sender, config.getTopListMaxSize());
        request.approvedStat(approvedStat);

        if (!request.isValid()) {
            MyLogger.logWarning("Dynamically created TopStatRequest for ores_mined was invalid!");
            outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
            return;
        }

        // Send calculating message
        outputManager.sendToCommandSender(sender,
                Component.text("Calculating: ").color(NamedTextColor.GRAY)
                        .append(Component.text(displayName).color(NamedTextColor.GOLD)));

        // Delegate processing
        RequestProcessor requestProcessor = StatRequestManager.getBukkitProcessor();
        requestProcessor.processTopRequest(request);
    }

    private void handleMined(CommandSender sender, String[] args) {
        // Validate no extra arguments for mined command
        if (args.length > 1) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_COMMAND_SYNTAX);
            sender.sendMessage(Component.text("Usage: /top mined").color(NamedTextColor.RED));
            return;
        }
        // Request total blocks mined by looking up a predefined alias, with dynamic fallback
        String alias = "total_blocks_mined";
        ApprovedStat approvedStat = config.getApprovedStat(alias);
        if (approvedStat == null) {
            MyLogger.logWarning("ApprovedStat alias '" + alias + "' not defined in approved-stats.yml, creating dynamic fallback.");
            // Fallback dynamic creation
            StatComponent component = new StatComponent(Statistic.MINE_BLOCK);
            // Mark as total request explicitly
            approvedStat = new ApprovedStat(alias, "Blocks Mined (Total)", component, true);
        }
        String displayName = approvedStat.displayName();

        // Create and process the request
        TopStatRequest request = new TopStatRequest(sender, config.getTopListMaxSize());
        request.approvedStat(approvedStat);

        if (!request.isValid()) {
            MyLogger.logWarning("Configured TopStatRequest for '" + alias + "' was invalid!");
            outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
            return;
        }

        // Send calculating message
        outputManager.sendToCommandSender(sender,
                Component.text("Calculating: ").color(NamedTextColor.GRAY)
                        .append(Component.text(displayName).color(NamedTextColor.GOLD)));

        // Delegate processing
        RequestProcessor requestProcessor = StatRequestManager.getBukkitProcessor();
        requestProcessor.processTopRequest(request);
    }

    private void handleCraft(CommandSender sender, String[] args) {
        // Validate argument count for craft command
        if (args.length > 2) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_COMMAND_SYNTAX);
            sender.sendMessage(Component.text("Usage: /top craft [<item>]").color(NamedTextColor.RED));
            return;
        }

        ApprovedStat approvedStat = null;
        String displayName = null;

        if (args.length >= 2) {
            // Specific item crafting
            String itemArg = args[1].toLowerCase(Locale.ENGLISH);
            Material itemMaterial = enumHandler.getItemEnum(itemArg);
            if (itemMaterial == null) {
                outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_SUBSTAT_NAME, itemArg);
                sender.sendMessage(Component.text("Invalid item name: " + itemArg).color(NamedTextColor.RED));
                return;
            }
            displayName = "Items Crafted (" + itemArg.substring(0, 1).toUpperCase() + itemArg.substring(1).replace('_', ' ') + ")";

            // Create dynamic ApprovedStat for specific item
            StatComponent component = new StatComponent(Statistic.CRAFT_ITEM, itemMaterial, true);
            String pseudoAlias = "craft_" + itemMaterial.name().toLowerCase();
            approvedStat = new ApprovedStat(pseudoAlias, displayName, List.of(component));

        } else {
            // Total items crafted - look up predefined alias
            String requiredAlias = "total_items_crafted"; // Assumes this alias is defined in approved-stats.yml
            approvedStat = config.getApprovedStat(requiredAlias);
            if (approvedStat == null) {
                MyLogger.logWarning("ApprovedStat alias '" + requiredAlias + "' not defined in approved-stats.yml, creating dynamic fallback.");
                // Fallback dynamic creation for total items crafted
                StatComponent component = new StatComponent(Statistic.CRAFT_ITEM);
                // Mark as total request explicitly
                approvedStat = new ApprovedStat(requiredAlias, "Items Crafted (Total)", component, true);
            }
            displayName = approvedStat.displayName(); // Use display name from config
        }

        // Create and process the request
        TopStatRequest request = new TopStatRequest(sender, config.getTopListMaxSize());
        request.approvedStat(approvedStat);

        if (!request.isValid()) {
            MyLogger.logWarning("TopStatRequest for crafting was invalid (Alias: " + approvedStat.alias() + ")!");
            outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
            return;
        }

        // Send calculating message
        outputManager.sendToCommandSender(sender,
                Component.text("Calculating: ").color(NamedTextColor.GRAY)
                        .append(Component.text(displayName).color(NamedTextColor.GOLD)));

        // Delegate processing
        RequestProcessor requestProcessor = StatRequestManager.getBukkitProcessor();
        requestProcessor.processTopRequest(request);
    }

    private void handlePlaytime(CommandSender sender, String[] args) {
        // Validate no extra arguments for play_time command
        if (args.length > 1) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_COMMAND_SYNTAX);
            sender.sendMessage(Component.text("Usage: /top play_time").color(NamedTextColor.RED));
            return;
        }
        // Use config-defined play_time stat if available
        String alias = "play_time";
        ApprovedStat approvedStat = config.getApprovedStat(alias);
        if (approvedStat == null) {
            MyLogger.logWarning("Required ApprovedStat alias '" + alias + "' is not defined in approved-stats.yml!");
            // Fallback dynamic creation
            StatComponent component = new StatComponent(Statistic.PLAY_ONE_MINUTE);
            approvedStat = new ApprovedStat(alias, "Play Time", List.of(component));
        }

        String displayName = approvedStat.displayName();

        // Create and process the request
        TopStatRequest request = new TopStatRequest(sender, config.getTopListMaxSize());
        request.approvedStat(approvedStat);

        if (!request.isValid()) {
            MyLogger.logWarning("Dynamically created TopStatRequest for playtime was invalid!");
            outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
            return;
        }

        // Send calculating message
        outputManager.sendToCommandSender(sender,
                Component.text("Calculating: ").color(NamedTextColor.GRAY)
                        .append(Component.text(displayName).color(NamedTextColor.GOLD)));

        // Delegate processing
        RequestProcessor requestProcessor = StatRequestManager.getBukkitProcessor();
        requestProcessor.processTopRequest(request);
    }

    private void sendHelpMessage(CommandSender sender) {
        TextComponent helpMsg = Component.text("Usage: /top <subcommand> [options...]", NamedTextColor.GOLD)
                .append(Component.newline())
                .append(Component.text("Available subcommands:", NamedTextColor.GRAY))
                .append(Component.newline());

        TextComponent.Builder subcommandListBuilder = Component.text();
        for (int i = 0; i < subcommands.size(); i++) {
            subcommandListBuilder.append(Component.text(subcommands.get(i), NamedTextColor.YELLOW));
            if (i < subcommands.size() - 1) {
                subcommandListBuilder.append(Component.text(", ", NamedTextColor.GRAY));
            }
        }
        helpMsg = helpMsg.append(subcommandListBuilder);

        outputManager.sendToCommandSender(sender, helpMsg);
    }

    // Provide dynamic access to subcommands and modes
    public static List<String> getSubcommands() {
        return subcommands;
    }

    public static Set<String> getDistanceModes() {
        return distanceModeStats.keySet();
    }
}
