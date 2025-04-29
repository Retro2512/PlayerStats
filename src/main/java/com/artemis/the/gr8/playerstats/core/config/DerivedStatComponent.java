package com.artemis.the.gr8.playerstats.core.config;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a single component statistic used in calculating a derived
 * statistic.
 *
 * @param alias The alias of the base {@link ApprovedStat} this component refers
 * to.
 * @param operation The arithmetic operation ('+', '-', '*') to apply with this
 * component's value.
 */
public record DerivedStatComponent(@NotNull
        String alias, char operation) {

    /**
     * Checks if the operation character is valid (+, -, *).
     *
     * @return true if the operation is valid, false otherwise.
     */
    public boolean isValidOperation() {
        return operation == '+' || operation == '-' || operation == '*';
    }
}
