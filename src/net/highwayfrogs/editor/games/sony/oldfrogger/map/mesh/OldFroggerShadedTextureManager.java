package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import lombok.Getter;
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
        return polygon.createPolygonShadeDefinition(this.mapMesh.getMap().getLevelTableEntry());
    }
}