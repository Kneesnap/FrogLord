package net.highwayfrogs.editor.games.konami.greatquest.animation.key;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcControlType;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a dummy/unimplemented kcTrackKey.
 * Created by Kneesnap on 5/2/2024.
 */
@Getter
public class kcTrackKeyDummy extends kcTrackKey {
    private byte[] rawBytes;

    public kcTrackKeyDummy(GreatQuestInstance instance, kcControlType controlType) {
        super(instance, controlType);
    }

    @Override
    protected void loadKeyData(DataReader reader, int endIndex) {
        this.rawBytes = reader.readBytes(endIndex - reader.getIndex());
    }

    @Override
    protected void saveKeyData(DataWriter writer, int endIndex) {
        int neededBytes = endIndex - writer.getIndex();

        if (this.rawBytes != null) {
            if (this.rawBytes.length != neededBytes)
                throw new RuntimeException("The raw byte array had " + this.rawBytes.length + " bytes, but we are expected to write " + neededBytes + " bytes.");

            writer.writeBytes(this.rawBytes);
        } else {
            writer.writeNull(neededBytes);
        }
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        builder.append(Utils.getSimpleName(this)).append("{Type=").append(getControlType())
                .append(",Timestamp=").append(getTimeStamp())
                .append(",RawBytes=").append(this.rawBytes != null ? this.rawBytes.length : "None").append("}");
    }
}
