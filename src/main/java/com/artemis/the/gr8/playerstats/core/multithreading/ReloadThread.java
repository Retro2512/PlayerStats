package com.artemis.the.gr8.playerstats.core.multithreading;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import com.artemis.the.gr8.playerstats.core.Main;
import com.artemis.the.gr8.playerstats.core.enums.StandardMessage;
import com.artemis.the.gr8.playerstats.core.msg.OutputManager;
import com.artemis.the.gr8.playerstats.core.utils.MyLogger;

/**
 * The Thread that is in charge of reloading PlayerStats.
 */
final class ReloadThread extends Thread {

    private final Main main;
    private static OutputManager outputManager;

    private final CommandSender sender;

    // Counter for console waiting message, to prevent spam
    private int waitingMsgCounter = 0;

    public ReloadThread(Main main, OutputManager m, int ID, @Nullable CommandSender se) {
        this.main = main;
        outputManager = m;
        sender = se;

        this.setName("ReloadThread-" + ID);
        MyLogger.logHighLevelMsg(this.getName() + " created!");
    }

    /**
     * This method will call reload() from Main.
     */
    @Override
    public void run() {
        MyLogger.logHighLevelMsg(this.getName() + " started!");

        // Wait for ongoing StatActions to finish
        try {
            while (ThreadManager.getActiveActionCount() > 0) {
                if (sender != null && waitingMsgCounter == 0) {
                    outputManager.sendFeedbackMsg(sender, StandardMessage.WAIT_A_MOMENT);
                }
                waitingMsgCounter = (waitingMsgCounter + 1) % 20; // Send message every second (20 * 50ms)
                MyLogger.logLowLevelMsg("ReloadThread waiting for " + ThreadManager.getActiveActionCount() + " stat actions to finish...");
                Thread.sleep(50); // Wait 50 milliseconds
            }
        } catch (InterruptedException e) {
            MyLogger.logWarning("ReloadThread interrupted while waiting for stat actions: " + e.getMessage());
            Thread.currentThread().interrupt(); // Re-interrupt the thread
            // Optionally send an error message to the sender
            if (sender != null) {
                outputManager.sendFeedbackMsg(sender, StandardMessage.INTERNAL_ERROR);
            }
            return; // Stop the reload process if interrupted
        }

        MyLogger.logLowLevelMsg("Reloading!");
        main.reloadPlugin();

        if (sender != null) {
            outputManager.sendFeedbackMsg(sender, StandardMessage.RELOADED_CONFIG);
        }
    }
}
