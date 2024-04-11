package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;

/**
 * Manages shaded textures for old Frogger.
 * Created by Kneesnap on 12/19/2023.
 */
@Getter
public class OldFroggerShadedTextureManager extends PSXMeshShadedTextureManager<OldFroggerMapPolygon> {
    public OldFroggerShadedTextureManager(OldFroggerMapMesh mapMesh) {
        super(mapMesh);
    }

    @Override
    public OldFroggerMapMesh getMesh() {
        return (OldFroggerMapMesh) super.getMesh();
    }

    @Override
    protected PSXShadeTextureDefinition createShadedTexture(OldFroggerMapPolygon polygon) {
        return polygon.createPolygonShadeDefinition(getMesh().getMap());
    }

    @Override
    protected void applyTextureShading(OldFroggerMapPolygon polygon, PSXShadeTextureDefinition shadedTexture) {
        polygon.loadDataFromShadeDefinition(getMesh().getMap(), shadedTexture);
    }
}