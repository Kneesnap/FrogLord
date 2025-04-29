package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents 'FOREST_BREAKING_BRANCH' as defined in ent_for.h, as well as for_breakingbranch in ENTITIES.TXT
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataBreakingBranch extends FroggerEntityDataMatrix {
    private int breakDelay = 150; // 5.0
    private int fallSpeed = 21845; // 10.0

    public FroggerEntityDataBreakingBranch(FroggerMapFile mapFile) {
        super(mapFile);
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
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addUnsignedFixedShort("Break Delay (secs)", this.breakDelay, newBreakDelay -> this.breakDelay = newBreakDelay, 30, 0, 3000);
        editor.addUnsignedFixedShort("Fall Speed (grid/sec)", this.fallSpeed, newFallSpeed -> this.fallSpeed = newFallSpeed, 2184);
    }
}