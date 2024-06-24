package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.volcano;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerEntityTriggerType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents the "VOL_COLOUR_TRIGGER" struct defined in ent_vol.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataColorTrigger extends FroggerEntityDataMatrix {
    private FroggerEntityTriggerType type = FroggerEntityTriggerType.BEGIN;
    private VolcanoTriggerColor color = VolcanoTriggerColor.RED;
    private final short[] uniqueIds = new short[COLOR_TRIGGER_MAX_IDS];

    private static final int COLOR_TRIGGER_MAX_IDS = 10;

    public FroggerEntityDataColorTrigger(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = FroggerEntityTriggerType.values()[reader.readUnsignedShortAsInt()];
        this.color = VolcanoTriggerColor.values()[reader.readUnsignedShortAsInt()];
        for (int i = 0; i < this.uniqueIds.length; i++)
            this.uniqueIds[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort((short) this.type.ordinal());
        writer.writeUnsignedShort((short) this.color.ordinal());
        for (int i = 0; i < this.uniqueIds.length; i++)
            writer.writeShort(this.uniqueIds[i]);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addEnumSelector("Trigger Type", this.type, FroggerEntityTriggerType.values(), false, newType -> this.type = newType);
        editor.addEnumSelector("Color", this.color, VolcanoTriggerColor.values(), false, newColor -> this.color = newColor);
        for (int i = 0; i < this.uniqueIds.length; i++) {
            final int tempI = i;
            editor.addSignedShortField("Trigger #" + (i + 1), this.uniqueIds[i], newVal -> this.uniqueIds[tempI] = newVal);
        }
    }

    public enum VolcanoTriggerColor {
        RED, BLUE, CYAN, GREEN, ORANGE, PINK, PURPLE, RED_ALTERNATE, WHITE
    }
}