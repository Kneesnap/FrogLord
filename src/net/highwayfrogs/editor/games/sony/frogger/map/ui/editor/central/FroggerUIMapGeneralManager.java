package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central;

import javafx.geometry.Orientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Separator;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGroup;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;

/**
 * Manages editing of general data in a Frogger map.
 * TODO: If we made wireframe meshes, I think the previews would be less laggy.
 *   - Consider generalizing the idea of a grid preview mesh.
 *   - Also, there's a lot of redundant code when making meshes. I'm not against the idea of needing several classes, but sometimes (like for grid meshes) we really don't need all the extra stuff. Reduce code duplication too.
 * Created by Kneesnap on 6/2/2024.
 */
public class FroggerUIMapGeneralManager extends FroggerCentralUIManager {
    @Getter private UISidePanel sidePanel;
    private GUIEditorGrid generalEditorGrid;
    private GUIEditorGrid groupEditorGrid;
    private CheckBox showGroupBoundsCheckBox;
    private CheckBox showCollisionGridCheckBox;
    private DisplayList groupPreviewList;
    private DisplayList gridPreviewList;

    public FroggerUIMapGeneralManager(MeshViewController<FroggerMapMesh> controller) {
        super(controller);
    }
    
    @Override
    public void onSetup() {
        super.onSetup();

        this.groupPreviewList = getRenderManager().createDisplayList();
        this.gridPreviewList = getRenderManager().createDisplayList();

        // Setup UI Pane & Grid.
        this.sidePanel = getController().createSidePanel("General Level Settings");
        this.generalEditorGrid = this.sidePanel.makeEditorGrid();
        this.sidePanel.add(new Separator(Orientation.HORIZONTAL));
        GUIEditorGrid groupMainGrid = this.sidePanel.makeEditorGrid();
        this.showGroupBoundsCheckBox = groupMainGrid.addCheckBox("Show Group Bounds", false, newState -> updateMapGroupPreview());
        this.showCollisionGridCheckBox = groupMainGrid.addCheckBox("Show Collision Grid", false, newState -> updateGridView());

        // For the group packet.
        this.groupEditorGrid = this.sidePanel.makeEditorGrid();
    }

    @Override
    public void updateEditor() {
        super.updateEditor();

        // Remake editors.
        this.generalEditorGrid.clearEditor();
        getMap().getGeneralPacket().setupEditor(getController(), this.generalEditorGrid);
        this.groupEditorGrid.clearEditor();
        getMap().getGroupPacket().setupEditor(this, this.groupEditorGrid);

        // Update the previews.
        updateMapGroupPreview();
        updateGridView();
    }

    /**
     * Update the map group display preview.
     */
    public void updateMapGroupPreview() {
        this.groupPreviewList.clear();
        if (!this.showGroupBoundsCheckBox.isSelected())
            return;

        FroggerMapFilePacketGroup groupPacket = getMap().getGroupPacket();
        SVector basePoint = groupPacket.getBasePoint();
        float baseX = basePoint.getFloatX();
        float baseZ = basePoint.getFloatZ();
        float xSize = DataUtils.fixedPointIntToFloat4Bit(groupPacket.getGroupXSize());
        float zSize = DataUtils.fixedPointIntToFloat4Bit(groupPacket.getGroupZSize());
        PhongMaterial material = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);
        for (int x = 0; x < groupPacket.getGroupXCount(); x++)
            for (int z = 0; z < groupPacket.getGroupZCount(); z++)
                this.groupPreviewList.addBoundingBoxFromMinMax(baseX + (x * xSize), 0, baseZ + (z * zSize), baseX + ((x + 1) * xSize), 0, baseZ + ((z + 1) * zSize), material, true);
    }

    /**
     * Update the group display.
     */
    public void updateGridView() {
        this.gridPreviewList.clear();
        if (!this.showCollisionGridCheckBox.isSelected())
            return;

        FroggerMapFilePacketGrid gridPacket = getMap().getGridPacket();
        float baseX = DataUtils.fixedPointIntToFloatNBits(gridPacket.getBaseGridX(), 4);
        float baseZ = DataUtils.fixedPointIntToFloatNBits(gridPacket.getBaseGridZ(), 4);
        float xSize = gridPacket.getGridXSizeAsFloat();
        float zSize = gridPacket.getGridZSizeAsFloat();
        PhongMaterial gridMaterial = Scene3DUtils.makeUnlitSharpMaterial(Color.RED);
        PhongMaterial heightMaterial = Scene3DUtils.makeUnlitSharpMaterial(Color.GREEN);
        for (int x = 0; x < gridPacket.getGridXCount(); x++) {
            for (int z = 0; z < gridPacket.getGridZCount(); z++) {
                this.gridPreviewList.addBoundingBoxFromMinMax(baseX + (x * xSize), 0, baseZ + (z * zSize), baseX + ((x + 1) * xSize), 0, baseZ + ((z + 1) * zSize), gridMaterial, true);
                FroggerGridStack stack = gridPacket.getGridStack(x, z);
                if (stack != null) {
                    float floatPos = stack.getAverageWorldHeightAsFloat();
                    this.gridPreviewList.addBoundingBoxFromMinMax(baseX + (x * xSize), -1 - floatPos, baseZ + (z * zSize), baseX + ((x + 1) * xSize), -1 - floatPos, baseZ + ((z + 1) * zSize), heightMaterial, true);
                }
            }
        }
    }
}