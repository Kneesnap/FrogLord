package net.highwayfrogs.editor.system;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.StringNode;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Configuration implementation converted from ModToolFramework.
 * A configuration file which can be used for storing data.
 *
 * The ModToolFramework configuration format is heavily inspired by ini in terms of syntax, but has some key differences that set it apart.
 * It's a tree structure, starting with a root "Config" node. (The term "Config" will be used interchangeably with "Config node" and "Section".)
 * Each node can contain these things.
 *
 * 1. A name.
 * 2. Key value pairs.
 * 3. Raw text lines.
 * 4. Child config nodes.
 *
 * Here is an example of a valid configuration.
 * [SectionName] # This declares the section. It would result in a section literally named "SectionName".
 * key1=value1 # Example key values. These values are of type string, but can be parsed to be basically anything. It's a breeze if you use static extension methods to allow for your own shared parsing logic.
 * key2=value2
 * This is an example line of text which would be considered a 'raw text line'.
 *
 * [[ChildSection]] # This is a new section, a child to the previous section. Note how there are two square brackets used here. This is to indicate that it is attached to the previous node, and not a node which is attached to the root.
 * # If you wanted to attach a child here, you'd create a section with three square brackets prefixing the section name.
 *
 * By default, any text is interpreted as being in the implicit root node. In other words, there is a default node which values will go into until a section is defined.
 *
 * Everything after '#' on a line of text will be ignored, as a comment. If you need to put '#' into a line of text or a value, escape it by typing '##' instead.
 * Created by Kneesnap on 4/25/2024.
 */
@SuppressWarnings("unused")
public class Config implements IBinarySerializable {
    private final Map<String, ConfigValueNode> keyValuePairs = new HashMap<>();
    private final List<String> orderedKeyValuePairs = new ArrayList<>();
    private final Map<String, List<Config>> childConfigsByName = new HashMap<>();
    private static final ConfigValueNode EMPTY_DEFAULT_NODE = new ConfigValueNode(null);
    public static final char COMMENT_CHARACTER = '#';
    public static final String COMMENT_CHARACTER_STRING = String.valueOf(COMMENT_CHARACTER); // "#"
    public static final String ESCAPED_COMMENT_CHAR = COMMENT_CHARACTER_STRING + COMMENT_CHARACTER; // "##"

    public static final String DEFAULT_EXTENSION = "cfg";
    public static final BrowserFileType DEFAULT_FILE_TYPE = new BrowserFileType("FrogLord Config", DEFAULT_EXTENSION);

    /**
     * The name of this section. Should NOT contain '[' or ']'.
     */
    @Getter
    private String sectionName;

    /**
     * The comment (if there is one) which is included on the section definition.
     */
    @Getter
    @Setter
    private String sectionComment;

    /**
     * This is the config which holds this config as a child config, if there is one.
     */
    @Getter
    private Config parentConfig;

    /**
     * A list of all child configs, in order.
     * This gives the children attached to THIS config, and ONLY this config.
     * This is intentionally not a Dictionary, because we want to support multiple sections with the same name. However, you can create a dictionary with CreateChildDictionary().
     */
    private final List<Config> internalChildConfigs = new ArrayList<>();

    /**
     * Get the child config nodes attached to this node.
     */
    public List<Config> getChildConfigNodes() {
        return Collections.unmodifiableList(this.internalChildConfigs);
    }

    /**
     * Returns the key value pairs.
     * To modify the contents of this, use the functions for doing so. Iterating through this will only give properties for this config, not any parent configs.
     * @return immutableKeyValuePairs
     */
    public Map<String, ConfigValueNode> getKeyValuePairs() {
        return Collections.unmodifiableMap(this.keyValuePairs);
    }

    /**
     * Gets the internal text tracking, which can be safely modified.
     * This gives the text specific to THIS config, and ONLY this config.
     */
    @Getter
    private final List<ConfigValueNode> internalText = new ArrayList<>();

    public void setSectionName(String newSectionName) {
        if (newSectionName == null)
            throw new NullPointerException("newSectionName");
        if (newSectionName.equalsIgnoreCase(this.sectionName))
            return;

        // Remove from parent tracking.
        if (this.parentConfig != null) {
            List<Config> configs = this.parentConfig.childConfigsByName.get(this.sectionName);
            if (configs.remove(this) && configs.isEmpty())
                this.parentConfig.childConfigsByName.remove(this.sectionName, configs);
        }

        this.sectionName = newSectionName;

        // Add to parent tracking.
        if (this.parentConfig != null) {
            List<Config> configsByName = this.parentConfig.childConfigsByName.computeIfAbsent(this.sectionName, key -> new ArrayList<>());
            int foundIndex = Collections.binarySearch(configsByName, this, Comparator.comparingInt(this.parentConfig.internalChildConfigs::lastIndexOf));
            if (foundIndex < 0)
                configsByName.add(-(foundIndex + 1), this);
        }
    }

    /**
     * Gets the individual lines of text from a list of config value nodes.
     * @return textStringList
     */
    public List<String> getTextWithoutComments() {
        List<String> textList = new ArrayList<>(this.internalText.size());
        for (int i = 0; i < this.internalText.size(); i++) {
            ConfigValueNode node = this.internalText.get(i);
            String text = node.getAsStringLiteral();
            if (!StringUtils.isNullOrWhiteSpace(text))
                textList.add(text);
        }

        return textList;
    }

    /**
     * Gets the individual lines of text (with their comments) from a list of config value nodes.
     * @return textStringList
     */
    public List<String> getTextWithComments() {
        List<String> textList = new ArrayList<>(this.internalText.size());
        for (int i = 0; i < this.internalText.size(); i++) {
            ConfigValueNode node = this.internalText.get(i);
            String text = node.getTextWithComments();
            if (text != null)
                textList.add(text);
        }

        return textList;
    }

    /**
     * Is this Config a root config? (A root config is the configuration which parents any child configs. There is no parent to the root config.)
     */
    public boolean isRootNode() {
        return this.parentConfig == null;
    }

    /**
     * Gets the config root node.
     */
    public Config getRootNode() {
        Config temp = this;
        while (temp.getParentConfig() != null)
            temp = temp.getParentConfig();

        return temp;
    }

    private static final byte BINARY_FORMAT_VERSION = 0;

    public Config(String name) {
        this.sectionName = name;
    }

    @Override
    public void load(DataReader reader) {
        this.loadFromReader(reader, null, new ConfigSettings());
    }

    private void loadFromReader(DataReader reader, Config parentConfig, ConfigSettings loadSettings) {
        this.parentConfig = parentConfig;

        if (loadSettings.isHeaderPending()) {
            reader.verifyString("CFG");
            byte version = reader.readByte();
            if (version != BINARY_FORMAT_VERSION)
                throw new UnsupportedOperationException("Unsupported binary config version " + version + ".");

            loadSettings.setReadingCommentsEnabled(reader.readInt() != 0);
            loadSettings.setHeaderPending(false);
        }

        short sectionNameLength = reader.readUnsignedByteAsShort();
        this.sectionName = reader.readTerminatedString(sectionNameLength);
        if (loadSettings.isReadingCommentsEnabled()) {
            int sectionCommentLength = reader.readInt();
            this.sectionComment = reader.readTerminatedString(sectionCommentLength);
        }

        // Read key value pairs.
        int keyValueCount = reader.readInt();
        for (int i = 0; i < keyValueCount; i++) {
            short keyLength = reader.readUnsignedByteAsShort();
            String key = reader.readTerminatedString(keyLength);
            ConfigValueNode node = new ConfigValueNode();
            node.loadFromReader(reader, loadSettings);
            if (this.keyValuePairs.put(key, node) == null)
                this.orderedKeyValuePairs.add(key);
        }

        // Read Text.
        int textEntries = reader.readInt();
        for (int i = 0; i < textEntries; i++) {
            ConfigValueNode node = new ConfigValueNode();
            node.loadFromReader(reader, loadSettings);
            this.internalText.add(node);
        }

        // Read child configs.
        int childCount = reader.readInt();
        for (int i = 0; i < childCount; i++) {
            Config childConfig = new Config("$UnknownChildConfigNode");
            childConfig.loadFromReader(reader, this, loadSettings);
            addChildConfig(childConfig);
        }
    }

    @Override
    public void save(DataWriter writer) {
        saveToWriter(writer, new ConfigSettings());
    }

    /**
     * Adds a new child config.
     * @param childConfig the child config to add.
     */
    public void addChildConfig(Config childConfig) {
        if (childConfig == null)
            throw new NullPointerException("childConfig");
        if (childConfig == this)
            throw new IllegalArgumentException("Cannot attach child config to itself.");

        if (this.internalChildConfigs.contains(childConfig))
            return;

        if (childConfig.getParentConfig() == null) {
            childConfig.parentConfig = this;
        } else if (childConfig.getParentConfig() != this) {
            throw new IllegalArgumentException("The provided child config is already added to another Config somewhere.");
        }

        this.internalChildConfigs.add(childConfig);
        this.childConfigsByName.computeIfAbsent(childConfig.getSectionName(), key -> new ArrayList<>()).add(childConfig);
    }

    /**
     * Remove a child config.
     * @param childConfig the child config to remove
     * @return if the removal was successful
     */
    public boolean removeChildConfig(Config childConfig) {
        if (childConfig == null)
            throw new NullPointerException("childConfig");

        // Remove.
        if (!this.internalChildConfigs.remove(childConfig))
            return false;

        childConfig.parentConfig = null;

        // Stop tracking for the name.
        List<Config> childConfigs = this.childConfigsByName.get(childConfig.getSectionName());
        if (childConfigs != null && childConfigs.remove(childConfig) && childConfigs.isEmpty())
            this.childConfigsByName.remove(childConfig.getSectionName(), childConfigs);

        return true;
    }

    private void saveToWriter(DataWriter writer, ConfigSettings saveSettings) {
        if (saveSettings.isHeaderPending()) {
            writer.writeStringBytes("CFG");
            writer.writeByte(BINARY_FORMAT_VERSION);
            writer.writeInt(saveSettings.isReadingCommentsEnabled() ? 1 : 0);
            saveSettings.setHeaderPending(false);
        }

        // Write section name.
        writer.writeUnsignedByte((short) (this.sectionName != null ? this.sectionName.length() : 0));
        if (this.sectionName != null && this.sectionName.length() > 0)
            writer.writeStringBytes(this.sectionName);

        // Write comment.
        if (saveSettings.isReadingCommentsEnabled()) {
            writer.writeInt(this.sectionComment != null ? this.sectionComment.length() : 0);
            if (this.sectionComment != null && this.sectionComment.length() > 0)
                writer.writeStringBytes(this.sectionComment);
        }

        // Write key value pairs.
        writer.writeInt(this.keyValuePairs.size());
        for (String entryKey : this.orderedKeyValuePairs) {
            ConfigValueNode valueNode = this.keyValuePairs.get(entryKey);
            writer.writeUnsignedByte((short) (entryKey != null ? entryKey.length() : 0));
            writer.writeStringBytes(entryKey);
            valueNode.saveToWriter(writer, saveSettings);
        }

        // Write Text.
        writer.writeInt(this.internalText.size());
        for (int i = 0; i < this.internalText.size(); i++)
            this.internalText.get(i).saveToWriter(writer, saveSettings);

        // Write child configs.
        writer.writeInt(this.internalChildConfigs.size());
        for (int i = 0; i < this.internalChildConfigs.size(); i++)
            this.internalChildConfigs.get(i).saveToWriter(writer, saveSettings);
    }

    /**
     * Removes a key value pair with the given key name.
     * @param keyName The name of the key to remove.
     * @return removedKeyValuePair
     */
    public ConfigValueNode removeKeyValueNode(String keyName) {
        ConfigValueNode removedNode = this.keyValuePairs.remove(keyName);
        if (removedNode != null)
            this.orderedKeyValuePairs.remove(keyName);
        return removedNode;
    }

    /**
     * Creates or updates the value in a key value pair.
     * Applies the value to this config, not a super config with the value.
     * @param keyName The name of the key.
     * @param node The value to apply
     * @throws IllegalArgumentException thrown if an invalid value is supplied
     * @return oldKeyValueNode
     */
    public ConfigValueNode setKeyValueNode(String keyName, ConfigValueNode node) {
        if (node == null) {
            return this.removeKeyValueNode(keyName);
        }

        ConfigValueNode existingNode = this.keyValuePairs.get(keyName);
        if (existingNode != null) { // Get existing node.
            existingNode.setAsString(node.getAsString(), node.isSurroundByQuotes());
            if (!StringUtils.isNullOrWhiteSpace(node.getComment()) && StringUtils.isNullOrWhiteSpace(existingNode.getComment()))
                existingNode.setComment(node.getComment());
        } else { // Create new node.
            this.keyValuePairs.put(keyName, node.clone());
            this.orderedKeyValuePairs.add(keyName);
        }

        return existingNode;
    }

    /**
     * Gets the config value node for a given key. Creates it if it does not exist.
     * Applies the value to this config, not a super config with the value.
     * The main purpose of this method is to be used for settings. Ie: getOrCreateKeyValueNode("testValue").setValue().
     * @param keyName The name of the key to get the value for.
     * @return configValueNode
     */
    public ConfigValueNode getOrCreateKeyValueNode(String keyName) {
        ConfigValueNode valueNode = this.keyValuePairs.get(keyName);
        if (valueNode != null)
            return valueNode;

        ConfigValueNode newNode = new ConfigValueNode("$NO_VALUE_WAS_SET$");
        if (this.keyValuePairs.put(keyName, newNode) == null)
            this.orderedKeyValuePairs.add(keyName);
        return newNode;
    }

    /**
     * Check if this config contains a key value pair with a given key.
     * @param keyName The key to look for.
     * @return hasKeyValueNode
     */
    public boolean hasKeyValueNode(String keyName) {
        return this.keyValuePairs.get(keyName) != null;
    }

    /**
     * Gets a value from a key value pair in the config. Throws an error if the value is not found.
     * @param keyName The name of the key. (Case-sensitive)
     * @return valueNode
     */
    public ConfigValueNode getKeyValueNodeOrError(String keyName) {
        ConfigValueNode valueNode = this.keyValuePairs.get(keyName);
        if (valueNode == null)
            throw new IllegalStateException("'" + keyName + "' was not found in the config.");

        return valueNode;
    }

    /**
     * Gets a value from a key value pair in the config, returning null if the value is not found.
     * @param keyName The name of the key. (Case-sensitive)
     * @return valueNode
     */
    public ConfigValueNode getOptionalKeyValueNode(String keyName) {
        return this.keyValuePairs.get(keyName);
    }

    /**
     * Gets a value from a key value pair in the config, returning a non-null entry if the value is not found.
     * @param keyName The name of the key. (Case-sensitive)
     * @return valueNode
     */
    public ConfigValueNode getOrDefaultKeyValueNode(String keyName) {
        ConfigValueNode valueNode = this.keyValuePairs.get(keyName);
        return valueNode != null ? valueNode : EMPTY_DEFAULT_NODE;
    }

    /**
     * Gets a child config with the supplied name, case-insensitive.
     * Returns null if there is no child with that name.
     * @param name The name of the child section.
     * @return childConfig
     */
    public Config getChildConfigByName(String name) {
        List<Config> childConfigs = this.childConfigsByName.get(name);
        return childConfigs != null ? childConfigs.get(0) : null;
    }

    /**
     * Gets a child config with the supplied name, case-insensitive.
     * Creates a child config if none exist with that name.
     * @param name The name of the child section.
     * @return childConfig
     */
    public Config getOrCreateChildConfigByName(String name) {
        List<Config> childConfigs = this.childConfigsByName.computeIfAbsent(name, key -> new ArrayList<>());
        if (childConfigs.isEmpty()) {
            Config newConfig = new Config(name);
            addChildConfig(newConfig);
            return newConfig;
        } else {
            return childConfigs.get(0);
        }
    }

    /**
     * Gets the child configs with the supplied name, case-insensitive.
     * @param name The child config name to look for.
     * @return childConfigsWithName
     */
    public List<Config> getAllChildConfigsByName(String name) {
        List<Config> childConfigs = this.childConfigsByName.get(name);
        return childConfigs != null ? Collections.unmodifiableList(childConfigs) : Collections.emptyList();
    }

    /**
     * Creates a dictionary of child configs based on their name.
     * If there are duplicate names, the first section with the name will be used.
     * @return childConfigDictionary
     */
    public Map<String, List<Config>> getChildConfigDictionary() {
        return Collections.unmodifiableMap(this.childConfigsByName);
    }

    /**
     * Save the config to a text file.
     * @param outputFile the file to save the config (in text form) to
     */
    public void saveTextFile(File outputFile) {
        if (outputFile == null)
            throw new NullPointerException("outputFile");
        if (outputFile.exists() && !outputFile.isFile())
            throw new IllegalArgumentException("File '" + outputFile + "' is not a file!");

        File parentFile = outputFile.getParentFile();
        if (parentFile != null && !parentFile.exists())
            FileUtils.makeDirectory(parentFile);

        FileUtils.writeBytesToFile(getLogger(), outputFile, toString().getBytes(StandardCharsets.UTF_8), true);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (!isRootNode())
            writeSectionHeader(builder, "[", "]", this);
        toString(builder, "[", "]");
        return builder.toString();
    }

    private void toString(StringBuilder builder, String sectionStart, String sectionEnd) {
        int builderStartLength = builder.length();

        // Write key value pairs.
        for (String entryKey : this.orderedKeyValuePairs) {
            builder.append(escapeKey(entryKey));
            builder.append("=");
            builder.append(this.keyValuePairs.get(entryKey).getTextWithComments());

            builder.append(Constants.NEWLINE);
        }

        // Write Raw text.
        for (int i = 0; i < this.internalText.size(); i++) {
            ConfigValueNode line = this.internalText.get(i);
            builder.append(line.getTextWithComments()).append(Constants.NEWLINE);
        }

        // Empty line between sections.
        if (builder.length() > builderStartLength)
            builder.append(Constants.NEWLINE);

        // Write child sections in order.
        if (this.internalChildConfigs.size() > 0) {
            String newSectionStart = sectionStart + "[";
            String newSectionEnd = sectionEnd + "]";

            for (int i = 0; i < this.internalChildConfigs.size(); i++){
                Config child = this.internalChildConfigs.get(i);
                writeSectionHeader(builder, sectionStart, sectionEnd, child);
                child.toString(builder, newSectionStart, newSectionEnd);
            }
        }
    }

    private static void writeSectionHeader(StringBuilder builder, String sectionStart, String sectionEnd, Config node) {
        builder.append(sectionStart);
        builder.append(escapeComment(node.getSectionName()));

        builder.append(sectionEnd);
        if (!StringUtils.isNullOrWhiteSpace(node.getSectionComment())) {
            builder.append(ConfigValueNode.DEFAULT_COMMENT_SEPARATOR);
            builder.append(node.getSectionComment());
        }

        builder.append(Constants.NEWLINE);
    }

    /**
     * Comment char escaping applied to various parts of a config.
     * @param value The value to escape.
     * @return escapedValue
     */
    public static String escapeComment(String value) {
        if (value == null)
            return null;

        return value.replace(COMMENT_CHARACTER_STRING, ESCAPED_COMMENT_CHAR);
    }

    /**
     * Unescape the comment-char escaping applied to various parts of a config.
     * @param value The value to unescape.
     * @return unescapedValue
     */
    public static String unescapeComment(String value) {
        if (value == null)
            return null;

        return value.replace(ESCAPED_COMMENT_CHAR, COMMENT_CHARACTER_STRING);
    }

    /**
     * Escape the value string in a key-value pair.
     * @param value The value to escape.
     * @return escapedValue
     */
    public static String escapeValue(String value) {
        if ("null".equals(value))
            return null;

        return escapeComment(value);
    }

    /**
     * Unescape the value string in a key-value pair.
     * @param value The value to unescape.
     * @return unescapedValue
     */
    public static String unescapeValue(String value) {
        if (value == null || value.equals("null")) {
            return null;
        } else if (value.equals("\"null\"")) {
            return "null";
        }

        return unescapeComment(value);
    }

    /**
     * Escapes a key string.
     * @param key The key to escape
     * @return escapedKey
     */
    public static String escapeKey(String key) {
        return escapeComment(key)
                .replace(" ", "\\ ")
                .replace("=", "\\=")
                .replace("\\", "\\\\");
    }

    /**
     * Unescapes a key string.
     * @param key The key to unescape.
     * @return unescapedKey
     */
    public static String unescapeKey(String key) {
        return unescapeComment(key)
                .replace("\\ ", " ")
                .replace("\\=", "=")
                .replace("\\\\", "\\");
    }

    /**
     * Gets the root configuration node.
     * @param config The node whose root we want to find.
     * @throws NullPointerException Thrown if the config is null.
     * @return rootConfigNode
     */
    public static Config getRootConfig(Config config) {
        if (config == null)
            throw new NullPointerException("Could not get the root config for null!");

        Config temp = config;
        while (temp.getParentConfig() != null)
            temp = temp.getParentConfig();
        return temp;
    }

    /**
     * Reads a configuration from a text file.
     * @param targetFile The file to read the configuration from.
     * @param createIfMissing if the file is not found, an empty config will still be returned when this is true.
     * @throws IllegalConfigSyntaxException Thrown if invalid configuration syntax is found.
     * @return readConfig
     */
    public static Config loadConfigFromTextFile(File targetFile, boolean createIfMissing) {
        if (targetFile == null)
            throw new NullPointerException("targetFile");

        String configFileText;
        if (!targetFile.exists() || !targetFile.isFile()) {
            if (!createIfMissing)
                throw new IllegalArgumentException("The file '" + targetFile + "' does not exist.");

            configFileText = "";
        } else {
            configFileText = String.join(Constants.NEWLINE, FileUtils.readLinesFromFile(targetFile));
        }

        return loadConfigFromString(configFileText, FileUtils.stripExtension(targetFile.getName()));
    }

    /**
     * Loads a text config from the URL.
     * @param url the url to resolve the config at
     * @return loadedConfig
     */
    public static Config loadTextConfigFromURL(URL url) {
        String versionConfigName = FileUtils.getFileNameWithoutExtension(url);
        InputStream inputStream;
        try {
            inputStream = url.openStream();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load URL: '" + url + "'.", e);
        }

        return loadTextConfigFromInputStream(inputStream, versionConfigName);
    }

    /**
     * Loads a text config from the input stream.
     * @param inputStream the input stream to read from
     * @param configFileName the name of the config
     * @return loadedConfig
     */
    public static Config loadTextConfigFromInputStream(InputStream inputStream, String configFileName) {
        List<String> configLines = FileUtils.readLinesFromStream(inputStream);
        String configFileText = String.join(Constants.NEWLINE, configLines);
        return loadConfigFromString(configFileText, configFileName);
    }


    /**
     * Reads a configuration from a string.
     * @param configString The string to read the configuration from.
     * @param configFileName The name of the root config node.
     * @throws IllegalConfigSyntaxException Thrown if invalid configuration syntax is found.
     * @return readConfig
     */
    public static Config loadConfigFromString(String configString, String configFileName) {
        try (BadStringReader reader = new BadStringReader(new StringReader(configString))) {
            return loadConfigFromString(reader, 0, configFileName, null);
        }
    }

    private static Config loadConfigFromString(BadStringReader stringReader, int layer, String sectionName, String sectionComment) {
        Config config = new Config(sectionName);
        config.setSectionComment(sectionComment);

        while (true) {
            String line = stringReader.readLine();
            if (line == null)
                return config; // Reached end of file?

            if (StringUtils.isNullOrWhiteSpace(line))
                continue; // Empty lines are skipped.

            String trimmedLine = line.trim();

            if (trimmedLine.charAt(0) == '[') {
                String comment = "";
                String sectionLine = trimmedLine;

                // Find the position where the comment starts, if there is one. Makes sure to ignore escaped comment chars.
                int commentIndex = sectionLine.indexOf(COMMENT_CHARACTER);
                while (commentIndex >= 0 && sectionLine.length() > commentIndex) {
                    if (sectionLine.length() > commentIndex + 1 && sectionLine.charAt(commentIndex + 1) == COMMENT_CHARACTER) {
                        commentIndex = sectionLine.indexOf(COMMENT_CHARACTER, commentIndex + 2); // This can be an index larger than the string.
                        if (commentIndex + 2 > commentIndex)
                            commentIndex = -1;

                        continue;
                    }

                    comment = StringUtils.trimStart(sectionLine.substring(commentIndex + 1));
                    sectionLine = StringUtils.trimEnd(sectionLine.substring(0, commentIndex));
                }

                // This is the start of a new section.
                if (sectionLine.charAt(sectionLine.length() - 1) != ']')
                    throw new IllegalConfigSyntaxException("Invalid section identifier. (Line: '" + sectionLine + "')");

                int leftLayer = 0;
                for (int i = 0; i < sectionLine.length(); i++) {
                    if (sectionLine.charAt(i) == '[') {
                        leftLayer++;
                    } else {
                        break;
                    }
                }

                int rightLayer = 0;
                for (int i = sectionLine.length() - 1; i >= 0; i--) {
                    if (sectionLine.charAt(i) == ']') {
                        rightLayer++;
                    } else {
                        break;
                    }
                }

                // Verify layer.
                if (leftLayer != rightLayer)
                    throw new IllegalConfigSyntaxException("Section identifier had mismatched tags! (Line: '" + sectionLine + "', Left: " + leftLayer + ", Right: " + rightLayer + ")");

                int sectionLayer = leftLayer;
                if (sectionLayer > layer + 1)
                    throw new IllegalConfigSyntaxException("Section identifier has too many brackets to connect to its parent! (Line: '" + sectionLine + "', Parent: " + layer + ", New Layer: " + sectionLayer + ")");

                int nameLength = sectionLine.length() - rightLayer - leftLayer;
                if (nameLength == 0)
                    throw new IllegalConfigSyntaxException("Section identifier had no name! (Line: '" + sectionLine + "')");

                // Determine super-section and section name.
                String newSectionName = unescapeComment(sectionLine.substring(leftLayer, leftLayer + nameLength));

                // Create child config.
                if (sectionLayer == layer + 1) {
                    // Read for this config.
                    Config loadedChildConfig = loadConfigFromString(stringReader, sectionLayer, newSectionName, comment);
                    config.addChildConfig(loadedChildConfig);
                } else {
                    // Some other config earlier in the hierarchy (earlier layer) owns this.
                    stringReader.cachedNextLine = line; // A parent reader will need to access the current line.
                    return config;
                }

                continue;
            }

            // Read the comment, if there is one.
            String text = trimmedLine;

            String commentSeparator = ConfigValueNode.DEFAULT_COMMENT_SEPARATOR;
            String commentText = "";
            int commentAt = -1;
            for (int i = 0; i < text.length(); i++) {
                char current = text.charAt(i);

                if (current == COMMENT_CHARACTER) {
                    if (text.length() > i + 1 && text.charAt(i + 1) == COMMENT_CHARACTER) {
                        i++; // Escaped character, skip it!
                    } else {
                        commentAt = i;
                        break;
                    }
                }
            }

            if (commentAt != -1) {
                String rawCommentText = text.substring(commentAt + 1);
                commentText = StringUtils.trimStart(rawCommentText);
                String rawText = text.substring(0, commentAt);
                text = StringUtils.trimEnd(rawText);
                commentSeparator = rawText.substring(text.length()) + COMMENT_CHARACTER + rawCommentText.substring(0, rawText.length() - text.length());
            }

            if (StringUtils.isNullOrWhiteSpace(text) && StringUtils.isNullOrWhiteSpace(commentText))
                continue; // If there is no text, and no comment on a line, skip it.

            // Determine if this is a key-value pair.
            int splitAt = -1;
            for (int i = 0; i < text.length(); i++) {
                char current = text.charAt(i);

                if (current == '\\') {
                    i++; // Skip the next character, it's escaped.
                } else if (current == '=') {
                    splitAt = i;
                    break;
                } else if (current == ' ') {
                    break; // It's not a key value pair if we see a space before =.
                }
            }

            // Parse/store the values.
            if (splitAt != -1) { // It's a key-value pair.
                String key = unescapeKey(text.substring(0, splitAt));
                String value = unescapeValue(text.substring(splitAt + 1));
                if (config.keyValuePairs.put(key, new ConfigValueNode(value, commentText, commentSeparator)) == null)
                    config.orderedKeyValuePairs.add(key);
            } else { // It's raw text.
                ConfigValueNode newNode = new ConfigValueNode(text, commentText, commentSeparator);
                newNode.setAsString(text); // Ensure it is not escaped, as escaped text isn't supported here.
                config.getInternalText().add(newNode);
            }
        }
    }

    public static class IllegalConfigSyntaxException extends IllegalArgumentException {
        public IllegalConfigSyntaxException(String message) {
            super(message);
        }

        public IllegalConfigSyntaxException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    /**
     * Settings/data used when loading a config.
     * This is an example of passing settings / data among IBinarySerializable can work.
     */
    @Setter
    @Getter
    public static class ConfigSettings {
        private boolean readingCommentsEnabled = true;
        private boolean headerPending = true;
    }

    /**
     * A node which contains some kind of value, and potentially a comment.
     */
    @Getter
    public static class ConfigValueNode extends StringNode {
        private String comment;
        @Setter @NonNull private String commentSeparator;

        public static final String DEFAULT_COMMENT_SEPARATOR = " " + COMMENT_CHARACTER + " ";

        public ConfigValueNode() {
            this("");
        }

        public ConfigValueNode(String value) {
            this(value, "");
        }

        public ConfigValueNode(String value, String comment) {
            this(value, comment, DEFAULT_COMMENT_SEPARATOR);
        }

        public ConfigValueNode(String value, String comment, String commentSeparator) {
            super(value);
            this.comment = comment;
            this.commentSeparator = commentSeparator;
        }

        /**
         * Loads the config node from the reader.
         * @param reader the reader to read data from
         * @param configSettings the settings to load with
         */
        public void loadFromReader(DataReader reader, ConfigSettings configSettings) {
            int valueLength = reader.readUnsignedShortAsInt();
            this.value = reader.readTerminatedString(valueLength);
            this.surroundByQuotes = reader.readByte() == 1;
            if (configSettings.isReadingCommentsEnabled()) {
                int commentLength = reader.readUnsignedShortAsInt();
                this.comment = reader.readTerminatedString(commentLength);
            }
        }

        /**
         * Saves the config node to the writer.
         * @param writer the writer to write data from
         * @param configSettings the settings to save
         */
        public void saveToWriter(DataWriter writer, ConfigSettings configSettings){
            // Write value.
            writer.writeUnsignedShort(this.value != null ? this.value.length() : 0);
            if (this.value != null)
                writer.writeStringBytes(this.value);

            // Write surrounded by quotes.
            writer.writeByte(this.surroundByQuotes ? (byte) 1 : (byte) 0);

            // Write comment.
            if (configSettings.isReadingCommentsEnabled()) {
                writer.writeUnsignedShort(this.comment != null ? this.comment.length() : 0);
                if (this.comment != null)
                    writer.writeStringBytes(this.comment);
            }
        }

        /**
         * Creates a copy of this object.
         * @return valueNodeCopy
         */
        @SuppressWarnings("MethodDoesntCallSuperMethod")
        public ConfigValueNode clone() {
            return new ConfigValueNode(getAsStringLiteral(), this.comment, this.commentSeparator);
        }

        /**
         * Sets the node value as a string.
         * @param newValue the new string value
         */
        public void setAsString(String newValue) {
            setAsString(newValue, false);
        }

        /**
         * Gets the text line with the comment(s) included.
         * This should not be used for key-value pairs.
         */
        public String getTextWithComments() {
            if (StringUtils.isNullOrWhiteSpace(this.comment))
                return escapeValue(getAsStringLiteral());
            return escapeValue(getAsStringLiteral()) + this.commentSeparator + this.comment;
        }

        /**
         * Sets the end-of-line comment
         * @param comment the comment to apply
         * @return this
         */
        public ConfigValueNode setComment(String comment) {
            this.comment = comment;
            return this;
        }
    }

    // This is scuffed, but it works.
    private static class BadStringReader implements Closeable {
        private final StringReader _internalReader;
        private final StringBuilder stringBuilder = new StringBuilder();
        public String cachedNextLine;

        public BadStringReader(StringReader reader) {
            this._internalReader = reader;
        }

        @Override
        public void close() {
            if (this._internalReader != null)
                this._internalReader.close();
        }

        /**
         * Reads the next line.
         */
        public String readLine() {
            // If there's a cached next line, return it.
            if (this.cachedNextLine != null) {
                String value = this.cachedNextLine;
                this.cachedNextLine = null;
                return value;
            }

            try {
                int temp;
                char tempChar;
                while ((temp = _internalReader.read()) != -1 && (tempChar = (char) temp) != '\n')
                    if (tempChar != '\r')
                        this.stringBuilder.append(tempChar);

                if (temp == -1 && this.stringBuilder.length() == 0)
                    return null; // Reached end of data.
            } catch (IOException ex) {
                throw new IllegalConfigSyntaxException("Failed to finish reading string '" + this.stringBuilder + "'.", ex);
            }

            // Build the string.
            String newLine = this.stringBuilder.toString();
            this.stringBuilder.setLength(0);
            return newLine;
        }
    }

    private static ILogger getLogger() {
        return ClassNameLogger.getLogger(null, Config.class);
    }
}