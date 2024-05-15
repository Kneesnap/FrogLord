package net.highwayfrogs.editor.games.sony.moonwarrior;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFilePacket;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the MoonWarrior entity packet.
 * TODO: The entity format is not currently understood / reverse engineered.
 * Created by Kneesnap on 5/12/2024.
 */
@Getter
public class MoonWarriorMapEntityPacket extends SCMapFilePacket<MoonWarriorMap, MoonWarriorInstance> implements IPropertyListCreator {
    private static final String IDENTIFIER = "MWEN"; // Moon Warrior Entities?
    private final List<MoonWarriorEntity> entities = new ArrayList<>();

    public MoonWarriorMapEntityPacket(MoonWarriorMap parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int entityCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE);
        int entityDataStartPointer = reader.readInt();
        if (reader.getIndex() != entityDataStartPointer)
            throw new RuntimeException("Couldn't start reading entity data, since we expected it to start at " + Utils.toHexString(entityDataStartPointer) + ", but it was actually at " + Utils.toHexString(reader.getIndex()));

        // Read entities.
        this.entities.clear();
        for (int i = 0; i < entityCount; i++) {
            MoonWarriorEntity newEntity = new MoonWarriorEntity(getParentFile());
            newEntity.load(reader);
            this.entities.add(newEntity);
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(this.entities.size());
        int entityDataStartPointer = writer.writeNullPointer();
        writer.writeAddressTo(entityDataStartPointer);

        // Write entities.
        for (int i = 0; i < this.entities.size(); i++)
            this.entities.get(i).save(writer);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Entity Count", this.entities.size());
        return propertyList;
    }

    /**
     * Represents Moon Warrior entity data.
     */
    @Getter
    public static class MoonWarriorEntity extends SCGameData<MoonWarriorInstance> {
        private final MoonWarriorMap mapFile;
        private int identifier; // Example: 'PLYR', 'FXW0', 'MONK'
        private int classUnknown1;
        private int classUnknown2;
        private byte[] unknownBytes;

        public MoonWarriorEntity(MoonWarriorMap mapFile) {
            super(mapFile.getGameInstance());
            this.mapFile = mapFile;
        }

        @Override
        public void load(DataReader reader) {
            int dataStartIndex = reader.getIndex();

            // Header.
            this.identifier = reader.readInt();
            this.classUnknown1 = reader.readInt();
            this.classUnknown2 = reader.readInt();
            int entitySizeInBytes = reader.readInt();
            this.unknownBytes = reader.readBytes(entitySizeInBytes - (reader.getIndex() - dataStartIndex));
        }

        @Override
        public void save(DataWriter writer) {
            int dataStartIndex = writer.getIndex();

            // Header.
            writer.writeInt(this.identifier);
            writer.writeInt(this.classUnknown1);
            writer.writeInt(this.classUnknown2);
            writer.writeInt(this.unknownBytes.length + (writer.getIndex() - dataStartIndex) + Constants.INTEGER_SIZE);
            writer.writeBytes(this.unknownBytes);
        }
    }
}