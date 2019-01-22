package net.highwayfrogs.editor.file.map.animation;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.gui.mesh.MeshData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the MAP_ANIM struct.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
@Setter
public class MAPAnimation extends GameObject {
    private int flags;
    private short uChange; // Delta U (Each frame)
    private short vChange;
    private int uvDuration; // Frames before resetting.
    private int texDuration; // Also known as celPeriod.
    private List<Short> textures = new ArrayList<>(); // Non-remapped texture id array.
    private List<MAPUVInfo> mapUVs = new ArrayList<>();

    private transient MAPFile parentMap;
    private transient int texturePointerAddress;
    private transient int uvPointerAddress;
    private transient MeshData tileHighlightData;

    public static final int FLAG_UV = Constants.BIT_FLAG_0; // Uses UV animation.
    public static final int FLAG_TEXTURE = Constants.BIT_FLAG_1; // Uses cel list animation.
    public static final int BYTE_SIZE = 2 + (7 * Constants.SHORT_SIZE) + (4 * Constants.INTEGER_SIZE);

    public MAPAnimation(MAPFile mapFile) {
        this.parentMap = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        this.uChange = reader.readUnsignedByteAsShort();
        this.vChange = reader.readUnsignedByteAsShort();
        this.uvDuration = reader.readUnsignedShortAsInt();
        reader.readBytes(4); // Four run-time bytes.

        // Texture information.
        short celCount = reader.readShort();
        reader.readShort(); // Run-time short.
        int celListPointer = reader.readInt();
        this.texDuration = reader.readUnsignedShortAsInt(); // Frames before resetting.
        reader.readShort(); // Run-time variable.

        //TODO: There appears to be corrupted animations in certain stages in the retail MWD. It has numbers that make no sense. I was unable to find any sort of condition in the data flagging it to not load textures. So, I'm just going a pretty nasty check here which seems to work more or less, but we really need to find a solution for this.
        if (celCount <= 1000) {
            reader.jumpTemp(celListPointer);
            for (int i = 0; i < celCount; i++)
                textures.add(reader.readShort());
            reader.jumpReturn();
        }

        this.flags = reader.readUnsignedShortAsInt();
        int polygonCount = reader.readUnsignedShortAsInt();
        reader.readInt(); // Texture pointer. Generated at run-time.

        reader.jumpTemp(reader.readInt()); // Map UV Pointer.
        for (int i = 0; i < polygonCount; i++) {
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
        writer.writeUnsignedShort(this.uvDuration);
        writer.writeNull(4); // Run-time.
        writer.writeShort((short) this.textures.size());
        writer.writeNull(Constants.SHORT_SIZE); // Run-time.

        this.texturePointerAddress = writer.getIndex();
        writer.writeNull(Constants.POINTER_SIZE);

        writer.writeUnsignedShort(this.texDuration);
        writer.writeShort((short) 0); // Runtime.
        writer.writeUnsignedShort(this.flags);
        writer.writeUnsignedShort(getMapUVs().size());
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
     * Setup an animation editor.
     * @param editor The editor to setup under.
     */
    public void setupEditor(MapUIController controller, GUIEditorGrid editor) {
        editor.addIntegerField("Flags", getFlags(), this::setFlags, null);
        editor.addShortField("u Frame Change", getUChange(), this::setUChange, null);
        editor.addShortField("v Frame Change", getVChange(), this::setVChange, null);
        editor.addIntegerField("UV Frame Count", getUvDuration(), this::setUvDuration, null);
        editor.addIntegerField("Tex Frame Count", getTexDuration(), this::setTexDuration, null);
        editor.addLabel("Textures", Arrays.toString(getTextures().toArray())); //TODO: TEXTURES.  Non-remapped texture id array. TODO: MAKE TEXTURES. TODO: Allow editing.

        editor.addButton("Edit", () -> controller.getController().editAnimation(this));
    }
}
