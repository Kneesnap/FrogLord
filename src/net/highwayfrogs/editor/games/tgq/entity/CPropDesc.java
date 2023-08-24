package net.highwayfrogs.editor.games.tgq.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.kcClassID;

/**
 * Represents the 'CPropDesc' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class CPropDesc extends kcActorBaseDesc {
    private int mode;
    private int event;
    private final int[] padProp = new int[64];

    @Override
    protected int getTargetClassID() {
        return kcClassID.PROP.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.mode = reader.readInt();
        this.event = reader.readInt();
        for (int i = 0; i < this.padProp.length; i++)
            this.padProp[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.mode);
        writer.writeInt(this.event);
        for (int i = 0; i < this.padProp.length; i++)
            writer.writeInt(this.padProp[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Mode: ").append(this.mode).append(Constants.NEWLINE);
        builder.append(padding).append("Event: ").append(this.event).append(Constants.NEWLINE);
    }
}
