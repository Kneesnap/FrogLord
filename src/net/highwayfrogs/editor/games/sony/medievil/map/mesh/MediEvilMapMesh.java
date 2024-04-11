package net.highwayfrogs.editor.games.sony.medievil.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.psx.shading.IPSXShadedMesh;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;

import java.awt.*;

/**
 * Represents the mesh for a MediEvil map.
 * Cloned from a file created by Kneesnap on 03/9/2024.
 */
@Getter
public class MediEvilMapMesh extends DynamicMesh implements IPSXShadedMesh {
    private final MediEvilMapFile map;
    private final MediEvilMapMeshNode mainNode;
    private final DynamicMeshOverlayNode highlightedPolygonNode;
    private final MediEvilShadedTextureManager shadedTextureManager;
    private AtlasTexture flatPlaceholderTexture;
    private AtlasTexture gouraudPlaceholderTexture;
    private boolean shadingEnabled;

    public static final CursorVertexColor CURSOR_COLOR = new CursorVertexColor(Color.ORANGE, Color.BLACK);
    public static final CursorVertexColor REMOVE_FACE_COLOR = new CursorVertexColor(Color.RED, Color.BLACK);
    public static final CursorVertexColor GRAY_COLOR = new CursorVertexColor(Color.GRAY, Color.BLACK);
    public static final CursorVertexColor YELLOW_COLOR = new CursorVertexColor(Color.YELLOW, Color.BLACK);
    public static final CursorVertexColor GREEN_COLOR = new CursorVertexColor(Color.GREEN, Color.BLACK);

    public MediEvilMapMesh(MediEvilMapFile mapFile) {
        super(new SequentialTextureAtlas(64, 64, true));
        this.map = mapFile;
        this.shadedTextureManager = new MediEvilShadedTextureManager(this);

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupMapTextures();
        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = new MediEvilMapMeshNode(this);
        addNode(this.mainNode);

        this.highlightedPolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedPolygonNode);
    }

    private void setupBasicTextures() {
        getTextureAtlas().addTexture(CURSOR_COLOR);
        getTextureAtlas().addTexture(REMOVE_FACE_COLOR);
        getTextureAtlas().addTexture(GRAY_COLOR);
        getTextureAtlas().addTexture(YELLOW_COLOR);
        getTextureAtlas().addTexture(GREEN_COLOR);
        getTextureAtlas().addTexture(BakedLandscapeUIManager.MATERIAL_POLYGON_HIGHLIGHT);
        this.flatPlaceholderTexture = getTextureAtlas().addTexture(UnknownTextureSource.CYAN_INSTANCE);
        this.gouraudPlaceholderTexture = getTextureAtlas().addTexture(UnknownTextureSource.GREEN_INSTANCE);
    }

    private void setupMapTextures() {
        // Find the level table entry for the map this mesh represents.
        MediEvilLevelTableEntry levelEntry = getMap().getLevelTableEntry();
        if (levelEntry == null) {
            getLogger().warning("No level table entry was found, so map textures have not been loaded.");
            return;
        }

        // Add gouraud shaded polygons.
        for (MediEvilMapPolygon polygon : getMap().getGraphicsPacket().getPolygons())
            this.shadedTextureManager.addPolygon(polygon);
    }

    @Override
    public void setShadingEnabled(boolean newState) {
        if (this.shadingEnabled == newState)
            return;

        this.shadingEnabled = newState;

        getMesh().pushBatchOperations();
        getTextureAtlas().startBulkOperations();
        for (MediEvilMapPolygon polygon : getMap().getGraphicsPacket().getPolygons())
            this.shadedTextureManager.updatePolygon(polygon, polygon.createPolygonShadeDefinition(getMap(), newState));
        getTextureAtlas().endBulkOperations();
        getMesh().popBatchOperations();
    }

    public static class MediEvilShadedTextureManager extends PSXMeshShadedTextureManager<MediEvilMapPolygon> {
        public MediEvilShadedTextureManager(MediEvilMapMesh mesh) {
            super(mesh);
        }

        @Override
        public MediEvilMapMesh getMesh() {
            return (MediEvilMapMesh) super.getMesh();
        }

        @Override
        protected PSXShadeTextureDefinition createShadedTexture(MediEvilMapPolygon polygon) {
            return polygon.createPolygonShadeDefinition(getMesh().getMap(), getMesh().isShadingEnabled());
        }

        @Override
        protected void applyTextureShading(MediEvilMapPolygon polygon, PSXShadeTextureDefinition shadedTexture) {
            polygon.loadDataFromShadeDefinition(getMesh().getMap(), shadedTexture, getMesh().isShadingEnabled());
        }
    }
}