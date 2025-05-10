package net.highwayfrogs.editor.file.config.exe.general;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Holds demo table entry data.
 * Created by Kneesnap on 11/21/2019.
 */
@Getter
@Setter
@AllArgsConstructor
public class DemoTableEntry {
    private FroggerMapLevelID level;
    private int demoResourceFile;
    private FroggerMapLevelID unlockLevel;
    private boolean validData;

    public static final int SKIP_INT = 0xFFFFFFFE; // -2

    public void save(DataWriter writer) {
        if (isValidData()) {
            writer.writeInt(this.level.ordinal());
            writer.writeInt(this.demoResourceFile);
            writer.writeInt(this.unlockLevel.ordinal());
        } else {
            writer.writeInt(SKIP_INT);
            writer.writeInt(SKIP_INT);
            writer.writeInt(SKIP_INT);
        }
    }
}
