package net.highwayfrogs.editor.file.map.entity.data.swamp;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.map.manager.EntityManager;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityRat extends MatrixData {
    private short speed;
    private SVector startTarget = new SVector();
    private SVector startRunTarget = new SVector();
    private SVector endRunTarget = new SVector();
    private SVector endTarget = new SVector();

    public EntityRat(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.speed = reader.readShort();
        reader.skipShort();
        this.startTarget = SVector.readWithPadding(reader);
        this.startRunTarget = SVector.readWithPadding(reader);
        this.endRunTarget = SVector.readWithPadding(reader);
        this.endTarget = SVector.readWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.speed);
        writer.writeUnsignedShort(0);
        this.startTarget.saveWithPadding(writer);
        this.startRunTarget.saveWithPadding(writer);
        this.endRunTarget.saveWithPadding(writer);
        this.endTarget.saveWithPadding(writer);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addShortField("Speed", getSpeed(), this::setSpeed, null);
    }

    @Override
    public void addData(EntityManager manager, GUIEditorGrid editor) {
        super.addData(manager, editor);
        editor.addFloatSVector("Start Target", getStartTarget(), manager.getController());
        editor.addFloatSVector("Start Run Target", getStartRunTarget(), manager.getController());
        editor.addFloatSVector("End Run Target", getEndRunTarget(), manager.getController());
        editor.addFloatSVector("End Target", getEndTarget(), manager.getController());
    }
}