package com.artemis.the.gr8.playerstats.core.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a statistic approved for use in the /top command, identified by a
 * unique alias. This can represent a single Bukkit statistic, a sum of multiple
 * Bukkit statistics, or a derived statistic calculated from other approved
 * stats.
 */
public final class ApprovedStat {

    private final @NotNull
    String alias;
    private final @NotNull
    String displayName;
    private final @NotNull
    StatType statType;

    // Fields for BUKKIT type
    private final @NotNull
    List<StatComponent> bukkitComponents;

    // Fields for DERIVED type
    private final @NotNull
    List<DerivedStatComponent> derivedComponents;

    /**
     * Internal record to hold the details of a single Bukkit statistic
     * component.
     */
    public record StatComponent(
            @NotNull Statistic statistic,
            @NotNull Statistic.Type type,
            @Nullable Material material,
            @Nullable EntityType entityType
            ) {

        // Constructor for UNTYPED stats
        public StatComponent(@NotNull Statistic statistic) {
            this(statistic, Statistic.Type.UNTYPED, null, null);
            if (statistic.getType() != Statistic.Type.UNTYPED) {
                throw new IllegalArgumentException("Statistic must be UNTYPED for this constructor");
            }
        }

        // Constructor for BLOCK stats
        public StatComponent(@NotNull Statistic statistic, @NotNull Material block) {
            this(statistic, Statistic.Type.BLOCK, block, null);
            if (statistic.getType() != Statistic.Type.BLOCK || !block.isBlock()) {
                throw new IllegalArgumentException("Statistic must be BLOCK type and Material must be a block");
            }
        }

        // Constructor for ITEM stats
        public StatComponent(@NotNull Statistic statistic, @NotNull Material item, boolean isItemMarker) {
            this(statistic, Statistic.Type.ITEM, item, null);
            if (!isItemMarker || statistic.getType() != Statistic.Type.ITEM || !item.isItem()) { // Need marker to distinguish from block constructor
                throw new IllegalArgumentException("Statistic must be ITEM type and Material must be an item");
            }
        }

        // Constructor for ENTITY stats
        public StatComponent(@NotNull Statistic statistic, @NotNull EntityType entityType) {
            this(statistic, Statistic.Type.ENTITY, null, entityType);
            if (statistic.getType() != Statistic.Type.ENTITY) {
                throw new IllegalArgumentException("Statistic must be ENTITY type");
            }
        }
    }

    /**
     * Constructor for a simple BUKKIT ApprovedStat (representing a single
     * statistic).
     */
    public ApprovedStat(@NotNull String alias, @NotNull String displayName,
            @NotNull Statistic statistic, @NotNull Statistic.Type type,
            @Nullable Material material, @Nullable EntityType entityType) {
        this.alias = alias.toLowerCase();
        this.displayName = displayName;
        this.statType = StatType.BUKKIT;
        // Validate inputs match type
        validateBukkitComponentArgs(statistic, type, material, entityType);
        this.bukkitComponents = List.of(new StatComponent(statistic, type, material, entityType));
        this.derivedComponents = Collections.emptyList();
    }

    /**
     * Constructor for a compound BUKKIT ApprovedStat (representing a sum of
     * statistics).
     */
    public ApprovedStat(@NotNull String alias, @NotNull String displayName, @NotNull List<StatComponent> bukkitComponents) {
        if (bukkitComponents.isEmpty()) {
            throw new IllegalArgumentException("Bukkit component list cannot be empty for a BUKKIT ApprovedStat");
        }
        this.alias = alias.toLowerCase();
        this.displayName = displayName;
        this.statType = StatType.BUKKIT;
        this.bukkitComponents = List.copyOf(bukkitComponents);
        this.derivedComponents = Collections.emptyList();
    }

    /**
     * Constructor for a DERIVED ApprovedStat.
     */
    public ApprovedStat(@NotNull String alias, @NotNull String displayName, @NotNull List<DerivedStatComponent> derivedComponents, boolean isDerivedMarker) {
        if (!isDerivedMarker) {
            throw new UnsupportedOperationException("Use the correct constructor overload");
        }
        if (derivedComponents.isEmpty()) {
            throw new IllegalArgumentException("Derived component list cannot be empty for a DERIVED ApprovedStat");
        }
        for (DerivedStatComponent comp : derivedComponents) {
            if (!comp.isValidOperation()) {
                throw new IllegalArgumentException("Invalid operation '" + comp.operation() + "' in derived component for alias '" + alias + "'");
            }
        }

        this.alias = alias.toLowerCase();
        this.displayName = displayName;
        this.statType = StatType.DERIVED;
        this.derivedComponents = List.copyOf(derivedComponents);
        this.bukkitComponents = Collections.emptyList();
    }

    /**
     * @return The unique alias for this approved stat (lowercase).
     */
    public @NotNull
    String alias() {
        return alias;
    }

    /**
     * @return The user-friendly display name for this stat.
     */
    public @NotNull
    String displayName() {
        return displayName;
    }

    /**
     * @return The type of this stat (BUKKIT or DERIVED).
     */
    public @NotNull
    StatType getStatType() {
        return statType;
    }

    /**
     * @return True if this ApprovedStat represents multiple underlying Bukkit
     * statistics summed together. Returns false for simple Bukkit stats and all
     * DERIVED stats.
     * @deprecated Use {@link #getStatType()} and check list sizes if needed.
     */
    @Deprecated
    public boolean isCompound() {
        return statType == StatType.BUKKIT && bukkitComponents.size() > 1;
    }

    /**
     * Gets the list of underlying Bukkit statistic components. This list is
     * non-empty only if {@link #getStatType()} is {@link StatType#BUKKIT}.
     *
     * @return An immutable list of Bukkit statistic components.
     */
    public @NotNull
    List<StatComponent> getBukkitComponents() {
        return bukkitComponents;
    }

    /**
     * Gets the list of components used to calculate a derived statistic. This
     * list is non-empty only if {@link #getStatType()} is
     * {@link StatType#DERIVED}.
     *
     * @return An immutable list of derived statistic components.
     */
    public @NotNull
    List<DerivedStatComponent> getDerivedComponents() {
        return derivedComponents;
    }

    // --- Compatibility/Convenience Methods (Only valid for StatType.BUKKIT) ---
    /**
     * Gets the primary Bukkit Statistic. Returns the Statistic of the *first*
     * component. **Warning:** Only call this if {@link #getStatType()} is
     * {@link StatType#BUKKIT}.
     *
     * @return The Statistic, or null if called on a DERIVED stat or if list is
     * empty (should not happen for BUKKIT).
     * @throws IllegalStateException if called on a DERIVED stat.
     */
    public @Nullable
    Statistic statistic() {
        if (statType == StatType.DERIVED) {
            throw new IllegalStateException("Cannot get Bukkit statistic for a DERIVED ApprovedStat");
        }
        return bukkitComponents.isEmpty() ? null : bukkitComponents.get(0).statistic();
    }

    /**
     * Gets the primary Bukkit Statistic Type. Returns the Type of the *first*
     * component. **Warning:** Only call this if {@link #getStatType()} is
     * {@link StatType#BUKKIT}.
     *
     * @return The Statistic.Type, or null if called on a DERIVED stat or if
     * list is empty.
     * @throws IllegalStateException if called on a DERIVED stat.
     */
    public @Nullable
    Statistic.Type type() {
        if (statType == StatType.DERIVED) {
            throw new IllegalStateException("Cannot get Bukkit type for a DERIVED ApprovedStat");
        }
        return bukkitComponents.isEmpty() ? null : bukkitComponents.get(0).type();
    }

    /**
     * Gets the primary Material sub-statistic (for BLOCK or ITEM types).
     * Returns the Material of the *first* component. **Warning:** Only call
     * this if {@link #getStatType()} is {@link StatType#BUKKIT}.
     *
     * @return The Material, or null.
     * @throws IllegalStateException if called on a DERIVED stat.
     */
    public @Nullable
    Material material() {
        if (statType == StatType.DERIVED) {
            throw new IllegalStateException("Cannot get Bukkit material for a DERIVED ApprovedStat");
        }
        return bukkitComponents.isEmpty() ? null : bukkitComponents.get(0).material();
    }

    /**
     * Gets the primary EntityType sub-statistic (for ENTITY type). Returns the
     * EntityType of the *first* component. **Warning:** Only call this if
     * {@link #getStatType()} is {@link StatType#BUKKIT}.
     *
     * @return The EntityType, or null.
     * @throws IllegalStateException if called on a DERIVED stat.
     */
    public @Nullable
    EntityType entityType() {
        if (statType == StatType.DERIVED) {
            throw new IllegalStateException("Cannot get Bukkit entityType for a DERIVED ApprovedStat");
        }
        return bukkitComponents.isEmpty() ? null : bukkitComponents.get(0).entityType();
    }

    // --- Utility Methods ---
    private static void validateBukkitComponentArgs(@NotNull Statistic statistic, @NotNull Statistic.Type type, @Nullable Material material, @Nullable EntityType entityType) {
        if (statistic.getType() != type) {
            throw new IllegalArgumentException("Provided Statistic.Type (" + type + ") does not match the actual type of the Statistic (" + statistic.getType() + ")");
        }
        switch (type) {
            case BLOCK:
                // Allow null material for total block counts
                if (material != null && !material.isBlock()) {
                    throw new IllegalArgumentException("Provided Material must be a block for BLOCK type statistics");
                }
                if (entityType != null) {
                    throw new IllegalArgumentException("EntityType must be null for BLOCK type statistics");
                }
                break;
            case ITEM:
                // Allow null material for total item counts
                if (material != null && !material.isItem()) {
                    throw new IllegalArgumentException("Provided Material must be an item for ITEM type statistics");
                }
                if (entityType != null) {
                    throw new IllegalArgumentException("EntityType must be null for ITEM type statistics");
                }
                break;
            case ENTITY:
                // Allow null entityType for total entity counts (e.g. MOB_KILLS)
                //if (entityType == null) {
                //    throw new IllegalArgumentException("EntityType cannot be null for ENTITY type statistics");
                //}
                if (material != null) {
                    throw new IllegalArgumentException("Material must be null for ENTITY type statistics");
                }
                break;
            case UNTYPED:
                if (material != null || entityType != null) {
                    throw new IllegalArgumentException("Material and EntityType must be null for UNTYPED statistics");
                }
                break;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApprovedStat that = (ApprovedStat) o;
        return alias.equals(that.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias);
    }

    @Override
    public String toString() {
        return "ApprovedStat{"
                + "alias='" + alias + '\''
                + ", displayName='" + displayName + '\''
                + ", statType=" + statType
                + ", bukkitComponents=" + bukkitComponents
                + ", derivedComponents=" + derivedComponents
                + '}';
    }
}
