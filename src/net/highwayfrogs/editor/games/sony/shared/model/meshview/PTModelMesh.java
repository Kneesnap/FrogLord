package net.highwayfrogs.editor.games.sony.shared.model.meshview;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.psx.shading.IPSXShadedMesh;
import net.highwayfrogs.editor.games.sony.shared.model.PTModel;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.IPTPrimitive;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.PTPolygon;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.PTPrimitiveBlock;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPart;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPartCel;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;

/**
 * Represents a model mesh.
 * Created by Kneesnap on 5/22/2024.
 */
@Getter
public class PTModelMesh extends DynamicMesh implements IPSXShadedMesh {
    private final PTModel model;
    private final PTModelMeshNode mainNode;
    private AtlasTexture flatPlaceholderTexture;
    private AtlasTexture gouraudPlaceholderTexture;
    private boolean shadingEnabled;

    public PTModelMesh(PTModel model) {
        super(new SequentialTextureAtlas(64, 64, true));
        this.model = model;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupModelTextures();
        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = new PTModelMeshNode(this);
        addNode(this.mainNode);
    }

    @Override
    public void setShadingEnabled(boolean newState) {
        if (this.shadingEnabled == newState)
            return;

        this.shadingEnabled = newState;

        getMesh().pushBatchOperations();
        getTextureAtlas().startBulkOperations();
        for (PTStaticPart part : this.model.getStaticMeshFile().getParts())
            for (PTStaticPartCel partCel : part.getPartCels())
                for (PTPrimitiveBlock block : partCel.getPrimitiveBlocks())
                    for (IPTPrimitive primitive : block.getPrimitives())
                        if (primitive instanceof PTPolygon)
                            getShadedTextureManager().updatePolygon((PTPolygon) primitive, ((PTPolygon) primitive).createPolygonShadeDefinition(isShadingEnabled()));
        getTextureAtlas().endBulkOperations();
        getMesh().popBatchOperations();
    }

    private void setupBasicTextures() {
        this.flatPlaceholderTexture = getTextureAtlas().addTexture(UnknownTextureSource.CYAN_INSTANCE);
        this.gouraudPlaceholderTexture = getTextureAtlas().addTexture(UnknownTextureSource.GREEN_INSTANCE);
    }

    private void setupModelTextures() {
        // Add gouraud shaded stuff.
        for (PTStaticPart part : this.model.getStaticMeshFile().getParts())
            for (PTStaticPartCel partCel : part.getPartCels())
                for (PTPrimitiveBlock block : partCel.getPrimitiveBlocks())
                    for (IPTPrimitive primitive : block.getPrimitives())
                        if (primitive instanceof PTPolygon)
                            getShadedTextureManager().addPolygon((PTPolygon) primitive);
    }

    @Override
    public PTModelShadedTextureManager getShadedTextureManager() {
        return (PTModelShadedTextureManager) super.getShadedTextureManager();
    }

    @Override
    protected PTModelShadedTextureManager createShadedTextureManager() {
        return new PTModelShadedTextureManager(this);
    }
}