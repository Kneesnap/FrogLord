package net.highwayfrogs.editor.file.config;

import lombok.Getter;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.io.InputStream;
import java.util.*;
import java.util.function.Function;

/**
 * Represents a Frogger config file.
 * Created by Kneesnap on 9/30/2018.
 */
@Getter
public class Config {
    private final String name;
    private final int layer;
    private final Map<String, String> values = new HashMap<>();
    private final Map<String, Config> children = new HashMap<>();
    private final List<String> text = new ArrayList<>();
    private final List<Config> orderedChildren = new ArrayList<>();

    public static final String VALUE_SPLIT = "=";
    public static final String EMPTY_LINE = "``";
    public static final String COMMENT_SPLIT = "#";
    public static final String CHILD_OPEN_TAG = "[";
    public static final String CHILD_CLOSE_TAG = "]";

    public Config(InputStream stream) {
        this(FileUtils.readLinesFromStream(stream));
    }

    public Config(InputStream stream, String name) {
        this(FileUtils.readLinesFromStream(stream), name, 0);
    }

    public Config(List<String> lines) {
        this(lines, null, 0);
    }

    private Config(List<String> lines, String name, int layer) {
        this.name = name;
        this.layer = layer;
        this.load(lines);
    }

    private void load(List<String> lines) {
        List<String> childLines = new ArrayList<>();
        boolean readingText = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = stripLine(lines.get(i));
            if (line.isEmpty())
                continue; // Skip empty lines.

            if (line.equalsIgnoreCase(EMPTY_LINE)) // If we want an actual empty line to be parsed. Since normal empty lines are skipped, we have to have this as an option.
                line = "";

            if (line.startsWith(CHILD_OPEN_TAG) && line.endsWith(CHILD_CLOSE_TAG)) { // Defining a child-config.
                int newLayer = getConfigLayer(line);
                String childName = line.substring(newLayer, line.length() - newLayer);
                int expectedLayer = this.layer + 1;
                if (newLayer != expectedLayer)
                    throw new RuntimeException("'" + line + "' is not the right layer. (New Layer: " + newLayer + ", Expected Layer: " + expectedLayer + ")");

                while (++i < lines.size()) {
                    String childLine = stripLine(lines.get(i));
                    if (childLine.startsWith(CHILD_OPEN_TAG) && childLine.endsWith(CHILD_CLOSE_TAG)) {
                        int childLayer = getConfigLayer(childLine);
                        if (childLayer > expectedLayer) {
                            childLines.add(childLine);
                        } else { // Exit, this is a new child layer for the current level or previous level.
                            i--;
                            break;
                        }
                    } else {
                        childLines.add(childLine);
                    }
                }

                Config childConfig = new Config(childLines, childName, newLayer);
                this.orderedChildren.add(childConfig);
                this.children.put(childName, childConfig);
                childLines.clear();
                continue;
            }

            String name = line.split(VALUE_SPLIT)[0];
            if (readingText || !line.contains(VALUE_SPLIT) || name.contains(" ")) { // Either we're already reading text, or it's time to move to text.
                readingText = true;
                getText().add(line);
                continue;
            }

            // Add key-value pairs, if the line is not empty.
            if (this.values.containsKey(name))
                System.out.println("Config Overwriting Key: " + name);
            this.values.put(name, line.substring(line.indexOf(VALUE_SPLIT) + 1));
        }
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
        if (!has(keyName))
            throw new RuntimeException("Config does not have key '" + keyName + "'.");
        return values.get(keyName);
    }

    /**
     * Get a config value by its key pair.
     * @param keyName The name of the key.
     * @return value
     */
    public String getString(String keyName, String fallback) {
        return has(keyName) ? getString(keyName) : fallback;
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
     * Get a boolean value.
     * @param keyName The name of the key.
     * @return booleanValue
     */
    public boolean getBoolean(String keyName, boolean defaultValue) {
        return has(keyName) ? getString(keyName).equalsIgnoreCase("true") : defaultValue;
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
     * Get an integer array from the config value.
     * @param keyName The name of the key.
     * @return intArray
     */
    public int[] getIntArray(String keyName) {
        return get(keyName, str -> {
            String[] split = str.split(",");
            int[] results = new int[split.length];
            for (int i = 0; i < results.length; i++)
                results[i] = Integer.parseInt(split[i]);

            return results;
        });
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
     * Get a long config value.
     * @param keyName The name of the key.
     * @return longValue
     */
    public long getLong(String keyName, long fallback) {
        return has(keyName) ? get(keyName, Long::decode) : fallback;
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
            Utils.handleError(null, e, false, "Failed to parse '%s' as %s's type.", value, keyName);
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

    private static int getConfigLayer(String childLine) {
        int openCount = 0;
        for (int j = 0; j < childLine.length(); j++) {
            if (childLine.charAt(j) != CHILD_OPEN_TAG.charAt(0))
                break;
            openCount++;
        }

        int closeCount = 0;
        for (int j = childLine.length() - 1; j >= 0; j--) {
            if (childLine.charAt(j) != CHILD_CLOSE_TAG.charAt(0))
                break;
            closeCount++;
        }

        if (openCount != closeCount)
            throw new RuntimeException("Config child identifier '" + childLine + "' has unbalanced brackets! [Open: " + openCount + ", Close: " + closeCount + "]");

        return openCount;
    }

    private static String stripLine(String line) {
        String[] split = line.split(COMMENT_SPLIT);
        if (split.length == 0)
            return "";

        return split[0].replaceAll("(\\s+)$", ""); // Remove comments.
    }
}