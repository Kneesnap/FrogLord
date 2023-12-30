package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest.SwarmEntityData.SwarmDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a bee swarm entity.
 * This data seems somewhat similar to the "Slime" entity data seen in the caves level.
 * Created by Kneesnap on 12/17/2023.
 */
public class SwarmEntityData extends OldFroggerEntityData<SwarmDifficultyData> {
    private int unknown1;
    private int unknown2;
    private int unknown3;
    private int unknown4;

    public SwarmEntityData(OldFroggerMapEntity entity) {
        super(entity, SwarmDifficultyData::new);
    }

    @Override
    protected void loadMainEntityData(DataReader reader) {
        this.unknown1 = reader.readUnsignedShortAsInt();
        this.unknown2 = reader.readUnsignedShortAsInt();
        this.unknown3 = reader.readUnsignedShortAsInt();
        this.unknown4 = reader.readUnsignedShortAsInt();
    }

    @Override
    protected void saveMainEntityData(DataWriter writer) {
        writer.writeUnsignedShort(this.unknown1);
        writer.writeUnsignedShort(this.unknown2);
        writer.writeUnsignedShort(this.unknown3);
        writer.writeUnsignedShort(this.unknown4);
    }

    @Override
    public float[] getPosition(float[] position) {
        return new float[6];
    }

    @Override
    public void setupMainEntityDataEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
        editor.addUnsignedFixedShort("Unknown #1", this.unknown1, newValue -> this.unknown1 = newValue, 1);
        editor.addUnsignedFixedShort("Unknown #2", this.unknown2, newValue -> this.unknown2 = newValue, 1);
        editor.addUnsignedFixedShort("Unknown #3", this.unknown3, newValue -> this.unknown3 = newValue, 1);
        editor.addUnsignedFixedShort("Unknown #4", this.unknown4, newValue -> this.unknown4 = newValue, 1);
    }

    @Getter
    public static class SwarmDifficultyData extends OldFroggerDifficultyData {
        private int speed = 2184; // This is a guess, but is probably right.

        public SwarmDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.speed = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.speed);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
        }
    }
}