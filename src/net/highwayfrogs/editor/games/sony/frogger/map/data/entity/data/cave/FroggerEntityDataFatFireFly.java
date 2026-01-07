package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerFlyScoreType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.IFroggerFlySpriteData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Holds data for the cave bug which completely lights up the cave, aka CAV_FAT_FIRE_FLY in ent_cav.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataFatFireFly extends FroggerEntityDataMatrix implements IFroggerFlySpriteData {
    private FroggerFlyScoreType flyType = FroggerFlyScoreType.SUPER_LIGHT; // Unused. Change has no effect.
    private final SVector target = new SVector(); // Unused, change has no effect. At one point this was the position to move the camera to upon eating.

    public FroggerEntityDataFatFireFly(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.flyType = FroggerFlyScoreType.values()[reader.readUnsignedShortAsInt()];
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.
        this.target.loadWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.flyType.ordinal());
        writer.writeNull(Constants.SHORT_SIZE); // Padding.
        this.target.saveWithPadding(writer);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        super.setupEditor(editor, manager);
        editor.addEnumSelector("Fly Score Type", this.flyType, FroggerFlyScoreType.values(), false, newType -> {
            this.flyType = newType;
            manager.updateEntityMesh(getParentEntity());
        }).setTooltip(FXUtils.createTooltip("This has never been tested as anything other than SUPER_LIGHT."));
        editor.addFloatSVector("Target (Unused)", this.target, manager.getController());
    }
}