package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;

/**
 * Represents the 'CPropDesc' struct.
 * Loaded by CProp::Init.
 * Props have gravity disabled.
 * TODO: This seems to be what rotates collisions differently. Eg: If it is this or extends this, I think this is what causes the rotations to be unexpected. Investigate further.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class CPropDesc extends kcActorBaseDesc {
    private int mode;
    private int event;
    private static final int PADDING_VALUES = 64;

    public CPropDesc(@NonNull kcCResourceGeneric resource) {
        super(resource);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.PROP.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.mode = reader.readInt();
        this.event = reader.readInt();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.mode);
        writer.writeInt(this.event);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Mode: ").append(this.mode).append(Constants.NEWLINE);
        builder.append(padding).append("Event: ").append(this.event).append(Constants.NEWLINE);
    }
}