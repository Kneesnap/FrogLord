package net.highwayfrogs.editor.games.shared.mods;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.system.Config.IllegalConfigSyntaxException;
import net.highwayfrogs.editor.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Typed access to the game-agnostic parts of a mod package manifest (mod.cfg), backed by a Config tree.
 * Created by jimbolina on 11/6/2026.
 */
public class ModManifest {
    @Getter @NonNull private final Config config;

    public ModManifest() {
        this(new Config(ModFormatConstants.MANIFEST_NAME));
    }

    public ModManifest(@NonNull Config config) {
        this.config = config;
    }

    /**
     * Parses manifest text.
     * @param manifestText the text of mod.cfg
     * @return parsedManifest
     * @throws IllegalConfigSyntaxException if the text is not valid Config syntax, including duplicate keys
     */
    public static ModManifest parse(@NonNull String manifestText) {
        Config config = Config.loadConfigFromString(manifestText, ModFormatConstants.MANIFEST_NAME);
        unescapeTextLines(config);
        return new ModManifest(config);
    }

    /**
     * Parses manifest text, reporting a syntax error as a problem instead of throwing.
     * @param manifestText the text of mod.cfg
     * @param problems the list to add a syntax problem to
     * @return parsedManifest, or null if the text could not be parsed
     */
    public static ModManifest parse(@NonNull String manifestText, @NonNull List<ModProblem> problems) {
        try {
            return parse(manifestText);
        } catch (IllegalArgumentException ex) {
            problems.add(ModProblem.error(null, null, "The manifest could not be parsed: " + ex.getMessage()));
            return null;
        }
    }

    private static void unescapeTextLines(Config config) {
        List<ConfigValueNode> textNodes = config.getInternalText();
        for (int i = 0; i < textNodes.size(); i++) {
            ConfigValueNode node = textNodes.get(i);
            String value = node.getAsString();
            if (value != null && value.contains(Config.ESCAPED_COMMENT_CHAR))
                node.setAsString(Config.unescapeComment(value));
        }

        for (Config child : config.getChildConfigNodes())
            unescapeTextLines(child);
    }

    /**
     * Gets the manifest as text, with LF line endings.
     * @return manifestText
     */
    public String toText() {
        return this.config.toString().replace("\r\n", "\n");
    }

    /**
     * Creates a deep copy of the Config tree behind this manifest.
     * @return configCopy
     */
    public Config toConfig() {
        return copyConfig(this.config);
    }

    /**
     * Creates a deep copy of this manifest.
     * @return manifestCopy
     */
    public ModManifest copy() {
        return new ModManifest(toConfig());
    }

    /**
     * Gets a top-level section by name.
     * @param sectionName the section name
     * @return section, or null if absent
     */
    public Config getSection(String sectionName) {
        return getFirstChild(this.config, sectionName);
    }

    /**
     * Gets a top-level section by name, creating it if absent.
     * @param sectionName the section name
     * @return section
     */
    public Config getOrCreateSection(String sectionName) {
        Config section = getFirstChild(this.config, sectionName);
        if (section == null) {
            section = new Config(sectionName);
            this.config.addChildConfig(section);
        }

        return section;
    }

    /**
     * Removes every top-level section with the given name.
     * @param sectionName the section name
     */
    public void removeSection(String sectionName) {
        removeChildren(this.config, sectionName);
    }

    private Config getModSection() {
        return getOrCreateSection(ModFormatConstants.SECTION_MOD);
    }

    private String getModValue(String key) {
        Config section = getSection(ModFormatConstants.SECTION_MOD);
        return section != null ? getString(section, key) : null;
    }

    private void setModValue(String key, String value) {
        setString(getModSection(), key, value);
    }

    /**
     * Gets the format version, or null when absent or not an integer.
     */
    public Integer getFormatVersion() {
        return parseInteger(getModValue(ModFormatConstants.KEY_FORMAT_VERSION));
    }

    /**
     * Sets the format version.
     * @param formatVersion the format version
     */
    public void setFormatVersion(int formatVersion) {
        setModValue(ModFormatConstants.KEY_FORMAT_VERSION, Integer.toString(formatVersion));
    }

    /**
     * Gets the game identifier.
     */
    public String getGame() {
        return getModValue(ModFormatConstants.KEY_GAME);
    }

    /**
     * Sets the game identifier.
     * @param game the game identifier
     */
    public void setGame(String game) {
        setModValue(ModFormatConstants.KEY_GAME, game);
    }

    /**
     * Gets the mod id (a UUID).
     */
    public String getId() {
        return getModValue(ModFormatConstants.KEY_ID);
    }

    /**
     * Sets the mod id (a UUID).
     * @param id the mod id
     */
    public void setId(String id) {
        setModValue(ModFormatConstants.KEY_ID, id);
    }

    /**
     * Gets the display name of the mod.
     */
    public String getName() {
        return getModValue(ModFormatConstants.KEY_NAME);
    }

    /**
     * Sets the display name of the mod.
     * @param name the display name
     */
    public void setName(String name) {
        setModValue(ModFormatConstants.KEY_NAME, name);
    }

    /**
     * Gets the semantic version of the mod.
     */
    public String getVersion() {
        return getModValue(ModFormatConstants.KEY_VERSION);
    }

    /**
     * Sets the semantic version of the mod.
     * @param version the version
     */
    public void setVersion(String version) {
        setModValue(ModFormatConstants.KEY_VERSION, version);
    }

    /**
     * Gets the author text.
     */
    public String getAuthor() {
        return getModValue(ModFormatConstants.KEY_AUTHOR);
    }

    /**
     * Sets the author text.
     * @param author the author text
     */
    public void setAuthor(String author) {
        setModValue(ModFormatConstants.KEY_AUTHOR, author);
    }

    /**
     * Gets the licence text.
     */
    public String getLicence() {
        return getModValue(ModFormatConstants.KEY_LICENCE);
    }

    /**
     * Sets the licence text.
     * @param licence the licence, or null to remove it
     */
    public void setLicence(String licence) {
        setModValue(ModFormatConstants.KEY_LICENCE, licence);
    }

    /**
     * Gets the homepage URL.
     */
    public String getHomepage() {
        return getModValue(ModFormatConstants.KEY_HOMEPAGE);
    }

    /**
     * Sets the homepage URL.
     * @param homepage the URL, or null to remove it
     */
    public void setHomepage(String homepage) {
        setModValue(ModFormatConstants.KEY_HOMEPAGE, homepage);
    }

    /**
     * Gets the zip path of the icon.
     */
    public String getIcon() {
        return getModValue(ModFormatConstants.KEY_ICON);
    }

    /**
     * Sets the zip path of the icon.
     * @param icon the zip path, or null to remove it
     */
    public void setIcon(String icon) {
        setModValue(ModFormatConstants.KEY_ICON, icon);
    }

    /**
     * Gets the zip paths of the screenshots, or null when absent.
     */
    public List<String> getScreenshots() {
        return parseList(getModValue(ModFormatConstants.KEY_SCREENSHOTS));
    }

    /**
     * Sets the zip paths of the screenshots.
     * @param screenshots the zip paths, or null to remove the key
     */
    public void setScreenshots(List<String> screenshots) {
        setModValue(ModFormatConstants.KEY_SCREENSHOTS, joinList(screenshots));
    }

    /**
     * Gets the name of the tool which exported the package.
     */
    public String getExportedBy() {
        return getModValue(ModFormatConstants.KEY_EXPORTED_BY);
    }

    /**
     * Sets the name of the tool which exported the package.
     * @param exportedBy the tool name, or null to remove it
     */
    public void setExportedBy(String exportedBy) {
        setModValue(ModFormatConstants.KEY_EXPORTED_BY, exportedBy);
    }

    /**
     * Gets the minimum FrogLord version required to install the package.
     */
    public String getMinFrogLordVersion() {
        return getModValue(ModFormatConstants.KEY_MIN_FROGLORD_VERSION);
    }

    /**
     * Sets the minimum FrogLord version required to install the package.
     * @param minFrogLordVersion the version, or null to remove it
     */
    public void setMinFrogLordVersion(String minFrogLordVersion) {
        setModValue(ModFormatConstants.KEY_MIN_FROGLORD_VERSION, minFrogLordVersion);
    }

    /**
     * Gets the description: the text lines of [Mod] joined by line feeds, without leading or trailing blank lines.
     * @return description, or null if there is none
     */
    public String getDescription() {
        Config section = getSection(ModFormatConstants.SECTION_MOD);
        if (section == null)
            return null;

        List<String> lines = getTextLines(section);
        int start = 0;
        int end = lines.size();
        while (start < end && StringUtils.isNullOrWhiteSpace(lines.get(start)))
            start++;
        while (end > start && StringUtils.isNullOrWhiteSpace(lines.get(end - 1)))
            end--;

        return start < end ? String.join("\n", lines.subList(start, end)) : null;
    }

    /**
     * Sets the description, replacing the text lines of [Mod].
     * @param description the description, or null to remove it
     */
    public void setDescription(String description) {
        List<String> lines = new ArrayList<>();
        if (!StringUtils.isNullOrWhiteSpace(description))
            Collections.addAll(lines, description.replace("\r\n", "\n").split("\n", -1));

        setTextLines(getModSection(), lines);
    }

    /**
     * Gets the targets the package was made for, in order.
     * @return targets
     */
    public List<ModTarget> getTargets() {
        Config section = getSection(ModFormatConstants.SECTION_TARGETS);
        if (section == null)
            return Collections.emptyList();

        List<ModTarget> targets = new ArrayList<>();
        for (Config child : section.getChildConfigNodes())
            targets.add(ModTarget.fromConfig(child));

        return targets;
    }

    /**
     * Replaces the [Targets] section.
     * @param targets the targets to write
     */
    public void setTargets(@NonNull List<ModTarget> targets) {
        Config section = getOrCreateSection(ModFormatConstants.SECTION_TARGETS);
        for (Config child : new ArrayList<>(section.getChildConfigNodes()))
            section.removeChildConfig(child);
        for (ModTarget target : targets)
            section.addChildConfig(target.toConfig());
    }

    /**
     * Adds a target to the [Targets] section.
     * @param target the target to add
     */
    public void addTarget(@NonNull ModTarget target) {
        getOrCreateSection(ModFormatConstants.SECTION_TARGETS).addChildConfig(target.toConfig());
    }

    /**
     * Finds the target with the given name, or the target which lists the given name as an alias.
     * @param nameOrAlias the build name to look up
     * @return target, or null
     */
    public ModTarget findTarget(String nameOrAlias) {
        if (nameOrAlias == null)
            return null;

        List<ModTarget> targets = getTargets();
        for (ModTarget target : targets)
            if (target.getName().equals(nameOrAlias))
                return target;
        for (ModTarget target : targets)
            if (target.getAliases().contains(nameOrAlias))
                return target;

        return null;
    }

    /**
     * Finds the first target listing the given executable checksum.
     * @param exeChecksum the CRC32 of the executable
     * @return target, or null
     */
    public ModTarget findTargetByChecksum(long exeChecksum) {
        for (ModTarget target : getTargets())
            if (target.getExeChecksums().contains(exeChecksum))
                return target;

        return null;
    }

    /**
     * Gets the [Files] lines as written in the manifest, in order, skipping blank lines.
     * @return filesLines, or null if there is no [Files] section
     */
    public List<String> getFilesLines() {
        Config section = getSection(ModFormatConstants.SECTION_FILES);
        if (section == null)
            return null;

        List<String> lines = new ArrayList<>();
        for (String line : getTextLines(section))
            if (!StringUtils.isNullOrWhiteSpace(line))
                lines.add(line.trim());

        return lines;
    }

    /**
     * Gets the content hash declared in [Files].
     * @return contentHash, or null
     */
    public String getContentHash() {
        Config section = getSection(ModFormatConstants.SECTION_FILES);
        return section != null ? getString(section, ModFormatConstants.KEY_CONTENT_HASH) : null;
    }

    /**
     * Replaces the [Files] section, placing it last.
     * @param contentHash the content hash
     * @param lines the canonical [Files] lines
     */
    public void setFiles(@NonNull String contentHash, @NonNull List<String> lines) {
        removeSection(ModFormatConstants.SECTION_FILES);
        Config section = new Config(ModFormatConstants.SECTION_FILES);
        setString(section, ModFormatConstants.KEY_CONTENT_HASH, contentHash);
        setTextLines(section, lines);
        this.config.addChildConfig(section);
    }

    /**
     * Gets the override subtree for a target, creating [Overrides] and the [[target]] child if absent.
     * Sections added to the returned node are shaped like root sections.
     * @param target the target name
     * @return overrideNode
     */
    public Config getOrCreateOverride(@NonNull String target) {
        Config overrides = getOrCreateSection(ModFormatConstants.SECTION_OVERRIDES);
        Config override = getFirstChild(overrides, target);
        if (override == null) {
            override = new Config(target);
            overrides.addChildConfig(override);
        }

        return override;
    }

    /**
     * Builds the configuration that applies to one target: the root without [Overrides], with that target's override subtree merged over it.
     * Keys replace keys, child sections merge by name recursively, text lines replace text lines, and list values replace as whole values.
     * @param target the target name or alias to build for, or null for the root alone
     * @return effectiveConfig
     */
    public Config getEffectiveConfig(String target) {
        Config effective = copyConfig(this.config);
        removeChildren(effective, ModFormatConstants.SECTION_OVERRIDES);
        if (target == null)
            return effective;

        Config overrides = getSection(ModFormatConstants.SECTION_OVERRIDES);
        if (overrides == null)
            return effective;

        Config override = getFirstChild(overrides, target);
        if (override == null) {
            ModTarget resolved = findTarget(target);
            if (resolved != null)
                override = getFirstChild(overrides, resolved.getName());
        }

        if (override != null)
            mergeConfig(effective, override);

        return effective;
    }

    /**
     * Merges one Config over another: keys replace, text lines replace when the source has any, child sections merge by name recursively.
     * @param destination the config to change
     * @param source the config to merge from
     */
    public static void mergeConfig(@NonNull Config destination, @NonNull Config source) {
        for (String key : getKeys(source))
            destination.setKeyValueNode(key, source.getOptionalKeyValueNode(key));

        if (!getTextLines(source).isEmpty() && hasNonBlankText(source))
            setTextLines(destination, getTextLines(source));

        for (Config sourceChild : source.getChildConfigNodes()) {
            Config destinationChild = getFirstChild(destination, sourceChild.getSectionName());
            if (destinationChild == null) {
                destination.addChildConfig(copyConfig(sourceChild));
            } else {
                mergeConfig(destinationChild, sourceChild);
            }
        }
    }

    private static boolean hasNonBlankText(Config config) {
        for (String line : getTextLines(config))
            if (!StringUtils.isNullOrWhiteSpace(line))
                return true;

        return false;
    }

    /**
     * Creates a deep copy of a Config node and its children, detached from any parent.
     * The copy is made through the text form so key order and text lines are kept exactly.
     * @param source the node to copy
     * @return copy
     */
    public static Config copyConfig(@NonNull Config source) {
        List<Integer> childPath = new ArrayList<>();
        Config root = source;
        while (root.getParentConfig() != null) {
            Config parent = root.getParentConfig();
            childPath.add(0, parent.getChildConfigNodes().indexOf(root));
            root = parent;
        }

        Config copy = Config.loadConfigFromString(root.toString(), root.getSectionName());
        unescapeTextLines(copy);
        for (int index : childPath)
            copy = copy.getChildConfigNodes().get(index);

        if (copy.getParentConfig() != null)
            copy.getParentConfig().removeChildConfig(copy);

        return copy;
    }

    /**
     * Gets the first child section with the given name.
     * @param parent the parent section
     * @param name the child name
     * @return child, or null
     */
    public static Config getFirstChild(@NonNull Config parent, String name) {
        List<Config> children = parent.getAllChildConfigsByName(name);
        return children.isEmpty() ? null : children.get(0);
    }

    /**
     * Removes every child section with the given name.
     * @param parent the parent section
     * @param name the child name
     */
    public static void removeChildren(@NonNull Config parent, String name) {
        for (Config child : new ArrayList<>(parent.getAllChildConfigsByName(name)))
            parent.removeChildConfig(child);
    }

    /**
     * Gets the keys of a section, sorted by name.
     * @param section the section to read
     * @return sortedKeys
     */
    public static List<String> getKeys(@NonNull Config section) {
        List<String> keys = new ArrayList<>(section.getKeyValuePairs().keySet());
        Collections.sort(keys);
        return keys;
    }

    /**
     * Gets a key's value as a string.
     * @param section the section to read
     * @param key the key
     * @return value, or null if absent
     */
    public static String getString(@NonNull Config section, String key) {
        ConfigValueNode node = section.getOptionalKeyValueNode(key);
        return node != null ? node.getAsString() : null;
    }

    /**
     * Sets a key's value as a string, or removes the key when the value is null.
     * @param section the section to write
     * @param key the key
     * @param value the value, or null
     */
    public static void setString(@NonNull Config section, String key, String value) {
        if (value == null) {
            section.removeKeyValueNode(key);
        } else {
            section.getOrCreateKeyValueNode(key).setAsString(value);
        }
    }

    /**
     * Gets the text lines of a section, in order, including blank lines.
     * @param section the section to read
     * @return textLines
     */
    public static List<String> getTextLines(@NonNull Config section) {
        List<String> lines = new ArrayList<>();
        for (ConfigValueNode node : section.getTextNodes())
            lines.add(node.getAsString() != null ? node.getAsString() : "");

        return lines;
    }

    /**
     * Replaces the text lines of a section.
     * @param section the section to write
     * @param lines the new text lines
     */
    public static void setTextLines(@NonNull Config section, @NonNull List<String> lines) {
        List<ConfigValueNode> textNodes = section.getInternalText();
        textNodes.clear();
        for (String line : lines) {
            ConfigValueNode node = new ConfigValueNode();
            node.setAsString(line);
            textNodes.add(node);
        }
    }

    /**
     * Parses a comma-separated list value.
     * @param value the value text
     * @return items (trimmed, empty items dropped), or null when the value is null
     */
    public static List<String> parseList(String value) {
        if (value == null)
            return null;

        List<String> items = new ArrayList<>();
        for (String item : value.split(",", -1)) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty())
                items.add(trimmed);
        }

        return items;
    }

    /**
     * Joins list items into a comma-separated value.
     * @param items the items
     * @return value, or null when the items are null
     */
    public static String joinList(List<String> items) {
        return items != null ? String.join(",", items) : null;
    }

    /**
     * Parses a decimal or '0x' hexadecimal integer.
     * @param value the value text
     * @return integer, or null when absent or invalid
     */
    public static Integer parseInteger(String value) {
        Long result = parseLong(value);
        return result != null && result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE ? (int) (long) result : null;
    }

    /**
     * Parses a decimal or '0x' hexadecimal long.
     * @param value the value text
     * @return long, or null when absent or invalid
     */
    public static Long parseLong(String value) {
        if (value == null)
            return null;

        String trimmed = value.trim();
        try {
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("0x"))
                return Long.parseLong(trimmed.substring(2), 16);
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Parses a 'true'/'false' boolean.
     * @param value the value text
     * @return boolean, or null when absent or invalid
     */
    public static Boolean parseBoolean(String value) {
        if (value == null)
            return null;

        String trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed))
            return Boolean.TRUE;
        if ("false".equalsIgnoreCase(trimmed))
            return Boolean.FALSE;

        return null;
    }

    /**
     * A base build the package was made for: its version-config name, executable checksums and aliases.
     * Created by jimbolina on 11/6/2026.
     */
    @Getter
    public static class ModTarget {
        @NonNull private final String name;
        @NonNull private final List<Long> exeChecksums;
        @NonNull private final List<String> aliases;

        public ModTarget(@NonNull String name, @NonNull List<Long> exeChecksums, @NonNull List<String> aliases) {
            this.name = name;
            this.exeChecksums = Collections.unmodifiableList(new ArrayList<>(exeChecksums));
            this.aliases = Collections.unmodifiableList(new ArrayList<>(aliases));
        }

        /**
         * Reads a target from its [[name]] child section. Malformed checksums are skipped; the validator reports them.
         * @param section the target section
         * @return target
         */
        public static ModTarget fromConfig(@NonNull Config section) {
            List<Long> checksums = new ArrayList<>();
            List<String> checksumTexts = parseList(getString(section, ModFormatConstants.KEY_EXE_CHECKSUM));
            if (checksumTexts != null) {
                for (String checksumText : checksumTexts) {
                    Long checksum = parseLong(checksumText);
                    if (checksum != null)
                        checksums.add(checksum);
                }
            }

            List<String> aliases = parseList(getString(section, ModFormatConstants.KEY_ALIASES));
            return new ModTarget(section.getSectionName(), checksums, aliases != null ? aliases : Collections.<String>emptyList());
        }

        /**
         * Writes this target as a [[name]] child section.
         * @return targetSection
         */
        public Config toConfig() {
            Config section = new Config(this.name);
            if (!this.exeChecksums.isEmpty()) {
                List<String> checksumTexts = new ArrayList<>();
                for (Long checksum : this.exeChecksums)
                    checksumTexts.add(Long.toString(checksum));
                setString(section, ModFormatConstants.KEY_EXE_CHECKSUM, joinList(checksumTexts));
            }

            if (!this.aliases.isEmpty())
                setString(section, ModFormatConstants.KEY_ALIASES, joinList(this.aliases));

            return section;
        }
    }
}
