package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerNullDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents entity data for the snail cave slime.
 * TODO: THIS IS ?????
 * TODO: Let's put this at the position above (below?) the snail entity. We should support an entity attachment system, which automatically updates, so if we update the position of the snail, the slime will move with it.
 * TODO: Entities with this come with difficulty flag 15 set. Could this mean the same data is used for all difficulty levels?
 * Created by Kneesnap on 12/17/2023.
 */
@Getter
public class SlimeEntityData extends OldFroggerEntityData<OldFroggerNullDifficultyData> {
    private int unknownValue1;
    private int unknownValue2;
    private int unknownValue3;
    private int unknownValue4;

    /*
    FLOAT MR_USHORT "Critical Distance" 0 32767 256 256
    FLOAT MR_USHORT "Frog Delay" 0 300 60 30
    FLOAT MR_USHORT "Swarm Speed" 0 32767 2184.5 2184.5
    FLOAT MR_USHORT "Interest Time" 0 3000 150 30
    */

    public SlimeEntityData(OldFroggerMapEntity entity) {
        super(entity, null);
    }

    @Override
    public void loadMainEntityData(DataReader reader) {
        this.unknownValue1 = reader.readUnsignedShortAsInt();
        this.unknownValue2 = reader.readUnsignedShortAsInt();
        this.unknownValue3 = reader.readUnsignedShortAsInt();
        this.unknownValue4 = reader.readUnsignedShortAsInt();
    }

    @Override
    public void saveMainEntityData(DataWriter writer) {
        writer.writeUnsignedShort(this.unknownValue1);
        writer.writeUnsignedShort(this.unknownValue2);
        writer.writeUnsignedShort(this.unknownValue3);
        writer.writeUnsignedShort(this.unknownValue4);
    }

    @Override
    public float[] getPosition(float[] position) {
        return new float[position.length];
    }

    @Override
    public void setupMainEntityDataEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
        editor.addUnsignedFixedShort("Value 1", this.unknownValue1, newValue -> this.unknownValue1 = newValue, 1);
        editor.addUnsignedFixedShort("Value 2", this.unknownValue2, newValue -> this.unknownValue2 = newValue, 1);
        editor.addUnsignedFixedShort("Value 3", this.unknownValue3, newValue -> this.unknownValue3 = newValue, 1);
        editor.addUnsignedFixedShort("Value 4", this.unknownValue4, newValue -> this.unknownValue4 = newValue, 1);
    }

    /*@Getter
    public static class SlimeDifficultyData extends OldFroggerDifficultyData {
        private int value = 200;
        private int blank = 200;
        private int showDelay = 10;
        private int showTime = 10;

        public SlimeDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.value = reader.readUnsignedShortAsInt();
            this.blank = reader.readUnsignedShortAsInt();
            this.showDelay = reader.readUnsignedShortAsInt();
            this.showTime = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.value);
            writer.writeUnsignedShort(this.blank);
            writer.writeUnsignedShort(this.showDelay);
            writer.writeUnsignedShort(this.showTime);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedInt("Value", this.value, newValue -> this.value = newValue, 1, 0, 1000);
            editor.addFixedInt("Blank", this.blank, newValue -> this.blank = newValue, 1, 0, 1000);
            editor.addFixedInt("Show Delay", this.showDelay, newValue -> this.showDelay = newValue, 30, 0, 1000);
            editor.addFixedInt("Show Time", this.showTime, newValue -> this.showTime = newValue, 30, 0, 1000);
        }
    }*/
}