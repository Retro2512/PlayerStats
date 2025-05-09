package com.artemis.the.gr8.playerstats.core.multithreading;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstats.core.config.ApprovedStat;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.config.DerivedStatComponent;
import com.artemis.the.gr8.playerstats.core.config.StatType;
import com.artemis.the.gr8.playerstats.core.utils.MyLogger;

/**
 * The action that is executed when a stat-command is triggered.
 */
final class StatAction extends RecursiveTask<ConcurrentHashMap<String, Integer>> {

    private static final long serialVersionUID = 7473931216461982360L;
    private static final int THRESHOLD = 1000; //Number of players processed by one task
    private static final int MAX_RECURSION_DEPTH = 10; // Added: Prevent infinite loops in derived stats

    private final OfflinePlayer[] playerList;
    private final StatRequest.Settings requestSettings;
    private final ConfigHandler configHandler; // Added: Need access to getApprovedStat

    /**
     * @param players an Array of OfflinePlayer objects
     * @param request the StatRequest Settings object with all the relevant
     * settings
     */
    public StatAction(OfflinePlayer[] players, StatRequest.Settings request) {
        MyLogger.actionCreated(players.length);
        playerList = players;
        requestSettings = request;
        this.configHandler = ConfigHandler.getInstance(); // Added: Initialize ConfigHandler
    }

    @Override
    protected ConcurrentHashMap<String, Integer> compute() {
        MyLogger.logLowLevelTask("Computing stats for " + playerList.length + " players...", System.currentTimeMillis());

        if (playerList.length < THRESHOLD) {
            // Decide calculation path based on whether ApprovedStat is present
            ApprovedStat approvedStat = requestSettings.getApprovedStat();
            if (approvedStat != null) {
                // Use newer ApprovedStat logic
                return getStats(playerList, approvedStat);
            } else {
                // Use legacy Statistic/Material/EntityType logic
                return getStatsLegacy(playerList, requestSettings);
            }
        } else {
            int mid = playerList.length / 2;
            OfflinePlayer[] leftList = new OfflinePlayer[mid];
            OfflinePlayer[] rightList = new OfflinePlayer[playerList.length - mid];
            System.arraycopy(playerList, 0, leftList, 0, mid);
            System.arraycopy(playerList, mid, rightList, 0, playerList.length - mid);

            StatAction left = new StatAction(leftList, requestSettings);
            StatAction right = new StatAction(rightList, requestSettings);
            right.fork();

            Map<String, Integer> leftResult = left.compute();
            Map<String, Integer> rightResult = right.join();

            ConcurrentHashMap<String, Integer> totalResult = new ConcurrentHashMap<>(leftResult);
            totalResult.putAll(rightResult);
            MyLogger.logLowLevelTask("Finished calculating stats for " + playerList.length + " players!", System.currentTimeMillis());
            return totalResult;
        }
    }

    /**
     * Gets the statistic data for all players in the provided playerList for
     * the given ApprovedStat.
     */
    private @NotNull
    ConcurrentHashMap<String, Integer> getStats(@NotNull OfflinePlayer[] players, @NotNull ApprovedStat statToCalculate) {
        ConcurrentHashMap<String, Integer> playerStats = new ConcurrentHashMap<>();
        for (OfflinePlayer player : players) {
            if (player != null) {
                // Calculate value using the recursive helper, starting with depth 0 and empty cache
                int statValue = calculatePlayerStatValueRecursive(player, statToCalculate.alias(), new HashMap<>(), 0);
                playerStats.put(player.getName(), statValue);
            }
        }
        return playerStats;
    }

    /**
     * Recursively calculates the value of an ApprovedStat (either BUKKIT or
     * DERIVED) for a specific player. Handles caching and recursion depth
     * limits.
     *
     * @param player The player to calculate the stat for.
     * @param alias The alias of the ApprovedStat to calculate.
     * @param resultCache Cache for intermediate results within this player's
     * calculation.
     * @param currentDepth Current recursion depth (to prevent cycles).
     * @return The calculated statistic value.
     */
    private int calculatePlayerStatValueRecursive(@NotNull OfflinePlayer player, @NotNull String alias, @NotNull Map<String, Integer> resultCache, int currentDepth) {
        // 1. Check cache
        if (resultCache.containsKey(alias)) {
            return resultCache.get(alias);
        }

        // 2. Check recursion depth
        if (currentDepth > MAX_RECURSION_DEPTH) {
            MyLogger.logWarning("Max recursion depth reached while calculating derived stat for alias '" + alias + "' for player " + player.getName() + ". Check for circular dependencies in config! Returning 0.");
            return 0;
        }

        // 3. Get the ApprovedStat definition
        ApprovedStat approvedStat = configHandler.getApprovedStat(alias);
        if (approvedStat == null) {
            MyLogger.logWarning("Could not find ApprovedStat definition for alias '" + alias + "' needed for derived calculation (player: " + player.getName() + "). Returning 0.");
            return 0;
        }

        int calculatedValue = 0;
        // 4. Calculate based on type
        if (approvedStat.getStatType() == StatType.BUKKIT) {
            // --- Calculate BUKKIT type (sum components) ---
            calculatedValue = calculateBukkitStatValue(player, approvedStat);

        } else if (approvedStat.getStatType() == StatType.DERIVED) {
            // --- Calculate DERIVED type (process components) ---
            // Initialize based on the first component/operation might be complex.
            // Let's assume a simple left-to-right evaluation for now.
            // A more robust approach might parse into an expression tree.

            // Start with the value of the *first* component.
            List<DerivedStatComponent> components = approvedStat.getDerivedComponents();
            if (!components.isEmpty()) {
                DerivedStatComponent firstComponent = components.get(0);
                // Initial value is the result of the first component's alias
                calculatedValue = calculatePlayerStatValueRecursive(player, firstComponent.alias(), resultCache, currentDepth + 1);

                // Apply subsequent operations
                for (int i = 1; i < components.size(); i++) {
                    DerivedStatComponent currentComponent = components.get(i);
                    int componentValue = calculatePlayerStatValueRecursive(player, currentComponent.alias(), resultCache, currentDepth + 1);
                    char operation = currentComponent.operation();

                    switch (operation) {
                        case '+':
                            calculatedValue += componentValue;
                            break;
                        case '-':
                            calculatedValue -= componentValue;
                            break;
                        case '*':
                            calculatedValue *= componentValue;
                            break;
                        default: // Should not happen due to config validation
                            MyLogger.logWarning("Invalid operation '" + operation + "' encountered during derived stat calculation for alias '" + alias + "'. Skipping operation.");
                            break;
                    }
                }
            } else {
                MyLogger.logWarning("Derived stat '" + alias + "' has no components defined! Returning 0.");
                calculatedValue = 0; // Should have been caught by ConfigHandler, but safety check
            }
        }

        // 5. Store in cache and return
        resultCache.put(alias, calculatedValue);
        return calculatedValue;
    }

    /**
     * Calculates the value of a BUKKIT ApprovedStat (simple or compound sum)
     * for a single player.
     */
    private int calculateBukkitStatValue(@NotNull OfflinePlayer player, @NotNull ApprovedStat bukkitStat) {
        if (bukkitStat.getStatType() != StatType.BUKKIT) {
            // Should not happen if called correctly
            MyLogger.logWarning("calculateBukkitStatValue called with a non-BUKKIT stat: " + bukkitStat.alias());
            return 0;
        }

        int totalValue = 0;
        for (ApprovedStat.StatComponent component : bukkitStat.getBukkitComponents()) {
            try {
                switch (component.type()) {
                    case UNTYPED:
                        totalValue += player.getStatistic(component.statistic());
                        break;
                    case BLOCK:
                        if (component.material() != null) { // Check if sub-stat is specified
                            totalValue += player.getStatistic(component.statistic(), component.material());
                        } else { // Material is null, assume total for this block statistic is intended (Bukkit might return 0 or error)
                            // Bukkit API for total block stats (like all blocks mined) is usually just the stat enum itself.
                            // Example: MINE_BLOCK (total), not MINE_BLOCK with null Material.
                            // Let's try calling the UNTYPED version if material is null.
                            if (component.statistic().getType() == Statistic.Type.BLOCK) {
                                // totalValue += player.getStatistic(component.statistic()); // This might work for MINE_BLOCK total
                                // Let's log a warning for now, as summing totals requires careful thought.
                                MyLogger.logLowLevelMsg("Calculating total for BLOCK stat '" + component.statistic() + "' in ApprovedStat '" + bukkitStat.alias() + "'. This might not yield expected results for compound sums.");
                                totalValue += player.getStatistic(component.statistic()); // Attempting UNTYPED call
                            } else {
                                MyLogger.logWarning("Unexpected: BLOCK type component has null material but stat isn't Type.BLOCK? Skipping component: " + component + " in " + bukkitStat.alias());
                            }
                        }
                        break;
                    case ITEM:
                        if (component.material() != null) {
                            totalValue += player.getStatistic(component.statistic(), component.material());
                        } else {
                            // Similar logic for ITEM totals
                            if (component.statistic().getType() == Statistic.Type.ITEM) {
                                MyLogger.logLowLevelMsg("Calculating total for ITEM stat '" + component.statistic() + "' in ApprovedStat '" + bukkitStat.alias() + "'. This might not yield expected results for compound sums.");
                                totalValue += player.getStatistic(component.statistic()); // Attempting UNTYPED call
                            } else {
                                MyLogger.logWarning("Unexpected: ITEM type component has null material but stat isn't Type.ITEM? Skipping component: " + component + " in " + bukkitStat.alias());
                            }
                        }
                        break;
                    case ENTITY:
                        if (component.entityType() != null) {
                            totalValue += player.getStatistic(component.statistic(), component.entityType());
                        } else {
                            // Similar logic for ENTITY totals (like MOB_KILLS)
                            if (component.statistic().getType() == Statistic.Type.ENTITY) {
                                MyLogger.logLowLevelMsg("Calculating total for ENTITY stat '" + component.statistic() + "' in ApprovedStat '" + bukkitStat.alias() + "'. This might not yield expected results for compound sums.");
                                totalValue += player.getStatistic(component.statistic()); // Attempting UNTYPED call
                            } else {
                                MyLogger.logWarning("Unexpected: ENTITY type component has null entityType but stat isn't Type.ENTITY? Skipping component: " + component + " in " + bukkitStat.alias());
                            }
                        }
                        break;
                }
            } catch (NullPointerException npe) {
                // Catch potential NPE if Bukkit returns null for a player/stat combo (e.g., stat doesn't exist for player)
                MyLogger.logLowLevelMsg("NPE caught getting stat component '" + component + "' for player " + player.getName() + " in ApprovedStat '" + bukkitStat.alias() + "'. Assuming 0 for this component.");
                // totalValue remains unchanged (effectively adding 0)
            } catch (Exception e) {
                // Catch other potential exceptions during Bukkit call
                MyLogger.logWarning("Exception caught getting stat component '" + component + "' for player " + player.getName() + " in ApprovedStat '" + bukkitStat.alias() + "': " + e.getMessage());
                // totalValue remains unchanged
            }
        }
        return totalValue;
    }

    /**
     * Gets the combined value for a potentially compound ApprovedStat for a
     * single player.
     *
     * @deprecated Replaced by calculatePlayerStatValueRecursive and
     * calculateBukkitStatValue
     */
    @Deprecated
    private int getStatValue(@NotNull OfflinePlayer player, @NotNull ApprovedStat approvedStat) {
        int totalValue = 0;
        for (ApprovedStat.StatComponent component : approvedStat.getBukkitComponents()) {
            switch (component.type()) {
                case UNTYPED ->
                    totalValue += player.getStatistic(component.statistic());
                case BLOCK -> {
                    if (component.material() != null) {
                        totalValue += player.getStatistic(component.statistic(), component.material());
                    } else {
                        // Handle case where BLOCK type needs total sum (if supported in future)
                        MyLogger.logWarning("Attempted to get total BLOCK stat for component in ApprovedStat, which is not yet fully supported for compound stats. Skipping component: " + component);
                    }
                }
                case ITEM -> {
                    if (component.material() != null) {
                        totalValue += player.getStatistic(component.statistic(), component.material());
                    } else {
                        // Handle case where ITEM type needs total sum (if supported in future)
                        MyLogger.logWarning("Attempted to get total ITEM stat for component in ApprovedStat, which is not yet fully supported for compound stats. Skipping component: " + component);
                    }
                }
                case ENTITY -> {
                    if (component.entityType() != null) {
                        totalValue += player.getStatistic(component.statistic(), component.entityType());
                    } else {
                        MyLogger.logWarning("Attempted to get ENTITY stat component without an EntityType. Skipping component: " + component);
                    }
                }
            }
        }
        return totalValue;
    }

    /**
     * Legacy method to get stat value based on individual fields in Settings.
     * Used as a fallback for requests not using ApprovedStat (e.g., original
     * /statistic command).
     */
    @Deprecated
    private int getStatValue_Legacy(@NotNull OfflinePlayer player, @NotNull StatRequest.Settings settings) {
        try {
            if (settings.getStatistic() == null) {
                return 0;
            }
            return switch (settings.getStatistic().getType()) {
                case UNTYPED ->
                    player.getStatistic(settings.getStatistic());
                case ENTITY ->
                    settings.getEntity() == null ? player.getStatistic(settings.getStatistic()) : player.getStatistic(settings.getStatistic(), settings.getEntity());
                case BLOCK ->
                    settings.getBlock() == null ? player.getStatistic(settings.getStatistic()) : player.getStatistic(settings.getStatistic(), settings.getBlock());
                case ITEM ->
                    settings.getItem() == null ? player.getStatistic(settings.getStatistic()) : player.getStatistic(settings.getStatistic(), settings.getItem());
            };
        } catch (NullPointerException npe) {
            MyLogger.logLowLevelMsg("NPE caught in legacy stat calculation for player " + player.getName() + ". Assuming 0.");
            return 0;
        } catch (Exception e) {
            MyLogger.logWarning("Exception caught in legacy stat calculation for player " + player.getName() + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Calculates the stats for a list of players using the legacy
     * StatRequest.Settings (Statistic, Material, EntityType).
     *
     * @param players The array of players to process.
     * @param settings The legacy request settings.
     * @return A map of player names to their calculated statistic values.
     */
    private @NotNull
    ConcurrentHashMap<String, Integer> getStatsLegacy(@NotNull OfflinePlayer[] players, @NotNull StatRequest.Settings settings) {
        ConcurrentHashMap<String, Integer> playerStats = new ConcurrentHashMap<>();
        Statistic statistic = settings.getStatistic();
        Material block = settings.getBlock();
        Material item = settings.getItem();
        EntityType entity = settings.getEntity();

        if (statistic == null) {
            MyLogger.logWarning("Cannot calculate legacy stats: Statistic is null in settings!");
            return playerStats; // Return empty map
        }

        for (OfflinePlayer player : players) {
            if (player != null) {
                int statValue = 0;
                try {
                    statValue = switch (statistic.getType()) {
                        case UNTYPED ->
                            player.getStatistic(statistic);
                        case BLOCK ->
                            (block != null) ? player.getStatistic(statistic, block) : 0; // Default to 0 if block somehow null
                        case ITEM ->
                            (item != null) ? player.getStatistic(statistic, item) : 0; // Default to 0 if item somehow null
                        case ENTITY ->
                            (entity != null) ? player.getStatistic(statistic, entity) : 0; // Default to 0 if entity somehow null
                    };
                } catch (NullPointerException npe) {
                    // Stat doesn't exist for player yet, default to 0
                    MyLogger.logLowLevelMsg("NPE caught getting legacy stat '" + statistic.name() + "' for player " + player.getName() + ". Assuming 0.");
                    statValue = 0;
                } catch (IllegalArgumentException iae) {
                    // Should not happen if StatCommand validation is correct, but log just in case
                    MyLogger.logWarning("IllegalArgumentException caught getting legacy stat '" + statistic.name() + "' for player " + player.getName() + ": " + iae.getMessage());
                    statValue = 0; // Treat as 0 if invalid combo occurs
                } catch (Exception e) {
                    MyLogger.logWarning("Unexpected Exception caught getting legacy stat '" + statistic.name() + "' for player " + player.getName() + ": " + e.getMessage());
                    statValue = 0; // Treat as 0 on other errors
                }
                playerStats.put(player.getName(), statValue);
            }
        }
        return playerStats;
    }
}
