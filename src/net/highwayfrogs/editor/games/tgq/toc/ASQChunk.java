package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

/**
 * Represents the ASQ chunk. It seems like it's either an entity definition, an animation definition, or something.
 * Created by Kneesnap on 3/23/2020.
 */
@Getter
public class ASQChunk extends TGQFileChunk {
    private String name;
    private int unknown1;
    private float unknown2;
    private int unknown3;
    private int unknown4;
    private int unknown5;

    private static final int NAME_SIZE = 32;

    public ASQChunk(TGQChunkedFile parentFile) {
        super(parentFile, null); //TODO
    }

    @Override
    public void load(DataReader reader) {
        this.name = reader.readTerminatedStringOfLength(NAME_SIZE);
        this.unknown1 = reader.readInt();
        this.unknown2 = reader.readFloat();
        this.unknown3 = reader.readInt();
        this.unknown4 = reader.readInt();
        this.unknown5 = reader.readInt();
        //TODO: More data that isn't always there. Have to figure that out before we can enable.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeTerminatedStringOfLength(this.name, NAME_SIZE);
        writer.writeInt(this.unknown1);
        writer.writeFloat(this.unknown2);
        writer.writeInt(this.unknown3);
        writer.writeInt(this.unknown4);
        writer.writeInt(this.unknown5);
    }
}
