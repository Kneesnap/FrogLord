package net.highwayfrogs.editor.file.map.entity.data.forest;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents the swaying branch entity data.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class SwayingBranchEntity extends MatrixData {
    private int swayAngle;
    private int swayDuration;
    private int onceOffDelay;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.swayAngle = reader.readUnsignedShortAsInt();
        this.swayDuration = reader.readUnsignedShortAsInt();
        if (!getConfig().isAtOrBeforeBuild11()) {
            this.onceOffDelay = reader.readUnsignedShortAsInt();
            reader.skipShort();
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.swayAngle);
        writer.writeUnsignedShort(this.swayDuration);
        if (!getConfig().isAtOrBeforeBuild11()) {
            writer.writeUnsignedShort(this.onceOffDelay);
            writer.writeUnsignedShort(0);
        }
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Sway Duration", getSwayDuration(), this::setSwayDuration, null);
        editor.addIntegerField("Sway Angle", getSwayAngle(), this::setSwayAngle, null);
        if (!getConfig().isAtOrBeforeBuild11())
            editor.addIntegerField("Once Off Delay", getOnceOffDelay(), this::setOnceOffDelay, null);
    }
}
