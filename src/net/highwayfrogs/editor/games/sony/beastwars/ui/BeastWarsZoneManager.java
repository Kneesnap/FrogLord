package net.highwayfrogs.editor.games.sony.beastwars.ui;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.beastwars.map.data.BeastWarsMapZone;
import net.highwayfrogs.editor.games.sony.beastwars.map.data.BeastWarsMapZone.BeastWarsMapZoneRegion;
import net.highwayfrogs.editor.games.sony.beastwars.map.mesh.BeastWarsMapMesh;
import net.highwayfrogs.editor.games.sony.beastwars.map.mesh.BeastWarsMapVertex;
import net.highwayfrogs.editor.games.sony.beastwars.ui.BeastWarsMapUIManager.BeastWarsMapListManager;
import net.highwayfrogs.editor.games.sony.beastwars.ui.BeastWarsZoneManager.ZoneVisualPreview;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode.OverlayTarget;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;

import java.util.List;

/**
 * This manager manages the display of Beast Wars Map Zones
 * TODO: We need to allow editing dimensions, auto-update the first zone, etc. This requires a buffed array management system.
 * Created by Kneesnap on 10/1/2023.
 */
public class BeastWarsZoneManager extends BeastWarsMapListManager<BeastWarsMapZone, ZoneVisualPreview> {
    private BeastWarsMapZoneRegion selectedRegion;

    public BeastWarsZoneManager(MeshViewController<BeastWarsMapMesh> controller) {
        super(controller);
    }

    @Override
    public String getTitle() {
        return "Map Zones";
    }

    @Override
    public String getValueName() {
        return "Map Zone";
    }

    @Override
    public List<BeastWarsMapZone> getValues() {
        return getMap().getZones();
    }

    @Override
    protected ZoneVisualPreview setupDisplay(BeastWarsMapZone beastWarsMapZone) {
        return new ZoneVisualPreview(this, beastWarsMapZone);
        // TODO: When highlighted face is clicked, attempt to select the corresponding region, or just select no region but display everything.
    }

    @Override
    protected void updateEditor(BeastWarsMapZone zone) {
        // Zone Data:
        getEditorGrid().addNormalLabel("Region Count: " + zone.getRegions().size());
        getEditorGrid().addNormalLabel("Zone Position: (" + zone.getMainRegion().getAbsoluteStartX() + ", " + zone.getMainRegion().getAbsoluteStartZ() + ")");
        getEditorGrid().addNormalLabel("Zone Size: " + (zone.getMainRegion().getXLength() + 1) + "x" + (zone.getMainRegion().getZLength() + 1));
        getEditorGrid().addSignedIntegerField("Unknown #1", zone.getUnknown1(), zone::setUnknown1);
        getEditorGrid().addSignedIntegerField("Unknown #2", zone.getUnknown2(), zone::setUnknown2);
        getEditorGrid().addSignedIntegerField("Unknown #3", zone.getUnknown3(), zone::setUnknown3);
        getEditorGrid().addSignedIntegerField("Unknown #4", zone.getUnknown4(), zone::setUnknown4);

        // De-select the selected region if it doesn't belong to the provided zone..
        if (this.selectedRegion != null && this.selectedRegion.getZone() != zone) {
            ZoneVisualPreview preview = getDelegatesByValue().get(this.selectedRegion.getZone());
            this.selectedRegion = null; // Make it null after finding the preview, but before updating it.
            if (preview != null)
                preview.updateDisplay();
        }

        // Add region selector.
        getEditorGrid().addSelectionBox("Region", this.selectedRegion, zone.getRegions(), newRegion -> {
            this.selectedRegion = newRegion;
            ZoneVisualPreview preview = getDelegatesByValue().get(newRegion.getZone());
            if (preview != null)
                preview.updateDisplay();
        }).setConverter(new AbstractIndexStringConverter<>(zone.getRegions(), (index, value) -> "Region " + index));

        getEditorGrid().addSeparator();

        // Add region info:
        if (this.selectedRegion != null) {
            // TODO: Allow edits
            getEditorGrid().addNormalLabel("Region Position: (" + this.selectedRegion.getAbsoluteStartX() + ", " + this.selectedRegion.getAbsoluteStartZ() + ")");
            getEditorGrid().addNormalLabel("Region Size: " + (this.selectedRegion.getXLength() + 1) + "x" + (this.selectedRegion.getZLength() + 1));
        }
    }

    @Override
    protected void setValuesVisible(boolean visible) {
        getDelegatesByValue().values().forEach(visualPreview -> visualPreview.setVisible(visible));
    }

    @Override
    protected void setVisible(BeastWarsMapZone beastWarsMapZone, ZoneVisualPreview zoneVisualPreview, boolean visible) {
        zoneVisualPreview.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(BeastWarsMapZone oldValue, ZoneVisualPreview oldPreview, BeastWarsMapZone newValue, ZoneVisualPreview newPreview) {
        if (oldPreview != null)
            oldPreview.updateDisplay();

        if (newPreview != null)
            newPreview.updateDisplay();
    }

    @Override
    protected BeastWarsMapZone createNewValue() {
        return new BeastWarsMapZone(getMap());
    }

    @Override
    protected void onDelegateRemoved(BeastWarsMapZone removedValue, ZoneVisualPreview visualPreview) {
        visualPreview.setVisible(false);
    }

    @Getter
    public static class ZoneVisualPreview {
        private final BeastWarsZoneManager manager;
        private final BeastWarsMapZone zone;
        private DynamicMeshOverlayNode overlayNode;

        public ZoneVisualPreview(BeastWarsZoneManager manager, BeastWarsMapZone zone) {
            this.manager = manager;
            this.zone = zone;
        }

        /**
         * Test if this is visible.
         */
        public boolean isVisible() {
            return this.overlayNode != null;
        }

        /**
         * Gets the mesh this preview applies to.
         */
        public BeastWarsMapMesh getMesh() {
            return this.manager.getMesh();
        }

        /**
         * Sets if the visual preview is visible or not.
         * @param visible If the preview should be visible.
         */
        public void setVisible(boolean visible) {
            if (visible == isVisible())
                return;

            if (visible) { // Show
                updateDisplay(true);
            } else { // Hide
                deleteActiveDisplay();
            }
        }

        private void deleteActiveDisplay() {
            if (this.overlayNode == null)
                return;

            getMesh().removeNode(this.overlayNode);
            this.overlayNode = null;
        }

        /**
         * Updates the display.
         */
        public void updateDisplay() {
            updateDisplay(false);
        }

        /**
         * Updates the display.
         */
        public void updateDisplay(boolean makeVisible) {
            if (!isVisible() && !makeVisible)
                return; // Don't update the display.

            deleteActiveDisplay();
            if (this.zone.getMainRegion() == null)
                return; // Can't display null...

            BeastWarsMapMesh mesh = getMesh();
            mesh.pushBatchOperations();
            this.zone.updateRegionCache();
            this.overlayNode = new DynamicMeshOverlayNode(mesh);
            mesh.addNode(this.overlayNode);
            for (int z = 0; z <= this.zone.getMainRegion().getZLength(); z++) {
                for (int x = 0; x <= this.zone.getMainRegion().getXLength(); x++) {
                    BeastWarsMapZoneRegion region = this.zone.getRegion(x, z);
                    ITextureSource textureSource = getOverlayTextureSource(region);
                    BeastWarsMapVertex vertex = getZone().getMapFile().getVertex(this.zone.getMainRegion().getAbsoluteStartX() + x, this.zone.getMainRegion().getAbsoluteStartZ() + z);
                    if (vertex != null)
                        this.overlayNode.add(new OverlayTarget(mesh.getMainNode().getDataEntry(vertex), textureSource));
                }
            }

            mesh.popBatchOperations();
        }

        private ITextureSource getOverlayTextureSource(BeastWarsMapZoneRegion region) {
            if (region == this.manager.selectedRegion) {
                return BeastWarsMapMesh.YELLOW_COLOR;
            } else if (region != this.zone.getMainRegion()) {
                return BeastWarsMapMesh.GREEN_COLOR;
            } else {
                return BeastWarsMapMesh.GRAY_COLOR;
            }
        }
    }
}