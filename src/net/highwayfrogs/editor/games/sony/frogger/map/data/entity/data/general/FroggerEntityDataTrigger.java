package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.general;

import javafx.scene.control.Alert.AlertType;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.Arrays;

/**
 * Represents a trigger entity 'ENTSTR_TRIGGER' in ENTLIB.H.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataTrigger extends FroggerEntityDataMatrix {
    private FroggerEntityTriggerType type = FroggerEntityTriggerType.BEGIN;
    private final short[] uniqueIds = new short[ENTITY_TYPE_TRIGGER_MAX_IDS];

    public static final int ENTITY_TYPE_TRIGGER_MAX_IDS = 10;

    public FroggerEntityDataTrigger(FroggerMapFile mapFile) {
        super(mapFile);
        Arrays.fill(this.uniqueIds, (short) -1);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = FroggerEntityTriggerType.values()[reader.readInt()];
        for (int i = 0; i < this.uniqueIds.length; i++)
            this.uniqueIds[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (int i = 0; i < this.uniqueIds.length; i++) {
            int id = this.uniqueIds[i];
            String errorMessage = getErrorMessageForEntityID(id);
            if (errorMessage != null) // These errors won't crash the game, so show a warning instead of throwing an exception.
                getGameInstance().showWarning(getLogger(), errorMessage);
        }

        writer.writeInt(this.type.ordinal());
        for (int i = 0; i < this.uniqueIds.length; i++)
            writer.writeShort(this.uniqueIds[i]);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addEnumSelector("Action", this.type, FroggerEntityTriggerType.values(), false, newType -> this.type = newType)
                .setTooltip(FXUtils.createTooltip("Controls what happens to the entities targetted by the entity when triggered."));

        for (int i = 0; i < this.uniqueIds.length; i++) {
            final int tempIndex = i;
            editor.addSignedShortField("Target Entity ID #" + (i + 1), this.uniqueIds[i], newId -> {
                String errorMessage = getErrorMessageForEntityID(newId);
                if (errorMessage != null) {
                    FXUtils.makePopUp(errorMessage, AlertType.ERROR);
                    return false;
                }

                return true;
            }, newVal -> this.uniqueIds[tempIndex] = newVal);
        }
    }

    private String getErrorMessageForEntityID(int entityId) {
        if (entityId == -1)
            return null; // -1 is OK.

        FroggerMapEntity entity = getMapFile().getEntityPacket().getEntityByUniqueId(entityId);
        if (entity == null)
            return "No target entity could be found with the entity ID: " + entityId + ".";

        return null;
    }

    /**
     * Registry of entity trigger types.
     */
    public enum FroggerEntityTriggerType {
        FREEZE, // Pauses/unpauses
        REVERSE, // Broken.
        FREEZE_UNUSED_DUPLICATE, // "START". Same as 'Freeze'.
        BEGIN,
    }
}