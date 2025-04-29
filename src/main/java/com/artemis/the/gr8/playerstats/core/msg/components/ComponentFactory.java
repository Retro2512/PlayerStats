package com.artemis.the.gr8.playerstats.core.msg.components;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.artemis.the.gr8.playerstats.api.enums.Target;
import com.artemis.the.gr8.playerstats.api.enums.Unit;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.enums.PluginColor;
import com.artemis.the.gr8.playerstats.core.msg.MessageBuilder;
import com.artemis.the.gr8.playerstats.core.msg.msgutils.LanguageKeyHandler;

import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.util.HSVLike;
import net.kyori.adventure.util.Index;

/**
 * Creates Components with the desired formatting for the {@link MessageBuilder}
 * to build messages with. This class can put Strings into formatted Components
 * with TextColor and TextDecoration, or return empty Components with the
 * desired formatting (as specified by the {@link ConfigHandler}).
 *
 * @see PluginColor
 */
public class ComponentFactory {

    private static ConfigHandler config;

    protected TextColor PREFIX;  //gold
    protected TextColor BRACKETS;  //gray
    protected TextColor UNDERSCORE;  //dark_purple
    protected TextColor HEARTS; //red

    protected TextColor FEEDBACK_MSG;  //lightest_blue
    protected TextColor FEEDBACK_MSG_ACCENT; //light_blue

    protected TextColor INFO_MSG;  //gold
    protected TextColor INFO_MSG_ACCENT_DARKEST;  //medium_gold
    protected TextColor INFO_MSG_ACCENT_MEDIUM;  //light_gold
    protected TextColor INFO_MSG_ACCENT_LIGHTEST;  //lightest_blue

    protected TextColor MSG_HOVER;  //lightest_blue
    protected TextColor MSG_CLICKED;  //light_purple

    public ComponentFactory() {
        config = ConfigHandler.getInstance();
        prepareColors();
    }

    protected void prepareColors() {
        PREFIX = PluginColor.GOLD.getColor();
        BRACKETS = PluginColor.GRAY.getColor();
        UNDERSCORE = PluginColor.DARK_PURPLE.getColor();
        HEARTS = PluginColor.RED.getColor();

        FEEDBACK_MSG = PluginColor.LIGHTEST_BLUE.getColor();
        FEEDBACK_MSG_ACCENT = PluginColor.LIGHT_BLUE.getColor();

        INFO_MSG = PluginColor.GOLD.getColor();
        INFO_MSG_ACCENT_DARKEST = PluginColor.MEDIUM_GOLD.getColor();
        INFO_MSG_ACCENT_MEDIUM = PluginColor.LIGHT_GOLD.getColor();
        INFO_MSG_ACCENT_LIGHTEST = PluginColor.LIGHTEST_BLUE.getColor();

        MSG_HOVER = PluginColor.LIGHTEST_BLUE.getColor();
        MSG_CLICKED = PluginColor.LIGHT_PURPLE.getColor();
    }

    @Contract("_ -> new")
    protected @NotNull
    TextComponent miniMessageToComponent(String input) {
        return text()
                .append(MiniMessage.miniMessage().deserialize(input))
                .build();
    }

    public boolean isConsoleFactory() {
        return false;
    }

    public TextComponent getExampleName() {
        return text("Artemis_the_gr8").color(FEEDBACK_MSG);
    }

    /**
     * Returns [PlayerStats].
     */
    public TextComponent pluginPrefix() {
        return Component.empty();
    }

    /**
     * Returns [PlayerStats] surrounded by underscores on both sides.
     */
    public TextComponent pluginPrefixAsTitle() {
        return Component.empty();
    }

    /**
     * Returns a TextComponent with the input String as content, with color Gray
     * and decoration Italic.
     */
    public TextComponent subTitle(String content) {
        return text(content).color(BRACKETS).decorate(TextDecoration.ITALIC);
    }

    /**
     * Returns a TextComponents in the style of a default plugin message, with
     * color Medium_Blue.
     */
    public TextComponent message() {
        return text().color(FEEDBACK_MSG).build();
    }

    public TextComponent messageAccent() {
        return text().color(FEEDBACK_MSG_ACCENT).build();
    }

    public TextComponent infoMessageAccent() {
        return text().color(INFO_MSG_ACCENT_MEDIUM).build();
    }

    public TextComponent title(String content, Target target) {
        return getComponent(content,
                getColorFromString(config.getTitleDecoration(target, false)),
                getStyleFromString(config.getTitleDecoration(target, true)));
    }

    public TextComponent titleNumber(int number) {
        return getComponent(number + "",
                getColorFromString(config.getTitleNumberDecoration(false)),
                getStyleFromString(config.getTitleNumberDecoration(true)));
    }

    public TextComponent rankNumber(int number) {
        return getComponent(number + ".",
                getColorFromString(config.getRankNumberDecoration(false)),
                getStyleFromString(config.getRankNumberDecoration(true)));
    }

    public TextComponent dots(String dots) {
        return getComponent(dots,
                getColorFromString(config.getDotsDecoration(false)),
                getStyleFromString(config.getDotsDecoration(true)));
    }

    public TextComponent serverName(String serverName) {
        TextComponent colon = text(":").color(getColorFromString(config.getServerNameDecoration(false)));
        return getComponent(serverName,
                getColorFromString(config.getServerNameDecoration(false)),
                getStyleFromString(config.getServerNameDecoration(true)))
                .append(colon);
    }

    public TextComponent playerName(String playerName, Target target) {
        return getComponent(playerName,
                getColorFromString(config.getPlayerNameDecoration(target, false)),
                getStyleFromString(config.getPlayerNameDecoration(target, true)));
    }

    public TextComponent sharerName(String sharerName) {
        return getComponent(sharerName,
                getColorFromString(config.getSharerNameDecoration(false)),
                getStyleFromString(config.getSharerNameDecoration(true)));
    }

    public TextComponent shareButton(int shareCode) {
        return surroundWithBrackets(
                text("Share")
                        .color(FEEDBACK_MSG_ACCENT)
                        .clickEvent(ClickEvent.runCommand("/statshare " + shareCode))
                        .hoverEvent(HoverEvent.showText(text("Click here to share this statistic in chat!")
                                .color(INFO_MSG_ACCENT_MEDIUM))));
    }

    public TextComponent sharedByMessage(Component playerName) {
        return surroundWithBrackets(
                text().append(
                        getComponent("Shared by",
                                getColorFromString(config.getSharedByTextDecoration(false)),
                                getStyleFromString(config.getSharedByTextDecoration(true))))
                        .append(space())
                        .append(playerName)
                        .build());
    }

    public TextComponent statResultInHoverText(TextComponent statResult) {
        return surroundWithBrackets(
                text().append(text("Hover Here")
                        .color(MSG_CLICKED)
                        .decorate(TextDecoration.ITALIC)
                        .hoverEvent(HoverEvent.showText(statResult)))
                        .build());
    }

    /**
     * @param prettyStatName a statName with underscores removed and each word
     * capitalized
     * @param prettySubStatName if present, a subStatName with underscores
     * removed and each word capitalized
     */
    public TextComponent statAndSubStatName(String prettyStatName, @Nullable String prettySubStatName, Target target) {
        TextComponent.Builder totalStatNameBuilder = getComponentBuilder(prettyStatName,
                getColorFromString(config.getStatNameDecoration(target, false)),
                getStyleFromString(config.getStatNameDecoration(target, true)));
        TextComponent subStat = subStatName(prettySubStatName, target);

        if (!subStat.equals(Component.empty())) {
            totalStatNameBuilder
                    .append(space().decorations(TextDecoration.NAMES.values(), false))
                    .append(subStatName(prettySubStatName, target));
        }
        return totalStatNameBuilder.build();
    }

    /**
     * Returns a TextComponent with TranslatableComponent as a child.
     *
     */
    public TextComponent statAndSubStatNameTranslatable(String statKey, @Nullable String subStatKey, Target target) {
        TextComponent.Builder totalStatNameBuilder = getComponentBuilder(null,
                getColorFromString(config.getStatNameDecoration(target, false)),
                getStyleFromString(config.getStatNameDecoration(target, true)));

        TextComponent subStat = subStatNameTranslatable(subStatKey, target);
        if (LanguageKeyHandler.isNormalKeyForKillEntity(statKey)) {
            return totalStatNameBuilder.append(killEntityBuilder(subStat)).build();
        } else if (LanguageKeyHandler.isNormalKeyForEntityKilledBy(statKey)) {
            return totalStatNameBuilder.append(entityKilledByBuilder(subStat)).build();
        } else {
            totalStatNameBuilder.append(translatable().key(statKey));
            if (!subStat.equals(Component.empty())) {
                totalStatNameBuilder.append(
                        space().decorations(TextDecoration.NAMES.values(), false)
                                .append(subStat));
            }
            return totalStatNameBuilder.build();
        }
    }

    public TextComponent statNumber(String prettyNumber, Target target) {
        return getComponent(prettyNumber,
                getColorFromString(config.getStatNumberDecoration(target, false)),
                getStyleFromString(config.getStatNumberDecoration(target, true)));
    }

    public TextComponent timeNumber(String prettyNumber, Target target) {
        return statNumber(prettyNumber, target);
    }

    public TextComponent timeNumberWithHoverText(String mainNumber, String hoverNumber, Target target) {
        return statNumberWithHoverText(mainNumber, hoverNumber, null, null, null, target);
    }

    public TextComponent damageNumber(String prettyNumber, Target target) {
        return statNumber(prettyNumber, target);
    }

    public TextComponent damageNumberWithHoverText(String mainNumber, String hoverNumber, String hoverUnitName, Target target) {
        return statNumberWithHoverText(mainNumber, hoverNumber, hoverUnitName, null, null, target);
    }

    public TextComponent damageNumberWithHeartUnitInHoverText(String mainNumber, String hoverNumber, Target target) {
        return statNumberWithHoverText(mainNumber, hoverNumber, null, null, heart(), target);
    }

    public TextComponent distanceNumber(String prettyNumber, Target target) {
        return statNumber(prettyNumber, target);
    }

    public TextComponent distanceNumberWithHoverText(String mainNumber, String hoverNumber, String hoverUnitName, Target target) {
        return statNumberWithHoverText(mainNumber, hoverNumber, hoverUnitName, null, null, target);
    }

    public TextComponent distanceNumberWithTranslatableHoverText(String mainNumber, String hoverNumber, String hoverUnitKey, Target target) {
        return statNumberWithHoverText(mainNumber, hoverNumber, null, hoverUnitKey, null, target);
    }

    public TextComponent statUnit(String unitName, Target target) {
        TextComponent statUnit = getComponentBuilder(unitName,
                getColorFromString(config.getSubStatNameDecoration(target, false)),
                getStyleFromString(config.getSubStatNameDecoration(target, true)))
                .build();
        return surroundWithBrackets(statUnit);
    }

    public TextComponent statUnitTranslatable(String unitKey, Target target) {
        TextComponent statUnit = getComponentBuilder(null,
                getColorFromString(config.getSubStatNameDecoration(target, false)),
                getStyleFromString(config.getSubStatNameDecoration(target, true)))
                .append(translatable()
                        .key(unitKey))
                .build();
        return surroundWithBrackets(statUnit);
    }

    public TextComponent heart() {
        return text()
                .content(String.valueOf('\u2764'))
                .color(HEARTS)
                .build();
    }

    public TextComponent heartBetweenBrackets() {
        return surroundWithBrackets(heart());
    }

    public TextComponent heartBetweenBracketsWithHoverText() {
        TextComponent heart = heart()
                .toBuilder()
                .hoverEvent(HoverEvent.showText(
                        text(Unit.HEART.getLabel())
                                .color(INFO_MSG_ACCENT_MEDIUM)))
                .build();
        return surroundWithBrackets(heart);
    }

    public TextComponent arrow() {
        return text("    →").color(INFO_MSG);  //4 spaces, alt + 26
    }

    public TextComponent bulletPoint() {
        return text("    •").color(INFO_MSG); //4 spaces, alt + 7
    }

    public TextComponent bulletPointIndented() {
        return text("        •").color(INFO_MSG); //8 spaces, alt + 7
    }

    /**
     * Returns a TextComponent for the subStatName, or an empty component.
     */
    private TextComponent subStatName(@Nullable String prettySubStatName, Target target) {
        if (prettySubStatName == null) {
            return Component.empty();
        } else {
            return getComponentBuilder(null,
                    getColorFromString(config.getSubStatNameDecoration(target, false)),
                    getStyleFromString(config.getSubStatNameDecoration(target, true)))
                    .append(text("("))
                    .append(text(prettySubStatName))
                    .append(text(")"))
                    .build();
        }
    }

    /**
     * Returns a TranslatableComponent for the subStatName, or an empty
     * component.
     */
    private TextComponent subStatNameTranslatable(@Nullable String subStatKey, Target target) {
        if (subStatKey != null) {
            return getComponentBuilder(null,
                    getColorFromString(config.getSubStatNameDecoration(target, false)),
                    getStyleFromString(config.getSubStatNameDecoration(target, true)))
                    .append(text("("))
                    .append(translatable()
                            .key(subStatKey))
                    .append(text(")"))
                    .build();
        }
        return Component.empty();
    }

    /**
     * Construct a custom translation for kill_entity with the language key for
     * commands.kill.success.single ("Killed %s").
     *
     * @return a TranslatableComponent Builder with the subStat Component as
     * args.
     */
    private @NotNull
    TranslatableComponent.Builder killEntityBuilder(@NotNull TextComponent subStat) {
        return translatable()
                .key(LanguageKeyHandler.getCustomKeyForKillEntity()) //"Killed %s"
                .args(subStat);
    }

    /**
     * Construct a custom translation for entity_killed_by with the language
     * keys for stat.minecraft.deaths ("Number of Deaths") and book.byAuthor
     * ("by %s").
     *
     * @return a TranslatableComponent Builder with stat.minecraft.deaths as
     * key, with a ChildComponent with book.byAuthor as key and the subStat
     * Component as args.
     */
    private @NotNull
    TranslatableComponent.Builder entityKilledByBuilder(@NotNull TextComponent subStat) {
        return translatable()
                .key(LanguageKeyHandler.getCustomKeyForEntityKilledBy()) //"Number of Deaths"
                .append(space())
                .append(translatable()
                        .key(LanguageKeyHandler.getCustomKeyForEntityKilledByArg()) //"by %s"
                        .args(subStat));
    }

    private @NotNull
    TextComponent statNumberWithHoverText(String mainNumber, String hoverNumber,
            @Nullable String hoverUnitName,
            @Nullable String hoverUnitKey,
            @Nullable TextComponent heartComponent, Target target) {

        TextColor baseColor = getColorFromString(config.getStatNumberDecoration(target, false));
        TextDecoration style = getStyleFromString(config.getStatNumberDecoration(target, true));

        TextComponent.Builder hoverText = getComponentBuilder(hoverNumber, getLighterColor(baseColor), style);
        if (heartComponent != null) {
            hoverText.append(space())
                    .append(heartComponent);
        } else if (hoverUnitKey != null) {
            hoverText.append(space())
                    .append(translatable().key(hoverUnitKey));
        } else if (hoverUnitName != null) {
            hoverText.append(space())
                    .append(text(hoverUnitName));
        }
        return getComponent(mainNumber, baseColor, style).hoverEvent(HoverEvent.showText(hoverText));
    }

    private @NotNull
    TextComponent surroundWithBrackets(TextComponent component) {
        return getComponent(null, BRACKETS, null)
                .append(text("["))
                .append(component)
                .append(text("]"));
    }

    protected TextComponent getComponent(String content, @NotNull TextColor color, @Nullable TextDecoration style) {
        return getComponentBuilder(content, color, style).build();
    }

    protected TextComponent.Builder getComponentBuilder(@Nullable String content, TextColor color, @Nullable TextDecoration style) {
        TextComponent.Builder builder = text()
                .decorations(TextDecoration.NAMES.values(), false)
                .color(color);
        if (content != null) {
            builder.append(text(content));
        }
        if (style != null) {
            builder.decorate(style);
        }
        return builder;
    }

    private TextColor getColorFromString(String configString) {
        if (configString != null) {
            try {
                if (configString.contains("#")) {
                    return getHexColor(configString);
                } else {
                    return getTextColorByName(configString);
                }
            } catch (IllegalArgumentException | NullPointerException exception) {
                Bukkit.getLogger().warning(exception.toString());
            }
        }
        return null;
    }

    protected TextColor getHexColor(String hexColor) {
        return TextColor.fromHexString(hexColor);
    }

    private TextColor getTextColorByName(String textColor) {
        Index<String, NamedTextColor> names = NamedTextColor.NAMES;
        return names.value(textColor);
    }

    private @NotNull
    TextColor getLighterColor(@NotNull TextColor color) {
        float multiplier = (float) ((100 - config.getHoverTextAmountLighter()) / 100.0);
        HSVLike oldColor = HSVLike.fromRGB(color.red(), color.green(), color.blue());
        HSVLike newColor = HSVLike.hsvLike(oldColor.h(), oldColor.s() * multiplier, oldColor.v());
        return TextColor.color(newColor);
    }

    private @Nullable
    TextDecoration getStyleFromString(@NotNull String configString) {
        if (configString.equalsIgnoreCase("none")) {
            return null;
        } else if (configString.equalsIgnoreCase("magic")) {
            return TextDecoration.OBFUSCATED;
        } else {
            Index<String, TextDecoration> styles = TextDecoration.NAMES;
            return styles.value(configString);
        }
    }

    /**
     * A simple separator component.
     */
    public TextComponent separator(TextColor color, String content) {
        return text(content).color(color);
    }
}
