package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerEntityTriggerType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

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
        writer.writeInt(this.type.ordinal());
        for (int i = 0; i < this.uniqueIds.length; i++)
            writer.writeShort(this.uniqueIds[i]);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addEnumSelector("Trigger Type", this.type, FroggerEntityTriggerType.values(), false, newType -> this.type = newType);
        for (int i = 0; i < this.uniqueIds.length; i++) {
            final int tempIndex = i;
            editor.addSignedShortField("Entity #" + (i + 1), this.uniqueIds[i], newVal -> this.uniqueIds[tempIndex] = newVal);
        }
    }
}