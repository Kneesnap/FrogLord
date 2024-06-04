package net.highwayfrogs.editor.games.sony.shared.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.psx.shading.IPSXShadedMesh;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygonPacket;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;

import java.awt.*;

/**
 * Represents the mesh for a MediEvil map.
 * Created by Kneesnap on 5/8/2024.
 */
@Getter
public class SCMapMesh extends DynamicMesh implements IPSXShadedMesh {
    private final SCMapFile<?> map;
    private final SCMapMeshNode mainNode;
    private final DynamicMeshOverlayNode highlightedPolygonNode;
    private final SCShadedTextureManager shadedTextureManager;
    private AtlasTexture flatPlaceholderTexture;
    private AtlasTexture gouraudPlaceholderTexture;
    private boolean shadingEnabled;

    public static final CursorVertexColor CURSOR_COLOR = new CursorVertexColor(Color.ORANGE, Color.BLACK);
    public static final CursorVertexColor REMOVE_FACE_COLOR = new CursorVertexColor(Color.RED, Color.BLACK);
    public static final CursorVertexColor GRAY_COLOR = new CursorVertexColor(Color.GRAY, Color.BLACK);
    public static final CursorVertexColor YELLOW_COLOR = new CursorVertexColor(Color.YELLOW, Color.BLACK);
    public static final CursorVertexColor GREEN_COLOR = new CursorVertexColor(Color.GREEN, Color.BLACK);

    public SCMapMesh(SCMapFile<?> mapFile) {
        super(new SequentialTextureAtlas(64, 64, true));
        this.map = mapFile;
        this.shadedTextureManager = createShadedTextureManager();

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupMapTextures();
        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = createMainNode();
        addNode(this.mainNode);

        this.highlightedPolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedPolygonNode);
    }

    /**
     * Creates the mesh node containing the main mesh data.
     */
    protected SCMapMeshNode createMainNode() {
        return new SCMapMeshNode(this);
    }

    /**
     * Creates the shaded texture manager.
     */
    protected SCShadedTextureManager createShadedTextureManager() {
        return new SCShadedTextureManager(this);
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
        ISCLevelTableEntry levelEntry = getMap().getLevelTableEntry();
        if (levelEntry == null) {
            getLogger().warning("No level table entry was found, so map textures have not been loaded.");
            return;
        }

        SCMapPolygonPacket<?> polygonPacket = getMap().getPolygonPacket();
        if (polygonPacket == null) {
            getLogger().warning("Failed to load polygon data from the map for shading purposes. No textures will be shown.");
            return;
        }

        // Add gouraud shaded polygons.
        for (SCMapPolygon polygon : polygonPacket.getPolygons())
            this.shadedTextureManager.addPolygon(polygon);
    }

    @Override
    public void setShadingEnabled(boolean newState) {
        if (this.shadingEnabled == newState)
            return;

        this.shadingEnabled = newState;

        // Update buffers.
        SCMapPolygonPacket<?> polygonPacket = getMap().getPolygonPacket();
        if (polygonPacket != null) {
            getMesh().pushBatchOperations();
            getTextureAtlas().startBulkOperations();
            for (SCMapPolygon polygon : polygonPacket.getPolygons())
                this.shadedTextureManager.updatePolygon(polygon, polygon.createPolygonShadeDefinition(getMap(), newState));
            getTextureAtlas().endBulkOperations();
            getMesh().popBatchOperations();
        }
    }

    /**
     * Manages the shaded texture recreations for the polygons in the map mesh.
     */
    public static class SCShadedTextureManager extends PSXMeshShadedTextureManager<SCMapPolygon> {
        public SCShadedTextureManager(SCMapMesh mesh) {
            super(mesh);
        }

        @Override
        public SCMapMesh getMesh() {
            return (SCMapMesh) super.getMesh();
        }

        @Override
        protected PSXShadeTextureDefinition createShadedTexture(SCMapPolygon polygon) {
            return polygon.createPolygonShadeDefinition(getMesh().getMap(), getMesh().isShadingEnabled());
        }

        @Override
        protected void applyTextureShading(SCMapPolygon polygon, PSXShadeTextureDefinition shadedTexture) {
            polygon.loadDataFromShadeDefinition(getMesh().getMap(), shadedTexture, getMesh().isShadingEnabled());
        }
    }
}