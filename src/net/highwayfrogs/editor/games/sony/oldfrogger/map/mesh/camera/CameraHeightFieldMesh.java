package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.camera;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerCameraHeightFieldManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;
import net.highwayfrogs.editor.gui.texture.basic.ColorBlendTextureSource;
import net.highwayfrogs.editor.gui.texture.basic.OutlineColorTextureSource;
import net.highwayfrogs.editor.gui.texture.basic.UnknownTextureSource;

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

    public static final OutlineColorTextureSource SELECTED_COLOR = new OutlineColorTextureSource(Color.YELLOW, Color.BLACK);
    public static final OutlineColorTextureSource UNSELECTED_COLOR = new OutlineColorTextureSource(Color.RED, Color.BLACK);

    public CameraHeightFieldMesh(OldFroggerCameraHeightFieldManager manager) {
        super(new SequentialTextureAtlas(32, 32, false), DynamicMeshTextureQuality.LIT_BLURRY);
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