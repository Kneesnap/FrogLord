package net.highwayfrogs.editor.games.renderware.struct;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;

/**
 * The struct used when the data is unsupported.
 * Created by Kneesnap on 8/12/2024.
 */
@Getter
public class RwUnsupportedStruct extends RwStruct {
    private byte[] rawData;

    public RwUnsupportedStruct(GameInstance instance) {
        super(instance, RwStructType.UNKNOWN);
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        this.rawData = reader.readBytes(byteLength);
    }

    @Override
    public void save(DataWriter writer, int version) {
        writer.writeBytes(this.rawData);
    }
}