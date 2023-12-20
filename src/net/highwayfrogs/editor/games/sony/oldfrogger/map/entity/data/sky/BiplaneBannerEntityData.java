package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky.BiplaneBannerEntityData.BiplaneBannerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a biplane banner entity.
 * Created by Kneesnap on 12/18/2023.
 */
public class BiplaneBannerEntityData extends PathEntityData<BiplaneBannerDifficultyData> {
    public BiplaneBannerEntityData(OldFroggerMapEntity entity) {
        super(entity, BiplaneBannerDifficultyData::new);
    }

    @Getter
    public static class BiplaneBannerDifficultyData extends OldFroggerDifficultyData {
        private int twistDelay = 500;
        private int bannerLength = 5;
        private int biplaneSpeed = 2184;
        private int splineDelay;

        public BiplaneBannerDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.twistDelay = reader.readUnsignedShortAsInt();
            this.bannerLength = reader.readUnsignedShortAsInt();
            this.biplaneSpeed = reader.readUnsignedShortAsInt();
            this.splineDelay = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.twistDelay);
            writer.writeUnsignedShort(this.bannerLength);
            writer.writeUnsignedShort(this.biplaneSpeed);
            writer.writeUnsignedShort(this.splineDelay);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Twist Delay", this.twistDelay, newValue -> this.twistDelay = newValue, 30, 0, 1000);
            editor.addUnsignedFixedShort("Banner Length", this.bannerLength, newValue -> this.bannerLength = newValue, 1, 1, 10);
            editor.addUnsignedFixedShort("Biplane Speed", this.biplaneSpeed, newValue -> this.biplaneSpeed = newValue, 2184);
            editor.addUnsignedFixedShort("Spline Delay", this.splineDelay, newValue -> this.splineDelay = newValue, 30, 0, 1000);
        }
    }
}