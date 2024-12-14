package net.highwayfrogs.editor.games.konami.greatquest.map.octree;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.Arrays;

/**
 * Represents a branch (node with children/without any non-octree data).
 * There do not appear to be any zeroed branches in the branches list.
 * Created by Kneesnap on 11/17/2024.
 */
@Getter
public class kcOctBranch extends GameData<GreatQuestInstance> implements IInfoWriter {
    private final short[] childNumbers = new short[CHILD_NODE_COUNT]; // Child branch IDs --> 0x8000 marks it as a leaf?
    private short parent = 0; // 0 indicates null/no parent/is root node.
    private byte localIndexWithinParent; // For the root branch, this is still zero.

    // These flags are used to determine the index of a child node. Each flag represents whether the position coordinate is >= the tree boundary.
    public static final short CHILD_INDEX_FLAG_X_HI = (short) Constants.BIT_FLAG_0;
    public static final short CHILD_INDEX_FLAG_Y_HI = (short) Constants.BIT_FLAG_1;
    public static final short CHILD_INDEX_FLAG_Z_HI = (short) Constants.BIT_FLAG_2;
    public static final short CHILD_NODE_COUNT = 8;
    public static final short MAX_CHILD_INDEX = CHILD_NODE_COUNT - 1;

    public kcOctBranch(GreatQuestInstance instance) {
        super(instance);
        Arrays.fill(this.childNumbers, kcOctTree.NULL_LEAF_ID); // By default, we provide a null leaf to indicate end of tree.
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.childNumbers.length; i++)
            this.childNumbers[i] = reader.readShort();
        this.parent = reader.readShort();
        reader.skipBytesRequireEmpty(1); // unused
        this.localIndexWithinParent = reader.readByte();
        validateData();
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.childNumbers.length; i++)
            writer.writeShort(this.childNumbers[i]);
        writer.writeShort(this.parent);
        writer.writeByte(Constants.NULL_BYTE); // unused
        writer.writeByte(this.localIndexWithinParent);
    }

    private void validateData() {
        if ((this.parent & kcOctTree.FLAG_IS_LEAF) == kcOctTree.FLAG_IS_LEAF)
            throw new RuntimeException("A kcOctBranch's parent node cannot be a leaf! (Got: " + NumberUtils.toHexString(this.parent) + ")");
        if (this.localIndexWithinParent > MAX_CHILD_INDEX || this.localIndexWithinParent < 0)
            throw new RuntimeException("Invalid localIndexWithinParent value of " + this.localIndexWithinParent);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        writeInfo(builder);
        return builder.toString();
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        builder.append("kcOctBranch[parent=").append(NumberUtils.to0PrefixedHexString(this.parent))
                .append(",localIndexWithinParent=").append(this.localIndexWithinParent)
                .append(",childNumbers=");

        kcOctLeaf.appendIds(builder, this.childNumbers);
        builder.append(']');
    }
}