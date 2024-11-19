package net.highwayfrogs.editor.games.konami.greatquest.map.octree;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;

/**
 * Represents a quad branch in a kcOctTree. In other words, it allows quad-tree searches in an octree.
 * Quad trees are used when a kcOctLeaf wants to search its neighbor (often for ray-casting), quad trees help ensure only nearby parts of the neighbor are searched.
 * Most of these branches appear to be fully zeroed. (At the end of each list).
 * Created by Kneesnap on 11/17/2024.
 */
@Getter
public class kcQuadBranch extends GameData<GreatQuestInstance> implements IInfoWriter {
    private final short[] childNodes = new short[CHILD_NODE_COUNT];

    public static final int CHILD_NODE_COUNT = 4;
    // If the leaf slot has CHILD_INDEX_FLAG_Y_HI, the first coordinate value is Y, otherwise it is X.
    // If the leaf slot has CHILD_INDEX_FLAG_Z_HI, the second coordinate value is Z, otherwise it is Y.
    // Thus, the valid coordinate pairs are: XY, XZ, YZ
    public static final int INDEX_FLAG_POSITIVE_COORDINATE_1 = Constants.BIT_FLAG_1;
    public static final int INDEX_FLAG_POSITIVE_COORDINATE_2 = Constants.BIT_FLAG_0;

    public kcQuadBranch(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.childNodes.length; i++)
            this.childNodes[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.childNodes.length; i++)
            writer.writeShort(this.childNodes[i]);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        writeInfo(builder);
        return builder.toString();
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        builder.append("kcQuadBranch[");
        kcOctLeaf.appendIds(builder, this.childNodes);
        builder.append(']');
    }
}