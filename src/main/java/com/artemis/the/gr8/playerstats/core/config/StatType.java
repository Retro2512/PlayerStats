package com.artemis.the.gr8.playerstats.core.config;

/**
 * Defines the type of an ApprovedStat, distinguishing between direct Bukkit
 * statistics (potentially summed) and derived statistics calculated from other
 * approved stats.
 */
public enum StatType {
    /**
     * Represents one or more direct Bukkit statistics (summed together).
     * Corresponds to entries with `statistic`, `type`, and optional
     * `sub-statistic` or a `components` list containing direct Bukkit stat
     * definitions.
     */
    BUKKIT,
    /**
     * Represents a statistic calculated by combining other approved stats using
     * arithmetic operations (+, -, *). Corresponds to entries with `type:
     * DERIVED` and a `components` list specifying aliases and operations.
     */
    DERIVED
}
