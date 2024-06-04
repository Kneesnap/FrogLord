package net.highwayfrogs.editor.games.sony.shared.model.meshview;

import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.PTPolygon;

/**
 * Created by Kneesnap on 5/22/2024.
 */
public class PTModelShadedTextureManager extends PSXMeshShadedTextureManager<PTPolygon> {
    public PTModelShadedTextureManager(PTModelMesh modelMesh) {
        super(modelMesh);
    }

    @Override
    public PTModelMesh getMesh() {
        return (PTModelMesh) super.getMesh();
    }

    @Override
    protected PSXShadeTextureDefinition createShadedTexture(PTPolygon polygon) {
        return polygon.createPolygonShadeDefinition(getMesh().isShadingEnabled());
    }

    @Override
    protected void applyTextureShading(PTPolygon polygon, PSXShadeTextureDefinition shadedTexture) {
        polygon.loadDataFromShadeDefinition(shadedTexture, getMesh().isShadingEnabled());
    }
}