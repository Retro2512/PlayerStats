package com.artemis.the.gr8.playerstats.core.msg.msgutils;

import java.text.DecimalFormat;

import org.jetbrains.annotations.NotNull;

import com.artemis.the.gr8.playerstats.api.StatNumberFormatter;
import com.artemis.the.gr8.playerstats.api.enums.Unit;

/**
 * A utility class that formats statistic numbers into something more readable.
 * It transforms numbers of {@link Unit.Type} Time, Damage, and Distance into
 * numbers that are easier to understand (for example: from ticks to hours) and
 * adds commas to break up large numbers.
 */
public final class NumberFormatter implements StatNumberFormatter {

    private final DecimalFormat format;
    private final DecimalFormat distanceFormat = new DecimalFormat("#,##0.0");
    private final DecimalFormat hoursFormat = new DecimalFormat("#,##0.0");

    public NumberFormatter() {
        format = new DecimalFormat();
        format.setGroupingUsed(true);
        format.setGroupingSize(3);
    }

    /**
     * Adds commas in groups of 3.
     */
    @Override
    public @NotNull
    String formatDefaultNumber(long number) {
        return format.format(number);
    }

    /**
     * The unit of damage-based statistics is half a heart by default. This
     * method turns the number into hearts.
     */
    @Override
    public @NotNull
    String formatDamageNumber(long number, @NotNull Unit statUnit) {  //7 statistics
        if (statUnit == Unit.HEART) {
            return format.format(Math.round(number / 2.0));
        } else {
            return format.format(number);
        }
    }

    /**
     * The unit of distance-based statistics is cm by default. This method turns
     * it into blocks by default, and turns it into km or leaves it as cm
     * otherwise, depending on the config settings.
     */
    @Override
    public @NotNull
    String formatDistanceNumber(long number, @NotNull Unit statUnit) {  //15 statistics
        switch (statUnit) {
            case CM -> {
                return format.format(number);
            }
            case MILE -> {
                return format.format(Math.round(number / 160934.4));  //to get from CM to Miles
            }
            case KM -> {
                return format.format(Math.round(number / 100000.0));  //divide by 100 to get M, divide by 1000 to get KM
            }
            default -> {
                return format.format(Math.round(number / 100.0));
            }
        }
    }

    /**
     * The unit of time-based statistics is ticks by default.
     *
     * @return a String with the form "1D 2H 3M 4S" (depending on the Unit range
     * selected)
     */
    @Override
    public String formatTimeNumber(long number, Unit biggestUnit, Unit smallestUnit) {  //5 statistics
        if (number <= 0) {
            return "-";
        }
        if (biggestUnit == Unit.TICK && smallestUnit == Unit.TICK || biggestUnit == Unit.NUMBER || smallestUnit == Unit.NUMBER) {
            return format.format(number);
        }

        Unit currUnit = biggestUnit;
        int leftoverSeconds = (int) Math.round(number / 20.0);
        StringBuilder output = new StringBuilder();

        while (currUnit != null) {
            //Define amount of units
            int amount;

            //Current unit is equal to the smallest unit, in this case round the remainder
            if (currUnit == smallestUnit) {
                amount = (int) Math.round(leftoverSeconds / currUnit.getSeconds());
            } //Leftover amount of seconds is greater than current unit, we can extract x units
            else if (leftoverSeconds >= currUnit.getSeconds()) {
                amount = (int) Math.floor(leftoverSeconds / currUnit.getSeconds());
            } //We did not have enough leftover to fill a unit
            else {
                if (output.toString().length() != 0) {
                    output.append(" 0").append(currUnit.getShortLabel());
                }
                currUnit = currUnit.getSmallerUnit(1);
                continue;
            }

            //Calculate new leftover
            leftoverSeconds = leftoverSeconds - (int) (amount * currUnit.getSeconds());

            //Append new values
            if (output.toString().length() != 0) {
                output.append(" ");
            }
            output.append(amount).append(currUnit.getShortLabel());

            //Check if we need to break
            if (currUnit == smallestUnit || leftoverSeconds == 0) {
                break;
            }

            //Lower current unit by 1
            currUnit = currUnit.getSmallerUnit(1);
        }

        return output.toString();
    }

    /**
     * Formats a distance given in CM into blocks (meters) with one decimal
     * place, adding commas for thousands separators and appending " blocks".
     *
     * @param numberInCm The distance in centimeters.
     * @return Formatted string (e.g., "1,234.5 blocks").
     */
    public @NotNull
    String formatDistanceInBlocks(long numberInCm) {
        double blocks = numberInCm / 100.0;
        return distanceFormat.format(blocks) + " blocks";
    }

    /**
     * Formats playtime given in ticks into hours with one decimal place, adding
     * commas for thousands separators and appending " hours".
     *
     * @param ticks The playtime in server ticks.
     * @return Formatted string (e.g., "1,234.5 hours").
     */
    public @NotNull
    String formatPlaytimeHours(long ticks) {
        if (ticks < 0) {
            return "0.0 hours"; // Handle potential negative values gracefully

        }
        double hours = ticks / 72000.0; // 20 ticks/sec * 3600 sec/hour
        return hoursFormat.format(hours) + " hours";
    }
}
