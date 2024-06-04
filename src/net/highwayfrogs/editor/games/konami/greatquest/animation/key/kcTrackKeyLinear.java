package net.highwayfrogs.editor.games.konami.greatquest.animation.key;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcControlType;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a linear track key.
 * Created by Kneesnap on 5/2/2024.
 */
@Getter
public class kcTrackKeyLinear extends kcTrackKey {
    private final kcVector4 vector = new kcVector4();
    // Rotation - Quaternion.
    // Position / Scale - Xyz Scalar

    public kcTrackKeyLinear(GreatQuestInstance instance, kcControlType controlType) {
        super(instance, controlType);
    }

    @Override
    protected void loadKeyData(DataReader reader, int dataEndIndex) {
        this.vector.load(reader);
    }

    @Override
    protected void saveKeyData(DataWriter writer, int dataEndIndex) {
        this.vector.save(writer);
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        builder.append(Utils.getSimpleName(this)).append("['").append(getControlType())
                .append("']: Timestamp=").append(getTimeStamp()).append(", ");
        this.vector.writeInfo(builder);
    }
}
