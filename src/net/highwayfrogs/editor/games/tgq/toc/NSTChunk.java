package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

/**
 * Holds entity instances? NST probably stands for instance.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class NSTChunk extends TGQFileChunk {
    private String name;
    private float x;
    private float y;
    private float z;
    private float yaw;
    private float pitch;
    private int unknown1 = 0xF0; // Usually 240, in some circumstances, 180. Flags? Maybe the 180s are invisible or something.
    private int unknown2 = 1; // Either 1 or 2. If it's a FrogInst, it's 2, otherwise, it's 1.
    private int unknown4; // Sometimes -1, sometimes an id?
    private float unknown5;
    private int unknown6; // Flags? 8, 256, sometimes something large like 133072. Could be flags.
    //Find: Entity id, entity type. Confirm rotation.

    private static final int NAME_SIZE = 32;

    private static final int FULL_SIZE = 272; //TODO: Always this size, except for some models which are 212 bytes.

    public NSTChunk(TGQChunkedFile parentFile) {
        super(parentFile, TGQChunkType.NST);
    }

    @Override
    public void load(DataReader reader) {
        this.name = reader.readTerminatedStringOfLength(NAME_SIZE);
        this.unknown1 = reader.readInt(); // Usually 0xF0
        this.yaw = reader.readFloat(); // Could be wrong.
        this.pitch = reader.readFloat();
        this.unknown2 = reader.readInt(); // Seems to be 1 usually.

        int alwaysZero = reader.readInt();
        if (alwaysZero != 0)
            throw new RuntimeException("NST always-zero value was not zero. (" + alwaysZero + ").");

        this.unknown4 = reader.readInt(); // -1.
        this.unknown5 = reader.readFloat(); // Seems to be consistent among models.
        this.unknown6 = reader.readInt(); // 0, 0x1000

        int alwaysOne = reader.readInt();
        if (alwaysOne != 1)
            throw new RuntimeException("NST always-one value was not one. (" + alwaysOne + ").");

        this.x = reader.readFloat();
        this.y = reader.readFloat();
        this.z = reader.readFloat();

        //System.out.println(this.name + " -> [" + this.x + ", " + this.y + ", " + this.z + "] " + this.unknown1 + ", " + this.unknown4 + ", " + this.unknown5 + ", " + this.unknown6 + " END [" + this.yaw + ", " + this.pitch + "]");


        //TODO: Model id?
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeTerminatedStringOfLength(this.name, NAME_SIZE);

        writer.writeInt(this.unknown1);
        writer.writeFloat(this.yaw);
        writer.writeFloat(this.pitch);
        writer.writeInt(this.unknown2);
        writer.writeInt(0);
        writer.writeInt(this.unknown4);
        writer.writeFloat(this.unknown5);
        writer.writeInt(this.unknown6);
        writer.writeInt(1);
        writer.writeFloat(this.x);
        writer.writeFloat(this.y);
        writer.writeFloat(this.z);

        //TODO
    }
}
