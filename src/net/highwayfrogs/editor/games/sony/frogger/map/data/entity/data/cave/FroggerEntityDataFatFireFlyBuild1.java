package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * An earlier version of the fat fire fly bug. Seems to be from before the camera movement from eating the bug.
 * Represents CAV_FAT_FIRE_FLY in ent_cav.h.
 * Created by Kneesnap on 1/10/2023.
 */
@Getter
public class FroggerEntityDataFatFireFlyBuild1 extends FroggerEntityDataMatrix {
    private final SVector target = new SVector(); // Appears to be the position which gets shown when eating the bug.

    public FroggerEntityDataFatFireFlyBuild1(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.target.loadWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        this.target.saveWithPadding(writer);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        super.setupEditor(editor, manager);
        editor.addFloatSVector("Target", this.target, manager.getController());
    }
}