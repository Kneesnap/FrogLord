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
 * This data seems somewhat similar to the "Swarm" entity data seen in the forest level.
 * TODO: Entities with this come with difficulty flag 15 set. Could this mean the same data is used for all difficulty levels?
 * Created by Kneesnap on 12/17/2023.
 */
@Getter
public class SlimeEntityData extends OldFroggerEntityData<OldFroggerNullDifficultyData> {
    private int unknownValue1;
    private int unknownValue2;
    private int unknownValue3;
    private int unknownValue4;

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
        editor.addUnsignedFixedShort("Unknown #1", this.unknownValue1, newValue -> this.unknownValue1 = newValue, 1);
        editor.addUnsignedFixedShort("Unknown #2", this.unknownValue2, newValue -> this.unknownValue2 = newValue, 1);
        editor.addUnsignedFixedShort("Unknown #3", this.unknownValue3, newValue -> this.unknownValue3 = newValue, 1);
        editor.addUnsignedFixedShort("Unknown #4", this.unknownValue4, newValue -> this.unknownValue4 = newValue, 1);
    }
}