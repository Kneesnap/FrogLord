package net.highwayfrogs.editor.file.map.zone;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerCameraRotation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapCameraZone;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapCameraZone.FroggerMapCameraZoneFlag;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapZone;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapZoneRegion;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

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

    /**
     * Converts the zone to the new map format.
     */
    public FroggerMapZone convertToNewFormat(FroggerMapFile mapFile) {
        FroggerMapCameraZone newMapZone = new FroggerMapCameraZone(mapFile);

        // Apply main region.
        newMapZone.getBoundingRegion().setXMin(this.mainRegion.getXMin());
        newMapZone.getBoundingRegion().setZMin(this.mainRegion.getZMin());
        newMapZone.getBoundingRegion().setXMax(this.mainRegion.getXMax());
        newMapZone.getBoundingRegion().setZMax(this.mainRegion.getZMax());

        // Apply regions
        for (ZoneRegion region : this.regions) {
            FroggerMapZoneRegion newRegion = new FroggerMapZoneRegion(newMapZone);
            newRegion.setXMin(region.getXMin());
            newRegion.setZMin(region.getZMin());
            newRegion.setXMax(region.getXMax());
            newRegion.setZMax(region.getZMax());
            newMapZone.addRegion(newRegion);
        }

        // Apply camera data.
        for (FroggerMapCameraZoneFlag camZoneFlag : FroggerMapCameraZoneFlag.values())
            newMapZone.setFlag(camZoneFlag, this.cameraZone.testFlag(camZoneFlag));
        Utils.verify(newMapZone.getFlags() == (short) this.cameraZone.getFlags(), "Flags did not match! (%d vs %d)", newMapZone.getFlags(), (short) this.cameraZone.getFlags());
        newMapZone.setForcedCameraDirection(FroggerCameraRotation.getCameraRotationFromID(this.cameraZone.getForceDirection()));
        newMapZone.getNorthSourceOffset().setValues(this.cameraZone.getNorthSourceOffset());
        newMapZone.getEastSourceOffset().setValues(this.cameraZone.getEastSourceOffset());
        newMapZone.getSouthSourceOffset().setValues(this.cameraZone.getSouthSourceOffset());
        newMapZone.getWestSourceOffset().setValues(this.cameraZone.getWestSourceOffset());
        newMapZone.getNorthTargetOffset().setValues(this.cameraZone.getNorthTargetOffset());
        newMapZone.getEastTargetOffset().setValues(this.cameraZone.getEastTargetOffset());
        newMapZone.getSouthTargetOffset().setValues(this.cameraZone.getSouthTargetOffset());
        newMapZone.getWestTargetOffset().setValues(this.cameraZone.getWestTargetOffset());

        return newMapZone;
    }
}
