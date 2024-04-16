package net.highwayfrogs.editor.games.konami.greatquest.generic;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;

/**
 * Represents a string.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcCResourceString extends GameData<GreatQuestInstance> {
    private String value;

    public kcCResourceString(GreatQuestInstance instance) {
        this(instance, null);
    }

    public kcCResourceString(GreatQuestInstance instance, String value) {
        super(instance);
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