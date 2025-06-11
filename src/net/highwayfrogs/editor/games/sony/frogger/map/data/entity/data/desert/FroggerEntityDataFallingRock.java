package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.desert;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the 'DESERT_FALLING_ROCK' entity data definition from ent_des.h.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class FroggerEntityDataFallingRock extends FroggerEntityDataMatrix {
    private final FallingRockTarget[] targets = new FallingRockTarget[ROCK_TARGET_COUNT];
    private int delay = 0; // Delay until rock starts moving.
    private short bounceCount = 1; // Number of bounces.
    private int flags; // Flags
    private int sound = -1; // Does this rock have a sound effect? Appears unused.

    public static final int ROCK_TARGET_COUNT = 12;
    public static final int FLAG_TARGETS_RESOLVED = Constants.BIT_FLAG_0; // Believed to be a run-time flag.
    public static final int FLAG_VALIDATION_MASK = 0;

    public FroggerEntityDataFallingRock(FroggerMapFile mapFile) {
        super(mapFile);
        for (int i = 0; i < this.targets.length; i++)
            this.targets[i] = new FallingRockTarget(this, i);
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
        editor.addUnsignedFixedShort("Initial Delay (secs)", this.delay, newDelay -> this.delay = newDelay, getGameInstance().getFPS())
                .setTooltip(FXUtils.createTooltip("Controls how long the boulder will wait at the starting position before leaving for the first target."));
        if (this.flags != 0)
            editor.addSignedIntegerField("Flags", this.flags, newFlags -> this.flags = newFlags);
        if (!getConfig().isAtOrBeforeBuild38() && this.sound != 0 && this.sound != -1)
            editor.addSignedIntegerField("Sound (Unused)", this.sound, newSound -> this.sound = newSound);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        super.setupEditor(editor, manager);

        // Allow changing the number of bounces before break.
        editor.addUnsignedByteField("Bounces Before Break", this.bounceCount, newBounceCount -> newBounceCount <= ROCK_TARGET_COUNT, newBounceCount -> {
            this.bounceCount = newBounceCount;
            manager.updateEditor();
        }).setTooltip(FXUtils.createTooltip("Controls how many bounces the boulder will make before it breaks/resets."));

        // Setup the editor for the enabled bounce targets.
        for (int i = 0; i < this.bounceCount; i++)
            this.targets[i].setupEditor(editor, manager.getController());
    }

    @Getter
    public static final class FallingRockTarget extends SCSharedGameData {
        private final FroggerEntityDataFallingRock parentData;
        private final int index;
        private final SVector target = new SVector(); // Target Position.
        @Setter private int time = 30; // Time to reach target.

        public FallingRockTarget(FroggerEntityDataFallingRock parentData, int index) {
            super(parentData.getGameInstance());
            this.parentData = parentData;
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
            grid.addFloatVector("Bounce Target #" + (this.index + 1), this.target, null, controller,
                    (targetPos, bits) -> this.parentData.selectNewPosition(controller, targetPos, bits));
            grid.addUnsignedFixedShort("Time to Target (secs)", this.time, newTime -> this.time = newTime, getGameInstance().getFPS())
                    .setTooltip(FXUtils.createTooltip("Controls how long it will take to reach the next boulder target from the moment Target #" + (this.index + 1) + " is reached."));
        }
    }
}