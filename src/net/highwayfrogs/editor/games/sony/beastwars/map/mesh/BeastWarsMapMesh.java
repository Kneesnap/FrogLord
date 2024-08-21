package net.highwayfrogs.editor.games.sony.beastwars.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsTexFile;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;

import java.awt.*;

/**
 * Represents a mesh for a beast wars map.
 * Created by Kneesnap on 9/22/2023.
 */
@Getter
public class BeastWarsMapMesh extends DynamicMesh {
    private final BeastWarsMapFile map;
    private final BeastWarsMapMeshNode mainNode;

    public static final CursorVertexColor CURSOR_COLOR = new CursorVertexColor(Color.ORANGE, Color.BLACK);
    public static final CursorVertexColor REMOVE_FACE_COLOR = new CursorVertexColor(Color.RED, Color.BLACK);
    public static final CursorVertexColor GRAY_COLOR = new CursorVertexColor(Color.GRAY, Color.BLACK);
    public static final CursorVertexColor YELLOW_COLOR = new CursorVertexColor(Color.YELLOW, Color.BLACK);
    public static final CursorVertexColor GREEN_COLOR = new CursorVertexColor(Color.GREEN, Color.BLACK);

    public BeastWarsMapMesh(BeastWarsMapFile mapFile) {
        super(new SequentialTextureAtlas(64, 64, true), DynamicMeshTextureQuality.UNLIT_SHARP);
        this.map = mapFile;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupMapTextures();
        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = new BeastWarsMapMeshNode(this);
        addNode(this.mainNode);
    }

    private void setupBasicTextures() {
        getTextureAtlas().addTexture(CURSOR_COLOR);
        getTextureAtlas().addTexture(REMOVE_FACE_COLOR);
        getTextureAtlas().addTexture(GRAY_COLOR);
        getTextureAtlas().addTexture(YELLOW_COLOR);
        getTextureAtlas().addTexture(GREEN_COLOR);
        getTextureAtlas().addTexture(UnknownTextureSource.CYAN_INSTANCE);
    }

    private void setupMapTextures() {
        // Register map textures into the atlas.
        BeastWarsMapFile map = getMap();
        BeastWarsTexFile texFile = map.getTextureFile();
        if (texFile != null)
            for (int i = 0; i < texFile.getImages().size(); i++)
                getTextureAtlas().addTexture(texFile.getImages().get(i));
    }
}