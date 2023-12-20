package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave.CaveLightEntityData.CaveLightDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents cave light entity data.
 * Created by Kneesnap on 12/17/2023.
 */
public class CaveLightEntityData extends MatrixEntityData<CaveLightDifficultyData> {
    public CaveLightEntityData(OldFroggerMapEntity entity) {
        super(entity, CaveLightDifficultyData::new);
    }

    @Getter
    public static class CaveLightDifficultyData extends OldFroggerDifficultyData {
        private int dieSpeed = 10;
        private int minRadius = 256;
        private int maxRadius = 256;
        private int fallOff = 256;

        public CaveLightDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.dieSpeed = reader.readUnsignedShortAsInt();
            this.minRadius = reader.readUnsignedShortAsInt();
            this.maxRadius = reader.readUnsignedShortAsInt();
            this.fallOff = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.dieSpeed);
            writer.writeUnsignedShort(this.minRadius);
            writer.writeUnsignedShort(this.maxRadius);
            writer.writeUnsignedShort(this.fallOff);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Die Speed", this.dieSpeed, newValue -> this.dieSpeed = newValue, 30);
            editor.addUnsignedFixedShort("Min Radius", this.minRadius, newValue -> this.minRadius = newValue, 256);
            editor.addUnsignedFixedShort("Max Radius", this.maxRadius, newValue -> this.maxRadius = newValue, 256);
            editor.addUnsignedFixedShort("Falloff", this.fallOff, newValue -> this.fallOff = newValue, 256);
        }
    }
}