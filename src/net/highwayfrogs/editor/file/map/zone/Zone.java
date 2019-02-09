package net.highwayfrogs.editor.file.map.zone;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "ZONE" struct.
 * This is written right after the pointers at the start.
 * Zone Header:
 * 4 Byte: "ZONE"
 * 2 Byte: zoneCount
 * <4 Byte * zoneCount> pointer to zones.
 * Zone:
 * Zone Struct
 * Camera Zone Data
 * Zone Region Data
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
@Setter
public class Zone extends GameObject {
    private ZoneRegion mainRegion = new ZoneRegion(); // All child regions are inside this region.
    private List<ZoneRegion> regions = new ArrayList<>();
    private CameraZone cameraZone = new CameraZone();

    private static final short ZONE_TYPE_CAMERA = (short) 0;

    @Override
    public void load(DataReader reader) {
        short zoneTypeId = reader.readShort();
        if (zoneTypeId != ZONE_TYPE_CAMERA)
            throw new RuntimeException("Invalid ZoneType: " + zoneTypeId);

        short regionCount = reader.readShort();
        this.mainRegion.load(reader);
        int regionOffset = reader.readInt();

        // Read Camera-Zone data.
        this.cameraZone.load(reader);

        // Read region data.
        reader.jumpTemp(regionOffset);
        for (int i = 0; i < regionCount; i++) {
            ZoneRegion region = new ZoneRegion();
            region.load(reader);
            getRegions().add(region);
        }
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(ZONE_TYPE_CAMERA);
        writer.writeShort((short) getRegionCount());
        this.mainRegion.save(writer);

        int regionOffset = writer.getIndex();
        regionOffset += Constants.INTEGER_SIZE; // After the offset bytes.
        regionOffset += CameraZone.BYTE_SIZE; // Camera.

        writer.writeInt(regionOffset); // The location regions are stored at. This is still written even if there are no regions.

        // Save camera.
        this.cameraZone.save(writer);

        // Save regions.
        for (ZoneRegion region : getRegions())
            region.save(writer);
    }

    /**
     * Get the amount of regions.
     * @return regionCount
     */
    public int getRegionCount() {
        return getRegions().size();
    }

    /**
     * Setup the zone editor.
     * @param controller The controller controlling this.
     * @param editor     The editor to create an interface under.
     */
    public void setupEditor(MapUIController controller, GUIEditorGrid editor) {
        getMainRegion().setupEditor(controller, editor);
        editor.addLabel("Regions", String.valueOf(getRegionCount())); //TODO
        getCameraZone().setupEditor(controller, editor);
    }

    /**
     * Check if this zone contains a grid tile.
     * @param gridX The grid x coordinate.
     * @param gridZ The grid z coordinate.
     * @return contains
     */
    public boolean contains(int gridX, int gridZ) {
        return getRegion(gridX, gridZ) != null;
    }

    /**
     * Gets the ZoneRegion for a grid tile.
     * @param gridX The grid tile x coordinate.
     * @param gridZ The grid tile z coordinate.
     * @return region
     */
    public ZoneRegion getRegion(int gridX, int gridZ) {
        if (!mainRegion.contains(gridX, gridZ))
            return null; // Not present within the master zone.

        if (getRegions().isEmpty())
            return mainRegion;

        for (ZoneRegion region : getRegions())
            if (region.contains(gridX, gridZ))
                return region;
        return null; // Failed to find a region at this spot.
    }
}
