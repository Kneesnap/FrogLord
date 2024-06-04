package net.highwayfrogs.editor.games.konami.greatquest.animation;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The name is probably wrong, as the original name is unknown.
 * This represents the distinction between kcControlType values as seen in 'kcControlCreate'.
 * Created by Kneesnap on 5/2/2024.
 */
@Getter
@AllArgsConstructor
public enum kcControlClassType {
    STANDARD(144),
    PRS(416); // Position, Rotation, Scale

    private final int runtimeSizeInBytes;
}
