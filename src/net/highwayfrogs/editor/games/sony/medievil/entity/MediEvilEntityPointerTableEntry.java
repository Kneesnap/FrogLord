package net.highwayfrogs.editor.games.sony.medievil.entity;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;

/**
 * Represents an entry in the MediEvil entity table.
 * Created by RampantSpirit on 3/11/2024.
 */
@Getter
public class MediEvilEntityPointerTableEntry extends SCGameData<MediEvilGameInstance> {
    private long entityDataPointer;
    private int overlayId;

    private static final int SIZE_IN_BYTES = 8;

    public MediEvilEntityPointerTableEntry(MediEvilGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.entityDataPointer = reader.readUnsignedIntAsLong();
        this.overlayId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedInt(this.entityDataPointer);
        writer.writeInt(this.overlayId);
    }
}