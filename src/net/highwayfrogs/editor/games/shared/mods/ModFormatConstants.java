package net.highwayfrogs.editor.games.shared.mods;

import net.highwayfrogs.editor.utils.Utils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Constants and small rules of the mod package format shared by every game: names, folders, paths and the [Files] fingerprint.
 * Created by jimbolina on 11/6/2026.
 */
public class ModFormatConstants {
    public static final int FORMAT_VERSION = 1;
    public static final String MANIFEST_NAME = "mod.cfg";
    public static final String EXTENSION = ".mod";

    public static final String FOLDER_MAPS = "maps/";
    public static final String FOLDER_MODELS = "models/";
    public static final String FOLDER_IMAGES = "images/";
    public static final String FOLDER_DEMOS = "demos/";
    public static final String FOLDER_PATCHES = "patches/";
    public static final String FOLDER_SCREENSHOTS = "screenshots/";

    public static final String SECTION_MOD = "Mod";
    public static final String SECTION_TARGETS = "Targets";
    public static final String SECTION_OVERRIDES = "Overrides";
    public static final String SECTION_FILES = "Files";

    public static final String KEY_FORMAT_VERSION = "formatVersion";
    public static final String KEY_GAME = "game";
    public static final String KEY_ID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_VERSION = "version";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_LICENCE = "licence";
    public static final String KEY_HOMEPAGE = "homepage";
    public static final String KEY_ICON = "icon";
    public static final String KEY_SCREENSHOTS = "screenshots";
    public static final String KEY_EXPORTED_BY = "exportedBy";
    public static final String KEY_MIN_FROGLORD_VERSION = "minFrogLordVersion";
    public static final String KEY_EXE_CHECKSUM = "exeChecksum";
    public static final String KEY_ALIASES = "aliases";
    public static final String KEY_CONTENT_HASH = "contentHash";

    public static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]*");
    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-([0-9a-fA-F])[0-9a-fA-F]{3}-([0-9a-fA-F])[0-9a-fA-F]{3}-[0-9a-fA-F]{12}");
    public static final Pattern SEMVER_PATTERN = Pattern.compile("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(-[0-9A-Za-z.-]+)?(\\+[0-9A-Za-z.-]+)?");
    public static final Pattern SHA1_PATTERN = Pattern.compile("[0-9a-fA-F]{40}");
    private static final Pattern FILES_LINE_PATTERN = Pattern.compile("([0-9a-fA-F]{40}) ([0-9]+) (.+)");

    private ModFormatConstants() {
    }

    /**
     * Checks a zip entry path or a manifest path value against the format's path rules.
     * Paths use '/', are relative, are not empty, and contain no empty, '.' or '..' segments, backslashes, drive letters or control characters.
     * @param path the path to check
     * @return null if the path is valid, otherwise the reason it is not
     */
    public static String getPathProblem(String path) {
        if (path == null || path.isEmpty())
            return "the path is empty";
        if (path.indexOf('\\') >= 0)
            return "the path '" + path + "' uses '\\' instead of '/'";
        if (path.startsWith("/"))
            return "the path '" + path + "' is absolute";
        if (path.length() >= 2 && path.charAt(1) == ':')
            return "the path '" + path + "' starts with a drive letter";

        for (int i = 0; i < path.length(); i++)
            if (Character.isISOControl(path.charAt(i)))
                return "the path '" + path + "' contains a control character";

        String[] segments = path.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty())
                return "the path '" + path + "' has an empty segment";
            if (segment.equals(".") || segment.equals(".."))
                return "the path '" + path + "' contains '" + segment + "'";
        }

        return null;
    }

    /**
     * Tests whether a path follows the format's path rules.
     * @param path the path to test
     * @return true iff the path is valid
     */
    public static boolean isValidPath(String path) {
        return getPathProblem(path) == null;
    }

    /**
     * Formats one [Files] line in its canonical form.
     * @param sha1 the SHA-1 of the file contents as hex
     * @param size the size of the file in bytes
     * @param path the zip path of the file
     * @return filesLine
     */
    public static String formatFilesLine(String sha1, long size, String path) {
        return sha1.toLowerCase(Locale.ROOT) + " " + size + " " + path;
    }

    /**
     * Parses a [Files] line of the form '&lt;sha1&gt; &lt;size&gt; &lt;path&gt;'.
     * @param line the line to parse
     * @return {sha1 (lower case), size, path}, or null if the line is malformed
     */
    public static String[] parseFilesLine(String line) {
        if (line == null)
            return null;

        Matcher matcher = FILES_LINE_PATTERN.matcher(line.trim());
        if (!matcher.matches())
            return null;

        try {
            Long.parseLong(matcher.group(2));
        } catch (NumberFormatException ex) {
            return null;
        }

        return new String[] {matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2), matcher.group(3)};
    }

    /**
     * Calculates the content hash: the SHA-1 over the canonical [Files] lines in order, each encoded as UTF-8 and followed by a line feed.
     * @param canonicalLines the [Files] lines in canonical form
     * @return contentHash as lower case hex
     */
    public static String calculateContentHash(List<String> canonicalLines) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < canonicalLines.size(); i++)
            builder.append(canonicalLines.get(i)).append('\n');

        return Utils.calculateSHA1Hash(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Calculates the SHA-1 of file contents.
     * @param data the bytes to hash
     * @return sha1 as lower case hex
     */
    public static String calculateFileHash(byte[] data) {
        return Utils.calculateSHA1Hash(data);
    }
}
