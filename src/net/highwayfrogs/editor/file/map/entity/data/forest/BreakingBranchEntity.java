package net.highwayfrogs.editor.file.map.entity.data.forest;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents "FOREST_BREAKING_BRANCH".
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class BreakingBranchEntity extends MatrixData {
    private int breakDelay;
    private int fallSpeed;

    public BreakingBranchEntity(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.breakDelay = reader.readUnsignedShortAsInt();
        this.fallSpeed = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.breakDelay);
        writer.writeUnsignedShort(this.fallSpeed);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Break Delay", getBreakDelay(), this::setBreakDelay, null);
        editor.addIntegerField("Fall Speed", getFallSpeed(), this::setFallSpeed, null);
    }
}