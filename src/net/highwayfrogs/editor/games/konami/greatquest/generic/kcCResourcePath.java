package net.highwayfrogs.editor.games.konami.greatquest.generic;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;

/**
 * Represents a resource path.
 * This appears unused by the game, but perhaps it was used by an editor.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcCResourcePath extends GameData<GreatQuestInstance> {
    private static final int PATH_LENGTH = 260;
    private int fileHash; // The hash to the chunk which the path is included for.
    private String filePath;

    public kcCResourcePath(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.fileHash = reader.readInt();
        this.filePath = reader.readTerminatedStringOfLength(PATH_LENGTH);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.fileHash);
        writer.writeTerminatedStringOfLength(this.filePath, PATH_LENGTH); // MTF Future Note: Terminator 0x00, Padding: 0xCC
    }
}