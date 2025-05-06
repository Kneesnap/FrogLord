package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the 'FOREST_HEDGEHOG' entity data definition in ent_for.h or 'sub_Hedgehog' in ENTITIES.TXT
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataHedgehog extends FroggerEntityDataPathInfo {
    private int runTime = 120;
    private int rollTime = 720;
    private int runSpeed = 16;
    private int rollSpeed = 32;

    public FroggerEntityDataHedgehog(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.runTime = reader.readUnsignedShortAsInt();
        this.rollTime = reader.readUnsignedShortAsInt();
        this.runSpeed = reader.readUnsignedShortAsInt();
        this.rollSpeed = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.runTime);
        writer.writeUnsignedShort(this.rollTime);
        writer.writeUnsignedShort(this.runSpeed);
        writer.writeUnsignedShort(this.rollSpeed);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        // Yes this looks weird, but I verified it's legit.
        editor.addUnsignedFixedShort("Run Time (sec)", this.runTime, newRunTime -> this.runTime = newRunTime, getGameInstance().getFPS())
                .setTooltip(FXUtils.createTooltip("How long the hedgehog runs before entering into a roll.\nIMPORTANT: Hedgehogs are set to running when they restart at the beginning of a path."));
        editor.addUnsignedFixedShort("Roll Time (sec)", this.rollTime, newRollTime -> this.rollTime = newRollTime, getGameInstance().getFPS())
                .setTooltip(FXUtils.createTooltip("How long the hedgehog rolls before it enters into a run.\nIMPORTANT: Hedgehogs are set to running when they restart at the beginning of a path."));
        editor.addUnsignedFixedShort("Run Speed (grid sq/sec)", this.runSpeed, newRunSpeed -> this.runSpeed = newRunSpeed, 16)
                .setTooltip(FXUtils.createTooltip("This is the path speed used while the hedgehog is running."));
        editor.addUnsignedFixedShort("Roll Speed (grid sq/sec)", this.rollSpeed, newRollSpeed -> this.rollSpeed = newRollSpeed, 16)
                .setTooltip(FXUtils.createTooltip("This is the path speed used while the hedgehog is rolling."));
    }
}