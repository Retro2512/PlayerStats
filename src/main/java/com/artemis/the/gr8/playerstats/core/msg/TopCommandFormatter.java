package com.artemis.the.gr8.playerstats.core.msg;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Statistic;
import org.jetbrains.annotations.NotNull;

import com.artemis.the.gr8.playerstats.api.enums.Target;
import com.artemis.the.gr8.playerstats.api.enums.Unit;
import com.artemis.the.gr8.playerstats.core.config.ApprovedStat;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.config.StatType;
import com.artemis.the.gr8.playerstats.core.msg.components.ComponentFactory;
import com.artemis.the.gr8.playerstats.core.msg.msgutils.NumberFormatter;
import com.artemis.the.gr8.playerstats.core.utils.EnumHandler;
import com.artemis.the.gr8.playerstats.core.utils.MyLogger;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Formats the output for the /top command.
 */
public final class TopCommandFormatter {

    private final ConfigHandler config;
    private final ComponentFactory componentFactory;
    private final NumberFormatter numberFormatter;
    private final EnumHandler enumHandler; // Added

    private final LinkedHashMap<String, Integer> topStats;
    private final int senderRank;
    private final int senderStatValue;
    private final @NotNull
    ApprovedStat approvedStat;
    private final int topListSize;
    private final String senderName;

    public TopCommandFormatter(@NotNull LinkedHashMap<String, Integer> topStats,
            int senderRank, int senderStatValue,
            @NotNull ApprovedStat approvedStat,
            int topListSize,
            @NotNull String senderName,
            @NotNull ComponentFactory componentFactory) {
        this.config = ConfigHandler.getInstance();
        this.componentFactory = componentFactory;
        this.numberFormatter = new NumberFormatter();
        this.enumHandler = EnumHandler.getInstance(); // Added

        this.topStats = topStats;
        this.senderRank = senderRank;
        this.senderStatValue = senderStatValue;
        this.approvedStat = approvedStat;
        this.topListSize = topListSize;
        this.senderName = senderName;
    }

    public TextComponent format() {
        TextComponent title = createTitleComponent();
        TextComponent list = createListComponent();
        TextComponent footer = createFooterComponent(); // Renamed from createSenderComponent

        TextComponent headerSeparator = componentFactory.separator(NamedTextColor.GRAY, "----------"); // Assuming ComponentFactory has or needs a separator method
        TextComponent footerSeparator = componentFactory.separator(NamedTextColor.GRAY, "---------------------"); // Longer for footer

        TextComponent.Builder totalMsg = text();
        totalMsg.append(headerSeparator)
                .append(space())
                .append(title)
                .append(space())
                .append(headerSeparator);

        if (list.children().isEmpty()) {
            totalMsg.append(newline());
            totalMsg.append(componentFactory.messageAccent().content("        (no results found)        ").colorIfAbsent(NamedTextColor.GRAY)); // Centered-ish
        } else {
            totalMsg.append(list); // createListComponent will handle newlines internally
        }

        totalMsg.append(newline());
        totalMsg.append(footerSeparator);
        totalMsg.append(newline());
        totalMsg.append(footer); // Append footer info

        return totalMsg.build();
    }

    private TextComponent createTitleComponent() {
        // Simplified title: "Top [Number]: [Stat Name]"
        TextComponent topLabel = text("Top ").color(NamedTextColor.GOLD);
        TextComponent topNumber = componentFactory.titleNumber(topListSize).colorIfAbsent(NamedTextColor.GOLD);
        TextComponent statName = componentFactory.title(approvedStat.displayName(), Target.TOP).colorIfAbsent(NamedTextColor.GOLD);

        return text().append(topLabel)
                .append(topNumber)
                .append(text(": ").color(NamedTextColor.GOLD))
                .append(statName)
                .build();
    }

    private TextComponent createListComponent() {
        TextComponent.Builder listBuilder = text();
        int count = 0;
        for (Map.Entry<String, Integer> entry : topStats.entrySet()) {
            count++;
            TextComponent line = createSingleLine(count, entry.getKey(), entry.getValue());
            listBuilder.append(newline()).append(line);
        }
        return listBuilder.build();
    }

    // Renamed and repurposed for the footer line
    private TextComponent createFooterComponent() {
        TextComponent yourRankLabel = text("Your Rank: ").color(NamedTextColor.GRAY);
        TextComponent rankIndicator;
        TextComponent yourValue = formatNumber(senderStatValue, approvedStat.statistic());

        if (senderRank <= 0) { // Not ranked or error
            rankIndicator = text("N/A").color(NamedTextColor.DARK_GRAY);
        } else {
            rankIndicator = text("#" + senderRank).color(NamedTextColor.GOLD);
        }

        TextComponent senderNameComponent = componentFactory.playerName(senderName, Target.TOP).colorIfAbsent(NamedTextColor.WHITE);

        return text().append(yourRankLabel)
                .append(rankIndicator)
                .append(text(" (").color(NamedTextColor.DARK_GRAY))
                .append(senderNameComponent)
                .append(text(") - ").color(NamedTextColor.DARK_GRAY))
                .append(yourValue)
                .build();
    }

    private TextComponent createSingleLine(int rank, String playerName, int statValue) {
        TextComponent rankNum = componentFactory.rankNumber(rank);
        TextComponent pName = componentFactory.playerName(playerName, Target.TOP);
        TextComponent statNum = formatNumber(statValue, approvedStat.statistic()); // Use first component's stat for formatting hints

        // Simple format: "Rank. PlayerName Value"
        return text()
                .append(space()) // Indent slightly
                .append(rankNum) // Includes the dot from factory
                .append(space())
                .append(pName)
                .append(space())
                .append(statNum)
                .build();
    }

    // Simplified number formatting - replicates parts of MessageBuilder
    private TextComponent formatNumber(long statNumber, Statistic statistic) { // Statistic parameter might be null for derived stats
        // If the associated ApprovedStat is DERIVED, always use default formatting
        if (approvedStat.getStatType() == StatType.DERIVED) {
            return componentFactory.statNumber(numberFormatter.formatDefaultNumber(statNumber), Target.TOP);
        }

        // Otherwise (it's BUKKIT), use the logic based on the (first) component's statistic type
        Statistic firstStat = approvedStat.statistic(); // Get the first Bukkit component's stat
        if (firstStat == null) {
            // Should not happen for BUKKIT type, but handle defensively
            MyLogger.logWarning("Cannot format number for BUKKIT stat '" + approvedStat.alias() + "' as first component statistic is null.");
            return componentFactory.statNumber(numberFormatter.formatDefaultNumber(statNumber), Target.TOP);
        }

        Statistic.Type type = firstStat.getType();
        if (type == Statistic.Type.UNTYPED) {
            switch (firstStat) { // Use the actual Statistic enum for the switch
                case DAMAGE_DEALT, DAMAGE_TAKEN, DAMAGE_BLOCKED_BY_SHIELD, DAMAGE_ABSORBED, DAMAGE_RESISTED, DAMAGE_DEALT_ABSORBED, DAMAGE_DEALT_RESISTED:
                    return componentFactory.damageNumber(numberFormatter.formatDamageNumber(statNumber, Unit.HEART), Target.TOP);
                case PLAY_ONE_MINUTE, TIME_SINCE_DEATH, TIME_SINCE_REST:
                    return componentFactory.timeNumber(numberFormatter.formatTimeNumber(statNumber, Unit.DAY, Unit.SECOND), Target.TOP);
                case TALKED_TO_VILLAGER, TRADED_WITH_VILLAGER:
                    return componentFactory.statNumber(numberFormatter.formatDefaultNumber(statNumber) + "x", Target.TOP);
                default:
                    return componentFactory.statNumber(numberFormatter.formatDefaultNumber(statNumber), Target.TOP);
            }
        } else if (type == Statistic.Type.BLOCK || type == Statistic.Type.ITEM) {
            return componentFactory.statNumber(numberFormatter.formatDefaultNumber(statNumber), Target.TOP);
        } else if (type == Statistic.Type.ENTITY) {
            return componentFactory.statNumber(numberFormatter.formatDefaultNumber(statNumber), Target.TOP);
        } else { // Should not happen, but fallback
            return componentFactory.statNumber(numberFormatter.formatDefaultNumber(statNumber), Target.TOP);
        }
    }
}
