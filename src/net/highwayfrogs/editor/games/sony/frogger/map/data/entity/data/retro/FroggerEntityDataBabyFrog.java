package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.retro;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents "ORG_BABY_FROG_DATA" in ent_org.h, which is the data for the pink frog you can pickup on the retro logs.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityDataBabyFrog extends FroggerEntityDataMatrix {
    private short logId; // The id of the log this frog will stand on.
    private short awardedPoints = 1000; // The points awarded when collected.

    public FroggerEntityDataBabyFrog(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.logId = reader.readShort();
        this.awardedPoints = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.logId);
        writer.writeShort(this.awardedPoints);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addSignedShortField("Log ID", this.logId, newLogId -> {
            FroggerMapEntity foundEntity = getMapFile().getEntityPacket().getEntityByUniqueId(newLogId);
            if (foundEntity == null)
                throw new RuntimeException("No entity exists in this map with the unique ID: " + newLogId);
            if (!"MOVING".equals(foundEntity.getTypeName()))
                throw new RuntimeException("Cannot attach baby frog entity to a(n) " + foundEntity.getTypeName() + ".");

            return true;
        }, newLogId -> this.logId = newLogId).setTooltip(FXUtils.createTooltip("The unique ID of the entity which the baby frog should ride."));
        editor.addSignedShortField("Points (Unused?)", this.awardedPoints, newAwardedPoints -> this.awardedPoints = newAwardedPoints)
                .setDisable(true);
    }
}