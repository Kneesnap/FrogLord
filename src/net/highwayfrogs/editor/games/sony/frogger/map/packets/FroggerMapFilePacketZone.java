package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapZone;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

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
    public int getKnownStartAddress() {
        return getParentFile().getHeaderPacket().getZonePacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Zone Count", this.zones.size());
        return propertyList;
    }
}