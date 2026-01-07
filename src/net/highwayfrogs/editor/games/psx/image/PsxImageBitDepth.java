package net.highwayfrogs.editor.games.psx.image;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the bit depth of a psx image.
 * Based on the specification of the .TMD file format <a href="https://psx.arthus.net/sdk/Psy-Q/DOCS/Devrefs/Filefrmt.pdf"/>.
 * Created by Kneesnap on 11/29/2025.
 */
@Getter
@RequiredArgsConstructor
public enum PsxImageBitDepth {
    CLUT4("4-Bit CLUT", 4, 4),
    CLUT8("8-Bit CLUT", 8, 2),
    SBGR1555("15-Bit Color", 16, 1); // STP 1, Blue 5, Green 5, Red 5
    // NOTE: Some sources claim .TIM files allegedly support 24-bit color.
    // After doing some digging, I couldn't find any case where this can be used with polygons, although Frogger uses 24-bit color mode for video playback.
    // DuckStation doesn't seem to implement it for polygons, nor does the official SDK documentation mention such capability.
    // So while this technically appears to be valid, its use is rare and probably breaks things like .VLO format, although this is untested.
    // I don't even think Frogger or any other Millennium game render in-game with this amount of color depth, and I'd be surprised if others did too,
    //  so I'll just leave this commented out for now and not worry about it.
    //RGB888(24, 2);

    private final String displayName;
    private final int bitsPerPixel;
    private final int pixelMultiple; // The width must be a multiple of this value.
}
