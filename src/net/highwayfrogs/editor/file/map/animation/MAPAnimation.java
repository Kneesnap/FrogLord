package net.highwayfrogs.editor.file.map.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MAP_ANIM struct.
 * TODO: Issue with reading textures.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
public class MAPAnimation extends GameObject {
    private short uChange; // Delta U (Each frame)
    private short vChange;
    private int duration; // Frames before resetting.

    private List<Short> textures = new ArrayList<>(); // Non-remapped texture id array.
    private int celPeriod;

    private int flags;
    private int polygonCount;
    private List<MAPUVInfo> mapUVs = new ArrayList<>();

    private transient MAPFile parentMap;
    private transient int texturePointerAddress;
    private transient int uvPointerAddress;

    public static final int FLAG_UV = 1; // Uses UV animation.
    public static final int FLAG_TEXTURE = 2; // Uses cel list animation.

    private static final int GLOBAL_TEXTURE_FLAG = 0x8000;
    public static final int FLAG_UV_ANIMATION = 1;
    public static final int FLAG_TEXTURE_ANIMATION = 2;

    public static final int BYTE_SIZE = 2 + (7 * Constants.SHORT_SIZE) + (4 * Constants.INTEGER_SIZE);

    public MAPAnimation(MAPFile mapFile) {
        this.parentMap = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        this.uChange = reader.readUnsignedByteAsShort();
        this.vChange = reader.readUnsignedByteAsShort();
        this.duration = reader.readUnsignedShortAsInt();
        reader.readBytes(4); // Four run-time bytes.

        // Texture information.
        short celCount = reader.readShort();
        reader.readShort(); // Run-time short.
        int celListPointer = reader.readInt();
        this.celPeriod = reader.readUnsignedShortAsInt(); // Frames before resetting.
        reader.readShort(); // Run-time variable.

        if (testFlag(FLAG_TEXTURE)) { // Only read textures if the texture flag is set.
            reader.jumpTemp(celListPointer);
            for (int i = 0; i < celCount; i++)
                textures.add(reader.readShort());
            reader.jumpReturn();
            System.out.println("Accepting: " + MWDFile.CURRENT_FILE_NAME + " (" + this.flags + ")");
        }

        this.flags = reader.readUnsignedShortAsInt();
        this.polygonCount = reader.readUnsignedShortAsInt();
        reader.readInt(); // Texture pointer. Generated at run-time.

        reader.jumpTemp(reader.readInt()); // Map UV Pointer.
        for (int i = 0; i < this.polygonCount; i++) {
            MAPUVInfo info = new MAPUVInfo(getParentMap());
            info.load(reader);
            mapUVs.add(info);
        }

        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte(this.uChange);
        writer.writeUnsignedByte(this.vChange);
        writer.writeUnsignedShort(this.duration);
        writer.writeNull(4); // Run-time.
        writer.writeShort((short) this.textures.size());
        writer.writeNull(Constants.SHORT_SIZE); // Run-time.

        this.texturePointerAddress = writer.getIndex();
        writer.writeNull(Constants.POINTER_SIZE);

        writer.writeUnsignedShort(this.celPeriod);
        writer.writeShort((short) 0); // Runtime.
        writer.writeUnsignedShort(this.flags);
        writer.writeUnsignedShort(this.polygonCount);
        writer.writeInt(0); // Run-time.

        this.uvPointerAddress = writer.getIndex();
        writer.writeNull(Constants.POINTER_SIZE);
    }

    /**
     * Called after animations are saved, this saves texture ids.
     * @param writer The writer to write to.
     */
    public void writeTextures(DataWriter writer) {
        Utils.verify(getTexturePointerAddress() > 0, "There is no saved address to write the texture pointer at.");

        if (!testFlag(FLAG_TEXTURE)) { // This doesn't have any textures, return.
            this.texturePointerAddress = 0;
            return;
        }

        int textureLocation = writer.getIndex();
        writer.jumpTemp(this.texturePointerAddress);
        writer.writeInt(textureLocation);
        writer.jumpReturn();

        for (short texId : getTextures())
            writer.writeShort(texId);

        this.texturePointerAddress = 0;
    }

    /**
     * Called after textures are written, this saves Map UVs.
     * @param writer The writer to write to.
     */
    public void writeMapUVs(DataWriter writer) {
        Utils.verify(getUvPointerAddress() > 0, "There is no saved address to write the uv pointer at.");

        int uvLocation = writer.getIndex();
        writer.jumpTemp(this.uvPointerAddress);
        writer.writeInt(uvLocation);
        writer.jumpReturn();

        for (MAPUVInfo mapUV : getMapUVs())
            mapUV.save(writer);

        this.uvPointerAddress = 0;
    }

    /**
     * Test if a flag is present for this animation.
     * @param flag The flag to test.
     * @return isFlagPresent
     */
    public boolean testFlag(int flag) {
        return (getFlags() & flag) == flag;
    }
}
