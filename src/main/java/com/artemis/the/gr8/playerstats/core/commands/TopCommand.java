package com.artemis.the.gr8.playerstats.core.commands;

import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.artemis.the.gr8.playerstats.core.config.ApprovedStat;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.enums.StandardMessage;
import com.artemis.the.gr8.playerstats.core.msg.OutputManager;
import com.artemis.the.gr8.playerstats.core.multithreading.ThreadManager;
import com.artemis.the.gr8.playerstats.core.statistic.RequestProcessor;
import com.artemis.the.gr8.playerstats.core.statistic.StatRequestManager;
import com.artemis.the.gr8.playerstats.core.statistic.TopStatRequest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class TopCommand implements CommandExecutor {

    private static OutputManager outputManager;
    private static ConfigHandler config;

    public TopCommand(ThreadManager threadManager) {
        TopCommand.outputManager = OutputManager.getInstance();
        TopCommand.config = ConfigHandler.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.COMMAND_PLAYER_ONLY);
            return true;
        }

        if (args.length != 1) {
            sendAvailableStats(player);
            return true;
        }

        String alias = args[0].toLowerCase();
        ApprovedStat approvedStat = config.getApprovedStat(alias);

        if (approvedStat == null) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_STAT_MSG); // Assuming this exists or creating a new one
            sendAvailableStats(player);
            return true;
        }

        // Create the StatRequest using ApprovedStat details
        TopStatRequest request = new TopStatRequest(sender, config.getTopListMaxSize()); // Get top N size from config
        request.approvedStat(approvedStat); // Set the approved stat directly

        if (!request.isValid()) {
            // This might happen if the ApprovedStat loaded from config is somehow invalid despite parsing
            // (e.g., if validation logic changes later)
            outputManager.sendFeedbackMsg(sender, StandardMessage.INVALID_STAT_MSG); // Or a more specific internal error
            return true;
        }

        // Send calculating message - use the approved stat's display name
        outputManager.sendFeedbackMsg(sender, StandardMessage.CALCULATING_MSG);
        sender.sendMessage(Component.text(approvedStat.displayName()).color(NamedTextColor.GOLD)); // Example color

        // Delegate processing to BukkitProcessor
        RequestProcessor requestProcessor = StatRequestManager.getBukkitProcessor();
        requestProcessor.processTopRequest(request); // Request is already a TopStatRequest

        return true;
    }

    private void sendAvailableStats(Player player) {
        Set<String> aliases = config.getApprovedAliases();
        if (aliases.isEmpty()) {
            outputManager.sendFeedbackMsg(player, StandardMessage.NO_APPROVED_STATS); // Need to add this message
        } else {
            // Construct message listing available aliases
            // Maybe format this nicely later in OutputManager
            outputManager.sendFeedbackMsg(player, StandardMessage.AVAILABLE_STATS); // Need to add this message
            player.sendMessage("Available stats: " + String.join(", ", aliases));
        }
    }
}
