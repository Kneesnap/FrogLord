package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapCameraHeightFieldPacket;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Manages editing of camera height-field data.
 * TODO: It'd be nice to have a connected mesh for editing. It would make it a lot easier to visualize. Yeah, once mesh editing is possible, let's make this a mesh.
 * TODO: It'd be nice to have a mode which forces the camera to the height shown in-game. Eg: allow WASD, but only for preview.
 * Created by Kneesnap on 12/25/2023.
 */
public class OldFroggerCameraHeightFieldManager extends OldFroggerMapUIManager {
    private GUIEditorGrid mainGrid;
    private GUIEditorGrid editorGrid;
    private DisplayList verticeDisplayList;

    private static final PhongMaterial MATERIAL_GREEN = Utils.makeSpecialMaterial(Color.LIMEGREEN);
    private static final PhongMaterial MATERIAL_RED = Utils.makeSpecialMaterial(Color.RED);

    public OldFroggerCameraHeightFieldManager(MeshViewController<OldFroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        this.verticeDisplayList = getRenderManager().createDisplayListWithNewGroup();
        OldFroggerMapCameraHeightFieldPacket heightFieldPacket = getMap().getCameraHeightFieldPacket();

        // Unchanging UI Fields
        VBox editorBox = this.getController().makeAccordionMenu("Camera Height Field");
        this.mainGrid = getController().makeEditorGrid(editorBox);
        this.mainGrid.addCheckBox("Visible", false, this.verticeDisplayList::setVisible);
        heightFieldPacket.setupEditor(this, this.mainGrid);

        // Separator, and grid setup.
        editorBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
        this.editorGrid = this.getController().makeEditorGrid(editorBox);

        // Add the vertices.
        for (int z = 0; z < heightFieldPacket.getHeightMap().length; z++) {
            float zPos = Utils.fixedPointIntToFloat4Bit(heightFieldPacket.getStartPositionZ() + (z * heightFieldPacket.getZSquareSize()));
            for (int x = 0; x < heightFieldPacket.getHeightMap()[z].length; x++) {
                float xPos = Utils.fixedPointIntToFloat4Bit(heightFieldPacket.getStartPositionX() + (x * heightFieldPacket.getXSquareSize()));
                float yPos = Utils.fixedPointIntToFloat4Bit(heightFieldPacket.getHeightMap()[z][x]);
                this.verticeDisplayList.addSphere(xPos, yPos, zPos, 1, MATERIAL_RED, false);
            }
        }

        this.verticeDisplayList.setVisible(false);
    }

    @Override
    public void updateEditor() {
        super.updateEditor();
        this.editorGrid.clearEditor();

        // TODO: Implement editor.
    }
}
