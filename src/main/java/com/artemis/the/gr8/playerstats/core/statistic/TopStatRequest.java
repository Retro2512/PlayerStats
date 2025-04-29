package com.artemis.the.gr8.playerstats.core.statistic;

import java.util.LinkedHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import com.artemis.the.gr8.playerstats.api.RequestGenerator;
import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstats.core.config.ApprovedStat;

public final class TopStatRequest extends StatRequest<LinkedHashMap<String, Integer>> implements RequestGenerator<LinkedHashMap<String, Integer>> {

    public TopStatRequest(int topListSize) {
        this(Bukkit.getConsoleSender(), topListSize);
    }

    public TopStatRequest(CommandSender sender, int topListSize) {
        super(sender);
        super.configureForTop(topListSize);
    }

    @Override
    public boolean isValid() {
        // Check using ApprovedStat first (newer system, intended for /top command)
        if (super.getSettings().getApprovedStat() != null) {
            // Original logic: only valid if ApprovedStat is set and matches
            return super.hasMatchingSubStat();
        } // Fallback: Check legacy fields if ApprovedStat is null (legacy /stat top usage)
        else {
            Statistic stat = super.getSettings().getStatistic();
            if (stat == null) {
                return false; // Need a statistic
            }
            return switch (stat.getType()) {
                case UNTYPED ->
                    true; // Untyped stats are valid
                case BLOCK ->
                    super.getSettings().getBlock() != null;
                case ITEM ->
                    super.getSettings().getItem() != null;
                case ENTITY ->
                    super.getSettings().getEntity() != null;
            };
        }
    }

    @Override
    public StatRequest<LinkedHashMap<String, Integer>> untyped(@NotNull Statistic statistic) throws IllegalArgumentException {
        super.configureUntyped(statistic);
        return this;
    }

    @Override
    public StatRequest<LinkedHashMap<String, Integer>> blockOrItemType(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {
        super.configureBlockOrItemType(statistic, material);
        return this;
    }

    @Override
    public StatRequest<LinkedHashMap<String, Integer>> entityType(@NotNull Statistic statistic, @NotNull EntityType entity) throws IllegalArgumentException {
        super.configureEntityType(statistic, entity);
        return this;
    }

    /**
     * Sets the ApprovedStat to be looked up. This is the primary method for
     * configuring a TopStatRequest based on an alias from the config.
     *
     * @param approvedStat The ApprovedStat object loaded from the config.
     * @return this StatRequest
     */
    public TopStatRequest approvedStat(@NotNull ApprovedStat approvedStat) {
        super.getSettings().setApprovedStat(approvedStat);
        return this;
    }
}
