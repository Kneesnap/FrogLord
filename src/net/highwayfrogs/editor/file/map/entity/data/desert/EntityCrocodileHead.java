package net.highwayfrogs.editor.file.map.entity.data.desert;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityCrocodileHead extends MatrixData {
    private int riseHeight;
    private int riseSpeed;
    private int snapDelay;
    private int pauseDelay;
    private int snapOrNot;
    private int submergedDelay;

    public EntityCrocodileHead(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.riseHeight = reader.readUnsignedShortAsInt();
        this.riseSpeed = reader.readUnsignedShortAsInt();
        this.snapDelay = reader.readUnsignedShortAsInt();
        this.pauseDelay = reader.readUnsignedShortAsInt();
        this.snapOrNot = reader.readUnsignedShortAsInt();
        this.submergedDelay = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.riseHeight);
        writer.writeUnsignedShort(this.riseSpeed);
        writer.writeUnsignedShort(this.snapDelay);
        writer.writeUnsignedShort(this.pauseDelay);
        writer.writeUnsignedShort(this.snapOrNot);
        writer.writeUnsignedShort(this.submergedDelay);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Rise Height", getRiseHeight(), this::setRiseHeight, null);
        editor.addIntegerField("Rise Speed", getRiseSpeed(), this::setRiseSpeed, null);
        editor.addIntegerField("Snap Delay", getSnapDelay(), this::setSnapDelay, null);
        editor.addIntegerField("Pause Delay", getPauseDelay(), this::setPauseDelay, null);
        editor.addIntegerField("Should Snap", getSnapOrNot(), this::setSnapOrNot, null);
        editor.addIntegerField("Submerged Delay", getSubmergedDelay(), this::setSubmergedDelay, null);
    }
}