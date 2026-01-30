package net.highwayfrogs.editor.games.sony.shared.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMesh.SCShadedTextureManager;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygonPacket;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.TreeTextureAtlas;
import net.highwayfrogs.editor.gui.texture.basic.OutlineColorTextureSource;
import net.highwayfrogs.editor.gui.texture.basic.UnknownTextureSource;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;

/**
 * Represents the mesh for a MediEvil map.
 * Created by Kneesnap on 5/8/2024.
 */
@Getter
public class SCMapMesh extends PSXShadedDynamicMesh<SCMapPolygon, SCShadedTextureManager> {
    private final SCMapFile<?> map;
    private final SCMapMeshNode mainNode;
    private final DynamicMeshOverlayNode highlightedPolygonNode;
    private AtlasTexture flatPlaceholderTexture;
    private AtlasTexture gouraudPlaceholderTexture;

    public static final OutlineColorTextureSource CURSOR_COLOR = new OutlineColorTextureSource(Color.ORANGE, Color.BLACK);
    public static final OutlineColorTextureSource REMOVE_FACE_COLOR = new OutlineColorTextureSource(Color.RED, Color.BLACK);
    public static final OutlineColorTextureSource GRAY_COLOR = new OutlineColorTextureSource(Color.GRAY, Color.BLACK);
    public static final OutlineColorTextureSource YELLOW_COLOR = new OutlineColorTextureSource(Color.YELLOW, Color.BLACK);
    public static final OutlineColorTextureSource GREEN_COLOR = new OutlineColorTextureSource(Color.GREEN, Color.BLACK);

    public SCMapMesh(SCMapFile<?> mapFile) {
        super(new TreeTextureAtlas(64, 64, true), DynamicMeshTextureQuality.UNLIT_SHARP, true, mapFile.getFileDisplayName());
        this.map = mapFile;

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

    @Override
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

        setupShadedPolygons();
    }

    /**
     * Creates the mesh node containing the main mesh data.
     */
    protected SCMapMeshNode createMainNode() {
        return new SCMapMeshNode(this);
    }

    @Override
    public Collection<SCMapPolygon> getAllShadedPolygons() {
        SCMapPolygonPacket<?> polygonPacket = getMap().getPolygonPacket();
        if (polygonPacket == null) {
            getLogger().warning("Failed to obtain polygon packet from the map for shading purposes. No textures will be shown.");
            return Collections.emptyList();
        }

        // Add gouraud shaded polygons.
        return polygonPacket.getPolygons();
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
            return polygon.createPolygonShadeDefinition(getMesh(), getMesh().isShadingEnabled());
        }

        @Override
        protected void updateLooseShadingTexCoords() {
            // Don't have any loose shade definitions.
        }
    }
}