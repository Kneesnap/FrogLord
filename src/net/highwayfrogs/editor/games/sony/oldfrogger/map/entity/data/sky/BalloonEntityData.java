package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky.BalloonEntityData.BalloonDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.path.OldFroggerPathData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a rising balloon entity.
 * Created by Kneesnap on 12/18/2023.
 */
@Getter
public class BalloonEntityData extends PathEntityData<BalloonDifficultyData> {
    private final OldFroggerPathData secondPath; // Seems to contain the same path data as the first path.

    public BalloonEntityData(OldFroggerMapEntity entity) {
        super(entity, BalloonDifficultyData::new);
        this.secondPath = new OldFroggerPathData(entity.getGameInstance());
    }

    @Override
    public void loadMainEntityData(DataReader reader) {
        super.loadMainEntityData(reader);
        this.secondPath.load(reader);
    }

    @Override
    public void saveMainEntityData(DataWriter writer) {
        super.saveMainEntityData(writer);
        this.secondPath.save(writer);
    }

    @Override
    public void setupMainEntityDataEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
        super.setupMainEntityDataEditor(manager, editor);

        editor.addBoldLabel("Second Path:");
        this.secondPath.setupEditor(manager, getEntity(), editor);
    }

    @Getter
    public static class BalloonDifficultyData extends OldFroggerDifficultyData {
        private short fallSpeed = 2184;
        private short speed = 2184;
        private int splineDelay = 30;

        public BalloonDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.fallSpeed = reader.readShort();
            this.speed = reader.readShort();
            this.splineDelay = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.fallSpeed);
            writer.writeShort(this.speed);
            writer.writeUnsignedShort(this.splineDelay);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedShort("Fall Speed", this.fallSpeed, newValue -> this.fallSpeed = newValue, 2184, 0, Short.MAX_VALUE);
            editor.addFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184, 0, Short.MAX_VALUE);
            editor.addUnsignedFixedShort("Spline Delay", this.splineDelay, newValue -> this.splineDelay = newValue, 30, 0, 1000);
        }
    }
}