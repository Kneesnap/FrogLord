package net.highwayfrogs.editor.file.map.entity.data.cave;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityRaceSnail extends PathData {
    private int forwardDistance;
    private int backwardDistance;

    public EntityRaceSnail(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.forwardDistance = reader.readUnsignedShortAsInt();
        this.backwardDistance = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.forwardDistance);
        writer.writeUnsignedShort(this.backwardDistance);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Forward Distance", getForwardDistance(), this::setForwardDistance, null);
        editor.addIntegerField("Backward Distance", getBackwardDistance(), this::setBackwardDistance, null);
    }
}