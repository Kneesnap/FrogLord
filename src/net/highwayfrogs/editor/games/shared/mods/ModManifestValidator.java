package net.highwayfrogs.editor.games.shared.mods;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.shared.mods.ModManifest.ModTarget;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks a mod manifest against the format profile and the sections a game declares, collecting problems instead of throwing.
 * The [Mod], [Targets], [Overrides] and [Files] rules are built in; each game registers its own sections with addSection.
 * Created by jimbolina on 11/6/2026.
 */
public class ModManifestValidator {
    @Getter private final String expectedGame;
    private final Map<String, SectionRule> sectionRules = new LinkedHashMap<>();

    private static final Set<String> REQUIRED_MOD_KEYS = new LinkedHashSet<>(Arrays.asList(
            ModFormatConstants.KEY_FORMAT_VERSION, ModFormatConstants.KEY_GAME, ModFormatConstants.KEY_ID,
            ModFormatConstants.KEY_NAME, ModFormatConstants.KEY_VERSION, ModFormatConstants.KEY_AUTHOR));

    /**
     * Creates a validator which knows only the shared sections.
     * @param expectedGame the game identifier the [Mod] game key must hold, or null to accept any
     */
    public ModManifestValidator(String expectedGame) {
        this.expectedGame = expectedGame;
        addSection(new SectionRule(ModFormatConstants.SECTION_MOD)
                .allowKeys(ModFormatConstants.KEY_FORMAT_VERSION, ModFormatConstants.KEY_GAME, ModFormatConstants.KEY_ID,
                        ModFormatConstants.KEY_NAME, ModFormatConstants.KEY_VERSION, ModFormatConstants.KEY_AUTHOR,
                        ModFormatConstants.KEY_LICENCE, ModFormatConstants.KEY_HOMEPAGE, ModFormatConstants.KEY_ICON,
                        ModFormatConstants.KEY_SCREENSHOTS, ModFormatConstants.KEY_EXPORTED_BY, ModFormatConstants.KEY_MIN_FROGLORD_VERSION)
                .pathKeys(ModFormatConstants.KEY_ICON).pathListKeys(ModFormatConstants.KEY_SCREENSHOTS)
                .allowText().noChildren());
        addSection(new SectionRule(ModFormatConstants.SECTION_TARGETS)
                .allowDescendantKeys(ModFormatConstants.KEY_EXE_CHECKSUM, ModFormatConstants.KEY_ALIASES));
        addSection(new SectionRule(ModFormatConstants.SECTION_FILES)
                .allowKeys(ModFormatConstants.KEY_CONTENT_HASH).allowText().noChildren());
    }

    /**
     * Registers the rules for a top-level section. A rule for an existing name replaces it.
     * @param rule the section rule
     * @return this
     */
    public ModManifestValidator addSection(@NonNull SectionRule rule) {
        this.sectionRules.put(rule.getName(), rule);
        return this;
    }

    /**
     * Parses and validates manifest text. A syntax error, including a duplicate key, is returned as a problem.
     * @param manifestText the manifest text
     * @return problems
     */
    public List<ModProblem> validate(@NonNull String manifestText) {
        List<ModProblem> problems = new ArrayList<>();
        ModManifest manifest = ModManifest.parse(manifestText, problems);
        if (manifest != null)
            problems.addAll(validate(manifest, null));

        return problems;
    }

    /**
     * Validates a manifest.
     * @param manifest the manifest to validate
     * @param packagePaths the zip paths the package holds, to check that path values exist; null skips that check
     * @return problems
     */
    public List<ModProblem> validate(@NonNull ModManifest manifest, Set<String> packagePaths) {
        List<ModProblem> problems = new ArrayList<>();
        Config root = manifest.getConfig();

        if (!root.getKeyValuePairs().isEmpty())
            problems.add(ModProblem.error(null, null, "Keys appear before the first section: " + ModManifest.getKeys(root) + "."));
        if (hasText(root))
            problems.add(ModProblem.warning(null, null, "Text appears before the first section and is ignored."));

        checkDuplicateChildren(root, null, problems);
        validateMod(manifest, problems);
        validateTargets(manifest, problems);
        validateFiles(manifest, problems);

        for (Config section : root.getChildConfigNodes()) {
            String name = section.getSectionName();
            if (ModFormatConstants.SECTION_OVERRIDES.equals(name)) {
                validateOverrides(manifest, section, packagePaths, problems);
                continue;
            }

            SectionRule rule = this.sectionRules.get(name);
            if (rule == null) {
                problems.add(ModProblem.error(name, null, "Unknown section [" + name + "]."));
                continue;
            }

            validateSection(section, rule, name, packagePaths, problems);
        }

        return problems;
    }

    private void validateMod(ModManifest manifest, List<ModProblem> problems) {
        String section = ModFormatConstants.SECTION_MOD;
        Config mod = manifest.getSection(section);
        if (mod == null) {
            problems.add(ModProblem.error(section, null, "The required section [Mod] is missing."));
            return;
        }

        for (String key : REQUIRED_MOD_KEYS)
            if (StringUtils.isNullOrWhiteSpace(ModManifest.getString(mod, key)))
                problems.add(ModProblem.error(section, key, "The required key '" + key + "' is missing or empty."));

        String formatText = ModManifest.getString(mod, ModFormatConstants.KEY_FORMAT_VERSION);
        if (formatText != null) {
            Integer formatVersion = ModManifest.parseInteger(formatText);
            if (formatVersion == null || formatVersion < 1) {
                problems.add(ModProblem.error(section, ModFormatConstants.KEY_FORMAT_VERSION, "'" + formatText + "' is not a positive integer."));
            } else if (formatVersion > ModFormatConstants.FORMAT_VERSION) {
                problems.add(ModProblem.error(section, ModFormatConstants.KEY_FORMAT_VERSION, "Format version " + formatVersion
                        + " is newer than the supported version " + ModFormatConstants.FORMAT_VERSION + "."));
            }
        }

        String game = manifest.getGame();
        if (game != null && this.expectedGame != null && !this.expectedGame.equals(game))
            problems.add(ModProblem.error(section, ModFormatConstants.KEY_GAME, "The package is for '" + game + "', not '" + this.expectedGame + "'."));

        String id = manifest.getId();
        if (!StringUtils.isNullOrWhiteSpace(id)) {
            Matcher matcher = ModFormatConstants.UUID_PATTERN.matcher(id);
            if (!matcher.matches()) {
                problems.add(ModProblem.error(section, ModFormatConstants.KEY_ID, "'" + id + "' is not a UUID."));
            } else if (!"4".equals(matcher.group(1)) || "89abAB".indexOf(matcher.group(2).charAt(0)) < 0) {
                problems.add(ModProblem.warning(section, ModFormatConstants.KEY_ID, "'" + id + "' is a UUID but not a version 4 UUID."));
            }
        }

        String version = manifest.getVersion();
        if (!StringUtils.isNullOrWhiteSpace(version) && !ModFormatConstants.SEMVER_PATTERN.matcher(version).matches())
            problems.add(ModProblem.error(section, ModFormatConstants.KEY_VERSION, "'" + version + "' is not a semantic version (MAJOR.MINOR.PATCH)."));
    }

    private void validateTargets(ModManifest manifest, List<ModProblem> problems) {
        String section = ModFormatConstants.SECTION_TARGETS;
        Config targets = manifest.getSection(section);
        if (targets == null || targets.getChildConfigNodes().isEmpty()) {
            problems.add(ModProblem.error(section, null, "At least one target is required in [Targets]."));
            return;
        }

        if (hasText(targets))
            problems.add(ModProblem.warning(section, null, "Text lines in [Targets] are ignored."));

        Set<String> seenAliases = new HashSet<>();
        for (Config target : targets.getChildConfigNodes()) {
            String sectionPath = section + "/" + target.getSectionName();
            String checksums = ModManifest.getString(target, ModFormatConstants.KEY_EXE_CHECKSUM);
            List<String> checksumTexts = ModManifest.parseList(checksums);
            if (checksumTexts != null) {
                for (String checksumText : checksumTexts) {
                    Long checksum = ModManifest.parseLong(checksumText);
                    if (checksum == null || checksum < 0 || checksum > 0xFFFFFFFFL)
                        problems.add(ModProblem.error(sectionPath, ModFormatConstants.KEY_EXE_CHECKSUM, "'" + checksumText + "' is not a CRC32 value."));
                }
            }

            List<String> aliases = ModManifest.parseList(ModManifest.getString(target, ModFormatConstants.KEY_ALIASES));
            if (aliases != null)
                for (String alias : aliases)
                    if (!seenAliases.add(alias))
                        problems.add(ModProblem.error(sectionPath, ModFormatConstants.KEY_ALIASES, "The alias '" + alias + "' is listed more than once."));

            if (!target.getChildConfigNodes().isEmpty())
                problems.add(ModProblem.warning(sectionPath, null, "Child sections of a target are ignored."));
        }
    }

    private void validateFiles(ModManifest manifest, List<ModProblem> problems) {
        String section = ModFormatConstants.SECTION_FILES;
        Config files = manifest.getSection(section);
        if (files == null)
            return;

        String contentHash = manifest.getContentHash();
        if (contentHash != null && !ModFormatConstants.SHA1_PATTERN.matcher(contentHash).matches()) {
            problems.add(ModProblem.error(section, ModFormatConstants.KEY_CONTENT_HASH, "'" + contentHash + "' is not a SHA-1 hash."));
        }

        Set<String> seenPaths = new HashSet<>();
        for (String line : manifest.getFilesLines()) {
            String[] parsed = ModFormatConstants.parseFilesLine(line);
            if (parsed == null) {
                problems.add(ModProblem.error(section, null, "'" + line + "' is not a '<sha1> <size> <path>' line."));
                continue;
            }

            String pathProblem = ModFormatConstants.getPathProblem(parsed[2]);
            if (pathProblem != null)
                problems.add(ModProblem.error(section, null, "Invalid path: " + pathProblem + "."));
            if (!seenPaths.add(parsed[2]))
                problems.add(ModProblem.error(section, null, "The path '" + parsed[2] + "' is listed more than once."));
        }
    }

    private void validateOverrides(ModManifest manifest, Config overrides, Set<String> packagePaths, List<ModProblem> problems) {
        String section = ModFormatConstants.SECTION_OVERRIDES;
        if (!overrides.getKeyValuePairs().isEmpty())
            problems.add(ModProblem.error(section, null, "[Overrides] holds only target children; keys found: " + ModManifest.getKeys(overrides) + "."));
        if (hasText(overrides))
            problems.add(ModProblem.warning(section, null, "Text lines in [Overrides] are ignored."));

        for (Config override : overrides.getChildConfigNodes()) {
            String targetName = override.getSectionName();
            String overridePath = section + "/" + targetName;
            ModTarget target = manifest.findTarget(targetName);
            if (target == null || !target.getName().equals(targetName))
                problems.add(ModProblem.error(overridePath, null, "'" + targetName + "' is not a target listed in [Targets]."));
            if (!override.getKeyValuePairs().isEmpty() || hasText(override))
                problems.add(ModProblem.error(overridePath, null, "An override holds only sections shaped like the root."));

            for (Config overrideSection : override.getChildConfigNodes()) {
                String name = overrideSection.getSectionName();
                String path = overridePath + "/" + name;
                if (ModFormatConstants.SECTION_MOD.equals(name) || ModFormatConstants.SECTION_TARGETS.equals(name)
                        || ModFormatConstants.SECTION_OVERRIDES.equals(name) || ModFormatConstants.SECTION_FILES.equals(name)) {
                    problems.add(ModProblem.error(path, null, "[" + name + "] cannot be overridden per target."));
                    continue;
                }

                SectionRule rule = this.sectionRules.get(name);
                if (rule == null) {
                    problems.add(ModProblem.error(path, null, "Unknown section [" + name + "]."));
                    continue;
                }

                validateSection(overrideSection, rule, path, packagePaths, problems);
            }
        }
    }

    private void validateSection(Config section, SectionRule rule, String sectionPath, Set<String> packagePaths, List<ModProblem> problems) {
        validateKeys(section, rule, sectionPath, rule.getKeys(), packagePaths, problems);
        if (!rule.isTextAllowed() && hasText(section))
            problems.add(ModProblem.warning(sectionPath, null, "Text lines in [" + rule.getName() + "] are ignored."));

        if (rule.isChildrenForbidden() && !section.getChildConfigNodes().isEmpty()) {
            problems.add(ModProblem.error(sectionPath, null, "[" + rule.getName() + "] does not take child sections."));
            return;
        }

        for (Config child : section.getChildConfigNodes()) {
            String childPath = sectionPath + "/" + child.getSectionName();
            if (rule.getAllowedChildNames() != null && !rule.getAllowedChildNames().contains(child.getSectionName())) {
                problems.add(rule.isStrict()
                        ? ModProblem.error(childPath, null, "Unknown child section '" + child.getSectionName() + "'.")
                        : ModProblem.warning(childPath, null, "Unknown child section '" + child.getSectionName() + "'."));
                continue;
            }

            validateDescendant(child, rule, childPath, packagePaths, problems);
        }
    }

    private void validateDescendant(Config section, SectionRule rule, String sectionPath, Set<String> packagePaths, List<ModProblem> problems) {
        validateKeys(section, rule, sectionPath, null, packagePaths, problems);
        if (!rule.isDescendantTextAllowed() && hasText(section))
            problems.add(ModProblem.warning(sectionPath, null, "Text lines here are ignored."));

        for (Config child : section.getChildConfigNodes())
            validateDescendant(child, rule, sectionPath + "/" + child.getSectionName(), packagePaths, problems);
    }

    private void validateKeys(Config section, SectionRule rule, String sectionPath, Set<String> allowedKeys, Set<String> packagePaths, List<ModProblem> problems) {
        for (String key : ModManifest.getKeys(section)) {
            if (!ModFormatConstants.KEY_PATTERN.matcher(key).matches())
                problems.add(ModProblem.error(sectionPath, key, "'" + key + "' is not a valid key name."));

            boolean known = allowedKeys != null ? allowedKeys.contains(key) : rule.isDescendantKeyAllowed(key);
            if (!known) {
                problems.add(rule.isStrict()
                        ? ModProblem.error(sectionPath, key, "Unknown key '" + key + "'.")
                        : ModProblem.warning(sectionPath, key, "Unknown key '" + key + "'."));
                continue;
            }

            String value = ModManifest.getString(section, key);
            if (rule.getPathKeys().contains(key)) {
                checkPath(sectionPath, key, value, packagePaths, problems);
            } else if (rule.getPathListKeys().contains(key)) {
                List<String> paths = ModManifest.parseList(value);
                if (paths != null)
                    for (String path : paths)
                        checkPath(sectionPath, key, path, packagePaths, problems);
            }
        }

        checkDuplicateChildren(section, sectionPath, problems);
    }

    private static void checkPath(String sectionPath, String key, String path, Set<String> packagePaths, List<ModProblem> problems) {
        String pathProblem = ModFormatConstants.getPathProblem(path);
        if (pathProblem != null) {
            problems.add(ModProblem.error(sectionPath, key, "Invalid path: " + pathProblem + "."));
        } else if (packagePaths != null && !packagePaths.contains(path)) {
            problems.add(ModProblem.error(sectionPath, key, "The package does not contain '" + path + "'."));
        }
    }

    private static void checkDuplicateChildren(Config section, String sectionPath, List<ModProblem> problems) {
        Set<String> seen = new HashSet<>();
        for (Config child : section.getChildConfigNodes())
            if (!seen.add(child.getSectionName()))
                problems.add(ModProblem.error(sectionPath, null, "The section '" + child.getSectionName() + "' appears more than once"
                        + (sectionPath != null ? " here." : " at the top level.")));
    }

    private static boolean hasText(Config section) {
        for (String line : ModManifest.getTextLines(section))
            if (!StringUtils.isNullOrWhiteSpace(line))
                return true;

        return false;
    }

    /**
     * The rules for one top-level manifest section and the child sections below it.
     * Created by jimbolina on 11/6/2026.
     */
    @Getter
    public static class SectionRule {
        @NonNull private final String name;
        private final Set<String> keys = new HashSet<>();
        private final Set<String> descendantKeys = new HashSet<>();
        private final List<Pattern> descendantKeyPatterns = new ArrayList<>();
        private final Set<String> pathKeys = new HashSet<>();
        private final Set<String> pathListKeys = new HashSet<>();
        private Set<String> allowedChildNames;
        private boolean anyDescendantKey;
        private boolean strict;
        private boolean textAllowed;
        private boolean descendantTextAllowed;
        private boolean childrenForbidden;

        public SectionRule(@NonNull String name) {
            this.name = name;
        }

        /**
         * Allows keys directly in the section.
         * @param keyNames the key names
         * @return this
         */
        public SectionRule allowKeys(String... keyNames) {
            Collections.addAll(this.keys, keyNames);
            return this;
        }

        /**
         * Allows keys in any child section, at any depth.
         * @param keyNames the key names
         * @return this
         */
        public SectionRule allowDescendantKeys(String... keyNames) {
            Collections.addAll(this.descendantKeys, keyNames);
            return this;
        }

        /**
         * Allows keys matching a regular expression in any child section.
         * @param regex the full-match pattern
         * @return this
         */
        public SectionRule allowDescendantKeyPattern(String regex) {
            this.descendantKeyPatterns.add(Pattern.compile(regex));
            return this;
        }

        /**
         * Allows any key in child sections, such as patch arguments.
         * @return this
         */
        public SectionRule allowAnyDescendantKey() {
            this.anyDescendantKey = true;
            return this;
        }

        /**
         * Marks keys whose values are zip paths.
         * @param keyNames the key names
         * @return this
         */
        public SectionRule pathKeys(String... keyNames) {
            Collections.addAll(this.pathKeys, keyNames);
            return this;
        }

        /**
         * Marks keys whose values are comma-separated lists of zip paths.
         * @param keyNames the key names
         * @return this
         */
        public SectionRule pathListKeys(String... keyNames) {
            Collections.addAll(this.pathListKeys, keyNames);
            return this;
        }

        /**
         * Restricts the names of direct child sections.
         * @param childNames the allowed names
         * @return this
         */
        public SectionRule allowChildren(String... childNames) {
            this.allowedChildNames = new HashSet<>(Arrays.asList(childNames));
            return this;
        }

        /**
         * Forbids child sections.
         * @return this
         */
        public SectionRule noChildren() {
            this.childrenForbidden = true;
            return this;
        }

        /**
         * Makes unknown keys and child names errors instead of warnings.
         * @return this
         */
        public SectionRule strict() {
            this.strict = true;
            return this;
        }

        /**
         * Allows text lines directly in the section.
         * @return this
         */
        public SectionRule allowText() {
            this.textAllowed = true;
            return this;
        }

        /**
         * Allows text lines in child sections.
         * @return this
         */
        public SectionRule allowDescendantText() {
            this.descendantTextAllowed = true;
            return this;
        }

        /**
         * Tests whether a key is allowed in a child section.
         * @param key the key name
         * @return true iff allowed
         */
        public boolean isDescendantKeyAllowed(String key) {
            if (this.anyDescendantKey || this.descendantKeys.contains(key))
                return true;

            for (Pattern pattern : this.descendantKeyPatterns)
                if (pattern.matcher(key).matches())
                    return true;

            return false;
        }
    }
}
