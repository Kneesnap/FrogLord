package net.highwayfrogs.editor.games.greatquest.generic;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a string.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcCResourceString extends GameObject {
    private String value;

    public kcCResourceString() {
        this(null);
    }

    public kcCResourceString(String value) {
        this.value = value;
    }

    @Override
    public void load(DataReader reader) {
        this.value = reader.readNullTerminatedString();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeTerminatorString(this.value);
    }
}