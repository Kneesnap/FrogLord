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

    public static final short NO_FORCED_CAMERA_DIRECTION = -1;

    /**
     * Gets the camera rotation from the ID, throwing if an invalid ID is provided.
     * Null will be returned if {@code NO_FORCED_CAMERA_DIRECTION} is provided.
     * @param id the ID to get the camera rotation from
     * @return cameraRotation
     */
    public static FroggerCameraRotation getCameraRotationFromID(short id) {
        if (id >= 0 && id < values().length) {
            return values()[id];
        } else if (id == NO_FORCED_CAMERA_DIRECTION) {
            return null;
        } else {
            throw new IllegalArgumentException("Invalid FroggerCameraRotation ID: " + id);
        }
    }

    /**
     * Gets the camera rotation ID from the provided rotation.
     * This must be static to support null camera rotations.
     * @param cameraRotation the rotation to get the ID from
     * @return cameraRotationId
     */
    public static short getCameraRotationID(FroggerCameraRotation cameraRotation) {
        return cameraRotation != null ? (short) cameraRotation.ordinal() : NO_FORCED_CAMERA_DIRECTION;
    }
}