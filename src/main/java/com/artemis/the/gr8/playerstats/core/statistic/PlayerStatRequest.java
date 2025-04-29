package com.artemis.the.gr8.playerstats.core.statistic;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import com.artemis.the.gr8.playerstats.api.RequestGenerator;
import com.artemis.the.gr8.playerstats.api.StatRequest;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.utils.OfflinePlayerHandler;

public final class PlayerStatRequest extends StatRequest<Integer> implements RequestGenerator<Integer> {

    public PlayerStatRequest(String playerName) {
        this(Bukkit.getConsoleSender(), playerName);
    }

    public PlayerStatRequest(CommandSender sender, String playerName) {
        super(sender);
        super.configureForPlayer(playerName);
    }

    @Override
    public boolean isValid() {
        if (!hasValidTarget()) {
            return false;
        }

        // Check using ApprovedStat first (newer system)
        if (super.getSettings().getApprovedStat() != null) {
            return super.hasMatchingSubStat();
        } // Fallback: Check legacy fields if ApprovedStat is null
        else {
            Statistic stat = super.getSettings().getStatistic();
            if (stat == null) {
                return false; // Need a statistic
            }
            return switch (stat.getType()) {
                case UNTYPED ->
                    true; // Untyped stats don't need a sub-stat
                case BLOCK ->
                    super.getSettings().getBlock() != null;
                case ITEM ->
                    super.getSettings().getItem() != null;
                case ENTITY ->
                    super.getSettings().getEntity() != null;
            };
        }
    }

    private boolean hasValidTarget() {
        StatRequest.Settings settings = super.getSettings();
        if (settings.getPlayerName() == null) {
            return false;
        }

        OfflinePlayerHandler offlinePlayerHandler = OfflinePlayerHandler.getInstance();
        if (offlinePlayerHandler.isExcludedPlayer(settings.getPlayerName())) {
            return ConfigHandler.getInstance().allowPlayerLookupsForExcludedPlayers();
        } else {
            return offlinePlayerHandler.isIncludedPlayer(settings.getPlayerName());
        }
    }

    @Override
    public StatRequest<Integer> untyped(@NotNull Statistic statistic) {
        super.configureUntyped(statistic);
        return this;
    }

    @Override
    public StatRequest<Integer> blockOrItemType(@NotNull Statistic statistic, @NotNull Material material) {
        super.configureBlockOrItemType(statistic, material);
        return this;
    }

    @Override
    public StatRequest<Integer> entityType(@NotNull Statistic statistic, @NotNull EntityType entityType) {
        super.configureEntityType(statistic, entityType);
        return this;
    }
}
