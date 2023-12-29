package net.highwayfrogs.editor.utils;

import lombok.Getter;

/**
 * This utility class with a counter that can be incremented or decremented.
 * The counter will throw an Exception if decremented below zero.
 * Created by Kneesnap on 12/28/2023.
 */
@Getter
public class IntegerCounter {
    private int counter;

    /**
     * The counter is considered active if the value is above zero.
     */
    public boolean isActive() {
        return this.counter > 0;
    }

    /**
     * Increments the counter.
     */
    public void increment() {
        this.counter++;
    }

    /**
     * Decrements the counter.
     * If the counter decrements below zero, an {@code IllegalStateException} will be thrown.
     * @return true if the counter is at zero, false if the counter is greater than zero.
     */
    public boolean decrement() {
        if (--this.counter == 0) {
            return true;
        } else if (this.counter < 0) {
            this.counter = 0;
            throw new IllegalStateException("startBulkRemovals() was called fewer times than endBulkRemovals() was called.");
        }

        return false;
    }
}
