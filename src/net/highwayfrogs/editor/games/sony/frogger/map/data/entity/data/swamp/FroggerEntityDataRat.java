package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the 'SWAMP_RAT' entity data definition in ent_swp.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataRat extends FroggerEntityDataMatrix {
    private short speed = 256;
    private final SVector startTarget = new SVector();
    private final SVector startRunTarget = new SVector();
    private final SVector endRunTarget = new SVector();
    private final SVector endTarget = new SVector();

    public FroggerEntityDataRat(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.speed = reader.readShort();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.
        this.startTarget.loadWithPadding(reader);
        this.startRunTarget.loadWithPadding(reader);
        this.endRunTarget.loadWithPadding(reader);
        this.endTarget.loadWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.speed);
        writer.writeNull(Constants.SHORT_SIZE); // Padding.
        this.startTarget.saveWithPadding(writer);
        this.startRunTarget.saveWithPadding(writer);
        this.endRunTarget.saveWithPadding(writer);
        this.endTarget.saveWithPadding(writer);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addFixedShort("Speed (World Units/sec)", this.speed, newSpeed -> this.speed = newSpeed, 256)
                .setTooltip(FXUtils.createTooltip("Controls how fast the rat moves"));
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        super.setupEditor(editor, manager);
        editor.addFloatVector("Start Target", this.startTarget, null, manager.getController(),
                (targetPos, bits) -> selectNewPosition(manager.getController(), targetPos, bits));
        editor.addFloatVector("Start Run Target", this.startRunTarget, null, manager.getController(),
                (targetPos, bits) -> selectNewPosition(manager.getController(), targetPos, bits));
        editor.addFloatVector("End Run Target", this.endRunTarget, null, manager.getController(),
                (targetPos, bits) -> selectNewPosition(manager.getController(), targetPos, bits));
        editor.addFloatVector("End Target", this.endTarget, null, manager.getController(),
                (targetPos, bits) -> selectNewPosition(manager.getController(), targetPos, bits));
    }
}