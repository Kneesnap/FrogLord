package net.highwayfrogs.editor.games.psx.polygon;

import lombok.Getter;

/**
 * Represents different polygon types the PlayStation is capable of rendering.
 * Created by Kneesnap on 12/9/2023.
 */
@Getter
public enum PSXPolygonType {
    POLY_F3(false, false, 3),
    POLY_FT3(false, true, 3),
    POLY_F4(false, false, 4),
    POLY_FT4(false, true, 4),
    POLY_G3(true, false, 3),
    POLY_GT3(true, true, 3),
    POLY_G4(true, false, 4),
    POLY_GT4(true, true, 4);

    private final boolean gouraud;
    private final boolean textured;
    private final int verticeCount;
    private final String name;

    PSXPolygonType(boolean gouraud, boolean textured, int verticeCount) {
        this.gouraud = gouraud;
        this.textured = textured;
        this.verticeCount = verticeCount;
        this.name = name().substring("POLY_".length());
    }

    /**
     * Test if flat shading is enabled.
     */
    public boolean isFlat() {
        return !this.gouraud;
    }

    /**
     * Get the number of colours for this polygon.
     * @return colorCount
     */
    public int getColorCount() {
        return this.gouraud ? this.verticeCount : 1;
    }
}