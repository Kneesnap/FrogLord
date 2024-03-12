package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.entity.MediEvilMapEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MediEvil map format entities packet.
 * Created by RampantSpirit on 3/11/2024.
 */
@Getter
public class MediEvilMapEntitiesPacket extends MediEvilMapPacket {
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
}