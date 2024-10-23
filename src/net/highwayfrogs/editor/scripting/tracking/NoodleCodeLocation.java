package net.highwayfrogs.editor.scripting.tracking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a location in a Noodle source file.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoodleCodeLocation {
    private NoodleCodeSource source;
    private int lineNumber;
    private int linePosition;

    private static final int MAX_VALUE = 0xFFFF;

    public static final NoodleCodeLocation NULL_CODE_LOCATION = new NoodleCodeLocation(null, 0, 0);
}
