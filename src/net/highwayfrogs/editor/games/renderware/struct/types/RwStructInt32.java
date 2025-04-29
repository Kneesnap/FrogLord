package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;

/**
 * Represents a struct containing a single integer value.
 * Created by Kneesnap on 8/26/2024.
 */
@Getter
public class RwStructInt32 extends RwStruct {
    private int value;

    public RwStructInt32(GameInstance instance) {
        super(instance, RwStructType.GENERIC_INTEGER);
    }

    public RwStructInt32(GameInstance instance, int value) {
        this(instance);
        this.value = value;
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        this.value = reader.readInt();
    }

    @Override
    public void save(DataWriter writer, int version) {
        writer.writeInt(this.value);
    }
}