package net.highwayfrogs.editor.utils.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents an integer count map.
 * Created by Kneesnap on 10/7/2025.
 */
@SuppressWarnings("Java8MapApi")
public class CountMap<T> {
    private final Map<T, IntegerCounter> countMap = new HashMap<>();

    /**
     * Clears the count map.
     */
    public void clear() {
        this.countMap.clear();
    }

    /**
     * Gets a set containing all tracked keys.
     */
    public Set<T> keySet() {
        return this.countMap.keySet();
    }

    /**
     * Tests if a key is found within the countMap.
     * @param key the key to test
     * @return true iff the key is found within the countMap
     */
    public boolean containsKey(T key) {
        return this.countMap.containsKey(key);
    }

    /**
     * Removes a key from the countMap.
     * @param key the key to remove
     * @return removedValue
     */
    public int remove(T key) {
        IntegerCounter counter = this.countMap.remove(key);
        return counter != null ? counter.getCounter() : 0;
    }

    /**
     * Sets the integer value for the key.
     * @param key the key which the value is associated with
     * @param newValue value to apply if there is no value associated with the key
     * @return value or fallback
     */
    public int set(T key, int newValue) {
        if (newValue == 0) {
            IntegerCounter counter = this.countMap.remove(key);
            return counter != null ? counter.getCounter() : 0;
        }

        IntegerCounter counter = this.countMap.get(key);
        int oldValue = counter != null ? counter.getCounter() : 0;
        if (counter == null)
            this.countMap.put(key, counter = new IntegerCounter());

        counter.set(newValue);
        return oldValue;
    }

    /**
     * Gets the integer value for the key, or if no integer value is tracked, zero will be returned.
     * @param key the key which the value is associated with
     * @return value or fallback
     */
    public int get(T key) {
        IntegerCounter counter = this.countMap.get(key);
        return counter != null ? counter.getCounter() : 0;
    }

    /**
     * Gets the integer value for the key, or if no integer value is tracked, a fallback value will be returned.
     * @param key the key which the value is associated with
     * @param fallbackValue the value to return if there is no value associated with the key
     * @return value or fallback
     */
    public int get(T key, int fallbackValue) {
        IntegerCounter counter = this.countMap.get(key);
        return counter != null ? counter.getCounter() : fallbackValue;
    }

    /**
     * Gets the integer value for the key, or throws an Exception if no value is found.
     * @param key the key which the value is associated with
     * @return value
     */
    public int getOrError(T key) {
        IntegerCounter counter = this.countMap.get(key);
        if (counter == null)
            throw new IllegalArgumentException("The key (" + key + ") had no integer value associated with it!");

        return counter.getCounter();
    }

    /**
     * Gets the value, and increments the tracked value afterward.
     * @param key the key to increment
     * @return the non-incremented value
     */
    public int getAndAdd(T key) {
        IntegerCounter counter = this.countMap.get(key);
        if (counter == null)
            this.countMap.put(key, counter = new IntegerCounter());

        int oldValue = counter.getCounter();
        counter.increment();
        return oldValue;
    }

    /**
     * Gets the value, and decrements the tracked value afterward.
     * @param key the key to decrement
     * @return the non-decremented value
     */
    public int getAndSubtract(T key) {
        IntegerCounter counter = this.countMap.get(key);
        if (counter == null)
            this.countMap.put(key, counter = new IntegerCounter());

        int oldValue = counter.getCounter();
        if (counter.decrement())
            this.countMap.remove(key, counter);
        return oldValue;
    }

    /**
     * Increments the tracked value and returns the incremented value.
     * @param key the key to increment
     * @return the incremented value
     */
    public int addAndGet(T key) {
        IntegerCounter counter = this.countMap.get(key);
        if (counter == null)
            this.countMap.put(key, counter = new IntegerCounter());

        return counter.increment();
    }

    /**
     * Decrements the value, and returns the decremented value.
     * @param key the key to decrement
     * @return the decremented value
     */
    public int subtractAndGet(T key) {
        IntegerCounter counter = this.countMap.get(key);
        if (counter == null)
            throw new IllegalArgumentException("Cannot decrement key (" + key + "), as it is not currently tracked.");

        if (counter.decrement())
            this.countMap.remove(key, counter);
        return counter.getCounter();
    }
}
