package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.IRwStreamSectionType;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.gui.ImageResource;

/**
 * Represents an unsupported renderware chunk.
 * Created by Kneesnap on 6/9/2020.
 */
public class RwUnsupportedChunk extends RwStreamChunk {
    public RwUnsupportedChunk(RwStreamFile streamFile, int chunkId, int rwVersion, RwStreamChunk parentChunk) {
        super(streamFile, new RwUnsupportedChunkType(chunkId), rwVersion, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        reader.skipBytes(dataLength); // We've already gotten the chunk data stored as the rawReadData.
    }

    @Override
    public void saveChunkData(DataWriter writer) {
        if (getRawReadData() != null)
            writer.writeBytes(getRawReadData());
    }

    @Override
    protected String getLoggerInfo() {
        return "id=" + Integer.toHexString(getSectionType().getTypeId()).toUpperCase() + "," + super.getLoggerInfo();
    }

    @Getter
    @RequiredArgsConstructor
    private static class RwUnsupportedChunkType implements IRwStreamSectionType {
        private final int typeId;

        @Override
        public String getDisplayName() {
            return "Unsupported (ID: " + Integer.toHexString(this.typeId).toUpperCase() + ")";
        }

        @Override
        public ImageResource getIcon() {
            return ImageResource.QUESTION_MARK_15;
        }
    }
}