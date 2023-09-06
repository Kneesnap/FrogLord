package net.highwayfrogs.editor.games.renderware;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a RWS (Renderware Stream) file. Can have different extensions, for instance,
 * http://wiki.xentax.com/index.php/RWS
 * https://www.grandtheftwiki.com/RenderWare
 * Chunk Resources:
 * https://gtamods.com/wiki/RenderWare_binary_stream_file
 * https://github.com/aap/rwtools/blob/master/include/renderware.h
 * https://github.com/scandinavianaddons/rwtools/blob/master/src/renderware.cpp
 * Created by Kneesnap on 6/9/2020.
 */
@Getter
public class RWSFile extends GameObject {
    private final List<RWSChunk> chunks = new ArrayList<>();

    @Override
    public void load(DataReader reader) {
        while (reader.hasMore())
            this.chunks.add(RWSChunkManager.readChunk(reader, null));
    }

    @Override
    public void save(DataWriter writer) {
        for (RWSChunk chunk : this.chunks)
            chunk.save(writer);
    }
}
