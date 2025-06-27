package net.highwayfrogs.editor.games.sony.shared.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFilePacket;
import net.highwayfrogs.editor.games.sony.shared.map.data.SCMapEntity;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the entity packet seen in standardized v2 maps. (Seen: MediEvil 2, C-12, Not Used: MoonWarrior)
 * Created by Kneesnap on 5/14/2024.
 */
@Getter
public class SCMapEntityPacket<TGameInstance extends SCGameInstance> extends SCMapFilePacket<SCMapFile<TGameInstance>, TGameInstance> implements IPropertyListCreator {
    public static final String IDENTIFIER = "ENTP";
    private final List<SCMapEntity> entities = new ArrayList<>();

    public SCMapEntityPacket(SCMapFile<TGameInstance> parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int entityCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE);
        int entityListPtr = reader.readInt();
        if (entityListPtr != reader.getIndex()) {
            getLogger().warning("Expected entity data to start at 0x%X, but it actually started at 0x%X.", reader.getIndex(), entityListPtr);
            reader.setIndex(entityListPtr);
        }

        // Read entities.
        this.entities.clear();
        for (int i = 0; i < entityCount; i++) {
            SCMapEntity entity = new SCMapEntity(getParentFile());
            entity.load(reader);
            this.entities.add(entity);
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.entities.size());
        writer.writeNull(Constants.SHORT_SIZE); // Padding.
        int entityListStartPtrAddress = writer.writeNullPointer();
        writer.writeAddressTo(entityListStartPtrAddress);

        // Write entities.
        for (int i = 0; i < this.entities.size(); i++)
            this.entities.get(i).save(writer);
    }

    @Override
    public void clear() {
        this.entities.clear();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Entity Count", this.entities.size());
        return propertyList;
    }
}