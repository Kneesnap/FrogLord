package net.highwayfrogs.editor.file.map.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MAP_ANIM struct.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
public class MAPAnimation extends GameObject {
    private short uChange; // Delta U (Each frame)
    private short vChange;
    private short duration; // Frames before resetting.

    private List<Short> textures = new ArrayList<>(); // Non-remapped texture id array.
    private short celPeriod;

    private short flags;
    private short polygonCount;
    private List<MAPUVInfo> mapUVs = new ArrayList<>();

    private transient MAPFile parentMap;

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
        this.duration = reader.readShort();
        reader.readBytes(4); // Four run-time bytes.

        // Texture information.
        short celCount = reader.readShort();
        reader.readShort(); // Run-time short.
        int celListPointer = reader.readInt();
        this.celPeriod = reader.readShort(); // Frames before resetting.
        reader.readShort(); // Run-time variable.

        reader.jumpTemp(celListPointer);
        System.out.println("Reading " + celCount + " textures from: " + Integer.toHexString(celListPointer));
        try {
            if (!MWDFile.CURRENT_FILE_NAME.equals("JUN1.MAP") && !MWDFile.CURRENT_FILE_NAME.equals("JUN2.MAP")) {
                for (int i = 0; i < celCount; i++)
                    textures.add(reader.readShort());
            }
        } catch (Exception ex) {
            System.out.println("Animation Index: " + getParentMap().getMapAnimations().size());
            throw new RuntimeException(ex);
        }
        reader.jumpReturn();

        this.flags = reader.readShort();
        this.polygonCount = reader.readShort();
        reader.readInt(); // Texture pointer. Generated at run-time.

        int mapUvInfoPointer = reader.readInt();
        reader.jumpTemp(mapUvInfoPointer);

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
        writer.writeShort(this.duration);
        writer.writeNull(4); // Run-time.
        writer.writeShort((short) this.textures.size());
        writer.writeNull(Constants.SHORT_SIZE); // Run-time.
        //TODO: Cel-List pointer. (After all anims.)

        writer.writeShort(this.celPeriod);
        writer.writeShort((short) 0); // Runtime.
        writer.writeShort(this.flags);
        writer.writeShort(this.polygonCount);
        writer.writeInt(0); // Run-time.
        //TODO: MAPUV Pointer.
    }
}
