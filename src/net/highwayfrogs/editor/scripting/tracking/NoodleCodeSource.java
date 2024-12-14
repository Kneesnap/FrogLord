package net.highwayfrogs.editor.scripting.tracking;

import lombok.Getter;

/**
 * Represents the source location (eg. file) of Noodle source code.
 */
@Getter
public abstract class NoodleCodeSource {
    /**
     * Gets the display string which represents the source location.
     */
    public abstract String getDisplay();
}
