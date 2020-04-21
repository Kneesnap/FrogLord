package net.highwayfrogs.editor.games.tgq.toc;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

/**
 * Represents a game script.
 * Created by Kneesnap on 3/23/2020.
 */
public class GENChunk extends TGQFileChunk {
    private String name;

    public static final int NAME_LENGTH = 32; //TODO: Actually might be two strings, each 16, name, desc. Not sure yet.

    public GENChunk(TGQChunkedFile parentFile) {
        super(parentFile, null); //TODO
    }

    @Override
    public void load(DataReader reader) {
        this.name = reader.readTerminatedStringOfLength(NAME_LENGTH);
        // Command?
        // Data Length.
        // Pointer to... something? Next Script? Seems to change each time. Could also be two shorts. TODO: Could be the hash to the next script.

        //TODO
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeTerminatedStringOfLength(this.name, NAME_LENGTH);
        //TODO

    }
}
