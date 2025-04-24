package net.highwayfrogs.editor.file.standard;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the MR_TEXTURE struct.
 * Created by Kneesnap on 2/1/2019.
 */
@Getter
public class MRTexture extends GameObject {
    private int flags;
    private short width;
    private short height;
    private ByteUV[] uvs = new ByteUV[UV_COUNT];
    private int clutId;
    private int texturePage;

    private static final int UV_COUNT = 4;
    public static final int BYTE_SIZE = (3 * Constants.SHORT_SIZE) + (2 * Constants.BYTE_SIZE) + (UV_COUNT * ByteUV.BYTE_SIZE);

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        this.width = reader.readUnsignedByteAsShort();
        this.height = reader.readUnsignedByteAsShort();
        readUV(reader, 0);
        this.clutId = reader.readUnsignedShortAsInt();
        readUV(reader, 1);
        this.texturePage = reader.readUnsignedShortAsInt();
        readUV(reader, 2);
        readUV(reader, 3);
    }

    private void readUV(DataReader reader, int index) {
        ByteUV uv = new ByteUV();
        uv.load(reader);
        this.uvs[index] = uv;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.flags);
        writer.writeUnsignedByte(this.width);
        writer.writeUnsignedByte(this.height);
        this.uvs[0].save(writer);
        writer.writeUnsignedShort(this.clutId);
        this.uvs[1].save(writer);
        writer.writeUnsignedShort(this.texturePage);
        this.uvs[2].save(writer);
        this.uvs[3].save(writer);
    }
}
