package net.highwayfrogs.editor.file.config.exe.general;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.config.data.MAPLevel;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Holds demo table entry data.
 * Created by Kneesnap on 11/21/2019.
 */
@Getter
@Setter
@AllArgsConstructor
public class DemoTableEntry {
    private MAPLevel level;
    private int demoResourceFile;
    private MAPLevel unlockLevel;
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
