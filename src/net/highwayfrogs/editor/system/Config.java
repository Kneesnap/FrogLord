package net.highwayfrogs.editor.system;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.IBinarySerializable;
import net.highwayfrogs.editor.utils.Utils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

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
 * ``` # Three backticks, as seen here will result in an empty line of text. If you just include an empty line of text normally, it will be ignored. You can also technically put a '#' on the line (Single-Line comment), as comments can exist on otherwise empty lines.
 *
 * By default, any text is interpreted as being in the implicit root node. In other words, there is a default node which values will go into until a section is defined.
 *
 * Everything after '#' on a line of text will be ignored, as a comment. If you need to put '#' into a line of text or a value, escape it by typing '\#' instead.
 * Created by Kneesnap on 4/25/2024.
 */
@SuppressWarnings("unused")
public class Config implements IBinarySerializable {
    private final Map<String, ConfigValueNode> keyValuePairs = new HashMap<>();
    private final Map<String, List<Config>> childConfigsByName = new HashMap<>();
    private static final ConfigValueNode EMPTY_DEFAULT_NODE = new ConfigValueNode("", "");

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
    @Setter(AccessLevel.PROTECTED)
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

    /**
     * Gets the individual lines of text from a list of config value nodes.
     * @return textStringList
     */
    public List<String> getTextWithoutComments() {
        List<String> textList = new ArrayList<>(this.internalText.size());
        for (int i = 0; i < this.internalText.size(); i++) {
            ConfigValueNode node = this.internalText.get(i);
            String text = node != null ? node.getValue() : null;
            if (!Utils.isNullOrWhiteSpace(text) && !node.isEscapedNewLine())
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

    private static final byte BINARY_FORMAT_VERSION = 0;

    public Config(String name) {
        this(null, name);
    }

    public Config(Config parentConfig, String name) {
        this.parentConfig = parentConfig;
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
        this.sectionName = reader.readString(sectionNameLength);
        if (loadSettings.isReadingCommentsEnabled()) {
            int sectionCommentLength = reader.readInt();
            this.sectionComment = reader.readString(sectionCommentLength);
        }

        // Read key value pairs.
        int keyValueCount = reader.readInt();
        for (int i = 0; i < keyValueCount; i++) {
            short keyLength = reader.readUnsignedByteAsShort();
            String key = reader.readString(keyLength);
            ConfigValueNode node = new ConfigValueNode("", "");
            node.loadFromReader(reader, loadSettings);
            this.keyValuePairs.put(key, node);
        }

        // Read Text.
        int textEntries = reader.readInt();
        for (int i = 0; i < textEntries; i++) {
            ConfigValueNode node = new ConfigValueNode("", "");
            node.loadFromReader(reader, loadSettings);
            this.internalText.add(node);
        }

        // Read child configs.
        int childCount = reader.readInt();
        for (int i = 0; i < childCount; i++) {
            Config childConfig = new Config(this, "$UnknownChildConfigNode");
            childConfig.loadFromReader(reader, this, loadSettings);
            addChildConfig(childConfig);
        }}

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

        if (this.internalChildConfigs.contains(childConfig))
            return;

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
        for (Entry<String, ConfigValueNode> entry : this.keyValuePairs.entrySet()) {
            writer.writeUnsignedByte((short) (entry.getKey() != null ? entry.getKey().length() : 0));
            writer.writeStringBytes(entry.getKey());
            entry.getValue().saveToWriter(writer, saveSettings);
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
        return this.keyValuePairs.remove(keyName);
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

        if (node.isEscapedNewLine())
            throw new IllegalArgumentException("Supplied config value '" + node.getValue() + "' had IsEscapedNewLine set to true. This cannot be used in key-value pairs!");

        ConfigValueNode existingNode = this.keyValuePairs.get(keyName);
        if (existingNode != null) { // Get existing node.
            existingNode.setValue(node.getValue());
            if (!Utils.isNullOrWhiteSpace(node.getComment()) && Utils.isNullOrWhiteSpace(existingNode.getComment()))
                existingNode.setComment(node.getComment());
        } else { // Create new node.
            this.keyValuePairs.put(keyName, node.clone());
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

        ConfigValueNode newNode = new ConfigValueNode("$NO_VALUE$", "");
        this.keyValuePairs.put(keyName, newNode);
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
     * /// <exception cref="KeyNotFoundException">Thrown if the key is not found, and errorIfNotFound is true.</exception>
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
            Config newConfig = new Config(this, name);
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
            Utils.makeDirectory(parentFile);

        Utils.writeBytesToFile(getLogger(), outputFile, toString().getBytes(StandardCharsets.UTF_8), true);
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
        // Write key value pairs.
        for (Entry<String, ConfigValueNode> entry : this.keyValuePairs.entrySet()){
            builder.append(escapeKey(entry.getKey()));
            builder.append("=");
            builder.append(entry.getValue());
            builder.append(Constants.NEWLINE);
        }

        // Write Raw text.
        for (int i = 0; i < this.internalText.size(); i++) {
            ConfigValueNode line = this.internalText.get(i);
            builder.append(line).append(Constants.NEWLINE);
        }

        // Empty line between sections.
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
        builder.append(escapeString(node.getSectionName()));

        builder.append(sectionEnd);
        if (!Utils.isNullOrWhiteSpace(node.getSectionComment())) {
            builder.append(" # ");
            builder.append(node.getSectionComment());
        }

        builder.append(Constants.NEWLINE);
    }

    /**
     * General escaping applied to various parts of a config.
     * @param value The value to escape.
     * @return escapedValue
     */
    public static String escapeString(String value) {
        if (value == null)
            return null;

        return value.replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\\", "\\\\")
                .replace("#", "\\#");
    }

    /**
     * Unescape the general escaping applied to various parts of a config.
     * @param value The value to unescape.
     * @return unescapedValue
     */
    public static String unescapeString(String value) {
        if (value == null)
            return null;

        return value.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\#", "#")
                .replace("\\\\", "\\");
    }

    /**
     * Escapes a key string.
     * @param key The key to escape
     * @return escapedKey
     */
    public static String escapeKey(String key) {
        return escapeString(key).replace(" ", "\\ ").replace("=", "\\=");
    }

    /**
     * Unescapes a key string.
     * @param key The key to unescape.
     * @return unescapedKey
     */
    public static String unescapeKey(String key) {
        return unescapeString(key).replace("\\ ", " ").replace("\\=", "=");
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
            configFileText = String.join(Constants.NEWLINE, Utils.readLinesFromFile(targetFile));
        }

        return loadConfigFromString(configFileText, Utils.stripExtension(targetFile.getName()));
    }

    /**
     * Loads a text config from the URL.
     * @param url the url to resolve the config at
     * @return loadedConfig
     */
    public static Config loadTextConfigFromURL(URL url) {
        String versionConfigName = Utils.getFileNameWithoutExtension(url);
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
        List<String> configLines = Utils.readLinesFromStream(inputStream);
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
            return loadConfigFromString(null, reader, 0, configFileName, null);
        }
    }

    private static Config loadConfigFromString(Config parentConfig, BadStringReader stringReader, int layer, String sectionName, String sectionComment) {
        Config config = new Config(parentConfig, sectionName);
        config.setSectionComment(sectionComment);

        while (true) {
            String line = stringReader.readLine();
            if (line == null)
                return config; // Reached end of file?

            if (Utils.isNullOrWhiteSpace(line))
                continue; // Empty lines are skipped.

            String trimmedLine = line.trim();

            if (trimmedLine.charAt(0) == '[') {
                String comment = "";
                String sectionLine = trimmedLine;

                // Read the comment, if there is one.
                int commentIndex = sectionLine.indexOf('#');
                if (commentIndex != -1) {
                    comment = Utils.trimStart(sectionLine.substring(commentIndex + 1));
                    sectionLine = Utils.trimEnd(sectionLine.substring(0, commentIndex));
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
                String newSectionName = unescapeString(sectionLine.substring(leftLayer, leftLayer + nameLength));

                // Create child config.
                if (sectionLayer == layer + 1) {
                    // Read for this config.
                    Config loadedChildConfig = loadConfigFromString(config, stringReader, sectionLayer, newSectionName, comment);
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

            String commentText = "";
            int commentAt = -1;
            for (int i = 0; i < text.length(); i++) {
                char current = text.charAt(i);

                if (current == '\\') {
                    i++; // Skip the next character, it's escaped.
                } else if (current == '#') {
                    commentAt = i;
                    break;
                }
            }

            if (commentAt != -1) {
                commentText = Utils.trimStart(text.substring(commentAt + 1));
                text = Utils.trimEnd(text.substring(0, commentAt));
            }

            if (Utils.isNullOrWhiteSpace(text) && Utils.isNullOrWhiteSpace(commentText))
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
                String value = unescapeString(text.substring(splitAt + 1));
                config.keyValuePairs.put(key, new ConfigValueNode(value, commentText));
            } else { // It's raw text.
                boolean isEmpty = text.equalsIgnoreCase("```");
                ConfigValueNode newNode = isEmpty ? new ConfigValueNode("", commentText) : new ConfigValueNode(text, commentText);
                newNode.setEscapedNewLine(isEmpty);
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
    public static class ConfigValueNode {
        @Setter private String comment;
        @Setter(AccessLevel.PRIVATE) private boolean escapedNewLine;
        @Setter private String value; // Can be empty, but not null.

        public ConfigValueNode(String value, String comment) {
            this.value = value != null ? value : "";
            this.comment = comment;
        }

        /**
         * Loads the config node from the reader.
         * @param reader the reader to read data from
         * @param configSettings the settings to load with
         */
        public void loadFromReader(DataReader reader, ConfigSettings configSettings) {
            int valueLength = reader.readUnsignedShortAsInt();
            this.value = reader.readString(valueLength);
            this.escapedNewLine = reader.readByte() != 0;
            if (configSettings.isReadingCommentsEnabled()) {
                int commentLength = reader.readUnsignedShortAsInt();
                this.comment = reader.readString(commentLength);
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

            // Is it an escaped newline?
            writer.writeByte((byte) (this.escapedNewLine ? 1 : 0));

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
            ConfigValueNode newNode = new ConfigValueNode(this.value, this.comment);
            newNode.escapedNewLine = this.escapedNewLine;
            return newNode;
        }

        /**
         * Gets the node value as a string.
         * @return stringValue
         */
        public String getAsString() {
            return this.value;
        }

        /**
         * Sets the node value as a string.
         * @param newValue the new string value
         */
        public void setAsString(String newValue) {
            this.value = newValue;
        }

        /**
         * Gets the node value as a boolean.
         * @return boolValue
         * @throws IllegalConfigSyntaxException Thrown if the node data is not a valid boolean.
         */
        public boolean getAsBoolean() {
            if ("true".equalsIgnoreCase(this.value)
                    || "yes".equalsIgnoreCase(this.value)
                    || "1".equalsIgnoreCase(this.value))
                return true;

            if ("false".equalsIgnoreCase(this.value)
                    || "no".equalsIgnoreCase(this.value)
                    || "0".equalsIgnoreCase(this.value))
                return false;

            throw new IllegalConfigSyntaxException("Don't know how to interpret '" + this.value + "' as a boolean.");
        }

        /**
         * Sets the node value to a boolean.
         * @param newValue The new value.
         */
        public void setAsBoolean(boolean newValue) {
            this.value = newValue ? "true" : "false";
        }

        /**
         * Gets the node value as an integer.
         * @return intValue
         * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as either an integer or a hex integer.
         */
        public int getAsInteger() {
            if (!Utils.isNullOrWhiteSpace(this.value)) {
                try {
                    return Utils.isHexInteger(this.value) ? Utils.parseHexInteger(this.value) : Integer.parseInt(this.value);
                } catch (NumberFormatException nfe) {
                    throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer.", nfe);
                }
            }


            throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer.");
        }

        /**
         * Gets the node value as an integer.
         * @param fallback the number to return if there is no value.
         * @return intValue
         * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as either an integer or a hex integer.
         */
        public double getAsInteger(int fallback) {
            if (!Utils.isNullOrWhiteSpace(this.value)) {
                try {
                    return Utils.isHexInteger(this.value) ? Utils.parseHexInteger(this.value) : Integer.parseInt(this.value);
                } catch (NumberFormatException nfe) {
                    throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer.", nfe);
                }
            }

            return fallback;
        }

        /**
         * Sets the node value to an integer.
         * @param newValue The new value.
         */
        public void setAsInteger(int newValue) {
            this.value = Integer.toString(newValue);
        }

        /**
         * Gets the node value as a double.
         * @return doubleValue
         * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as either an integer or a hex integer.
         */
        public double getAsDouble() {
            if (!Utils.isNullOrWhiteSpace(this.value)) {
                try {
                    return Double.parseDouble(this.value);
                } catch (NumberFormatException nfe) {
                    throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number.", nfe);
                }
            }

            throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number.");
        }

        /**
         * Gets the node value as a double.
         * @param fallback the number to return if there is no value.
         * @return doubleValue
         * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as either an integer or a hex integer.
         */
        public double getAsDouble(double fallback) {
            if (!Utils.isNullOrWhiteSpace(this.value)) {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException nfe) {
                    throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number.", nfe);
                }
            }

            return fallback;
        }

        /**
         * Sets the node value to a double.
         * @param newValue The new value.
         */
        public void setAsDouble(double newValue){
            this.value = Double.toString(newValue);
        }

        /**
         * Gets the value as an enum.
         * @return enumValue
         * @throws IllegalConfigSyntaxException Thrown if the value was not a valid enum or a valid enum index.
         */
        public <TEnum extends Enum<TEnum>> TEnum getAsEnum(Class<TEnum> enumClass) {
            if (Utils.isNullOrEmpty(this.value))
                return null;

            try {
                return Enum.valueOf(enumClass, this.value);
            } catch (Exception e) {
                throw new IllegalConfigSyntaxException("The value '" + this.value + "' could not be interpreted as an enum value from " + enumClass.getSimpleName() + ".", e);
            }
        }

        /**
         * Gets the value as an enum.
         * @return enumValue
         * @throws IllegalConfigSyntaxException Thrown if the value was not a valid enum or a valid enum index.
         */
        public <TEnum extends Enum<TEnum>> TEnum getAsEnum(TEnum defaultEnum) {
            if (defaultEnum == null)
                throw new NullPointerException("defaultEnum");

            if (!Utils.isNullOrWhiteSpace(this.value)) {
                try {
                    return Enum.valueOf(defaultEnum.getDeclaringClass(), this.value);
                } catch (Exception e) {
                    throw new IllegalConfigSyntaxException("The value '" + this.value + "' could not be interpreted as an enum value from " + defaultEnum.getDeclaringClass().getSimpleName() + ".", e);
                }
            }

            return defaultEnum;
        }

        /**
         * Sets the node value to an enum.
         * @param newValue The new value.
         * @param <TEnum> The enum value type
         */
        public <TEnum extends Enum<TEnum>> void setAsEnum(TEnum newValue) {
            this.value = newValue != null ? newValue.name() : "";
        }

        @Override
        public String toString() {
            if (this.escapedNewLine)
                return "```" + (Utils.isNullOrWhiteSpace(this.comment) ? "" : " # " + this.comment);
            if (Utils.isNullOrWhiteSpace(this.comment))
                return Config.escapeString(this.value != null ? this.value : "");
            return Config.escapeString(this.value != null ? this.value : "")
                    + (Utils.isNullOrWhiteSpace(this.value) ? "# " : " # ") + this.comment;
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

    private static Logger getLogger() {
        return Logger.getLogger(Config.class.getSimpleName());
    }
}