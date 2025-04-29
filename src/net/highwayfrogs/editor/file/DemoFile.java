package net.highwayfrogs.editor.file;

import javafx.scene.image.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.ui.DemoController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a frogger key-map sequence that plays if you idle on the main menu for too long.
 * Created by Kneesnap on 8/14/2018.
 */
@Getter
@Setter
public class DemoFile extends SCGameFile<FroggerGameInstance> {
    private DemoFrame[] frames = new DemoFrame[MAX_DEMO_FRAMES];
    private int frameCount;
    private int startX;
    private int startZ;

    private static final int MAX_DEMO_FRAMES = 30 * 60;
    private static final int FILE_SIZE = MAX_DEMO_FRAMES + (3 * Constants.INTEGER_SIZE);

    public DemoFile(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    @Override
    public void load(DataReader reader) {
        this.frameCount = reader.readInt();
        this.startX = reader.readInt();
        this.startZ = reader.readInt();
        for (int i = 0; i < MAX_DEMO_FRAMES && reader.hasMore(); i++)
            this.frames[i] = new DemoFrame(reader.readByte());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.frameCount);
        writer.writeInt(this.startX);
        writer.writeInt(this.startZ);
        for (DemoFrame frame : this.frames)
            writer.writeByte(frame.toByte());
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.GAME_CONTROLLER_32.getFxImage();
    }

    @Override
    public DemoController makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-demo", new DemoController(getGameInstance()), this);
    }

    @AllArgsConstructor
    public static final class DemoFrame {
        private byte frameByte;

        /**
         * Set the action state.
         * @param action   The action to set.
         * @param newState The state of the action.
         */
        public void setActionState(DemoAction action, boolean newState) {
            boolean oldState = getActionState(action);
            if (newState == oldState)
                return; // States match.

            if (oldState) {
                this.frameByte ^= action.getId();
            } else {
                this.frameByte |= action.getId();
            }
        }

        /**
         * Sets the base action.
         * @param action The base action to use.
         */
        public void setBaseAction(DemoAction action) {
            DemoAction oldAction = getBaseAction();
            if (oldAction != null) {
                if (oldAction.isBitWise()) {
                    setActionState(oldAction, false);
                } else {
                    this.frameByte -= oldAction.getId();
                }
            }

            this.frameByte += action.getId();
        }

        /**
         * Gets the base action.
         */
        public DemoAction getBaseAction() {
            for (DemoAction testAction : DemoAction.getNonAdditives())
                if (getActionState(testAction))
                    return testAction;
            return null;
        }

        /**
         * Gets the action state.
         * @param action The action to test.
         * @return isStateTrue.
         */
        public boolean getActionState(DemoAction action) {
            return action.test(this.frameByte);
        }

        /**
         * Get this action as a byte.
         * @return byte
         */
        public byte toByte() {
            return this.frameByte;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (DemoAction testAction : DemoAction.values())
                if ((testAction != DemoAction.UP || !getActionState(DemoAction.SKIP)) && getActionState(testAction))
                    sb.append((sb.length() > 0 ? ", " : "")).append(testAction.name());
            return sb.toString();
        }
    }

    @Getter
    @AllArgsConstructor
    public enum DemoAction {
        SKIP("Do Nothing", (byte) Constants.BIT_FLAG_7, false),
        UP("Move Up", (byte) 0x00, false),
        RIGHT("Move Right", (byte) 0x01, false),
        DOWN("Move Down", (byte) 0x02, false),
        LEFT("Move Left", (byte) 0x03, false),
        // Bit 2 is unused, it isn't croak, it's just plain unused.
        SUPER_HOP("Super Hop", (byte) Constants.BIT_FLAG_3, true),
        TONGUE("Tongue", (byte) Constants.BIT_FLAG_4, true),
        ROTATE_COUNTER_CLOCKWISE("Rotate Counter-Clockwise", (byte) Constants.BIT_FLAG_5, true),
        ROTATE_CLOCKWISE("Rotate Clockwise", (byte) Constants.BIT_FLAG_6, true);

        private final String info;
        private final byte id;
        private final boolean additive;
        private static DemoAction[] cachedAdditives;
        private static DemoAction[] cachedNonAdditives;

        /**
         * Test if a byte passes this action.
         * @param actionId The action to test.
         * @return testPass
         */
        public boolean test(byte actionId) {
            return isBitWise()
                    ? (actionId & getId()) == getId()
                    : (actionId & 0b11) == getId();
        }

        /**
         * Are operations performed with this value bit-wise?
         * @return isBitWise
         */
        public boolean isBitWise() {
            return isAdditive() || this == SKIP;
        }

        /**
         * Get additive actions.
         * @return additives
         */
        public static DemoAction[] getAdditives() {
            if (cachedAdditives == null) {
                List<DemoAction> actions = new ArrayList<>();
                for (DemoAction action : values())
                    if (action.isAdditive())
                        actions.add(action);
                cachedAdditives = actions.toArray(new DemoAction[0]);
            }

            return cachedAdditives;
        }

        /**
         * Get non additive actions.
         * @return additives
         */
        public static DemoAction[] getNonAdditives() {
            if (cachedNonAdditives == null) {
                List<DemoAction> actions = new ArrayList<>();
                for (DemoAction action : values())
                    if (!action.isAdditive())
                        actions.add(action);
                cachedNonAdditives = actions.toArray(new DemoAction[0]);
            }

            return cachedNonAdditives;
        }
    }
}