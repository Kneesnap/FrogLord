package net.highwayfrogs.editor.games.sony.medievil.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;

import java.awt.*;

/**
 * Represents the mesh for a MediEvil map.
 * Cloned from a file created by Kneesnap on 03/9/2024.
 */
@Getter
public class MediEvilMapMesh extends DynamicMesh {
    private final MediEvilMapFile map;
    private final MediEvilMapMeshNode mainNode;
    //private final OldFroggerShadedTextureManager shadedTextureManager;
    private AtlasTexture flatPlaceholderTexture;
    private AtlasTexture gouraudPlaceholderTexture;

    public static final CursorVertexColor CURSOR_COLOR = new CursorVertexColor(Color.ORANGE, Color.BLACK);
    public static final CursorVertexColor REMOVE_FACE_COLOR = new CursorVertexColor(Color.RED, Color.BLACK);
    public static final CursorVertexColor GRAY_COLOR = new CursorVertexColor(Color.GRAY, Color.BLACK);
    public static final CursorVertexColor YELLOW_COLOR = new CursorVertexColor(Color.YELLOW, Color.BLACK);
    public static final CursorVertexColor GREEN_COLOR = new CursorVertexColor(Color.GREEN, Color.BLACK);

    public MediEvilMapMesh(MediEvilMapFile mapFile) {
        super(new SequentialTextureAtlas(64, 64, true));
        this.map = mapFile;
        //this.shadedTextureManager = new OldFroggerShadedTextureManager(this);

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupMapTextures();
        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = new MediEvilMapMeshNode(this);
        addNode(this.mainNode);
    }

    private void setupBasicTextures() {
        getTextureAtlas().addTexture(CURSOR_COLOR);
        getTextureAtlas().addTexture(REMOVE_FACE_COLOR);
        getTextureAtlas().addTexture(GRAY_COLOR);
        getTextureAtlas().addTexture(YELLOW_COLOR);
        getTextureAtlas().addTexture(GREEN_COLOR);
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

        if (levelEntry.getRemap() != null)
            getLogger().info("Found level entry with " + levelEntry.getRemap().getTextureIds().size() + " texture remaps.");  // TODO: TOSS

        // Register VLO textures for the level.
        VLOArchive levelVlo = levelEntry.getVloFile();
        if (levelVlo != null)
            SCUtils.addAtlasTextures(getTextureAtlas(), levelVlo);

        // Add gouraud shaded stuff.
        //if (getMap().getFormatVersion() == OldFroggerMapVersion.MILESTONE3)
        //    for (OldFroggerMapGrid grid : getMap().getGridPacket().getGrids())
        //        for (OldFroggerMapPolygon polygon : grid.getPolygons())
        //            this.shadedTextureManager.addPolygon(polygon);
    }
}