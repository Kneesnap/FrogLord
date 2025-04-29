package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central;

import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Translate;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerMapGroup;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGroup;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.Arrays;

/**
 * Manages editing of general data in a Frogger map.
 * TODO: If we made wireframe meshes for the grid preview, I think the previews would be less laggy, especially for large previews.
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
    private Box[][] groupPreviewBoxes;
    private Box selectedMapGroupPreviewBox;
    private DisplayList gridPreviewList;

    private static final PhongMaterial GROUP_PREVIEW_BOX_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);
    private static final PhongMaterial GROUP_PREVIEW_SELECTED_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.RED);

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
        this.showGroupBoundsCheckBox = groupMainGrid.addCheckBox("Show Group Bounds", false, newState -> updateMapGroup3DView());
        this.showCollisionGridCheckBox = groupMainGrid.addCheckBox("Show Collision Grid", false, newState -> updateGridView());

        // For the group packet.
        this.groupEditorGrid = this.sidePanel.makeEditorGrid();
        getController().getInputManager().addKeyListener(this::handleKeyPressForMapGroup);
    }

    @Override
    public void updateEditor() {
        super.updateEditor();

        // Remake editors.
        this.generalEditorGrid.clearEditor();
        getMap().getGeneralPacket().setupEditor(getController(), this.generalEditorGrid);
        updateMapGroupUI();

        // Update the 3D views.
        updateMapGroup3DView();
        updateGridView();
    }

    /**
     * Updates the Map Group editor UI.
     */
    public void updateMapGroupUI() {
        this.groupEditorGrid.clearEditor();
        getMap().getGroupPacket().setupEditor(this, this.groupEditorGrid);
    }

    /**
     * Update the map group display preview.
     */
    public void updateMapGroup3DView() {
        this.groupPreviewList.clear();
        this.selectedMapGroupPreviewBox = null;
        if (!this.showGroupBoundsCheckBox.isSelected()) {
            if (this.groupPreviewBoxes != null)
                for (int i = 0; i < this.groupPreviewBoxes.length; i++)
                    Arrays.fill(this.groupPreviewBoxes[i], null);

            return;
        }

        FroggerMapFilePacketGroup groupPacket = getMap().getGroupPacket();
        float baseX = groupPacket.getGroupBaseWorldX();
        float baseZ = groupPacket.getGroupBaseWorldZ();
        float xSize = groupPacket.getGroupXSizeAsFloat();
        float zSize = groupPacket.getGroupZSizeAsFloat();

        if (this.groupPreviewBoxes == null || this.groupPreviewBoxes.length != groupPacket.getGroupZCount() || this.groupPreviewBoxes.length == 0|| this.groupPreviewBoxes[0].length != groupPacket.getGroupXCount())
            this.groupPreviewBoxes = new Box[groupPacket.getGroupZCount()][groupPacket.getGroupXCount()];

        EventHandler<? super MouseEvent> groupClickHandler = this::handleGroupBoxClick;
        for (int x = 0; x < groupPacket.getGroupXCount(); x++) {
            for (int z = 0; z < groupPacket.getGroupZCount(); z++) {
                Box groupBox = this.groupPreviewList.addBoundingBoxFromMinMax(baseX + (x * xSize), 0, baseZ + (z * zSize), baseX + ((x + 1) * xSize), 0, baseZ + ((z + 1) * zSize), GROUP_PREVIEW_BOX_MATERIAL, true);
                this.groupPreviewBoxes[z][x] = groupBox;
                groupBox.setOnMouseClicked(groupClickHandler);
                groupBox.setMouseTransparent(false);
            }
        }
    }

    private void handleKeyPressForMapGroup(InputManager manager, KeyEvent event) {
        if (event.getEventType() != KeyEvent.KEY_PRESSED || this.selectedMapGroupPreviewBox == null)
            return;

        int deltaX = 0, deltaZ = 0;
        switch (event.getCode()) {
            case UP:
                deltaZ = 1;
                break;
            case DOWN:
                deltaZ = -1;
                break;
            case LEFT:
                deltaX = -1;
                break;
            case RIGHT:
                deltaX = 1;
                break;
            default:
                return;
        }

        // Move the group.
        event.consume();
        FroggerMapFilePacketGroup groupPacket = getMap().getGroupPacket();
        FroggerMapGroup oldGroup = getMapGroupFromBox(this.selectedMapGroupPreviewBox);
        int newX = oldGroup.getX() + deltaX;
        int newZ = oldGroup.getZ() + deltaZ;
        if (newX >= 0 && newX < groupPacket.getGroupXCount() && newZ >= 0 && newZ < groupPacket.getGroupZCount())
            setSelectedMapGroupPreviewBox(this.groupPreviewBoxes[newZ][newX]);
    }

    private void handleGroupBoxClick(MouseEvent event) {
        Box box = (Box) event.getTarget();
        setSelectedMapGroupPreviewBox(box != this.selectedMapGroupPreviewBox ? box : null);
    }

    private FroggerMapGroup getMapGroupFromBox(Box box) {
        FroggerMapFilePacketGroup groupPacket = getMap().getGroupPacket();
        Translate position = Scene3DUtils.get3DTranslation(box);
        return groupPacket.getMapGroup(groupPacket.getGroupXFromWorldX((float) position.getX()), groupPacket.getGroupZFromWorldZ((float) position.getZ()));
    }

    private void setSelectedMapGroupPreviewBox(Box newBox) {
        // Deselect existing box.
        getMesh().getHighlightedGroupPolygonNode().clear();
        if (this.selectedMapGroupPreviewBox != null)
            this.selectedMapGroupPreviewBox.setMaterial(GROUP_PREVIEW_BOX_MATERIAL);


        // Abort if we clicked the already active preview.
        if (newBox == null) {
            this.selectedMapGroupPreviewBox = null;
            return;
        }

        FroggerMapGroup mapGroup = getMapGroupFromBox(newBox);
        this.selectedMapGroupPreviewBox = newBox;
        newBox.setMaterial(GROUP_PREVIEW_SELECTED_MATERIAL);

        // Highlight selected group polygons.
        for (FroggerMapPolygon polygon : mapGroup.getAllPolygons()) {
            DynamicMeshDataEntry polygonEntry = getMesh().getMainNode().getDataEntry(polygon);
            getMesh().getHighlightedGroupPolygonNode().setOverlayTexture(polygonEntry, BakedLandscapeUIManager.MATERIAL_POLYGON_HIGHLIGHT);
        }
    }

    /**
     * Update the group display.
     */
    public void updateGridView() {
        this.gridPreviewList.clear();
        if (!this.showCollisionGridCheckBox.isSelected())
            return;

        FroggerMapFilePacketGrid gridPacket = getMap().getGridPacket();
        float baseX = DataUtils.fixedPointIntToFloat4Bit(gridPacket.getBaseGridX());
        float baseZ = DataUtils.fixedPointIntToFloat4Bit(gridPacket.getBaseGridZ());
        float xSize = gridPacket.getGridXSizeAsFloat();
        float zSize = gridPacket.getGridZSizeAsFloat();
        boolean enableCliffHeightPreview = gridPacket.doGridStacksHaveCliffHeights();
        PhongMaterial gridMaterial = Scene3DUtils.makeUnlitSharpMaterial(Color.RED);
        PhongMaterial heightMaterial = Scene3DUtils.makeUnlitSharpMaterial(Color.GREEN);
        for (int x = 0; x < gridPacket.getGridXCount(); x++) {
            for (int z = 0; z < gridPacket.getGridZCount(); z++) {
                this.gridPreviewList.addBoundingBoxFromMinMax(baseX + (x * xSize), 0, baseZ + (z * zSize), baseX + ((x + 1) * xSize), 0, baseZ + ((z + 1) * zSize), gridMaterial, true);
                FroggerGridStack stack = gridPacket.getGridStack(x, z);
                if (enableCliffHeightPreview) {
                    float floatPos = stack.getCliffHeightAsFloat();
                    this.gridPreviewList.addBoundingBoxFromMinMax(baseX + (x * xSize), -1 - floatPos, baseZ + (z * zSize), baseX + ((x + 1) * xSize), -1 - floatPos, baseZ + ((z + 1) * zSize), heightMaterial, true);
                }
            }
        }
    }
}