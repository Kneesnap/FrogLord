package net.highwayfrogs.editor.file.map.entity.data.retro;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntitySnake extends MatrixData {
    private short logId;
    private int speed;

    public EntitySnake(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.logId = reader.readShort();
        this.speed = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.logId);
        writer.writeUnsignedShort(this.speed);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addShortField("Log ID", getLogId(), this::setLogId, null);
        editor.addIntegerField("Speed", getSpeed(), this::setSpeed, null);
    }
}