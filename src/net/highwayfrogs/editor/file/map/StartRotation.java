package net.highwayfrogs.editor.file.map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the different values the frog's start rotation can be.
 * Created by Kneesnap on 1/26/2019.
 */
@Getter
@AllArgsConstructor
public enum StartRotation {
    NORTH("↑"), // 0
    EAST("→"), // 1
    SOUTH("↓"), // 2
    WEST("←"); // 3

    private final String arrow;
}
