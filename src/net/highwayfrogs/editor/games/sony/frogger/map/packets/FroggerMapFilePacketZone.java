package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapCameraZone;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapZone;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents camera zone data.
 * Created by Kneesnap on 5/25/2024.
 */
@Getter
public class FroggerMapFilePacketZone extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "ZONE";
    private final List<FroggerMapZone> zones = new ArrayList<>();

    public FroggerMapFilePacketZone(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.zones.clear();
        if (getParentFile().isEarlyMapFormat() && (getConfig().isAtOrBeforeBuild1() || !getParentFile().isIsland())) { // ISLAND.MAP, it's just the island placeholders and QB.MAP we want to skip.
            if (getParentFile().getHeaderPacket().getFormPacketAddress() > reader.getIndex())
                reader.setIndex(getParentFile().getHeaderPacket().getFormPacketAddress());
            return;
        }

        int zoneCount = reader.readInt();
        int zonePointerList = reader.getIndex();
        reader.setIndex(zonePointerList + (zoneCount * Constants.POINTER_SIZE));
        for (int i = 0; i < zoneCount; i++) {
            // Read from the pointer list.
            reader.jumpTemp(zonePointerList);
            int nextZoneStartAddress = reader.readInt();
            zonePointerList = reader.getIndex();
            reader.jumpReturn();

            // Read zone.
            reader.requireIndex(getLogger(), nextZoneStartAddress, "Expected FroggerMapZone");
            this.zones.add(FroggerMapZone.readFroggerMapZone(reader, getParentFile()));
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(this.zones.size()); // zoneCount

        // Write slots for pointers to the zone data.
        int zonePointerListAddress = writer.getIndex();
        for (int i = 0; i < this.zones.size(); i++)
            writer.writeNullPointer();

        // Write the zones.
        for (int i = 0; i < this.zones.size(); i++) {
            // Write the pointer to the zone we're about to save.
            int nextZoneStartAddress = writer.getIndex();
            writer.jumpTemp(zonePointerListAddress);
            writer.writeInt(nextZoneStartAddress);
            zonePointerListAddress = writer.getIndex();
            writer.jumpReturn();

            // Write zone data.
            this.zones.get(i).save(writer);
        }
    }

    @Override
    public void clear() {
        this.zones.clear();
    }

    @Override
    public void copyAndConvertData(SCFilePacket<? extends SCChunkedFile<FroggerGameInstance>, FroggerGameInstance> newChunk) {
        if (!(newChunk instanceof FroggerMapFilePacketZone))
            throw new ClassCastException("The provided chunk was of type " + Utils.getSimpleName(newChunk) + " when " + FroggerMapFilePacketZone.class.getSimpleName() + " was expected.");

        FroggerMapFilePacketZone newZoneChunk = (FroggerMapFilePacketZone) newChunk;
        for (int i = 0; i < this.zones.size(); i++)
            newZoneChunk.getZones().add(this.zones.get(i).clone(newZoneChunk.getParentFile()));
    }

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getHeaderPacket().getZonePacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Zone Count", this.zones.size());
        return propertyList;
    }

    /**
     * Finds the first camera zone which contains the given position.
     * @param gridX the x grid coordinate to find
     * @param gridZ the z grid coordinate to find
     * @return cameraZone, if one exists
     */
    public FroggerMapCameraZone getCameraZone(int gridX, int gridZ) {
        for (int i = 0; i < this.zones.size(); i++) {
            FroggerMapZone mapZone = this.zones.get(i);
            if (mapZone instanceof FroggerMapCameraZone && mapZone.contains(gridX, gridZ))
                return (FroggerMapCameraZone) mapZone;
        }

        return null;
    }
}