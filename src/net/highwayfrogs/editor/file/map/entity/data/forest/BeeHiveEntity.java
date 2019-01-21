package net.highwayfrogs.editor.file.map.entity.data.forest;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class BeeHiveEntity extends MatrixData {
    private int releaseDistance;
    private int swarmSpeed;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.releaseDistance = reader.readInt();
        this.swarmSpeed = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.releaseDistance);
        writer.writeInt(this.swarmSpeed);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Release Distance", getReleaseDistance(), this::setReleaseDistance, null);
        editor.addIntegerField("Swarm Speed", getSwarmSpeed(), this::setSwarmSpeed, null);
    }
}
