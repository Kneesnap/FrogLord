package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.retro;

import javafx.scene.control.Alert.AlertType;
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

        String errorMessage = getErrorMessageForEntityId(this.logId);
        if (errorMessage != null)
            throw new RuntimeException("A snake entity in " + getMapFile().getFileDisplayName() + " was not riding a valid entity!\n" + errorMessage);

        writer.writeShort(this.logId);
        writer.writeUnsignedShort(this.speed);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addSignedShortField("Log ID", this.logId, newLogId -> {
            String errorMessage = getErrorMessageForEntityId(newLogId);
            if (errorMessage != null) {
                FXUtils.makePopUp(errorMessage, AlertType.WARNING);
                return false;
            }

            return true;
        }, newLogId -> this.logId = newLogId).setTooltip(FXUtils.createTooltip("The unique ID of the entity which the snake should ride."));
        editor.addUnsignedFixedShort("Speed", this.speed, newSpeed -> this.speed = newSpeed, 2184.5)
                .setTooltip(FXUtils.createTooltip("How fast along the log the snake moves. Seems to be roughly measured in grid squares per second."));
    }

    private String getErrorMessageForEntityId(int logEntityId) {
        FroggerMapEntity foundEntity = getMapFile().getEntityPacket().getEntityByUniqueId(logEntityId);
        if (foundEntity == null)
            return "No ridable entity (for the snake) was found with the unique ID: " + logEntityId + ".";
        if (!"MOVING".equals(foundEntity.getTypeName()) && foundEntity.getPathInfo() == null) // The snake entity specifically uses path data.
            return "Cannot attach snake entity to a(n) " + foundEntity.getTypeName() + " entity, only path followers are allowed.";
        return null;
    }
}