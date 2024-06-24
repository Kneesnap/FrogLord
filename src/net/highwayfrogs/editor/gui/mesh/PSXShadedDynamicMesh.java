package net.highwayfrogs.editor.gui.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.shading.IPSXShadedMesh;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;

import java.util.Collection;

/**
 * Represents a DynamicMesh with PSX shading.
 * Created by Kneesnap on 6/17/2024.
 */
@Getter
public abstract class PSXShadedDynamicMesh<TPolygon, TPSXShadingManager extends PSXShadedTextureManager<TPolygon>> extends DynamicMesh implements IPSXShadedMesh {
    private final TPSXShadingManager shadedTextureManager;
    private boolean shadingEnabled;

    public PSXShadedDynamicMesh(TextureAtlas atlas, boolean shadingEnabledByDefault) {
        super(atlas);
        this.shadingEnabled = shadingEnabledByDefault;
        this.shadedTextureManager = createShadedTextureManager();
    }

    @Override
    public void setShadingEnabled(boolean newState) {
        if (this.shadingEnabled == newState)
            return;

        this.shadingEnabled = newState;

        getMesh().pushBatchOperations();
        getTextureAtlas().startBulkOperations();
        for (TPolygon polygon : getAllShadedPolygons())
            this.shadedTextureManager.updatePolygon(polygon);
        getTextureAtlas().endBulkOperations();
        getMesh().popBatchOperations();
    }

    @Override
    protected void onFree() {
        super.onFree();
        this.shadedTextureManager.onDispose();
    }

    /**
     * Gets all the shaded polygons.
     */
    public abstract Collection<TPolygon> getAllShadedPolygons();

    /**
     * Sets up the shaded polygons.
     */
    protected void setupShadedPolygons() {
        for (TPolygon polygon : getAllShadedPolygons())
            this.shadedTextureManager.addPolygon(polygon);
    }

    /**
     * Creates the shaded texture manager for this mesh.
     */
    protected abstract TPSXShadingManager createShadedTextureManager();
}