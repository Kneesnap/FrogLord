package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert.FallingRockEntityData.FallingRockDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents data belonging to a falling rock entity.
 * Created by Kneesnap on 12/19/2023.
 */
public class FallingRockEntityData extends MatrixEntityData<FallingRockDifficultyData> {
    public FallingRockEntityData(OldFroggerMapEntity entity) {
        super(entity, FallingRockDifficultyData::new);
    }

    @Getter
    public static class FallingRockDifficultyData extends OldFroggerDifficultyData {
        private int moveDelay = 150;
        private final SVector positionOne = new SVector();
        private int timeToPositionOne = 150;
        private final SVector positionTwo = new SVector();
        private int timeToPositionTwo = 90;
        private final SVector positionThree = new SVector();
        private int timeToPositionThree = 30;
        private short bounceCount = 1;

        public FallingRockDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.moveDelay = reader.readUnsignedShortAsInt();
            this.positionOne.loadWithPadding(reader);
            this.timeToPositionOne = reader.readUnsignedShortAsInt();
            this.positionTwo.loadWithPadding(reader);
            this.timeToPositionTwo = reader.readUnsignedShortAsInt();
            this.positionThree.loadWithPadding(reader);
            this.timeToPositionThree = reader.readUnsignedShortAsInt();
            this.bounceCount = reader.readUnsignedByteAsShort();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.moveDelay);
            this.positionOne.saveWithPadding(writer);
            writer.writeUnsignedShort(this.timeToPositionOne);
            this.positionTwo.saveWithPadding(writer);
            writer.writeUnsignedShort(this.timeToPositionTwo);
            this.positionThree.saveWithPadding(writer);
            writer.writeUnsignedShort(this.timeToPositionThree);
            writer.writeUnsignedByte(this.bounceCount);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Move Delay (secs)", this.moveDelay, newValue -> this.moveDelay = newValue, 30);
            editor.addFloatSVector("Position 1", this.positionOne, manager.getController());
            editor.addUnsignedFixedShort("Time to Position 1 (secs)", this.timeToPositionOne, newValue -> this.timeToPositionOne = newValue, 30);
            editor.addFloatSVector("Position 2", this.positionOne, manager.getController());
            editor.addUnsignedFixedShort("Time to Position 2 (secs)", this.timeToPositionTwo, newValue -> this.timeToPositionTwo = newValue, 30);
            editor.addFloatSVector("Position 3", this.positionOne, manager.getController());
            editor.addUnsignedFixedShort("Time to Position 3 (secs)", this.timeToPositionThree, newValue -> this.timeToPositionThree = newValue, 30);
            editor.addUnsignedFixedShort("Bounce Count", this.bounceCount, newValue -> this.bounceCount = (short) (int) newValue, 1, 1, 3);
        }
    }
}