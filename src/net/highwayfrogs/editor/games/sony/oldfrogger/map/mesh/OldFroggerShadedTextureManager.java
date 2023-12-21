package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.sony.shared.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.shared.shading.PSXShadedTextureManager.PSXShadedTextureAtlasManager;

/**
 * Manages shaded textures for old Frogger.
 * Created by Kneesnap on 12/19/2023.
 */
@Getter
public class OldFroggerShadedTextureManager extends PSXShadedTextureAtlasManager<OldFroggerMapPolygon> {
    private final OldFroggerMapMesh mapMesh;

    public OldFroggerShadedTextureManager(OldFroggerMapMesh mapMesh) {
        super(mapMesh.getTextureAtlas());
        this.mapMesh = mapMesh;
    }

    @Override
    protected PSXShadeTextureDefinition createShadedTexture(OldFroggerMapPolygon polygon) {
        PSXShadeTextureDefinition definition = polygon.createPolygonShadeDefinition(this.mapMesh.getMap().getLevelTableEntry());

        // Enable max brightness for cave levels. (The game lights areas with code)
        if (isCaveLightingEnabled(definition.getColors()))
            for (int i = 0; i < definition.getColors().length; i++)
                definition.getColors()[i].fromRGB(0xFFFFFF);

        return definition;
    }

    private boolean isCaveLightingEnabled(CVector[] colors) {
        if (!getMapMesh().getMap().getFileDisplayName().endsWith("CAVES.MAP"))
            return false; // Only the cave map has cave lighting.

        for (int i = 0; i < colors.length; i++) {
            CVector color = colors[i];
            if (color.getRed() != 0 || color.getGreen() != 0 || color.getBlue() != 0)
                return false; // Found a color shading value that wasn't zero.
        }

        return true;
    }
}