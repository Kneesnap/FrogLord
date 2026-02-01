package net.highwayfrogs.editor.games.psx.image;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the PSX transparency modes (Sometimes called ABR).
 * These values are only used if the polygon has the semitransparent flag set.
 * NOTE: JavaFX is incapable of mimicking this behavior in 3D.
 * Created by Kneesnap on 12/16/2025.
 */
@Getter
@RequiredArgsConstructor
public enum PsxAbrTransparency {
    DEFAULT("Default", .5, .5, (byte) 0x7F), // (.5 * backgroundPixel) + (.5 * polygonPixel). -> Example: Water from Frogger.
    COMBINE("Combine", 1, 1D, (byte) 0x7F), // backgroundPixel + polygonPixel -> Example: Frogger 2D text images (I thought this should be 0xFF, but it breaks water if I do that)
    SUBTRACT("Subtract", 1, -1, (byte) 0xFF), // We don't really have a good way to approximate how this looks on PSX. PC doesn't even bother to implement it though.
    FAINT("Faint", 1, .25, (byte) 0x3F); // Acceptable PSX approximation.

    private final String displayName;
    private final double backgroundPixelColorMultiplier;
    private final double polygonPixelColorMultiplier;
    private final byte standaloneAlpha;
}
