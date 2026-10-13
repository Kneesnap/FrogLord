package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerFlyScoreType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.IFroggerFlySpriteData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Includes the identifier which identifies a fly as 'GEN_BONUS_FLY' from ent_gen.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataBonusFly extends FroggerEntityDataMatrix implements IFroggerFlySpriteData {
    private int flyTypeId = FroggerFlyScoreType.values()[0].ordinal();

    public FroggerEntityDataBonusFly(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.flyTypeId = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.flyTypeId);
    }

    @Override
    public FroggerFlyScoreType getFlyType() {
        return (this.flyTypeId >= 0 && this.flyTypeId < FroggerFlyScoreType.values().length)
                ? FroggerFlyScoreType.values()[this.flyTypeId] : null;
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        super.setupEditor(editor, manager);

        FroggerFlyScoreType flyType = getFlyType();
        if (flyType != null) {
            editor.addEnumSelector("Fly Score Type", flyType, FroggerFlyScoreType.values(), false, newType -> {
                this.flyTypeId = newType.ordinal();
                if (manager != null)
                    manager.updateEntityMesh(getParentEntity());
            });
            editor.addBoldLabel("SCORE_X:");
            editor.addNormalLabel("Adds X amount to the player's score.");
            editor.addBoldLabel("LIGHT_BOOST:");
            editor.addNormalLabel("Provides a small boost to the player's light.");
            editor.addNormalLabel("Only useful in Cave levels.");
            editor.addBoldLabel("SUPER_LIGHT:");
            editor.addNormalLabel("Provides a huge boost to the player's light");
            editor.addNormalLabel("Only useful in Cave levels.");
            editor.addNormalLabel("For unique flying pattern and sound effects,");
            editor.addNormalLabel("use CAV_FAT_FIRE_FLY instead.");
            editor.addBoldLabel("TIME_MIN/MED/MAX:");
            editor.addNormalLabel("Adds 2/5/10 seconds to the timer.");
            editor.addNormalLabel("Caps at 75 seconds on PSX, and 99 on PC.");
            editor.addNormalLabel("Hardcoded to respawn every new life.");
            editor.addBoldLabel("REDUCE_SCORE_100:");
            editor.addNormalLabel("Subtracts *500* points from the player's score.");
            editor.addNormalLabel("Attempting to go below 0 will briefly glitch");
            editor.addNormalLabel("the score, awarding the next 10,000 point 1UP.");
            editor.addBoldLabel("FAST_TIMER_SPEED:");
            editor.addNormalLabel("Causes the level timer to decrease much");
            editor.addNormalLabel("faster for a short duration.");
            editor.addNormalLabel("Loses roughly 8 seconds of time.");
            editor.addBoldLabel("ADD_EXTRA_LIFE:");
            editor.addNormalLabel("Adds 1 life to the player's total.");
            editor.addNormalLabel("Cannot exceed 10 lives.");
            editor.addBoldLabel("FROG_SUPER_TONGUE:");
            editor.addNormalLabel("Doubles the range of the player's tongue.");
            editor.addNormalLabel("Lasts for 15 seconds.");
            editor.addBoldLabel("FROG_QUICK_JUMP:");
            editor.addNormalLabel("Doubles the player's normal jump speed.");
            editor.addNormalLabel("Lasts for 8 seconds.");
            editor.addBoldLabel("FROG_AUTO_HOP:");
            editor.addNormalLabel("Allows directions to be held down for");
            editor.addNormalLabel("continuous movement. Lasts for 10 seconds.");
            editor.addNormalLabel("Points will not be gained for normal");
            editor.addNormalLabel("hops while active.");
        } else {
            editor.addSignedIntegerField("Fly Score Type ID", this.flyTypeId, newTypeId -> {
                this.flyTypeId = newTypeId;
                if (manager != null)
                    manager.updateEntityMesh(getParentEntity());
            });
        }
    }
}