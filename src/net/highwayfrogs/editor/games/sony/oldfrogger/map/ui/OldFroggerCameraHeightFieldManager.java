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
 * Created by Kneesnap on 12/25/2023.
 */
public class OldFroggerCameraHeightFieldManager extends OldFroggerMapUIManager {
    private GUIEditorGrid mainGrid;
    private GUIEditorGrid editorGrid;
    private DisplayList verticeDisplayList;

    private static final PhongMaterial MATERIAL_GREEN = Utils.makeSpecialMaterial(Color.LIMEGREEN);

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

        // Add the vertices. TODO: These should be offset from the basePoint I think. This does position it right.
        int xCenterOffset = (heightFieldPacket.getXSize() / 2);
        int zCenterOffset = (heightFieldPacket.getZSize() / 2);
        for (int z = 0; z < heightFieldPacket.getHeightMap().length; z++)
            for (int x = 0; x < heightFieldPacket.getHeightMap()[z].length; x++)
                this.verticeDisplayList.addSphere((x - xCenterOffset) << 4, Utils.fixedPointIntToFloat4Bit(heightFieldPacket.getHeightMap()[z][x]), (z - zCenterOffset) << 4, 1, MATERIAL_GREEN, false);

        this.verticeDisplayList.setVisible(false);
    }

    @Override
    public void updateEditor() {
        super.updateEditor();
        this.editorGrid.clearEditor();

        // TODO: Show selected heightfield editor.
    }
}
