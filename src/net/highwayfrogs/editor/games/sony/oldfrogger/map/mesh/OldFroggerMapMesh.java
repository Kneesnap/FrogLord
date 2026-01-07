package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerLevelTableEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapVersion;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapGridHeaderPacket.OldFroggerMapGrid;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;
import net.highwayfrogs.editor.gui.texture.basic.OutlineColorTextureSource;
import net.highwayfrogs.editor.gui.texture.basic.UnknownTextureSource;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents the mesh for a pre-recode frogger map.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerMapMesh extends PSXShadedDynamicMesh<OldFroggerMapPolygon, OldFroggerShadedTextureManager> {
    private final OldFroggerMapFile map;
    private final OldFroggerMapMeshNode mainNode;
    private final DynamicMeshOverlayNode highlightedPolygonNode;
    private AtlasTexture flatPlaceholderTexture;

    public static final OutlineColorTextureSource CURSOR_COLOR = new OutlineColorTextureSource(Color.ORANGE, Color.BLACK);
    public static final OutlineColorTextureSource REMOVE_FACE_COLOR = new OutlineColorTextureSource(Color.RED, Color.BLACK);
    public static final OutlineColorTextureSource GRAY_COLOR = new OutlineColorTextureSource(Color.GRAY, Color.BLACK);
    public static final OutlineColorTextureSource YELLOW_COLOR = new OutlineColorTextureSource(Color.YELLOW, Color.BLACK);
    public static final OutlineColorTextureSource GREEN_COLOR = new OutlineColorTextureSource(Color.GREEN, Color.BLACK);

    public OldFroggerMapMesh(OldFroggerMapFile mapFile) {
        super(new SequentialTextureAtlas(64, 64, true), DynamicMeshTextureQuality.UNLIT_SHARP, true, mapFile.getFileDisplayName());
        this.map = mapFile;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupMapTextures();
        getTextureAtlas().endBulkOperations();

        // Setup main node.
        if (mapFile.getMapConfig().getVersion() == OldFroggerMapVersion.EARLY) {
            // TODO: This is temporary, until we support the differences in the format.
            this.mainNode = null;
        } else {
            this.mainNode = new OldFroggerMapMeshNode(this);
            addNode(this.mainNode);
        }

        this.highlightedPolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedPolygonNode);
    }

    private void setupBasicTextures() {
        getTextureAtlas().addTexture(CURSOR_COLOR);
        getTextureAtlas().addTexture(REMOVE_FACE_COLOR);
        getTextureAtlas().addTexture(GRAY_COLOR);
        getTextureAtlas().addTexture(YELLOW_COLOR);
        getTextureAtlas().addTexture(GREEN_COLOR);
        this.flatPlaceholderTexture = getTextureAtlas().addTexture(UnknownTextureSource.CYAN_INSTANCE);
    }

    private void setupMapTextures() {
        // Find the level table entry for the map this mesh represents.
        OldFroggerLevelTableEntry levelEntry = getMap().getLevelTableEntry();
        if (levelEntry == null) {
            getLogger().warning("No level table entry was found, so map textures have not been loaded.");
            return;
        }

        setupShadedPolygons();
    }

    @Override
    public Collection<OldFroggerMapPolygon> getAllShadedPolygons() {
        List<OldFroggerMapPolygon> polygons = new ArrayList<>();
        if (getMap().getFormatVersion() == OldFroggerMapVersion.MILESTONE3)
            for (OldFroggerMapGrid grid : getMap().getGridPacket().getGrids())
                polygons.addAll(grid.getPolygons());
        return polygons;
    }

    @Override
    protected OldFroggerShadedTextureManager createShadedTextureManager() {
        return new OldFroggerShadedTextureManager(this);
    }
}