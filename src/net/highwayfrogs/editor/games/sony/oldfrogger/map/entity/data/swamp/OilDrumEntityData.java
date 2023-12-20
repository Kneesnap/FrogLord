package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp.OilDrumEntityData.OilDrumDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to an oil drum entity.
 * Created by Kneesnap on 12/17/2023.
 */
public class OilDrumEntityData extends PathEntityData<OilDrumDifficultyData> {
    public OilDrumEntityData(OldFroggerMapEntity entity) {
        super(entity, OilDrumDifficultyData::new);
    }

    @Getter
    public static class OilDrumDifficultyData extends OldFroggerDifficultyData {
        private int splineSpeed = 2184;

        public OilDrumDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.splineSpeed = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.splineSpeed);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Spline Speed", this.splineSpeed, newValue -> this.splineSpeed = newValue, 2184);
        }
    }
}