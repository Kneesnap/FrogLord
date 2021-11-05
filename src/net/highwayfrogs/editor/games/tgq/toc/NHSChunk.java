package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

/**
 * Appears to be something related to animations?
 * Created by Kneesnap on 8/26/2019.
 */
@Getter
public class NHSChunk extends kcCResource {
    private String name;
    private float unknown1;
    private float unknown2;
    private String animName;

    private static final int NAME_SIZE = 48;
    private static final int ANIM_NAME_SIZE = 32;

    public NHSChunk(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.NAMEDHASH);
    }

    @Override
    public void load(DataReader reader) {
        this.name = reader.readTerminatedStringOfLength(NAME_SIZE);
        this.unknown1 = reader.readFloat();
        this.unknown2 = reader.readFloat();
        this.animName = reader.readTerminatedStringOfLength(ANIM_NAME_SIZE);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeTerminatedStringOfLength(this.name, NAME_SIZE);
        int endIndex = (writer.getIndex() + NAME_SIZE);
        writer.writeStringBytes(this.name);
        writer.writeTo(endIndex);
        writer.writeFloat(this.unknown1);
        writer.writeFloat(this.unknown2);

        endIndex = (writer.getIndex() + ANIM_NAME_SIZE);
        writer.writeStringBytes(this.animName);
        writer.writeTo(endIndex);
    }
}
