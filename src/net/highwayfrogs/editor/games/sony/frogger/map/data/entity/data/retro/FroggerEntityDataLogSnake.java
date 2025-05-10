package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.retro;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the 'ORG_LOG_SNAKE_DATA'
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataLogSnake extends FroggerEntityDataMatrix {
    private short logId;
    private int speed = 2184;

    public FroggerEntityDataLogSnake(FroggerMapFile mapFile) {
        super(mapFile);
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
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addSignedShortField("Log ID", this.logId, newLogId -> {
            FroggerMapEntity foundEntity = getMapFile().getEntityPacket().getEntityByUniqueId(newLogId);
            if (foundEntity == null)
                throw new RuntimeException("No entity exists in this map with the unique ID: " + newLogId);
            if (!"MOVING".equals(foundEntity.getTypeName()))
                throw new RuntimeException("Cannot attach log snake entity to a(n) " + foundEntity.getTypeName() + ".");

            return true;
        }, newLogId -> this.logId = newLogId);
        editor.addUnsignedFixedShort("Speed", this.speed, newSpeed -> this.speed = newSpeed, 2184.5)
                .setTooltip(FXUtils.createTooltip("How fast along the log the snake moves. Seems to be roughly measured in grid squares per second."));
    }
}