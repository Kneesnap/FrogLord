package net.highwayfrogs.editor.games.konami.greatquest.map.octree;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the '_kcOctLeaf' struct.
 * The leaf represents a box capable of tracking values. (At least while the game is running, when it's data, it's just a bounding box)
 * The default values for this class are all zero, and leaves at the end of the list are often zeroed.
 * I assume this means null, as the first leaf seems to always be fully zeroed as well.
 * Created by Kneesnap on 11/17/2024.
 */
@Getter
public class kcOctLeaf extends GameData<GreatQuestInstance> implements IInfoWriter {
    // The node dimensions/position appear to always cover the full area available.
    // Ie: It covers the area fully.
    private short nodeDepth; // The distance away from the root node. (The root node would report zero, but it's not a leaf, so it doesn't have this property.)
    private short nodeX; // Used by kcOctTreeReverseTraversalIntoNeighbor/kcOctTreeCastRaySegment/kcOctTreeReverseTraversalInt. Multiplied against kcOctTree.maxResolution, then added to the tree offset to get the world position.
    private short nodeY; // Used by kcOctTreeReverseTraversalIntoNeighbor/kcOctTreeCastRaySegment/kcOctTreeReverseTraversalInt.
    private short nodeZ; // Used by kcOctTreeReverseTraversalIntoNeighbor/kcOctTreeCastRaySegment/kcOctTreeReverseTraversalInt.
    private final short[] sideNumbers = new short[CUBE_SIDE_COUNT]; // Within each leaf is a quad tree, this points to neighbor leafs or to quad tree branches used to reach neighbor leafs. nodes of the quad trees of neighboring octree nodes. Used by: kcOctTreeReverseTraversalThroughQuadTreeInt/kcOctTreeCastRaySegmentInt
    private short parent; // Zero is the default, and it should remain this way, so the default leaf state will be empty.
    private boolean enabled; // This was previously a flags value, but as there is only one bit flag, it is now a bool.
    private byte localIndexWithinParent; // Zero is the default, and it should remain this way, so the default leaf state will be empty.

    private static final int FLAG_ENABLED = Constants.BIT_FLAG_7; // 0x80 This is the only flag.
    private static final int CONTEXT_BYTE_LENGTH = 8;

    // These are the indices into sideNumbers[].
    public static final int SIDE_NUMBER_NEGATIVE_X = 0;
    public static final int SIDE_NUMBER_POSITIVE_X = 1;
    public static final int SIDE_NUMBER_NEGATIVE_Y = 2;
    public static final int SIDE_NUMBER_POSITIVE_Y = 3;
    public static final int SIDE_NUMBER_NEGATIVE_Z = 4;
    public static final int SIDE_NUMBER_POSITIVE_Z = 5;
    public static final int CUBE_SIDE_COUNT = 6;

    public kcOctLeaf(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.nodeDepth = reader.readShort();
        this.nodeX = reader.readShort();
        this.nodeY = reader.readShort();
        this.nodeZ = reader.readShort();
        for (int i = 0; i < this.sideNumbers.length; i++)
            this.sideNumbers[i] = reader.readShort();
        this.parent = reader.readShort();
        byte flags = reader.readByte();
        this.localIndexWithinParent = reader.readByte();
        reader.skipBytesRequireEmpty(CONTEXT_BYTE_LENGTH);

        // There is only one flag, so we've condensed it down to a single bool.
        warnAboutInvalidBitFlags(flags & 0xFF, FLAG_ENABLED);
        this.enabled = (flags & FLAG_ENABLED) == FLAG_ENABLED;
        validateData();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.nodeDepth);
        writer.writeShort(this.nodeX);
        writer.writeShort(this.nodeY);
        writer.writeShort(this.nodeZ);
        for (int i = 0; i < this.sideNumbers.length; i++)
            writer.writeShort(this.sideNumbers[i]);
        writer.writeShort(this.parent);
        writer.writeByte((byte) (this.enabled ? FLAG_ENABLED : 0));
        writer.writeByte(this.localIndexWithinParent);
        writer.writeNull(CONTEXT_BYTE_LENGTH);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        writeInfo(builder);
        return builder.toString();
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        builder.append("kcOctLeaf[depth=").append(this.nodeDepth)
                .append(",enabled=").append(this.enabled)
                .append(",parent=").append(NumberUtils.to0PrefixedHexString(this.parent))
                .append(",localIndexWithinParent=").append(this.localIndexWithinParent)
                .append(",pos=").append(this.nodeX).append(",").append(this.nodeY).append(",").append(this.nodeZ)
                .append(",sideNumbers=");

        appendIds(builder, this.sideNumbers);
        builder.append(']');
    }

    /**
     * Returns true iff the leaf contains no data.
     */
    public boolean isEmpty() {
        return this.nodeDepth == 0 && this.nodeX == 0 && this.nodeY == 0 && this.nodeZ == 0
                && this.parent == 0 && !this.enabled && this.localIndexWithinParent == 0
                && this.sideNumbers[0] == 0 && this.sideNumbers[1] == 0 && this.sideNumbers[2] == 0
                && this.sideNumbers[3] == 0 && this.sideNumbers[4] == 0 && this.sideNumbers[5] == 0;
    }

    /**
     * Calculates the world position of this leaf.
     * @param tree the tree this leaf belongs to
     * @param output the output storage for the position
     * @return worldPosition
     */
    public Vector3f getWorldCornerPosition(kcOctTree tree, Vector3f output) {
        if (tree == null)
            throw new NullPointerException("tree");
        if (output == null)
            output = new Vector3f();

        int smallestCubeLength = tree.getSmallestNodeSize();
        output.setX(tree.getOffset().getX() + (smallestCubeLength * this.nodeX));
        output.setY(tree.getOffset().getY() + (smallestCubeLength * this.nodeY));
        output.setZ(tree.getOffset().getZ() + (smallestCubeLength * this.nodeZ));
        return output;
    }

    /**
     * Calculates the world position of the center of the box covered by this leaf.
     * @param tree the tree this leaf belongs to
     * @param output the output storage for the position
     * @return worldPosition
     */
    public Vector3f getWorldCenterPosition(kcOctTree tree, Vector3f output) {
        output = getWorldCornerPosition(tree, output);
        float sizeOver2 = getCubeDimensions(tree) * .5F;
        return output.add(sizeOver2, sizeOver2, sizeOver2);
    }

    /**
     * Gets the dimensions of the cube represented by this leaf.
     * @param tree the tree to calculate it from
     * @return cubeDimensions
     */
    public int getCubeDimensions(kcOctTree tree) {
        if (tree == null)
            throw new NullPointerException("tree");

        return tree.getNodeSize(this.nodeDepth);
    }

    private void validateData() {
        if ((this.parent & kcOctTree.FLAG_IS_LEAF) == kcOctTree.FLAG_IS_LEAF)
            throw new RuntimeException("A kcOctBranch's parent node cannot be a leaf! (Got: " + NumberUtils.toHexString(this.parent) + ")");
        if (this.localIndexWithinParent > kcOctBranch.MAX_CHILD_INDEX || this.localIndexWithinParent < 0)
            throw new RuntimeException("Invalid localIndexWithinParent value of " + this.localIndexWithinParent);
    }

    /**
     * Append ids used in kcOctree to the string builder.
     * @param builder the builder to append to
     * @param ids the ids to write
     */
    public static void appendIds(StringBuilder builder, short[] ids) {
        if (builder == null)
            throw new NullPointerException("builder");
        if (ids == null)
            throw new NullPointerException("ids");

        for (int i = 0; i < ids.length; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(NumberUtils.to0PrefixedHexString(ids[i]));
        }
    }
}