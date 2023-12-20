package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky.JetEntityData.JetDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

import java.util.Arrays;

/**
 * Represents data belonging to a jet entity.
 * Created by Kneesnap on 12/18/2023.
 */
public class JetEntityData extends PathEntityData<JetDifficultyData> {
    public JetEntityData(OldFroggerMapEntity entity) {
        super(entity, JetDifficultyData::new);
    }

    @Getter
    public static class JetDifficultyData extends OldFroggerDifficultyData {
        private final int[] cloudForms = new int[8];
        private final int[] cloudDelays = new int[8];
        private int cloudDuration = 10;
        private int numberOfClouds;
        private int jetSpeed = 2184;
        private int splineDelay;

        public JetDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
            Arrays.fill(this.cloudForms, 70);
            Arrays.fill(this.cloudDelays, 10);
        }

        @Override
        public void load(DataReader reader) {
            for (int i = 0; i < this.cloudForms.length; i++)
                this.cloudForms[i] = reader.readUnsignedShortAsInt();
            for (int i = 0; i < this.cloudDelays.length; i++)
                this.cloudDelays[i] = reader.readUnsignedShortAsInt();
            this.cloudDuration = reader.readUnsignedShortAsInt();
            this.numberOfClouds = reader.readUnsignedShortAsInt();
            this.jetSpeed = reader.readUnsignedShortAsInt();
            this.splineDelay = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            for (int i = 0; i < this.cloudForms.length; i++)
                writer.writeUnsignedShort(this.cloudForms[i]);
            for (int i = 0; i < this.cloudDelays.length; i++)
                writer.writeUnsignedShort(this.cloudDelays[i]);
            writer.writeUnsignedShort(this.cloudDuration);
            writer.writeUnsignedShort(this.numberOfClouds);
            writer.writeUnsignedShort(this.jetSpeed);
            writer.writeUnsignedShort(this.splineDelay);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            for (int i = 0; i < this.cloudForms.length; i++) {
                final int index = i;
                editor.addUnsignedFixedShort("Cloud Form " + (i + 1), this.cloudForms[i], newValue -> this.cloudForms[index] = newValue, 1, 0, 1000);
            }

            for (int i = 0; i < this.cloudDelays.length; i++) {
                final int index = i;
                editor.addUnsignedFixedShort("Cloud Delay " + (i + 1), this.cloudDelays[i], newValue -> this.cloudDelays[index] = newValue, 30, 0, 1000);
            }

            editor.addUnsignedFixedShort("Cloud Duration", this.cloudDuration, newValue -> this.cloudDuration = newValue, 30, 0, 1000);
            editor.addUnsignedFixedShort("Number of Clouds", this.numberOfClouds, newValue -> this.numberOfClouds = newValue, 1, 0, 10);
            editor.addUnsignedFixedShort("Jet Speed", this.jetSpeed, newValue -> this.jetSpeed = newValue, 2184);
            editor.addUnsignedFixedShort("Spline Delay", this.splineDelay, newValue -> this.splineDelay = newValue, 30, 0, 3000);
        }
    }
}