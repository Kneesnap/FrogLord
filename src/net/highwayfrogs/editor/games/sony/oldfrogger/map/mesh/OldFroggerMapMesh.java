package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerLevelTableEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapVersion;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapGridHeaderPacket.OldFroggerMapGrid;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;

import java.awt.*;

/**
 * Represents the mesh for a pre-recode frogger map.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerMapMesh extends DynamicMesh {
    private final OldFroggerMapFile map;
    private final OldFroggerMapMeshNode mainNode;
    private final OldFroggerShadedTextureManager shadedTextureManager;
    private AtlasTexture flatPlaceholderTexture;
    private AtlasTexture gouarudPlaceholderTexture;

    public static final CursorVertexColor CURSOR_COLOR = new CursorVertexColor(Color.ORANGE, Color.BLACK);
    public static final CursorVertexColor REMOVE_FACE_COLOR = new CursorVertexColor(Color.RED, Color.BLACK);
    public static final CursorVertexColor GRAY_COLOR = new CursorVertexColor(Color.GRAY, Color.BLACK);
    public static final CursorVertexColor YELLOW_COLOR = new CursorVertexColor(Color.YELLOW, Color.BLACK);
    public static final CursorVertexColor GREEN_COLOR = new CursorVertexColor(Color.GREEN, Color.BLACK);

    public OldFroggerMapMesh(OldFroggerMapFile mapFile) {
        super(new SequentialTextureAtlas(64, 64, true));
        this.map = mapFile;
        this.shadedTextureManager = new OldFroggerShadedTextureManager(this);

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
    }

    private void setupBasicTextures() {
        getTextureAtlas().addTexture(CURSOR_COLOR);
        getTextureAtlas().addTexture(REMOVE_FACE_COLOR);
        getTextureAtlas().addTexture(GRAY_COLOR);
        getTextureAtlas().addTexture(YELLOW_COLOR);
        getTextureAtlas().addTexture(GREEN_COLOR);
        this.flatPlaceholderTexture = getTextureAtlas().addTexture(UnknownTextureSource.CYAN_INSTANCE);
        this.gouarudPlaceholderTexture = getTextureAtlas().addTexture(UnknownTextureSource.GREEN_INSTANCE);
    }

    private void setupMapTextures() {
        // Find the level table entry for the map this mesh represents.
        OldFroggerLevelTableEntry levelEntry = getMap().getLevelTableEntry();
        if (levelEntry == null) {
            getLogger().warning("No level table entry was found, so map textures have not been loaded.");
            return;
        }

        // Add gouraud shaded stuff.
        if (getMap().getFormatVersion() == OldFroggerMapVersion.MILESTONE3)
            for (OldFroggerMapGrid grid : getMap().getGridPacket().getGrids())
                for (OldFroggerMapPolygon polygon : grid.getPolygons())
                    this.shadedTextureManager.addPolygon(polygon);
    }
}