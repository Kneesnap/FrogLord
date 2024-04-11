package net.highwayfrogs.editor.games.greatquest.script.interim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Implementation of the 'kcScriptTOC' class.
 * Created by Kneesnap on 8/23/2023.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class kcScriptTOC extends GameObject {
    private long causeTypes;
    private long causeStartIndex;
    private long causeCount;
    private long effectCount;

    @Override
    public void load(DataReader reader) {
        this.causeTypes = reader.readUnsignedIntAsLong();
        this.causeStartIndex = reader.readUnsignedIntAsLong();
        this.causeCount = reader.readUnsignedIntAsLong();
        this.effectCount = reader.readUnsignedIntAsLong();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedInt(this.causeTypes);
        writer.writeUnsignedInt(this.causeStartIndex);
        writer.writeUnsignedInt(this.causeCount);
        writer.writeUnsignedInt(this.effectCount);
    }
}