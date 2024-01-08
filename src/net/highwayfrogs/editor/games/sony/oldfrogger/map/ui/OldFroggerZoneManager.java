package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapZonePacket.OldFroggerMapZone;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapZonePacket.OldFroggerMapZoneRegion;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerMapUIManager.OldFroggerMapListManager;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerZoneManager.ZonePreview3D;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the UI which allows editing zones.
 * Created by Kneesnap on 12/20/2023.
 */
public class OldFroggerZoneManager extends OldFroggerMapListManager<OldFroggerMapZone, ZonePreview3D> {
    private DisplayList boxDisplayList;

    private static final PhongMaterial DEFAULT_MATERIAL = Utils.makeSpecialMaterial(Color.LIME);
    private static final PhongMaterial SELECTED_MATERIAL = Utils.makeSpecialMaterial(Color.YELLOW);
    private static final PhongMaterial WHITE_MATERIAL = Utils.makeSpecialMaterial(Color.WHITE);
    private static final PhongMaterial BLUE_MATERIAL = Utils.makeSpecialMaterial(Color.LIGHTBLUE);

    public OldFroggerZoneManager(MeshViewController<OldFroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        this.boxDisplayList = getRenderManager().createDisplayList();
        super.onSetup();
    }

    @Override
    public String getTitle() {
        return "Zones";
    }

    @Override
    public String getValueName() {
        return "Zone";
    }

    @Override
    public List<OldFroggerMapZone> getValues() {
        return getMap().getZonePacket().getZones();
    }

    @Override
    protected ZonePreview3D setupDisplay(OldFroggerMapZone zone) {
        float minX = Utils.fixedPointIntToFloat4Bit(zone.getObjBoundAX1());
        float maxX = Utils.fixedPointIntToFloat4Bit(zone.getObjBoundAX2());
        float y = Utils.fixedPointShortToFloat4Bit(zone.getPlaneY());
        float minZ = Utils.fixedPointIntToFloat4Bit(zone.getObjBoundAZ1());
        float maxZ = Utils.fixedPointIntToFloat4Bit(zone.getObjBoundAZ2());

        Box box = this.boxDisplayList.addBoundingBoxFromMinMax(minX, y - 5, minZ, maxX, y + 5, maxZ, DEFAULT_MATERIAL, false);
        box.setOnMouseClicked(event -> getValueSelectionBox().setValue(zone));
        ZonePreview3D zonePreview = new ZonePreview3D(this.boxDisplayList, box);

        // Display regions
        for (int i = 0; i < zone.getRegions().size(); i++)
            zonePreview.addRegionPreview(zone, zone.getRegions().get(i));

        return zonePreview;
    }

    @Override
    protected void updateEditor(OldFroggerMapZone selectedZone) {
        selectedZone.setupEditor(getEditorGrid(), getDelegatesByValue().get(selectedZone));
    }

    @Override
    protected void setVisible(OldFroggerMapZone zone, ZonePreview3D preview, boolean visible) {
        preview.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(OldFroggerMapZone oldValue, ZonePreview3D oldPreview, OldFroggerMapZone newValue, ZonePreview3D newPreview) {
        if (oldPreview != null)
            oldPreview.onDeselect();
        if (newPreview != null)
            newPreview.onSelect();
    }

    @Override
    protected OldFroggerMapZone createNewValue() {
        return new OldFroggerMapZone(getMap().getGameInstance());
    }

    @Override
    protected void onDelegateRemoved(OldFroggerMapZone oldFroggerMapZone, ZonePreview3D preview) {
        preview.remove(this.boxDisplayList);
    }

    /**
     * Represents the 3D preview of a zone.
     */
    @Getter
    public static class ZonePreview3D {
        private final DisplayList displayList;
        private final Box box;
        private final List<ZoneRegionPreview3D> regionPreviews = new ArrayList<>();
        private final Map<OldFroggerMapZoneRegion, ZoneRegionPreview3D> previewsByRegion = new HashMap<>();

        public ZonePreview3D(DisplayList list, Box box) {
            this.displayList = list;
            this.box = box;
        }

        /**
         * Add a region preview.
         * @param region The region to preview.
         */
        public void addRegionPreview(OldFroggerMapZone zone, OldFroggerMapZoneRegion region) {
            float y = Utils.fixedPointIntToFloat4Bit(zone.getPlaneY());
            float x = Utils.fixedPointIntToFloat4Bit(region.getWorldX());
            float regionMinZ = Utils.fixedPointIntToFloat4Bit(region.getWorldZ1());
            float regionMaxZ = Utils.fixedPointIntToFloat4Bit(region.getWorldZ2());
            Cylinder line = this.displayList.addLine(x, y - 10, regionMinZ, x, y - 10, regionMaxZ, 1, WHITE_MATERIAL);
            Sphere startSphere = this.displayList.addSphere(x, y - 10, regionMinZ, 3, BLUE_MATERIAL, false);
            Sphere endSphere = this.displayList.addSphere(x, y - 10, regionMaxZ, 3, BLUE_MATERIAL, false);
            ZoneRegionPreview3D regionPreview = new ZoneRegionPreview3D(startSphere, endSphere, line);
            this.regionPreviews.add(regionPreview);
            this.previewsByRegion.put(region, regionPreview);
        }

        /**
         * Remove the provided region.
         * @param region The region to remove.
         * @return If the region was removed successfully
         */
        public boolean removeRegion(OldFroggerMapZoneRegion region) {
            ZoneRegionPreview3D preview = this.previewsByRegion.remove(region);
            if (preview == null)
                return false;

            if (!this.regionPreviews.remove(preview))
                return false;

            preview.remove(this.displayList);
            return true;
        }

        /**
         * Updates the box position.
         * @param zone The zone to update the position from.
         */
        public void updateBoxPosition(OldFroggerMapZone zone) {
            for (Transform transform : this.box.getTransforms()) {
                if (!(transform instanceof Translate))
                    continue;

                Translate translate = (Translate) transform;
                translate.setX(Utils.fixedPointIntToFloat4Bit((zone.getObjBoundAX1() + zone.getObjBoundAX2()) / 2));
                translate.setY(Utils.fixedPointIntToFloat4Bit(zone.getPlaneY()));
                translate.setZ(Utils.fixedPointIntToFloat4Bit((zone.getObjBoundAZ1() + zone.getObjBoundAZ2()) / 2));
            }
        }

        /**
         * Remove the preview from a display list.
         * @param displayList The display list to remove the data from.
         */
        public void remove(DisplayList displayList) {
            displayList.remove(this.box);
            for (int i = 0; i < this.regionPreviews.size(); i++)
                this.regionPreviews.get(i).remove(displayList);
        }

        /**
         * Set if the preview is visible.
         * @param visible If the preview should be visible.
         */
        public void setVisible(boolean visible) {
            this.box.setVisible(visible);
            for (int i = 0; i < this.regionPreviews.size(); i++)
                this.regionPreviews.get(i).setVisible(visible);
        }

        /**
         * Called when the zone is selected.
         */
        public void onSelect() {
            this.box.setMaterial(SELECTED_MATERIAL);
            for (int i = 0; i < this.regionPreviews.size(); i++)
                this.regionPreviews.get(i).setVisible(true);
        }

        /**
         * Called when the zone is deselected.
         */
        public void onDeselect() {
            this.box.setMaterial(DEFAULT_MATERIAL);
            for (int i = 0; i < this.regionPreviews.size(); i++)
                this.regionPreviews.get(i).setVisible(false);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ZoneRegionPreview3D {
        private Sphere startSphere;
        private Sphere endSphere;
        private Cylinder line;

        /**
         * Remove the zone region preview from the display list.
         * @param displayList The display list to remove from.
         */
        public void remove(DisplayList displayList) {
            displayList.remove(this.startSphere);
            displayList.remove(this.endSphere);
            displayList.remove(this.line);
        }

        /**
         * Set if the zone region preview is visible.
         * @param visible If the region preview is visible
         */
        public void setVisible(boolean visible) {
            this.startSphere.setVisible(visible);
            this.endSphere.setVisible(visible);
            this.line.setVisible(visible);
        }

        /**
         * Updates the position of displayed nodes in 3D space.
         * @param region The region to update data from.
         */
        public void updateRegionLinePreview(OldFroggerMapZoneRegion region) {
            float newX = Utils.fixedPointIntToFloat4Bit(region.getWorldX());
            float newZ1 = Utils.fixedPointIntToFloat4Bit(region.getWorldZ1());
            float newZ2 = Utils.fixedPointIntToFloat4Bit(region.getWorldZ2());
            Scene3DUtils.get3DTranslation(this.startSphere).setX(newX);
            Scene3DUtils.get3DTranslation(this.startSphere).setZ(newZ1);
            Scene3DUtils.get3DTranslation(this.endSphere).setX(newX);
            Scene3DUtils.get3DTranslation(this.endSphere).setZ(newZ2);
            Scene3DUtils.get3DTranslation(this.line).setX(newX);
            Scene3DUtils.get3DTranslation(this.line).setZ((newZ1 + newZ2) / 2);
            this.line.setHeight(Math.abs(newZ2 - newZ1)); // delta x and delta y are both zero, so it's just sqrt(delta z * delta z)
        }

        /**
         * Update the y position of the line in 3D space.
         * @param zone The zone to update height from
         */
        public void updateRegionLineHeight(OldFroggerMapZone zone) {
            float newY = Utils.fixedPointIntToFloat4Bit(zone.getPlaneY()) - 10;
            Scene3DUtils.get3DTranslation(this.startSphere).setY(newY);
            Scene3DUtils.get3DTranslation(this.endSphere).setY(newY);
            Scene3DUtils.get3DTranslation(this.line).setY(newY);
        }
    }

    /**
     * Represents the zone region editor, controlling state necessary to update the 3D view.
     */
    @Getter
    @Setter
    public static class ZoneRegionEditor {
        private OldFroggerMapZone zone;
        private OldFroggerMapZoneRegion region;
        private boolean mainSetup;
        private ZonePreview3D zonePreview;
        private ZoneRegionPreview3D regionPreview;
        private Button removeValueButton;
        private TextField xTextField;
        private TextField z1TextField;
        private TextField z2TextField;

        public ZoneRegionEditor(OldFroggerMapZone zone, ZonePreview3D zonePreview, OldFroggerMapZoneRegion region) {
            this.zone = zone;
            this.zonePreview = zonePreview;
            this.region = region;
        }

        /**
         * Setup the editor for the zone region.
         * @param editor The editor to build the UI around.
         * @param region The current region to setup.
         */
        public void setupEditor(GUIEditorGrid editor, OldFroggerMapZoneRegion region, ZoneRegionPreview3D regionPreview) {
            this.region = region;
            this.regionPreview = regionPreview;

            if (!this.mainSetup) {
                this.mainSetup = true;

                // Select the zone region.
                ComboBox<OldFroggerMapZoneRegion> regionSelector = editor
                        .addSelectionBox("Zone Region", region, this.zone.getRegions(),
                                newRegion -> setupEditor(editor, newRegion, this.zonePreview.getPreviewsByRegion().get(this.region)));
                regionSelector.setConverter(new AbstractIndexStringConverter<>(this.zone.getRegions(), (index, zoneRegion) -> "Region #" + (index + 1)));

                // Add & remove.
                Button addValueButton = new Button("Add Region");
                addValueButton.setOnAction(evt -> {
                    // Register and update UI.
                    OldFroggerMapZoneRegion newRegion = new OldFroggerMapZoneRegion(this.zone.getGameInstance());
                    this.zone.getRegions().add(newRegion);
                    regionSelector.setItems(FXCollections.observableArrayList(this.zone.getRegions())); // Update UI
                    this.zonePreview.addRegionPreview(this.zone, newRegion);
                    regionSelector.getSelectionModel().selectLast(); // Select the new region.
                });
                addValueButton.setAlignment(Pos.CENTER);
                editor.setupNode(addValueButton);

                // Value Removal Button
                this.removeValueButton = new Button("Remove Region");
                this.removeValueButton.setOnAction(evt -> {
                    if (this.region == null)
                        return;

                    // Remove region and update ui.
                    if (this.zone.getRegions().remove(this.region)) {
                        regionSelector.getSelectionModel().clearSelection();
                        regionSelector.setItems(FXCollections.observableArrayList(this.zone.getRegions()));
                        this.zonePreview.removeRegion(this.region); // Remove / delete preview.
                    }
                });
                this.removeValueButton.setAlignment(Pos.CENTER);
                editor.setupSecondNode(this.removeValueButton, false);
                editor.addRow();
            }

            this.removeValueButton.setDisable(region == null);

            if (this.xTextField != null) {
                this.xTextField.setDisable(region == null);
                if (region != null)
                    this.xTextField.setText(String.valueOf(Utils.fixedPointIntToFloat4Bit(region.getWorldX())));
            } else if (region != null) {
                this.xTextField = editor.addFixedInt("X", this.region.getWorldX(), newValue -> {
                    this.region.setWorldX(newValue);
                    this.regionPreview.updateRegionLinePreview(this.region);
                });
            }

            if (this.z1TextField != null) {
                this.z1TextField.setDisable(region == null);
                if (region != null)
                    this.z1TextField.setText(String.valueOf(Utils.fixedPointIntToFloat4Bit(region.getWorldZ1())));
            } else if (region != null) {
                this.z1TextField = editor.addFixedInt("Min Z", this.region.getWorldZ1(), newValue -> {
                    this.region.setWorldZ1(newValue);
                    this.regionPreview.updateRegionLinePreview(this.region);
                });
            }

            if (this.z2TextField != null) {
                this.z2TextField.setDisable(region == null);
                if (region != null)
                    this.z2TextField.setText(String.valueOf(Utils.fixedPointIntToFloat4Bit(region.getWorldZ2())));
            } else if (region != null) {
                this.z2TextField = editor.addFixedInt("Max Z", this.region.getWorldZ2(), newValue -> {
                    this.region.setWorldZ2(newValue);
                    this.regionPreview.updateRegionLinePreview(this.region);
                });
            }
        }
    }
}
