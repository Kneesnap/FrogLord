package net.highwayfrogs.editor.file.map.entity.data.forest;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

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

    public SwayingBranchEntity(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.swayAngle = reader.readUnsignedShortAsInt();
        this.swayDuration = reader.readUnsignedShortAsInt();
        if (!getConfig().isAtOrBeforeBuild11() && !getConfig().isWindowsBeta()) {
            this.onceOffDelay = reader.readUnsignedShortAsInt();
            reader.skipShort();
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.swayAngle);
        writer.writeUnsignedShort(this.swayDuration);
        if (!getConfig().isAtOrBeforeBuild11() && !getConfig().isWindowsBeta()) {
            writer.writeUnsignedShort(this.onceOffDelay);
            writer.writeUnsignedShort(0);
        }
    }
}