package net.highwayfrogs.editor.games.renderware.chunks;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.RwStreamSectionType;
import net.highwayfrogs.editor.games.renderware.struct.types.RpWorldChunkInfo;

/**
 * Represents the RenderWare world section.
 * Created by Kneesnap on 8/12/2024.
 */
public class RwWorldSection extends RwStreamChunk {
    public RwWorldSection(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamSectionType.WORLD, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        RpWorldChunkInfo worldInfo = readStruct(reader, RpWorldChunkInfo.class); // TODO:

        // TODO: Implement.
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        // TODO: Implement.
    }
}