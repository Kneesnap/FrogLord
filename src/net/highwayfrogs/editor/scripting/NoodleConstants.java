package net.highwayfrogs.editor.scripting;

import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;

/**
 * Contains static constants for noodle scripts.
 */
public class NoodleConstants {
    public static final String NOODLE_CODE_TYPE_INFO = "Noodle Script";
    public static final String NOODLE_CODE_EXTENSION = "ndl";
    public static final int MAX_RECURSIVE_INCLUDES = 500;
    public static final BrowserFileType NOODLE_FILE_TYPE = new BrowserFileType(NOODLE_CODE_TYPE_INFO, NOODLE_CODE_EXTENSION);
    public static final SavedFilePath NOODLE_SCRIPT_FILE_PATH = new SavedFilePath("noodleScriptPath", "Please select the script to run...", NOODLE_FILE_TYPE);
}