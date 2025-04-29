package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the 'DESERT_FALLING_ROCK' entity data definition from ent_des.h.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataFallingRock extends FroggerEntityDataMatrix {
    private final FallingRockTarget[] targets = new FallingRockTarget[ROCK_TARGET_COUNT];
    private int delay = 0; // Delay until rock starts moving.
    private short bounceCount = 1; // Number of bounces.
    private int flags; // Flags
    private int sound; // Does this rock have a sound effect? Appears unused.

    public static final int ROCK_TARGET_COUNT = 12;
    public static final int FLAG_TARGETS_RESOLVED = Constants.BIT_FLAG_0; // Believed to be a run-time flag.
    public static final int FLAG_VALIDATION_MASK = 0;

    public FroggerEntityDataFallingRock(FroggerMapFile mapFile) {
        super(mapFile);
        for (int i = 0; i < this.targets.length; i++)
            this.targets[i] = new FallingRockTarget(i);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        for (int i = 0; i < this.targets.length; i++)
            this.targets[i].load(reader);

        this.delay = reader.readUnsignedShortAsInt();
        this.bounceCount = reader.readUnsignedByteAsShort();
        reader.skipBytesRequireEmpty(1); // Padding
        this.flags = reader.readInt();
        if (!getConfig().isAtOrBeforeBuild38()) // At/before build 38 it does seem there's stuff written. No idea what it means though.
            warnAboutInvalidBitFlags(this.flags, FLAG_VALIDATION_MASK);
        this.sound = getConfig().isAtOrBeforeBuild38() ? -1 : reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (int i = 0; i < this.targets.length; i++)
            this.targets[i].save(writer);

        writer.writeUnsignedShort(this.delay);
        writer.writeUnsignedByte(this.bounceCount);
        writer.writeByte(Constants.NULL_BYTE); // Padding.
        writer.writeInt(this.flags);
        if (!getConfig().isAtOrBeforeBuild38())
            writer.writeInt(this.sound);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addUnsignedFixedShort("Delay (secs)", this.delay, newDelay -> this.delay = newDelay, 30);
        if (this.flags != 0)
            editor.addSignedIntegerField("Flags", this.flags, newFlags -> this.flags = newFlags);
        if (!getConfig().isAtOrBeforeBuild38() && this.sound != 0)
            editor.addSignedIntegerField("Sound (Unused)", this.sound, newSound -> this.sound = newSound);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        super.setupEditor(editor, manager);

        // Allow changing the number of bounces before break.
        editor.addUnsignedByteField("Bounces Before Break", this.bounceCount, newBounceCount -> newBounceCount <= ROCK_TARGET_COUNT, newBounceCount -> {
            this.bounceCount = newBounceCount;
            manager.updateEditor();
        });

        // Setup the editor for the enabled bounce targets.
        for (int i = 0; i < this.bounceCount; i++)
            this.targets[i].setupEditor(editor, manager.getController());
    }

    @Getter
    public static final class FallingRockTarget extends GameObject {
        private final int index;
        private final SVector target = new SVector(); // Target Position.
        private int time = 30; // Time to reach target.

        public FallingRockTarget(int index) {
            this.index = index;
        }

        @Override
        public void load(DataReader reader) {
            this.target.loadWithPadding(reader);
            this.time = reader.readUnsignedShortAsInt();
            reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.
        }

        @Override
        public void save(DataWriter writer) {
            this.target.saveWithPadding(writer);
            writer.writeUnsignedShort(this.time);
            writer.writeNull(Constants.SHORT_SIZE); // Padding.
        }

        /**
         * Sets up the editor.
         * @param grid the editor grid to create the editor for
         * @param controller the controller to use
         */
        public void setupEditor(GUIEditorGrid grid, FroggerMapMeshController controller) {
            grid.addFloatSVector("Bounce Target #" + (this.index + 1), this.target, controller);
            grid.addUnsignedFixedShort("Time (secs)", this.time, newTime -> this.time = newTime, 30);
        }
    }
}