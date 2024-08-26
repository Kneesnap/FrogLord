package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.IRwStreamChunkWithEmbeddedStruct;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a fake struct. Or rather, it represents a struct which is just a proxy containing data which should be associated with a parent chunk.
 * NOTE: Do not use automatic extension chunk reading with this type, as such data would not be kept to save.
 * Created by Kneesnap on 8/26/2024.
 */
@Getter
public class RwStructParentData<TChunk extends RwStreamChunk & IRwStreamChunkWithEmbeddedStruct> extends RwStruct {
    private final TChunk parentChunk;

    public RwStructParentData(TChunk parentChunk) {
        super(parentChunk.getGameInstance(), RwStructType.PARENT_DATA_PROXY);
        this.parentChunk = parentChunk;
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        this.parentChunk.loadEmbeddedStructData(reader, version, byteLength);
    }

    @Override
    public void save(DataWriter writer, int version) {
        this.parentChunk.saveEmbeddedStructData(writer, version);
    }

    @Override
    public String getLoggerInfo() {
        return super.getLoggerInfo() + ",parentChunkType=" + Utils.getSimpleName(this.parentChunk);
    }
}