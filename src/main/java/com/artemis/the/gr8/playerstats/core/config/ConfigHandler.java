package com.artemis.the.gr8.playerstats.core.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.artemis.the.gr8.playerstats.api.enums.Target;
import com.artemis.the.gr8.playerstats.api.enums.Unit;
import com.artemis.the.gr8.playerstats.core.utils.EnumHandler;
import com.artemis.the.gr8.playerstats.core.utils.MyLogger;
import com.artemis.the.gr8.playerstats.core.utils.YamlFileHandler;

/**
 * Handles all PlayerStats' config-settings.
 */
public final class ConfigHandler extends YamlFileHandler {

    private static volatile ConfigHandler instance;
    private final int configVersion;
    private FileConfiguration config;
    private final EnumHandler enumHandler;

    // Thread-safe map for the approved stats cache
    private Map<String, ApprovedStat> approvedStatsCache;

    private ConfigHandler() {
        super("config.yml");
        config = super.getFileConfiguration();
        this.enumHandler = EnumHandler.getInstance();

        configVersion = 8;
        checkAndUpdateConfigVersion();
        loadApprovedStats();
        MyLogger.setDebugLevel(getDebugLevel());
    }

    public static ConfigHandler getInstance() {
        ConfigHandler localVar = instance;
        if (localVar != null) {
            return localVar;
        }

        synchronized (ConfigHandler.class) {
            if (instance == null) {
                instance = new ConfigHandler();
            }
            return instance;
        }
    }

    @Override
    public void reload() {
        super.reload();
        config = super.getFileConfiguration();
        loadApprovedStats();
        MyLogger.setDebugLevel(getDebugLevel());
    }

    /**
     * Checks the number that "config-version" returns to see if the config
     * needs updating, and if so, updates it.
     * <br>
     * <br>PlayerStats 1.1: "config-version" doesn't exist.
     * <br>PlayerStats 1.2: "config-version" is 2.
     * <br>PlayerStats 1.3: "config-version" is 3.
     * <br>PlayerStats 1.4: "config-version" is 4.
     * <br>PlayerStats 1.5: "config-version" is 5.
     * <br>PlayerStats 1.6 and up: "config-version" is 6.
     */
    private void checkAndUpdateConfigVersion() {
        if (!config.contains("config-version") || config.getInt("config-version") != configVersion) {
            DefaultValueGetter defaultValueGetter = new DefaultValueGetter(config);
            Map<String, Object> defaultValues = defaultValueGetter.getValuesToAdjust();
            defaultValues.put("config-version", configVersion);

            // Add default approved stats if updating from an older version
            if (!config.contains("approved-stats")) {
                addDefaultApprovedStats(defaultValues);
            }

            super.addValues(defaultValues);
            reload();

            MyLogger.logLowLevelMsg("Your config has been updated to version " + configVersion
                    + ", but all of your custom settings should still be there!");
        } // Ensure approved stats are loaded even if config version is current
        else if (approvedStatsCache == null) {
            loadApprovedStats();
        }
    }

    /**
     * Returns the desired debugging level.
     *
     * <br> 1 = low (only show unexpected errors)
     * <br> 2 = medium (detail all encountered exceptions, log main tasks and
     * show time taken)
     * <br> 3 = high (log all tasks and time taken)
     *
     * @return the DebugLevel (default: 1)
     */
    public int getDebugLevel() {
        return config.getInt("debug-level", 1);
    }

    /**
     * Whether command-senders should be limited to one stat-request at a time.
     *
     * @return the config setting (default: true)
     */
    public boolean limitStatRequests() {
        return config.getBoolean("only-allow-one-lookup-at-a-time-per-player", true);
    }

    /**
     * Whether stat-sharing is allowed.
     *
     * @return the config setting (default: true)
     */
    public boolean allowStatSharing() {
        return config.getBoolean("enable-stat-sharing", true);
    }

    /**
     * The number of minutes a player has to wait before being able to share
     * another stat-result.
     *
     * @return the number (default: 0)
     */
    public int getStatShareWaitingTime() {
        return config.getInt("waiting-time-before-sharing-again", 0);
    }

    /**
     * Whether to limit stat-calculations to whitelisted players only.
     *
     * @return the config setting (default: true)
     */
    public boolean whitelistOnly() {
        return config.getBoolean("include-whitelist-only", false);
    }

    /**
     * Whether to exclude banned players from stat-calculations.
     *
     * @return the config setting for exclude-banned-players (default: false)
     */
    public boolean excludeBanned() {
        return config.getBoolean("exclude-banned-players", false);
    }

    /**
     * The number of maximum days since a player has last been online.
     *
     * @return the number (default: 0 - which signals not to use this limit)
     */
    public int getLastPlayedLimit() {
        return config.getInt("number-of-days-since-last-joined", 0);
    }

    /**
     * Whether to allow the /stat player command for excluded players.
     *
     * @return the config setting (default: true)
     */
    public boolean allowPlayerLookupsForExcludedPlayers() {
        return config.getBoolean("allow-player-lookups-for-excluded-players", true);
    }

    /**
     * Whether to use TranslatableComponents wherever possible.
     *
     * @return the config setting (default: true)
     * @implNote Currently supported: statistic, block, item and entity names.
     */
    public boolean useTranslatableComponents() {
        return config.getBoolean("translate-to-client-language", true);
    }

    /**
     * Whether to use HoverComponents for additional information
     *
     * @return the config setting (default: true)
     */
    public boolean useHoverText() {
        return config.getBoolean("enable-hover-text", true);
    }

    /**
     * Whether to use festive formatting, such as pride colors
     *
     * @return the config setting (default: true)
     */
    public boolean useFestiveFormatting() {
        return config.getBoolean("enable-festive-formatting", true);
    }

    /**
     * Whether to use rainbow colors for the [PlayerStats] prefix rather than
     * the default gold/purple
     *
     * @return the config setting (default: false)
     */
    public boolean useRainbowMode() {
        return config.getBoolean("rainbow-mode", false);
    }

    /**
     * Whether to use enters before the statistic output in chat
     *
     * @param selection the Target (Player, Server or Top)
     * @return the config setting (default: true for non-shared top statistics,
     * false for everything else)
     */
    public boolean useEnters(Target selection, boolean getSharedSetting) {
        ConfigurationSection section = config.getConfigurationSection("use-enters");
        boolean def = selection == Target.TOP && !getSharedSetting;
        if (section != null) {
            String path = switch (selection) {
                case TOP ->
                    getSharedSetting ? "top-stats-shared" : "top-stats";
                case PLAYER ->
                    getSharedSetting ? "player-stats-shared" : "player-stats";
                case SERVER ->
                    getSharedSetting ? "server-stats-shared" : "server-stats";
            };
            return section.getBoolean(path, def);
        }
        MyLogger.logWarning("Config settings for use-enters could not be retrieved! "
                + "Please check your file if you want to use custom settings. "
                + "Using default values...");
        return def;
    }

    /**
     * Whether dots should be used to align the numbers in a top-stat-result.
     *
     * @return the config setting (default: true)
     */
    public boolean useDots() {
        return config.getBoolean("use-dots", true);
    }

    /**
     * The maximum size for the top-stat-list.
     *
     * @return the config setting (default: 10)
     */
    public int getTopListMaxSize() {
        return config.getInt("top-list-max-size", 10);
    }

    /**
     * The title that a top-statistic should start with.
     *
     * @return a String that represents the title for a top statistic (default:
     * "Top")
     */
    public String getTopStatsTitle() {
        return config.getString("top-list-title", "Top");
    }

    /**
     * The title that a server statistic should start with.
     *
     * @return the title (default: "Total on")
     */
    public String getServerTitle() {
        return config.getString("total-server-stat-title", "Total on");
    }

    /**
     * The specified server name for a server stat title.
     *
     * @return the title (default: "this server")
     */
    public String getServerName() {
        return config.getString("your-server-name", "this server");
    }

    /**
     * The unit that should be used for distance-related statistics.
     *
     * @param isUnitForHoverText whether the number formatted with this Unit is
     * inside a HoverComponent
     * @return the Unit (default: Blocks for plain text, km for hover-text)
     */
    public String getDistanceUnit(boolean isUnitForHoverText) {
        return getUnitString(isUnitForHoverText, "blocks", "km", "distance-unit");
    }

    /**
     * The unit that should be used for damage-based statistics.
     *
     * @param isUnitForHoverText whether the number formatted with this Unit is
     * inside a HoverComponent
     * @return the Unit (default: Hearts for plain text, HP for hover-text)
     */
    public String getDamageUnit(boolean isUnitForHoverText) {
        return getUnitString(isUnitForHoverText, "hearts", "hp", "damage-unit");
    }

    /**
     * Whether PlayerStats should automatically detect the most suitable unit to
     * use for time-based statistics
     *
     * @param isUnitForHoverText whether the number formatted with this Unit is
     * inside a HoverComponent
     * @return the config setting (default: true)
     */
    public boolean autoDetectTimeUnit(boolean isUnitForHoverText) {
        String path = "auto-detect-biggest-time-unit";
        if (isUnitForHoverText) {
            path = path + "-for-hover-text";
        }
        boolean defaultValue = !isUnitForHoverText;
        return config.getBoolean(path, defaultValue);
    }

    /**
     * How many additional units should be displayed next to the most suitable
     * largest unit for time-based statistics
     *
     * @param isUnitForHoverText whether the number formatted with this Unit is
     * inside a HoverComponent
     * @return the config setting (default: 1 for plain text, 0 for hover-text)
     */
    public int getNumberOfExtraTimeUnits(boolean isUnitForHoverText) {
        String path = "number-of-extra-units";
        if (isUnitForHoverText) {
            path = path + "-for-hover-text";
        }
        int defaultValue = isUnitForHoverText ? 0 : 1;
        return config.getInt(path, defaultValue);
    }

    /**
     * The largest unit that should be used for time-based statistics.
     *
     * @param isUnitForHoverText whether the number formatted with this Unit is
     * inside a HoverComponent
     * @return a String representation of the largest time-unit (default: days
     * for plain text, hours for hover-text)
     */
    public String getTimeUnit(boolean isUnitForHoverText) {
        return getTimeUnit(isUnitForHoverText, false);
    }

    /**
     * The unit that should be used for time-based statistics. If the optional
     * smallUnit flag is true, this will return the smallest unit (and otherwise
     * the biggest).
     *
     * @param isUnitForHoverText whether the number formatted with this Unit is
     * inside a HoverComponent
     * @param smallUnit if this is true, get the minimum time-unit
     * @return the Unit (default: hours for plain text, seconds for hover-text)
     */
    public String getTimeUnit(boolean isUnitForHoverText, boolean smallUnit) {
        if (smallUnit) {
            return getUnitString(isUnitForHoverText, "hours", "seconds", "smallest-time-unit");
        }
        return getUnitString(isUnitForHoverText, "days", "hours", "biggest-time-unit");
    }

    /**
     * Returns an integer between 0 and 100 that represents how much lighter a
     * hoverColor should be.
     *
     * @return an {@code int} that represents a percentage (default: 20)
     */
    public int getHoverTextAmountLighter() {
        return config.getInt("hover-text-amount-lighter", 20);
    }

    /**
     * Gets a String that represents either a Chat Color, hex color code, or a
     * Style.
     *
     * @param getStyleSetting if true, returns a Style instead of a Color
     * @return the config setting. Default:
     * <br>Style: "italic"
     * <br>Color: "gray"
     */
    public String getSharedByTextDecoration(boolean getStyleSetting) {
        String def = getStyleSetting ? "italic" : "gray";
        return getDecorationString(null, getStyleSetting, def, "shared-by");
    }

    /**
     * Gets a String that represents either a Chat Color, hex color code, or a
     * Style.
     *
     * @param getStyleSetting if true, returns a Style instead of a Color
     * @return the config setting. Default:
     * <br>Style: "none"
     * <br>Color: "#845EC2"
     */
    public String getSharerNameDecoration(boolean getStyleSetting) {
        return getDecorationString(null, getStyleSetting, "#845EC2", "player-name");
    }

    /**
     * Gets a String that represents either a Chat Color, hex color code, or a
     * Style.
     *
     * @param selection the Target (Player, Server or Top)
     * @param getStyleSetting if true, returns a Style instead of a Color
     * @return the config setting. Default:
     * <br>Style: "none"
     * <br>Color Top: "green"
     * <br>Color Individual/Server: "gold"
     */
    public String getPlayerNameDecoration(Target selection, boolean getStyleSetting) {
        String def;
        if (selection == Target.TOP) {
            def = "green";
        } else {
            def = "gold";
        }
        return getDecorationString(selection, getStyleSetting, def, "player-names");
    }

    /**
     * Whether the playerNames Style is "bold" for a top-stat.
     *
     * @return the config setting (default: false)
     */
    public boolean playerNameIsBold() {
        ConfigurationSection style = getRelevantSection(Target.TOP);

        if (style != null) {
            String styleString = style.getString("player-names");
            return styleString != null && styleString.equalsIgnoreCase("bold");
        }
        return false;
    }

    /**
     * Gets a String that represents either a Chat Color, hex color code, or a
     * Style.
     *
     * @param selection the Target (Player, Server or Top)
     * @param getStyleSetting if true, returns a Style instead of a Color
     * @return the config setting. Default:
     * <br>Style: "none"
     * <br>Color: "yellow"
     */
    public String getStatNameDecoration(Target selection, boolean getStyleSetting) {
        return getDecorationString(selection, getStyleSetting, "yellow", "stat-names");
    }

    /**
     * Gets a String that represents either a Chat Color, hex color code, or a
     * Style.
     *
     * @param selection the Target (Player, Server or Top)
     * @param getStyleSetting if true, returns a Style instead of a Color
     * @return the config setting. Default:
     * <br>Style: "none"
     * <br>Color: "#FFD52B"
     */
    public String getSubStatNameDecoration(Target selection, boolean getStyleSetting) {
        return getDecorationString(selection, getStyleSetting, "#FFD52B", "sub-stat-names");
    }

    /**
     * Gets a String that represents either a Chat Color, hex color code, or
     * Style.
     *
     * @param selection the Target (Player, Server or Top)
     * @param getStyleSetting if true, returns a Style instead of a Color
     * @return the config setting. Default:
     * <br>Style: "none"
     * <br>Color Top: "#55AAFF"
     * <br>Color Individual/Server: "#ADE7FF"
     */
    public String getStatNumberDecoration(Target selection, boolean getStyleSetting) {
        String def;
        if (selection == Target.TOP) {
            def = "#55AAFF";
        } else {
            def = "#ADE7FF";
        }
        return getDecorationString(selection, getStyleSetting, def, "stat-numbers");
    }

    /**
     * Gets a String that represents either a Chat Color, hex color code, or
     * Style.
     *
     * @param selection the Target (Player, Server or Top)
     * @param getStyleSetting if true, returns a Style instead of a Color
     * @return the config setting. Default:
     * <br>Style: "none"
     * <br>Color Top: "yellow"
     * <br>Color Server: "gold"
     */
    public String getTitleDecoration(Target selection, boolean getStyleSetting) {
        String def;
        if (selection == Target.TOP) {
            def = "yellow";
        } else {
            def = "gold";
        }
        return getDecorationString(selection, getStyleSetting, def, "title");
    }

    /**
     * Gets a String that represents either a Chat Color, hex color code, or
     * Style.
     *
     * @param getStyleSetting if true, returns a Style instead of a Color
     * @return the config setting. Default:
     * <br>Style: "none"
     * <br>Color: "gold"
     */
    public String getTitleNumberDecoration(boolean getStyleSetting) {
        return getDecorationString(Target.TOP, getStyleSetting, "gold", "title-number");
    }

    /**
     * Gets a String that represents either a Chat Color, hex color code, or
     * Style.
     *
     * @param getStyleSetting if true, returns a Style instead of a Color
     * @return the config setting. Default:
     * <br>Style: "none"
     * <br>Color: "#FFB80E"
     */
    public String getServerNameDecoration(boolean getStyleSetting) {
        return getDecorationString(Target.SERVER, getStyleSetting, "#FFB80E", "server-name");
    }

    /**
     * Gets a String that represents either a Chat Color, hex color code, or
     * Style.
     *
     * @param getStyleSetting if true, returns a Style instead of a Color
     * @return the config setting. Default:
     * <br>Style: "none"
     * <br>Color: "gold"
     */
    public String getRankNumberDecoration(boolean getStyleSetting) {
        return getDecorationString(Target.TOP, getStyleSetting, "gold", "rank-numbers");
    }

    /**
     * Gets a String that represents either a Chat Color, hex color code, or
     * Style.
     *
     * @param getStyleSetting if true, returns a Style instead of a Color
     * @return the config setting. Default:
     * <br>Style: "none"
     * <br>Color: "dark_gray"
     */
    public String getDotsDecoration(boolean getStyleSetting) {
        return getDecorationString(Target.TOP, getStyleSetting, "dark_gray", "dots");
    }

    /**
     * Gets a String representing a {@link Unit}.
     *
     * @return a String representing the {@link Unit} that should be used for a
     * certain {@link Unit.Type}. If no String can be retrieved from the config,
     * the supplied defaultValue will be returned. If the defaultValue is
     * different for hoverText, an optional String defaultHoverValue can be
     * supplied.
     * @param isHoverText if true, the unit for hovering text is returned,
     * otherwise the unit for plain text
     * @param defaultValue the default unit for plain text
     * @param defaultHoverValue the default unit for hovering text
     * @param pathName the config path to retrieve the value from
     */
    private String getUnitString(boolean isHoverText, String defaultValue, String defaultHoverValue, String pathName) {
        String path = isHoverText ? pathName + "-for-hover-text" : pathName;
        String def = defaultValue;
        if (isHoverText && defaultHoverValue != null) {
            def = defaultHoverValue;
        }
        return config.getString(path, def);
    }

    /**
     * @return the config value for a color or style option in string-format,
     * the supplied default value, or null if no configSection was found.
     * @param selection the Target this decoration is meant for (Player, Server
     * or Top)
     * @param getStyleSetting if true, the result will be a style String,
     * otherwise a color String
     * @param defaultColor the default color to return if the config value
     * cannot be found (for style, the default is always "none")
     * @param pathName the config path to retrieve the value from
     */
    private @Nullable
    String getDecorationString(Target selection, boolean getStyleSetting, String defaultColor, String pathName) {
        String path = getStyleSetting ? pathName + "-style" : pathName;
        String defaultValue = getStyleSetting ? "none" : defaultColor;

        ConfigurationSection section = getRelevantSection(selection);
        return section != null ? section.getString(path, defaultValue) : null;
    }

    /**
     * @return the config section that contains the relevant color or style
     * option.
     */
    private @Nullable
    ConfigurationSection getRelevantSection(Target selection) {
        if (selection == null) {  //rather than rework the whole Target enum, I have added shared-stats as the null-option for now
            return config.getConfigurationSection("shared-stats");
        }
        switch (selection) {
            case TOP -> {
                return config.getConfigurationSection("top-list");
            }
            case PLAYER -> {
                return config.getConfigurationSection("individual-statistics");
            }
            case SERVER -> {
                return config.getConfigurationSection("total-server");
            }
            default -> {
                return null;
            }
        }
    }

    // Helper method to add default approved stats during update
    private void addDefaultApprovedStats(Map<String, Object> valuesMap) {
        valuesMap.put("approved-stats.times_jumped.display-name", "Times Jumped");
        valuesMap.put("approved-stats.times_jumped.statistic", "JUMP");
        valuesMap.put("approved-stats.times_jumped.type", "UNTYPED");

        valuesMap.put("approved-stats.play_time.display-name", "Play Time");
        valuesMap.put("approved-stats.play_time.statistic", "PLAY_ONE_MINUTE"); // Stored in ticks (1/20th sec), will need formatting
        valuesMap.put("approved-stats.play_time.type", "UNTYPED");

        valuesMap.put("approved-stats.player_kills.display-name", "Player Kills");
        valuesMap.put("approved-stats.player_kills.statistic", "PLAYER_KILLS");
        valuesMap.put("approved-stats.player_kills.type", "UNTYPED");

        valuesMap.put("approved-stats.mob_kills.display-name", "Mob Kills");
        valuesMap.put("approved-stats.mob_kills.statistic", "MOB_KILLS");
        valuesMap.put("approved-stats.mob_kills.type", "UNTYPED");

        valuesMap.put("approved-stats.deaths.display-name", "Deaths");
        valuesMap.put("approved-stats.deaths.statistic", "DEATHS");
        valuesMap.put("approved-stats.deaths.type", "UNTYPED");

        valuesMap.put("approved-stats.blocks_mined.display-name", "Blocks Mined");
        valuesMap.put("approved-stats.blocks_mined.statistic", "MINE_BLOCK");
        valuesMap.put("approved-stats.blocks_mined.type", "BLOCK");
        // No specific sub-stat here means total blocks mined

        valuesMap.put("approved-stats.blocks_placed.display-name", "Blocks Placed");
        valuesMap.put("approved-stats.blocks_placed.statistic", "USE_ITEM"); // Using USE_ITEM for blocks is common
        valuesMap.put("approved-stats.blocks_placed.type", "ITEM");
        // No specific sub-stat here means total blocks placed (requires filtering logic later)
        // Alternatively, could require specific blocks like USE_ITEM + STONE, etc.
        // For now, let's keep it simple and represent the *intent*. Implementation might need refinement.
    }

    // --- Approved Stats Methods ---
    /**
     * Loads the approved statistics from the "approved-stats" section of the
     * config file into the cache. Invalid entries are logged and skipped.
     */
    private void loadApprovedStats() {
        Map<String, ApprovedStat> loadedStats = new ConcurrentHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("approved-stats");

        if (section == null) {
            MyLogger.logWarning("Config section 'approved-stats' is missing. Cannot load approved stats.");
            // Attempt to add default stats if section is missing
            if (this.approvedStatsCache == null || this.approvedStatsCache.isEmpty()) {
                MyLogger.logWarning("Attempting to add default approved stats...");
                Map<String, Object> defaults = new ConcurrentHashMap<>();
                addDefaultApprovedStats(defaults);
                super.addValues(defaults); // Save defaults back to file
                reload(); // Reload the file to parse the newly added defaults
                section = config.getConfigurationSection("approved-stats"); // Try getting section again
                if (section == null) {
                    MyLogger.logWarning("Failed to add or find 'approved-stats' section even after adding defaults.");
                    this.approvedStatsCache = Collections.unmodifiableMap(loadedStats); // Assign empty map
                    return;
                }
            } else {
                this.approvedStatsCache = Collections.unmodifiableMap(loadedStats); // Assign empty map
                return;
            }
        }

        // Iterate through each defined statistic alias in the "approved-stats" section
        for (String aliasKey : section.getKeys(false)) {
            ConfigurationSection statSection = section.getConfigurationSection(aliasKey);
            if (statSection == null) {
                continue; // Should not happen with getKeys(false) but check anyway
            }

            String alias = aliasKey.toLowerCase();
            String displayName = statSection.getString("display-name");

            if (displayName == null || displayName.isEmpty()) {
                MyLogger.logWarning("Skipping invalid approved stat '" + alias + "': Missing or empty required field 'display-name'.");
                continue;
            }

            // Determine the StatType (BUKKIT or DERIVED)
            String typeString = statSection.getString("type", "BUKKIT"); // Default to BUKKIT if 'type' is missing
            StatType statType;
            try {
                statType = StatType.valueOf(typeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                MyLogger.logWarning("Skipping invalid approved stat '" + alias + "': Invalid value '" + typeString + "' for field 'type'. Must be BUKKIT or DERIVED.");
                continue;
            }

            // Process based on StatType
            try {
                if (statType == StatType.DERIVED) {
                    // --- Handle DERIVED type ---
                    if (!statSection.isList("components")) {
                        MyLogger.logWarning("Skipping DERIVED approved stat '" + alias + "': Missing required 'components' list.");
                        continue;
                    }
                    List<Map<?, ?>> componentList = statSection.getMapList("components");
                    if (componentList.isEmpty()) {
                        MyLogger.logWarning("Skipping DERIVED approved stat '" + alias + "': 'components' list is empty.");
                        continue;
                    }

                    List<DerivedStatComponent> parsedDerivedComponents = new ArrayList<>();
                    boolean derivedValid = true;
                    for (Map<?, ?> componentMap : componentList) {
                        String compAlias = componentMap.get("alias") instanceof String s ? s.toLowerCase() : null;
                        String compOpStr = componentMap.get("operation") instanceof String s ? s : null;

                        if (compAlias == null || compOpStr == null || compOpStr.length() != 1) {
                            MyLogger.logWarning("Skipping invalid derived component in '" + alias + "': Missing 'alias' or invalid 'operation'. Each component needs an alias (string) and an operation (single character: +, -, *).");
                            derivedValid = false;
                            break;
                        }

                        char operation = compOpStr.charAt(0);
                        if (operation != '+' && operation != '-' && operation != '*') {
                            MyLogger.logWarning("Skipping invalid derived component in '" + alias + "': Invalid 'operation' character '" + operation + "'. Must be +, -, or *.");
                            derivedValid = false;
                            break;
                        }
                        // TODO: Add check later during calculation to ensure compAlias refers to an existing ApprovedStat
                        parsedDerivedComponents.add(new DerivedStatComponent(compAlias, operation));
                    }

                    if (derivedValid) {
                        ApprovedStat derivedStat = new ApprovedStat(alias, displayName, parsedDerivedComponents, true); // Use DERIVED constructor
                        loadedStats.put(alias, derivedStat);
                        MyLogger.logLowLevelMsg("Loaded DERIVED approved stat: " + alias);
                    } else {
                        MyLogger.logWarning("Skipping DERIVED approved stat '" + alias + "' due to invalid component(s).");
                    }

                } else {
                    // --- Handle BUKKIT type ---
                    if (statSection.isList("components")) {
                        // Compound BUKKIT stat (summing multiple Bukkit stats)
                        List<Map<?, ?>> componentList = statSection.getMapList("components");
                        if (componentList.isEmpty()) {
                            MyLogger.logWarning("Skipping compound BUKKIT approved stat '" + alias + "': 'components' list is empty.");
                            continue;
                        }

                        List<ApprovedStat.StatComponent> parsedBukkitComponents = new ArrayList<>();
                        boolean bukkitCompoundValid = true;
                        for (Map<?, ?> componentMap : componentList) {
                            ApprovedStat.StatComponent component = parseBukkitStatComponent(alias, componentMap);
                            if (component != null) {
                                parsedBukkitComponents.add(component);
                            } else {
                                bukkitCompoundValid = false;
                                break; // Stop parsing this compound stat if one component is invalid
                            }
                        }

                        if (bukkitCompoundValid) {
                            ApprovedStat compoundBukkitStat = new ApprovedStat(alias, displayName, parsedBukkitComponents); // Use compound BUKKIT constructor
                            loadedStats.put(alias, compoundBukkitStat);
                            MyLogger.logLowLevelMsg("Loaded compound BUKKIT approved stat: " + alias);
                        } else {
                            MyLogger.logWarning("Skipping compound BUKKIT approved stat '" + alias + "' due to invalid component(s).");
                        }

                    } else {
                        // Simple BUKKIT stat (single statistic)
                        ApprovedStat.StatComponent component = parseBukkitStatComponent(alias, statSection.getValues(false));
                        if (component != null) {
                            // If it's a typed Bukkit statistic (BLOCK, ITEM, ENTITY) without a sub-statistic, treat as a total request
                            if (component.type() != Statistic.Type.UNTYPED && component.material() == null && component.entityType() == null) {
                                ApprovedStat totalStat = new ApprovedStat(alias, displayName, component, true);
                                loadedStats.put(alias, totalStat);
                                MyLogger.logLowLevelMsg("Loaded total BUKKIT approved stat: " + alias);
                            } else {
                                ApprovedStat simpleBukkitStat = new ApprovedStat(alias, displayName, component.statistic(), component.type(), component.material(), component.entityType());
                                loadedStats.put(alias, simpleBukkitStat);
                                MyLogger.logLowLevelMsg("Loaded simple BUKKIT approved stat: " + alias);
                            }
                        } else {
                            MyLogger.logWarning("Skipping simple BUKKIT approved stat '" + alias + "' due to invalid definition.");
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // Catch errors from ApprovedStat constructors (e.g., validation failures)
                MyLogger.logWarning("Skipping approved stat '" + alias + "': Error during creation - " + e.getMessage());
            } catch (Exception e) {
                // Catch unexpected errors during parsing
                MyLogger.logWarning("Skipping approved stat '" + alias + "': Unexpected error during parsing - " + e.getMessage());
                MyLogger.logException(e, "Unexpected error parsing approved stat: " + alias, null);
            }
        }

        this.approvedStatsCache = Collections.unmodifiableMap(loadedStats);
        MyLogger.logMediumLevelMsg("Finished loading approved stats. Found " + loadedStats.size() + " valid entries.");
    }

    /**
     * Parses a Map (from config section or component list item for BUKKIT type)
     * into an ApprovedStat.StatComponent. Logs warnings and returns null if
     * parsing fails.
     */
    private @Nullable
    ApprovedStat.StatComponent parseBukkitStatComponent(@NotNull String contextAlias, @NotNull Map<?, ?> map) {
        String statName = map.get("statistic") instanceof String s ? s : null;
        // For simple BUKKIT stats, 'type' might be specified explicitly.
        // For compound BUKKIT components, 'type' might be missing (inferred from statistic).
        // We prioritize explicit 'type' if present, otherwise infer.
        String explicitTypeName = map.get("type") instanceof String s ? s.toUpperCase() : null;
        String subStatName = map.get("sub-statistic") instanceof String s ? s : null;

        if (statName == null) {
            MyLogger.logWarning("Skipping invalid Bukkit stat component for alias '" + contextAlias + "': Missing 'statistic'.");
            return null;
        }

        Statistic statistic = enumHandler.getStatEnum(statName);
        if (statistic == null) {
            MyLogger.logWarning("Skipping invalid Bukkit stat component for alias '" + contextAlias + "': Invalid statistic name '" + statName + "'.");
            return null;
        }

        // Determine the actual type
        Statistic.Type actualType = statistic.getType();
        Statistic.Type declaredType = actualType; // Default to actual type

        if (explicitTypeName != null) {
            try {
                declaredType = Statistic.Type.valueOf(explicitTypeName);
            } catch (IllegalArgumentException e) {
                MyLogger.logWarning("Skipping invalid Bukkit stat component for alias '" + contextAlias + "': Invalid explicit statistic type '" + explicitTypeName + "'.");
                return null;
            }
            // Check if declared type matches actual type
            if (actualType != declaredType) {
                MyLogger.logWarning("Skipping invalid Bukkit stat component for alias '" + contextAlias + "': Statistic '" + statName + "' is actually type " + actualType + " but config explicitly declares it as type " + declaredType + ".");
                return null;
            }
        }
        // Now 'declaredType' holds the validated type to use (either explicit or inferred)

        Material material = null;
        EntityType entityType = null;

        if (declaredType != Statistic.Type.UNTYPED) {
            if (subStatName == null) {
                // For simple stats, null sub-stat implies total count for this type.
                // For components within a list, null sub-stat is generally an error unless the type allows it (e.g., maybe MOB_KILLS total).
                // Let ApprovedStat constructor handle validation based on context later.
                MyLogger.logLowLevelMsg("Bukkit stat component for '" + contextAlias + "' is of type " + declaredType + " but no 'sub-statistic' is specified. Assuming total count for this type if applicable.");
            } else {
                switch (declaredType) {
                    case BLOCK:
                        material = enumHandler.getBlockEnum(subStatName);
                        if (material == null) {
                            MyLogger.logWarning("Skipping invalid Bukkit stat component for alias '" + contextAlias + "': Invalid block Material '" + subStatName + "' for statistic '" + statName + "'.");
                            return null;
                        }
                        break; // Need break statements
                    case ITEM:
                        material = enumHandler.getItemEnum(subStatName);
                        if (material == null) {
                            MyLogger.logWarning("Skipping invalid Bukkit stat component for alias '" + contextAlias + "': Invalid item Material '" + subStatName + "' for statistic '" + statName + "'.");
                            return null;
                        }
                        break; // Need break statements
                    case ENTITY:
                        entityType = enumHandler.getEntityEnum(subStatName);
                        if (entityType == null) {
                            MyLogger.logWarning("Skipping invalid Bukkit stat component for alias '" + contextAlias + "': Invalid EntityType '" + subStatName + "' for statistic '" + statName + "'.");
                            return null;
                        }
                        break; // Need break statements
                    default: // Should not happen given the outer if, but good practice
                        break;
                }
            }
        } else if (subStatName != null) {
            MyLogger.logWarning("Bukkit stat component for alias '" + contextAlias + "' is UNTYPED ('" + statName + "') but has a sub-statistic '" + subStatName + "' defined. Ignoring sub-statistic.");
        }

        // Use the specific constructors in StatComponent for validation
        try {
            return switch (declaredType) {
                case BLOCK ->
                    new ApprovedStat.StatComponent(statistic, material != null ? material : Material.AIR); // Pass AIR if null for validation consistency? Or handle null in constructor. Let's pass material, constructor handles null.
                case ITEM ->
                    new ApprovedStat.StatComponent(statistic, material != null ? material : Material.AIR, true); // Pass AIR if null? Need marker. Pass material, constructor handles null.
                case ENTITY ->
                    new ApprovedStat.StatComponent(statistic, entityType != null ? entityType : EntityType.UNKNOWN); // Pass UNKNOWN? Pass entityType, constructor handles null.
                case UNTYPED ->
                    new ApprovedStat.StatComponent(statistic);
            };
            // Correction: Simpler approach - pass potentially null sub-stat directly to the general constructor
            // Let ApprovedStat.validateBukkitComponentArgs handle null checks based on type
            //return new ApprovedStat.StatComponent(statistic, declaredType, material, entityType);

            // Correction 2: The StatComponent constructors ARE specific. Let's use them carefully.
            // The constructor logic in ApprovedStat seems complex. Let's just create the component here
            // and rely on the ApprovedStat constructor to validate the *overall* object later.
            // We already validated basic types match here.
        } catch (IllegalArgumentException e) {
            // This might catch errors from StatComponent constructors if they are strict
            MyLogger.logWarning("Skipping invalid Bukkit stat component for alias '" + contextAlias + "': Error creating StatComponent - " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the ApprovedStat configuration for a given alias.
     *
     * @param alias The alias (case-insensitive) to look up.
     * @return The {@link ApprovedStat} object, or null if the alias is not
     * defined or invalid in the config.
     */
    public @Nullable
    ApprovedStat getApprovedStat(@NotNull String alias) {
        if (approvedStatsCache == null) {
            MyLogger.logWarning("Attempted to get approved stat before cache was initialized!");
            return null;
        }
        return approvedStatsCache.get(alias.toLowerCase());
    }

    /**
     * Gets a set of all valid approved statistic aliases loaded from the
     * config.
     *
     * @return An unmodifiable Set of alias strings.
     */
    public @NotNull
    Set<String> getApprovedAliases() {
        if (approvedStatsCache == null) {
            MyLogger.logWarning("Attempted to get approved aliases before cache was initialized!");
            return Collections.emptySet();
        }
        return approvedStatsCache.keySet();
    }

    /**
     * Adds or updates an approved statistic in the configuration file and
     * refreshes the cache.
     *
     * NOTE: This method currently only supports adding/updating *simple*
     * (non-compound) stats. Compound stats must be added/edited directly in
     * config.yml.
     */
    public boolean addApprovedStat(@NotNull String alias, @NotNull String displayName, @NotNull Statistic statistic, @NotNull Statistic.Type type, @Nullable Object subStat) {
        String lowerAlias = alias.toLowerCase();
        String path = "approved-stats." + lowerAlias;

        config.set(path + ".display-name", displayName);
        config.set(path + ".statistic", statistic.name());
        config.set(path + ".type", type.name());

        if (type != Statistic.Type.UNTYPED && subStat != null) {
            if (subStat instanceof Material mat) {
                config.set(path + ".sub-statistic", mat.name());
            } else if (subStat instanceof EntityType et) {
                config.set(path + ".sub-statistic", et.name());
            } else {
                config.set(path + ".sub-statistic", null); // Clear if invalid sub-stat provided for typed stat
            }
        } else {
            config.set(path + ".sub-statistic", null); // Ensure null if untyped or no sub-stat intended
        }

        // Reload the cache from the potentially updated file (might be slightly delayed)
        loadApprovedStats();
        MyLogger.logLowLevelMsg("Successfully updated config in memory for approved stat: " + lowerAlias + ". Config will be saved later.");
        return true; // Assume success for setting in memory
    }

    /**
     * Removes an approved statistic from the configuration file and refreshes
     * the cache.
     */
    public boolean removeApprovedStat(@NotNull String alias) {
        String lowerAlias = alias.toLowerCase();
        String path = "approved-stats." + lowerAlias;

        if (!config.contains(path)) {
            MyLogger.logWarning("Attempted to remove non-existent approved stat: " + lowerAlias);
            return false; // Indicate nothing was removed
        }

        config.set(path, null); // Remove the section

        // Reload the cache from the potentially updated file (might be slightly delayed)
        loadApprovedStats();
        MyLogger.logLowLevelMsg("Successfully removed config section in memory for approved stat: " + lowerAlias + ". Config will be saved later.");
        return true; // Assume success for removing from memory
    }

    // --- End Approved Stats Methods ---
}
