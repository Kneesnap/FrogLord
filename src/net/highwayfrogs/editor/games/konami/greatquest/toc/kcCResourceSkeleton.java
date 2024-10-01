package net.highwayfrogs.editor.games.konami.greatquest.toc;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a hierarchy or a model animation skeleton.
 * Created by Kneesnap on 4/16/2024.
 */
@Getter
public class kcCResourceSkeleton extends kcCResource implements IMultiLineInfoWriter {
    private final kcNode rootNode;

    public kcCResourceSkeleton(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.HIERARCHY);
        this.rootNode = new kcNode(this, null);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        reader.verifyString(KCResourceID.HIERARCHY.getSignature()); // For some reason this is here again.
        reader.skipInt(); // Skip the size.
        this.rootNode.load(reader);
        if (this.rootNode.loadEndPosition >= 0) // Ensure the reader is placed after the end of the node data.
            reader.setIndex(this.rootNode.loadEndPosition);

        warnIfTagsDoNotFollowExpectedPattern();
    }

    @Override
    public void save(DataWriter writer) {
        warnIfTagsDoNotFollowExpectedPattern();

        int dataStartAddress = writer.getIndex();
        super.save(writer);
        writer.writeStringBytes(KCResourceID.TRACK.getSignature());
        int dataSizeAddress = writer.writeNullPointer();
        this.rootNode.save(writer);

        // Ensure we get the size right.
        writer.writeAddressAt(dataSizeAddress, writer.getIndex() - dataStartAddress);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("kcCResourceSkeleton['").append(getName()).append("'/").append(getHashAsHexString()).append("]:").append(Constants.NEWLINE);
        this.rootNode.writeMultiLineInfo(builder, padding + " ");
    }

    /**
     * Get all skeleton nodes ordered by tag.
     */
    public List<kcNode> getAllNodes() {
        List<kcNode> results = new ArrayList<>();
        List<kcNode> nodeQueue = new ArrayList<>();
        nodeQueue.add(this.rootNode);
        while (nodeQueue.size() > 0) {
            kcNode tempNode = nodeQueue.remove(0);
            if (tempNode.children.size() > 0)
                nodeQueue.addAll(0, tempNode.children);

            results.add(tempNode);
        }

        return results;
    }

    /**
     * Generates new tag ids for all nodes to ensure they follow the expected pattern.
     */
    public void updateTags() {
        // Generate new node tags which follow the expected pattern.
        int nextTag = 0;
        List<kcNode> nodeQueue = new ArrayList<>();
        nodeQueue.add(this.rootNode);
        while (nodeQueue.size() > 0) {
            kcNode tempNode = nodeQueue.remove(0);
            if (tempNode.children.size() > 0)
                nodeQueue.addAll(0, tempNode.children);

            tempNode.tag = nextTag++;
        }
    }

    private void warnIfTagsDoNotFollowExpectedPattern() {
        // Warn if the ids do not follow the expected pattern.
        // (This will break our search algorithms, so it's good to confirm)
        int expectedTag = 0;
        List<kcNode> nodeQueue = new ArrayList<>();
        nodeQueue.add(this.rootNode);
        while (nodeQueue.size() > 0) {
            kcNode tempNode = nodeQueue.remove(0);
            if (tempNode.children.size() > 0)
                nodeQueue.addAll(0, tempNode.children);

            if (tempNode.getTag() != expectedTag) {
                getLogger().warning("kcNode['" + tempNode.getName() + "',tag=" + tempNode.getTag() + "] was expected to have tag " + expectedTag + ".");
                expectedTag = tempNode.getTag() + 1;
            } else {
                expectedTag++;
            }
        }
    }

    /**
     * Finds a node by its tag.
     * @param tag the tag to find the node by.
     * @return node, or null if no node exists with the tag.
     */
    public kcNode getNodeByTag(int tag) {
        if (tag < 0)
            return null;

        // Warn if the ids do not follow the expected pattern.
        // (This will break our search algorithms, so it's good to confirm)
        kcNode tempNode = this.rootNode;
        while (tempNode != null) {
            // Test if the current node is our target.
            if (tempNode.getTag() == tag)
                return tempNode;

            // We could binary search here.
            // However, I do not think it common for there to be more than 3 child nodes per bone, let alone have that happen in a nested manner.
            // So, a simple linear search is probably best for now.
            kcNode lastNode = null;
            for (int i = 0; i < tempNode.getChildren().size(); i++) {
                kcNode childNode = tempNode.getChildren().get(i);
                if (childNode.getTag() > tag)
                    break; // Gone too far up, use the last node we've seen.

                lastNode = childNode;
            }

            // Apply the node which could contain the tag as our next node.
            tempNode = lastNode;
        }

        // Didn't find any node with this one.
        return null;
    }

    public static class kcNode extends GameData<GreatQuestInstance> implements IMultiLineInfoWriter {
        @Getter private final kcCResourceSkeleton skeleton;
        @Getter private final kcNode parent;
        @Getter private final List<kcNode> children = new ArrayList<>();
        @Getter private String name;
        @Getter private int tag; // Seems to be an incremental ID used to identify each node.
        @Getter private int flags = FLAG_VALIDATION_MASK; // The default value seems to be the only value.
        @Getter private final kcMatrix matrix;
        private transient int loadEndPosition = -1;

        private static final int NAME_SIZE = 32;
        private static final int CHILD_COUNT_MASK_SHIFT = 16; // 16 bits into the flag value is a child node count.
        private static final int CHILD_COUNT_MASK = 0xFF; // This is a guess.
        private static final int FLAG_VALIDATION_MASK = 0b00100111;
        private static final int FLAG_UNKNOWN00 = Constants.BIT_FLAG_0; // TODO: WHAT IS THIS FLAG?
        private static final int FLAG_UNKNOWN01 = Constants.BIT_FLAG_1; // TODO: WHAT IS THIS FLAG?
        private static final int FLAG_UNKNOWN02 = Constants.BIT_FLAG_2; // TODO: WHAT IS THIS FLAG?
        private static final int FLAG_UNKNOWN05 = Constants.BIT_FLAG_5; // TODO: WHAT IS THIS FLAG?

        public kcNode(kcCResourceSkeleton skeleton, kcNode parent) {
            super(skeleton.getGameInstance());
            this.skeleton = skeleton;
            this.parent = parent;
            this.matrix = new kcMatrix(getGameInstance());
        }

        @Override
        public void load(DataReader reader) {
            int nodeBaseAddress = reader.getIndex();
            this.name = reader.readNullTerminatedFixedSizeString(NAME_SIZE, Constants.NULL_BYTE);
            this.tag = reader.readInt();
            this.flags = reader.readInt();
            reader.skipPointer(); // Runtime pointer. (There does seem to be a pointer here, but I think it's overwritten at runtime)
            reader.skipPointer(); // Runtime pointer. (There does seem to be a pointer here, but I think it's overwritten at runtime)
            int childCount = reader.readInt();
            int nodeDataLength = reader.readInt();
            reader.skipPointer(); // Pointer to the node data.

            // Strip & validate masked flag child count.
            int flagChildCount = (this.flags >>> CHILD_COUNT_MASK_SHIFT) & CHILD_COUNT_MASK;
            if (flagChildCount != childCount)
                throw new RuntimeException("The number of child nodes written in the bit flags (" + flagChildCount + ") did not match the explicit child node count! (" + childCount + ")");

            this.flags &= ~(CHILD_COUNT_MASK << CHILD_COUNT_MASK_SHIFT);
            warnAboutInvalidBitFlags(this.flags, FLAG_VALIDATION_MASK);

            // Read node data.
            if (nodeDataLength != kcMatrix.BYTE_SIZE) // TODO: We might instead treat this as 3 kcVector4s named [pos rot, scl],PositionRotationScale or [from, at, up],Target, with 16 bytes of padding. This is a union basically. Not sure yet what actually makes the code choose which union to use, but it's probably flags or tag.
                throw new RuntimeException("Expected data " + nodeDataLength);

            this.matrix.load(reader);

            // Read child nodes.
            this.children.clear();
            for (int i = 0; i < childCount; i++) {
                int childDataStartAddress = reader.readInt();

                // Read child node.
                kcNode newChild = new kcNode(this.skeleton, this);
                reader.jumpTemp(nodeBaseAddress + childDataStartAddress);
                newChild.load(reader);
                int childEndIndex = Math.max(reader.getIndex(), newChild.loadEndPosition);
                reader.jumpReturn();
                this.children.add(newChild);

                // Get reader end position.
                if (childEndIndex > this.loadEndPosition)
                    this.loadEndPosition = childEndIndex;
            }
        }

        @Override
        public void save(DataWriter writer) {
            if (this.children.size() > CHILD_COUNT_MASK)
                throw new RuntimeException("The skeleton has " + this.children.size() + " child nodes attached to a single node, but the maximum is " + CHILD_COUNT_MASK);

            int nodeBaseAddress = writer.getIndex();
            writer.writeNullTerminatedFixedSizeString(this.name, NAME_SIZE, Constants.NULL_BYTE);
            writer.writeInt(this.tag);
            writer.writeInt(this.flags | (this.children.size() << CHILD_COUNT_MASK_SHIFT));
            writer.writeNullPointer(); // Runtime pointer. (There does seem to be a pointer here, but I think it's overwritten at runtime)
            writer.writeNullPointer(); // Runtime pointer. (There does seem to be a pointer here, but I think it's overwritten at runtime)
            writer.writeInt(this.children.size());
            writer.writeInt(kcMatrix.BYTE_SIZE);
            writer.writeNullPointer(); // Pointer to the node data.
            if (this.matrix != null)
                this.matrix.save(writer);

            // Write slots for child node offsets.
            int childNodeOffsetList = writer.getIndex();
            for (int i = 0; i < this.children.size(); i++)
                writer.writeNullPointer();

            // Write child nodes.
            for (int i = 0; i < this.children.size(); i++) {
                writer.writeAddressAt(childNodeOffsetList + (i * Constants.INTEGER_SIZE), writer.getIndex() - nodeBaseAddress);
                this.children.get(i).save(writer);
            }
        }

        @Override
        public void writeMultiLineInfo(StringBuilder builder, String padding) {
            builder.append(padding).append("'").append(this.name)
                    .append("'{Tag=").append(this.tag)
                    .append(",Flags=").append(Integer.toHexString(this.flags).toUpperCase())
                    .append(",Children=").append(this.children.size())
                    .append(",Data=").append(this.matrix).append('}');
            if (this.children.size() > 0) {
                builder.append(':').append(Constants.NEWLINE);

                String newPadding = padding + " ";
                for (int i = 0; i < this.children.size(); i++)
                    this.children.get(i).writeMultiLineInfo(builder, newPadding);
            } else {
                builder.append(Constants.NEWLINE);
            }
        }
    }
}