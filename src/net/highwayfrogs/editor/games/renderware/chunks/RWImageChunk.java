package net.highwayfrogs.editor.games.renderware.chunks;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RWSChunk;

/**
 * Created by Kneesnap on 6/9/2020.
 */
public class RWImageChunk extends RWSChunk {

    public RWImageChunk(int typeId, int renderwareVersion, RWSChunk parentChunk) {
        super(typeId, renderwareVersion, parentChunk);
    }

    @Override
    public void loadChunkData(DataReader reader) {
        int width; //TODO: Get from parent.
        int height;
        //BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
    }

    @Override
    public void saveChunkData(DataWriter writer) {

    }
}
