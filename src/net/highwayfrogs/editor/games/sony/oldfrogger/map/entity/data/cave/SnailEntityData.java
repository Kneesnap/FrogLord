package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave.SnailEntityData.SnailDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents entity data for the cave snail entity.
 * It seems the cave level in Milestone 3 has old data or something for most of the snails.
 * This is because most snails contain unusual data. However, there is a snail (The one near the rope bridge in the middle of the level) which has normal looking data.
 * Looking in-game, it seems the snails with very large values create a very large trail of slime which takes forever to decay. However, the snail with small values decays in the amount of time shown.
 * Thus, while the data seen by snails seems odd, I do believe I have supported it correctly.
 * Created by Kneesnap on 12/17/2023.
 */
public class SnailEntityData extends PathEntityData<SnailDifficultyData> {
    public SnailEntityData(OldFroggerMapEntity entity) {
        super(entity, SnailDifficultyData::new);
    }

    @Getter
    public static class SnailDifficultyData extends OldFroggerDifficultyData {
        private int speed = 2184;
        private int slimeDelay = 30;
        private int slimeDecay = 30;

        public SnailDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.speed = reader.readUnsignedShortAsInt();
            this.slimeDelay = reader.readUnsignedShortAsInt();
            this.slimeDecay = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(this.slimeDelay);
            writer.writeUnsignedShort(this.slimeDecay);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
            editor.addUnsignedFixedShort("Slime Delay", this.slimeDelay, newValue -> this.slimeDelay = newValue, 30);
            editor.addUnsignedFixedShort("Slime Decay", this.slimeDecay, newValue -> this.slimeDecay = newValue, 30);
        }
    }
}