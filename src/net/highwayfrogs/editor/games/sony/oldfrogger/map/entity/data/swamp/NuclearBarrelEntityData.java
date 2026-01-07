package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp.NuclearBarrelEntityData.NuclearBarrelDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Nuclear barrel entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class NuclearBarrelEntityData extends PathEntityData<NuclearBarrelDifficultyData> {
    public NuclearBarrelEntityData(OldFroggerMapEntity entity) {
        super(entity, NuclearBarrelDifficultyData::new);
    }

    @Getter
    public static class NuclearBarrelDifficultyData extends OldFroggerDifficultyData {
        private final SVector target = new SVector();
        private int splineSpeed = 2184;

        public NuclearBarrelDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.target.loadWithPadding(reader);
            this.splineSpeed = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            this.target.saveWithPadding(writer);
            writer.writeUnsignedShort(this.splineSpeed);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFloatSVector("Target", this.target, manager.getController());
            editor.addUnsignedFixedShort("Spline Speed", this.splineSpeed, newValue -> this.splineSpeed = newValue, 2184);
        }
    }
}