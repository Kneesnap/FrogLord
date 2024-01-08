package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.camera;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.ColorBlendTextureSource;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerCameraHeightFieldManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;

import java.awt.*;

/**
 * This mesh is a visual representation of the camera height-field.
 * Created by Kneesnap on 12/30/2023.
 */
@Getter
public class CameraHeightFieldMesh extends DynamicMesh {
    private final OldFroggerMapFile map;
    private final OldFroggerCameraHeightFieldManager manager;
    private final CameraHeightFieldMeshNode mainNode;
    private final ColorBlendTextureSource selectedTextureBlending;
    private final AtlasTexture selectedTexture;

    public static final CursorVertexColor SELECTED_COLOR = new CursorVertexColor(Color.YELLOW, Color.BLACK);
    public static final CursorVertexColor UNSELECTED_COLOR = new CursorVertexColor(Color.RED, Color.BLACK);

    public CameraHeightFieldMesh(OldFroggerCameraHeightFieldManager manager) {
        super(new SequentialTextureAtlas(32, 32, false));
        this.map = manager.getMap();
        this.manager = manager;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        this.selectedTextureBlending = new ColorBlendTextureSource(Color.RED, Color.YELLOW);
        this.selectedTexture = getTextureAtlas().addTexture(this.selectedTextureBlending);
        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = new CameraHeightFieldMeshNode(this);
        addNode(this.mainNode);
    }
}
