package net.highwayfrogs.editor.file.map.entity.data.cave;

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
public class EntityFrogLight extends MatrixData {
    private int minRadius;
    private int maxRadius;
    private int dieSpeed;
    private int count;
    private int setup;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.minRadius = reader.readInt();
        this.maxRadius = reader.readInt();
        this.dieSpeed = reader.readInt();
        this.count = reader.readInt();
        this.setup = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.minRadius);
        writer.writeInt(this.maxRadius);
        writer.writeInt(this.dieSpeed);
        writer.writeInt(this.count);
        writer.writeInt(this.setup);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Min Radius", getMinRadius(), this::setMinRadius, null);
        editor.addIntegerField("Max Radius", getMaxRadius(), this::setMaxRadius, null);
        editor.addIntegerField("Die Speed", getDieSpeed(), this::setDieSpeed, null);
        editor.addIntegerField("Count", getCount(), this::setCount, null);
        editor.addIntegerField("Setup", getSetup(), this::setSetup, null);

    }
}
