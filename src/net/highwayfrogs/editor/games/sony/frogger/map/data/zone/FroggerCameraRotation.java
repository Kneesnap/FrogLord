package net.highwayfrogs.editor.games.sony.frogger.map.data.zone;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A registry of different frogger camera rotations.
 * Created by Kneesnap on 5/27/2024.
 */
@Getter
@AllArgsConstructor
public enum FroggerCameraRotation {
    NORTH("↑ (North)"), // Towards Positive Z
    EAST("→ (East)"), // Towards Positive X
    SOUTH("↓ (South)"), // Towards Negative Z
    WEST("← (West)"); // Towards Negative X

    private final String displayString;
}