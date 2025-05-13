package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.volcano;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
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
 * Represents the "VOL_COLOUR_TRIGGER" struct defined in ent_vol.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataColorTrigger extends FroggerEntityDataMatrix {
    private FroggerEntityColorTriggerType type = FroggerEntityColorTriggerType.FREEZE;
    private VolcanoTriggerColor color = VolcanoTriggerColor.RED;
    private final short[] uniqueIds = new short[COLOR_TRIGGER_MAX_IDS];

    private static final int COLOR_TRIGGER_MAX_IDS = 10;

    public FroggerEntityDataColorTrigger(FroggerMapFile mapFile) {
        super(mapFile);

        // Any buttons with 0s will crash the game when stepped on, because no entity with id 0 exists
        // So initialize to -1 instead, the value for no unique id
        Arrays.fill(this.uniqueIds, (short) -1);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = FroggerEntityColorTriggerType.values()[reader.readUnsignedShortAsInt()];
        this.color = VolcanoTriggerColor.values()[reader.readUnsignedShortAsInt()];
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

        writer.writeUnsignedShort((short) this.type.ordinal());
        writer.writeUnsignedShort((short) this.color.ordinal());
        for (int i = 0; i < this.uniqueIds.length; i++)
            writer.writeShort(this.uniqueIds[i]);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addEnumSelector("Action", this.type, FroggerEntityColorTriggerType.values(), false, newType -> this.type = newType)
                .setTooltip(FXUtils.createTooltip("Controls what happens to the entities targetted by the switch when pressed."));

        ComboBox<?> colorField = editor.addEnumSelector("Color (PSX Only)", this.color, VolcanoTriggerColor.values(), false, newColor -> this.color = newColor);
        colorField.setTooltip(FXUtils.createTooltip("Sets the color to use for the switch.\nThis feature doesn't appear to do anything on the PC version. (PSX Untested)"));
        if (getGameInstance().isPC())
            colorField.setDisable(true);

        for (int i = 0; i < this.uniqueIds.length; i++) {
            final int tempIndex = i;
            editor.addSignedShortField("Trigger #" + (i + 1), this.uniqueIds[i], newEntityId -> {
                String errorMessage = getErrorMessageForEntityID(newEntityId);
                if (errorMessage != null) {
                    FXUtils.makePopUp(errorMessage, AlertType.ERROR);
                    return false;
                }

                return true;
            }, newEntityId -> this.uniqueIds[tempIndex] = newEntityId);
        }
    }

    private String getErrorMessageForEntityID(int entityId) {
        if (entityId == -1)
            return null; // -1 is OK.

        FroggerMapEntity entity = getMapFile().getEntityPacket().getEntityByUniqueId(entityId);
        if (entity == null)
            return "No target entity could be found with the entity ID: " + entityId + ".";

        // If the entity is not VOL_PLATFORM2, the entity data won't be right, meaning the entity will be LIKELY (but not guaranteed) to start in a moving state, regardless of their 'No Movement' flag.
        return null;
    }

    public enum VolcanoTriggerColor {
        RED, BLUE, CYAN, GREEN, ORANGE, PINK, PURPLE, RED_ALTERNATE, WHITE
    }

    public enum FroggerEntityColorTriggerType {
        FREEZE, // Freezes/unfreezes the entity.
        REVERSE, // Reverses the path runner.
        FREEZE_UNUSED_DUPLICATE, // "START". Same as 'Freeze'.
    }
}