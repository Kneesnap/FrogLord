package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.entity.MediEvilMapEntity;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MediEvil map format entities packet.
 * Created by RampantSpirit on 3/11/2024.
 */
@Getter
public class MediEvilMapEntitiesPacket extends MediEvilMapPacket implements IPropertyListCreator {
    public static final String IDENTIFIER = "PTME"; // 'EMTP'.
    private final List<MediEvilMapEntity> entities = new ArrayList<>();

    public MediEvilMapEntitiesPacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int entityCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequire((byte) 0xFF, Constants.SHORT_SIZE); // Padding
        int entityListPtr = reader.readInt();

        // Read entities.
        this.entities.clear();
        reader.requireIndex(getLogger(), entityListPtr, "Expected entity list");
        for (int i = 0; i < entityCount; i++) {
            MediEvilMapEntity entity = new MediEvilMapEntity(getParentFile());
            entity.load(reader);
            this.entities.add(entity);
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.entities.size());
        writer.writeShort((short) -1); // Padding.
        int entityListPtr = writer.writeNullPointer();

        writer.writeAddressTo(entityListPtr);
        for (int i = 0; i < this.entities.size(); i++)
            this.entities.get(i).save(writer);
    }

    @Override
    public void clear() {
        this.entities.clear();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.add("Entities", this.entities.size());
    }
}