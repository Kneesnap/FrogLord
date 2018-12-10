package net.highwayfrogs.editor.file.config;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a Frogger config file.
 * Created by Kneesnap on 9/30/2018.
 */
@Getter
public class Config {
    private Map<String, String> values = new HashMap<>();
    private Map<String, Config> children = new HashMap<>();
    @Getter private List<String> text = new ArrayList<>();

    public static final String VALUE_SPLIT = "=";
    public static final String EMPTY_LINE = "``";
    public static final String COMMENT_SPLIT = "#";
    public static final String CHILD_OPEN_TAG = "[";
    public static final String CHILD_CLOSE_TAG = "]";

    public Config(InputStream stream) throws IOException {
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader bufferedReader = new BufferedReader(reader);
        List<String> lines = bufferedReader.lines().collect(Collectors.toList());
        reader.close();
        bufferedReader.close();

        this.load(lines);
    }

    public Config(List<String> lines) {
        this.load(lines);
    }

    private void load(List<String> lines) {
        String childName = null;
        List<String> childLines = new ArrayList<>();
        boolean readingText = false;

        for (String line : lines) {
            line = line.split(COMMENT_SPLIT)[0].trim(); // Remove comments.
            if (line.isEmpty())
                continue; // Skip empty lines.

            if (line.equalsIgnoreCase(EMPTY_LINE)) // If we want an actual empty line to be parsed. Since normal empty lines are skipped, we have to have this as an option.
                line = "";

            if (line.startsWith(CHILD_OPEN_TAG) && line.endsWith(CHILD_CLOSE_TAG)) { // Defining a child-config.
                if (childName != null) // Done with the current child, add it.
                    children.put(childName, new Config(childLines));

                childLines.clear();
                childName = line.substring(1, line.length() - 1);
                continue;
            }

            if (childName != null) { // Add to child.
                childLines.add(line);
                continue;
            }

            String name = line.split(VALUE_SPLIT)[0];
            if (readingText || !line.contains(VALUE_SPLIT) || name.contains(" ")) { // Either we're already reading text, or it's time to move to text.
                readingText = true;
                getText().add(line);
                continue;
            }

            // Add key-value pairs, if the line is not empty.
            values.put(name, line.substring(line.indexOf(VALUE_SPLIT) + 1));
        }

        if (childName != null) // Add the final child config, if there is one.
            children.put(childName, new Config(childLines));

        this.onLoad();
    }

    /**
     * Called when this config is loaded.
     */
    protected void onLoad() {

    }

    /**
     * Do we have a given key-value pair?
     * @param keyName The key to check for.
     * @return foundPair
     */
    public boolean has(String keyName) {
        return values.containsKey(keyName);
    }

    /**
     * Get a config value by its key pair.
     * @param keyName The name of the key.
     * @return value
     */
    public String getString(String keyName) {
        Utils.verify(has(keyName), "Config does not have key '%s'.", keyName);
        return values.get(keyName);
    }

    /**
     * Get a boolean value.
     * @param keyName The name of the key.
     * @return booleanValue
     */
    public boolean getBoolean(String keyName) {
        return has(keyName) && getString(keyName).equalsIgnoreCase("true");
    }

    /**
     * Get an enum config value.
     * @param keyName   The name of the key.
     * @param enumClass The class of the enum to get.
     * @return enumValue
     */
    public <E extends Enum<E>> E getEnum(String keyName, Class<E> enumClass) {
        return Enum.valueOf(enumClass, getString(keyName));
    }

    /**
     * Get an enum config value.
     * @param keyName     The name of the key.
     * @param defaultEnum The default enum value to return.
     * @return enumValue
     */
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnum(String keyName, E defaultEnum) {
        Utils.verify(defaultEnum != null, "Default Enum cannot be null.");
        return has(keyName) ? getEnum(keyName, (Class<E>) defaultEnum.getClass()) : defaultEnum;
    }

    /**
     * Get an integer config value.
     * @param keyName The name of the key.
     * @return intValue
     */
    public int getInt(String keyName) {
        return get(keyName, Integer::decode);
    }

    /**
     * Get an integer config value.
     * @param keyName  The name of the key.
     * @param fallback The value to return if not present.
     * @return intValue
     */
    public int getInt(String keyName, int fallback) {
        return has(keyName) ? getInt(keyName) : fallback;
    }

    /**
     * Get a long config value.
     * @param keyName The name of the key.
     * @return longValue
     */
    public long getLong(String keyName) {
        return get(keyName, Long::decode);
    }

    /**
     * Gets a value from its string. Throws an error if the value does not exist.
     * @param keyName    The name of the value's key.
     * @param fromString How to get the object we want from its string.
     * @return value
     */
    public <T> T get(String keyName, Function<String, T> fromString) {
        String value = getString(keyName);
        try {
            return fromString.apply(value);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to parse '" + value + "' as " + keyName + "'s type.");
        }

        return null;
    }

    /**
     * Gets a value from its key, or a fallback if the key is not present.
     * @param keyName    The key to get the value by.
     * @param fromString How to get the object value from a string.
     * @param fallback   The value to return if the key is not present.
     * @return value
     */
    public <T> T get(String keyName, Function<String, T> fromString, T fallback) {
        return has(keyName) ? get(keyName, fromString) : fallback;
    }

    /**
     * Return if there is any text attached to this config.
     * @return hasText
     */
    public boolean hasText() {
        return !getText().isEmpty();
    }

    /**
     * Check if we have a child config by a given name.
     * @param childName The name of the child to test for.
     * @return hasChild
     */
    public boolean hasChild(String childName) {
        return children.containsKey(childName);
    }

    /**
     * Get a config-child by its name.
     * @param childName The name of the config-child.
     * @return childConfig
     */
    public Config getChild(String childName) {
        Utils.verify(hasChild(childName), "Unknown child-name '%s'.", childName);
        return children.get(childName);
    }

    /**
     * Get a list of valid children by their keys.
     * @return children
     */
    public Set<String> getChildren() {
        return children.keySet();
    }

    /**
     * Get a set of all keys we keep track of.
     * @return keys
     */
    public Set<String> keySet() {
        return values.keySet();
    }
}
