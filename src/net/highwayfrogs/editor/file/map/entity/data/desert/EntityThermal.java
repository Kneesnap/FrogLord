package net.highwayfrogs.editor.file.map.entity.data.desert;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.EntityData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityThermal extends EntityData {
    private int rotateTime;

    public EntityThermal(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.rotateTime = reader.readUnsignedShortAsInt();
        reader.skipShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.rotateTime);
        writer.writeUnsignedShort(0);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        editor.addIntegerField("Rotate Time", getRotateTime(), this::setRotateTime, null);
    }
}