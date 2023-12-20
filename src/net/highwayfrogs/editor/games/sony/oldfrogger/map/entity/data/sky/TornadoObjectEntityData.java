package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky.TornadoObjectEntityData.TornadoObjectDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * This represents data for an entity considered to be part of a tornado.
 * Created by Kneesnap on 12/18/2023.
 */
@Getter
public class TornadoObjectEntityData extends PathEntityData<TornadoObjectDifficultyData> {
    public TornadoObjectEntityData(OldFroggerMapEntity entity) {
        super(entity, TornadoObjectDifficultyData::new);
    }

    @Getter
    public static class TornadoObjectDifficultyData extends OldFroggerDifficultyData {
        private int maxAvX = 8947848;
        private int maxAvY = 8947848;
        private int maxAvZ = 8947848;
        private int safeStartTime = 150;
        private int safeTime = 150;
        private int speed = 2184;
        private int splineDelay;

        public TornadoObjectDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.maxAvX = reader.readInt();
            this.maxAvY = reader.readInt();
            this.maxAvZ = reader.readInt();
            this.safeStartTime = reader.readUnsignedShortAsInt();
            this.safeTime = reader.readUnsignedShortAsInt();
            this.speed = reader.readUnsignedShortAsInt();
            this.splineDelay = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.maxAvX);
            writer.writeInt(this.maxAvY);
            writer.writeInt(this.maxAvZ);
            writer.writeUnsignedShort(this.safeStartTime);
            writer.writeUnsignedShort(this.safeTime);
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(this.splineDelay);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedInt("Max AV (X)", this.maxAvX, newValue -> this.maxAvX = newValue, 8947848, 0, 178956960);
            editor.addFixedInt("Max AV (Y)", this.maxAvY, newValue -> this.maxAvY = newValue, 8947848, 0, 178956960);
            editor.addFixedInt("Max AV (Z)", this.maxAvZ, newValue -> this.maxAvZ = newValue, 8947848, 0, 178956960);
            editor.addUnsignedFixedShort("Safe Start Time", this.safeStartTime, newValue -> this.safeStartTime = newValue, 30, 0, 1000);
            editor.addUnsignedFixedShort("Safe Time", this.safeTime, newValue -> this.safeTime = newValue, 30, 0, 1000);
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
            editor.addUnsignedFixedShort("Spline Delay", this.splineDelay, newValue -> this.splineDelay = newValue, 30, 0, 1000);
        }
    }
}