package net.highwayfrogs.editor.file.mof;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimation;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a MOF file.
 * TODO: 12ax and 11ax are animation files.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFFile extends GameFile {
    private boolean dummy; // Is this dummied out?
    private GameObject animation; // Animation data. For some reason they thought it'd be a good idea to make MOF have two different data structures.

    private byte id;
    private int flags;
    private int extra;
    private List<MOFPart> parts = new ArrayList<>();

    public static final int FLAG_OFFSETS_RESOLVED = 1; // Fairly sure this is applied by frogger.exe runtime, and not something that should be true in the MWD. (Verify though.)
    public static final int FLAG_SIZES_RESOLVED = 2; // Like before, this is likely frogger.exe run-time only. But, we should confirm that.
    public static final int FLAG_TEXTURES_RESOLVED = 4; // Again.
    public static final int FLAG_ANIMATION_FILE = 8; // This is an animation MOF file.
    public static final int FLAG_ANIMATED_POLY_FILE = 16; // MOF contains some animated textured polys. What's the difference between this and the normal animation MOF?
    public static final int FLAG_FLIPBOOK_FILE = 32; // Static flipbook file. (What does this mean?)

    public static final int FLAG_ANIM_TRANSFORMS_INDEXED = 1 << 16; // Appears like the only thing this is used for is making sure it's present. Otherwise, the game will crash.
    public static final int FLAG_ANIM_INDEXED_TRANSFORMS_IN_PARTS = 1 << 17; // I believe this should always be false.

    public static final int MOF_ID = 3;
    public static final int MAP_MOF_ID = 4;

    private static final Image ICON = loadIcon("swampy");
    private static final byte[] MOF_SIGNATURE = "FOM".getBytes();
    private static final byte[] DUMMY_DATA = "DUMY".getBytes();

    @Override
    public void load(DataReader reader) {
        byte id = reader.readByte();
        byte[] follow = reader.readBytes(MOF_SIGNATURE.length);

        if (Arrays.equals(follow, MOF_SIGNATURE)) { // Signature matches, read id. TODO: Accept 1ax too.
            this.id = id;
        } else {
            this.dummy = true;
            return;
        }

        reader.readInt(); // File length, including header.
        this.flags = reader.readInt();

        if (testFlag(FLAG_ANIMATION_FILE)) {
            resolveAnimatedMOF(reader);
        } else {
            resolveStaticMOF(reader);
        }

        //TODO
    }

    private void resolveStaticMOF(DataReader reader) {
        this.extra = reader.readInt();
        int partCount = this.extra;

        // Read MR_Parts
        for (int i = 0; i < partCount; i++) {
            MOFPart part = new MOFPart();
            part.load(reader);
            parts.add(part);
        }
    }

    private void resolveAnimatedMOF(DataReader reader) {
        this.animation = new MOFAnimation();
        this.animation.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        if (dummy) {
            writer.writeBytes(DUMMY_DATA);
            return;
        }

        writer.writeByte(this.id);
        writer.writeBytes(MOF_SIGNATURE);
        writer.writeInt(0); // File length. Should be ok to use zero for now, but if it causes problems, we know where to look.
        writer.writeInt(this.flags);

        if (animation != null) { // If this is an animation, save the animation.
            animation.save(writer);
            return;
        }

        writer.writeInt(this.extra);
        for (MOFPart part : getParts())
            part.save(writer);

        //TODO
    }

    /**
     * Test if a flag is present.
     * @param flag The flag to test.
     * @return flagPresent
     */
    public boolean testFlag(int flag) {
        return (this.flags & flag) == flag;
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public Node makeEditor() {
        return null;
    }
}
