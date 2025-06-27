package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.entity.MediEvilMapEntity;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
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
        reader.skipShort(); // Padding (-1)
        int entityListPtr = reader.readInt();

        // Read entities.
        this.entities.clear();
        reader.jumpTemp(entityListPtr);
        for (int i = 0; i < entityCount; i++) {
            MediEvilMapEntity entity = new MediEvilMapEntity(getParentFile());
            entity.load(reader);
            this.entities.add(entity);
        }

        reader.setIndex(endIndex);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        // TODO: Implement.
    }

    @Override
    public void clear() {
        this.entities.clear();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Entities", this.entities.size());
        return propertyList;
    }
}