package net.highwayfrogs.editor.file;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a frogger key-map sequence that plays if you idle on the main menu for too long.
 * Created by Kneesnap on 8/14/2018.
 */
@Getter
public class DemoFile extends GameFile {
    private List<DemoAction> actions = new ArrayList<>();
    private int startX;
    private int startZ;
    private int startY;

    public static final int TYPE_ID = 6;
    private static final int FILE_SIZE = 714;

    @Override
    public void load(DataReader reader) {
        this.startX = reader.readInt();
        this.startZ = reader.readInt();
        this.startY = reader.readInt();

        while (reader.hasMore()) {
            byte actionId = reader.readByte();
            DemoAction action = null;

            for (DemoAction test : DemoAction.values()) {
                for (int check : test.getIds())
                    if (actionId == (byte) check)
                        action = test;
            }

            Utils.verify(action != null, "Unknown action for action id 0x%s.", Utils.toByteString(actionId));
            getActions().add(action);
            if (action == DemoAction.STOP)
                break;
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getStartX());
        writer.writeInt(getStartZ());
        writer.writeInt(getStartY());
        for (DemoAction action : getActions())
            writer.writeByte((byte) action.getIds()[0]);
        writer.jumpTo(FILE_SIZE); // Jump to end of file.
    }

    @Getter
    public enum DemoAction {
        UP("Move Up", 0, 4),
        RIGHT("Move Right", 1, 5),
        DOWN("Move Down", 2, 6),
        LEFT("Move Left", 3, 7),
        SUPER_HOP("Super Hop", 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x88),
        ROTATE_COUNTER_CLOCKWISE("Rotate Camera Counter-Clockwise", 0x10, 0x20, 0xB0),
        ROTATE_CLOCKWISE("Rotate Camera Clockwise", 0x30, 0x40, 0x50, 0xC0, 0xD0, 0xF0),
        SKIP("Do Nothing", 0x80),
        TONGUE("Tongue", 0x90),
        STOP("End Demo", 0xA0);

        private int[] ids;
        private String info;

        DemoAction(String display, int... ids) {
            this.info = display;
            this.ids = ids;
        }
    }
}
