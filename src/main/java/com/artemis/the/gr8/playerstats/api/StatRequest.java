package com.artemis.the.gr8.playerstats.api;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.artemis.the.gr8.playerstats.api.enums.Target;
import com.artemis.the.gr8.playerstats.core.config.ApprovedStat;

/**
 * Holds all the information PlayerStats needs to perform a lookup, and can be
 * executed by the {@link StatManager} to get the results.
 */
public abstract class StatRequest<T> {

    private final Settings settings;

    protected StatRequest(CommandSender requester) {
        settings = new Settings(requester);
    }

    public abstract boolean isValid();

    /**
     * Use this method to view the settings that have been configured for this
     * StatRequest.
     */
    public Settings getSettings() {
        return settings;
    }

    protected void configureForPlayer(String playerName) {
        this.settings.target = Target.PLAYER;
        this.settings.playerName = playerName;
    }

    protected void configureForServer() {
        this.settings.target = Target.SERVER;
    }

    protected void configureForTop(int topListSize) {
        this.settings.target = Target.TOP;
        this.settings.topListSize = topListSize;
    }

    protected void configureUntyped(@NotNull Statistic statistic) {
        if (statistic.getType() != Statistic.Type.UNTYPED) {
            throw new IllegalArgumentException("This statistic is not of Type.Untyped");
        }
        this.settings.statistic = statistic;
    }

    protected void configureBlockOrItemType(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {
        Statistic.Type type = statistic.getType();
        if (type == Statistic.Type.BLOCK && material.isBlock()) {
            this.settings.block = material;
        } else if (type == Statistic.Type.ITEM && material.isItem()) {
            this.settings.item = material;
        } else {
            throw new IllegalArgumentException("Either this statistic is not of Type.Block or Type.Item, or no valid block or item has been provided");
        }
        this.settings.statistic = statistic;
        this.settings.subStatEntryName = material.toString();
    }

    protected void configureEntityType(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {
        if (statistic.getType() != Statistic.Type.ENTITY) {
            throw new IllegalArgumentException("This statistic is not of Type.Entity");
        }
        this.settings.statistic = statistic;
        this.settings.entity = entityType;
        this.settings.subStatEntryName = entityType.toString();
    }

    protected boolean hasMatchingSubStat() {
        ApprovedStat approvedStat = settings.getApprovedStat();
        if (approvedStat == null) {
            return false; // Cannot validate without an ApprovedStat
        }
        if (approvedStat.isCompound()) {
            return true; // Compound stats don't have a single type to check against, assume valid structure
        }

        // For simple stats, check the single component
        ApprovedStat.StatComponent component = approvedStat.getBukkitComponents().get(0);
        return switch (component.type()) {
            case UNTYPED ->
                true; // Untyped always matches
            case BLOCK ->
                component.material() != null;
            case ITEM ->
                component.material() != null;
            case ENTITY ->
                component.entityType() != null;
        };
    }

    public static final class Settings {

        private final CommandSender sender;
        private ApprovedStat approvedStat; // Main storage for stat details
        private String playerName;
        private Target target;
        private int topListSize;

        // Fields below are deprecated/redundant if approvedStat is set
        // Keep for now if StatCommand still uses them directly, but should refactor
        private Statistic statistic; // DEPRECATED - Get from approvedStat
        private String subStatEntryName; // DEPRECATED - Get from approvedStat component(s)
        private EntityType entity; // DEPRECATED - Get from approvedStat component(s)
        private Material block; // DEPRECATED - Get from approvedStat component(s)
        private Material item; // DEPRECATED - Get from approvedStat component(s)

        /**
         * @param sender the CommandSender who prompted this RequestGenerator
         */
        private Settings(@NotNull CommandSender sender) {
            this.sender = sender;
        }

        public @NotNull
        CommandSender getCommandSender() {
            return sender;
        }

        public boolean isConsoleSender() {
            return sender instanceof ConsoleCommandSender;
        }

        /**
         * @deprecated Use {@link #getApprovedStat()} and access its components.
         */
        @Deprecated
        public Statistic getStatistic() {
            return approvedStat != null ? approvedStat.statistic() : statistic;
        }

        /**
         * @deprecated Use {@link #getApprovedStat()} and access its components.
         */
        @Deprecated
        public @Nullable
        String getSubStatEntryName() {
            // Cannot reliably get a single name for compound stats
            if (approvedStat != null && approvedStat.isCompound()) {
                return null;
            }
            if (approvedStat != null) {
                ApprovedStat.StatComponent comp = approvedStat.getBukkitComponents().get(0);
                if (comp.material() != null) {
                    return comp.material().name();
                }
                if (comp.entityType() != null) {
                    return comp.entityType().name();
                }
                return null; // Untyped
            }
            return subStatEntryName;
        }

        public String getPlayerName() {
            return playerName;
        }

        public @NotNull
        Target getTarget() {
            return target;
        }

        public int getTopListSize() {
            return this.topListSize;
        }

        /**
         * @deprecated Use {@link #getApprovedStat()} and access its components.
         */
        @Deprecated
        public EntityType getEntity() {
            return approvedStat != null ? approvedStat.entityType() : entity;
        }

        /**
         * @deprecated Use {@link #getApprovedStat()} and access its components.
         */
        @Deprecated
        public Material getBlock() {
            return approvedStat != null ? approvedStat.material() : block;
        }

        /**
         * @deprecated Use {@link #getApprovedStat()} and access its components.
         */
        @Deprecated
        public Material getItem() {
            return approvedStat != null ? approvedStat.material() : item;
        }

        public @Nullable
        ApprovedStat getApprovedStat() {
            return approvedStat;
        }

        public void setApprovedStat(@Nullable ApprovedStat approvedStat) {
            this.approvedStat = approvedStat;
        }
    }
}
