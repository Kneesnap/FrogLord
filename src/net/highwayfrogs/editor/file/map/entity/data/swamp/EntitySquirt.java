package net.highwayfrogs.editor.file.map.entity.data.swamp;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.ui.mapeditor.EntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntitySquirt extends MatrixData {
    private short timeDelay;
    private short dropTime;
    private SVector target = new SVector();

    public EntitySquirt(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.timeDelay = reader.readShort();
        this.dropTime = reader.readShort();
        this.target = SVector.readWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.timeDelay);
        writer.writeShort(this.dropTime);
        this.target.saveWithPadding(writer);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addShortField("Time Delay", getTimeDelay(), this::setTimeDelay, null);
        editor.addShortField("Drop Time", getDropTime(), this::setDropTime, null);
    }

    @Override
    public void addData(EntityManager manager, GUIEditorGrid editor) {
        super.addData(manager, editor);
        editor.addFloatSVector("Target", getTarget(), manager.getController());
    }
}