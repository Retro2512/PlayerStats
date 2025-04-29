package com.artemis.the.gr8.playerstats.core.enums;

/**
 * All standard messages PlayerStats can send as feedback. These are all the
 * messages that can be sent without needing additional parameters.
 */
public enum StandardMessage {
    RELOADED_CONFIG,
    STILL_RELOADING,
    EXCLUDE_FAILED,
    INCLUDE_FAILED,
    MISSING_STAT_NAME,
    MISSING_PLAYER_NAME,
    PLAYER_IS_EXCLUDED,
    WAIT_A_MOMENT,
    WAIT_A_MINUTE,
    REQUEST_ALREADY_RUNNING,
    STILL_ON_SHARE_COOLDOWN,
    RESULTS_ALREADY_SHARED,
    STAT_RESULTS_TOO_OLD,
    UNKNOWN_ERROR,
    // Added for /top and /statadmin
    COMMAND_PLAYER_ONLY, // "This command can only be run by a player."
    INVALID_STAT_MSG, // "Invalid statistic name!"
    MISSING_SUBSTAT_NAME, // "Missing substat name!"
    MISSING_PERMISSION, // "You do not have permission to use this command."
    NO_APPROVED_STATS, // "No approved stats are configured yet!"
    AVAILABLE_STATS, // "Available approved stats:"
    PLAYER_NOT_FOUND, // "Player not found!"
    INTERNAL_ERROR, // "An internal error occurred. Please check the console."
    CALCULATING_MSG, // "Calculating statistic: " (used in TopCommand, needs appending)
    INVALID_SUBSTAT_NAME, // "'...' is not a valid item, block, or entity name!"
    INVALID_COMMAND_SYNTAX // "Invalid command syntax!"
}
