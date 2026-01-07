package net.highwayfrogs.editor.games.sony.shared.model.meshview;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.shared.model.PTModel;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.IPTPrimitive;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.PTPolygon;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.PTPrimitiveBlock;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPart;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPartCel;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.TreeTextureAtlas;
import net.highwayfrogs.editor.gui.texture.basic.UnknownTextureSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a model mesh.
 * Created by Kneesnap on 5/22/2024.
 */
@Getter
public class PTModelMesh extends PSXShadedDynamicMesh<PTPolygon, PTModelShadedTextureManager> {
    private final PTModel model;
    private final PTModelMeshNode mainNode;
    private AtlasTexture flatPlaceholderTexture;
    private AtlasTexture gouraudPlaceholderTexture;

    public PTModelMesh(PTModel model) {
        super(new TreeTextureAtlas(64, 64, true), DynamicMeshTextureQuality.UNLIT_SHARP, false, model.getName());
        this.model = model;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupShadedPolygons();
        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = new PTModelMeshNode(this);
        addNode(this.mainNode);
    }

    @Override
    public Collection<PTPolygon> getAllShadedPolygons() {
        List<PTPolygon> polygons = new ArrayList<>();
        for (PTStaticPart part : this.model.getStaticMeshFile().getParts())
            for (PTStaticPartCel partCel : part.getPartCels())
                for (PTPrimitiveBlock block : partCel.getPrimitiveBlocks())
                    for (IPTPrimitive primitive : block.getPrimitives())
                        if (primitive instanceof PTPolygon)
                            polygons.add((PTPolygon) primitive);

        return polygons;
    }

    private void setupBasicTextures() {
        this.flatPlaceholderTexture = getTextureAtlas().addTexture(UnknownTextureSource.CYAN_INSTANCE);
        this.gouraudPlaceholderTexture = getTextureAtlas().addTexture(UnknownTextureSource.GREEN_INSTANCE);
    }

    @Override
    protected PTModelShadedTextureManager createShadedTextureManager() {
        return new PTModelShadedTextureManager(this);
    }
}