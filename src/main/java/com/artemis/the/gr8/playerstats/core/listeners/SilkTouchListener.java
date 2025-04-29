package com.artemis.the.gr8.playerstats.core.listeners;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Internal listener to prevent counting of ore blocks mined with Silk Touch.
 */
@ApiStatus.Internal
public class SilkTouchListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStatisticIncrement(PlayerStatisticIncrementEvent event) {
        // Only for block-mining statistics
        if (event.getStatistic() != Statistic.MINE_BLOCK) {
            return;
        }
        Material material = event.getMaterial();
        if (material == null) {
            return;
        }
        // Skip counting if block is an ore or ancient debris
        if (!(material.name().endsWith("_ORE") || material == Material.ANCIENT_DEBRIS)) {
            return;
        }
        Player player = event.getPlayer();
        // Check if the tool used had Silk Touch
        if (player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
            // Cancel statistic increment
            event.setCancelled(true);
            // Revert to previous value if already updated
            player.setStatistic(Statistic.MINE_BLOCK, material, event.getPreviousValue());
        }
    }
}
