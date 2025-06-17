package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcMatrix;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcQuat;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector3;
import net.highwayfrogs.editor.system.math.Matrix4x4f;
import net.highwayfrogs.editor.system.math.Quaternion;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.system.math.Vector4f;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a hierarchy or a model animation skeleton.
 * kcCActorBase::Init() -> create a new kcCSkeleton() and run kcCSkeleton::InitHierarchy().
 *  -> kcSkeletonCreate()
 *
 * kcSkeletonExecute() is what actually builds the matrices used for rendering.
 * Created by Kneesnap on 4/16/2024.
 */
@Getter
public class kcCResourceSkeleton extends kcCResource implements IMultiLineInfoWriter {
    private final kcNode rootNode;

    public static final int MAXIMUM_BONE_COUNT = 255;

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

        super.save(writer);
        int dataStartAddress = writer.getIndex();
        writer.writeStringBytes(KCResourceID.HIERARCHY.getSignature());
        int dataSizeAddress = writer.writeNullPointer();
        this.rootNode.save(writer);

        // Ensure we get the size right.
        writer.writeIntAtPos(dataSizeAddress, writer.getIndex() - dataStartAddress);
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
                getLogger().warning("kcNode['%s',tag=%d] was expected to have tag %d.", tempNode.getName(), tempNode.getTag(), expectedTag);
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

    /**
     * Finds a node by its name.
     * @param name the name to find the node by.
     * @return node, or null if no node exists with the name.
     */
    public kcNode getNodeByName(String name) {
        if (name == null)
            throw new NullPointerException("name");

        List<kcNode> nodeQueue = new ArrayList<>();
        nodeQueue.add(this.rootNode);
        while (nodeQueue.size() > 0) {
            kcNode tempNode = nodeQueue.remove(0);
            if (name.equalsIgnoreCase(tempNode.getName()))
                return tempNode;

            if (tempNode.children.size() > 0)
                nodeQueue.addAll(0, tempNode.children);
        }

        // Didn't find any node with the given name.
        return null;
    }

    public static class kcNode extends GameData<GreatQuestInstance> implements IMultiLineInfoWriter {
        @Getter private final kcCResourceSkeleton skeleton;
        @Getter private final kcNode parent;
        @Getter private final List<kcNode> children = new ArrayList<>();
        @Getter private String name;
        @Getter private int tag; // Seems to be an incremental ID used to identify each node.
        @Getter private int flags = DEFAULT_FLAGS; // The default value seems to be the only value.
        private final kcVector3 localPosition = new kcVector3(); // This is saved in a union with 4Vector4, aka the size of a kcMatrix.
        private final kcQuat localRotation = new kcQuat(); // However, we can be confident these are the values since ComputeWtoLInit() is called by kcSkeletonCreate() unconditionally, we know what this data is.
        private final kcVector3 localScale = new kcVector3(1, 1, 1);
        private final Matrix4x4f hierarchyMatrix = new Matrix4x4f();
        private final Matrix4x4f inverseHierarchyMatrix = new Matrix4x4f();
        @Getter private boolean hierarchyMatrixDirty = true;
        @Getter private boolean inverseMatrixDirty = true;
        private transient int loadEndPosition = -1;

        private static final int NAME_SIZE = 32;
        private static final int CHILD_COUNT_MASK_SHIFT = 16; // 16 bits into the flag value is a child node count.
        private static final int CHILD_COUNT_MASK = 0x1F; // This is seen in many places, such as kcSkeletonExecute.
        public static final int FLAG_HAS_POSITION = Constants.BIT_FLAG_0; // Determined by checking the BoneSetup() callback method.
        public static final int FLAG_HAS_ROTATION = Constants.BIT_FLAG_1; // Determined by checking the BoneSetup() callback method.
        public static final int FLAG_HAS_SCALE = Constants.BIT_FLAG_2; // Determined by checking the BoneSetup() callback method. NOT USED WHEN CALCULATING THE NON-ANIMATED BONE/MODEL SPACE MATRICES. ONLY USED DURING ACTUAL ANIMATIONS.
        public static final int FLAG_IS_MATRIX = Constants.BIT_FLAG_3; // CURRENTLY NEVER SEEN, BUT APPEARS CODED. This flag indicates that the data included in the node is a matrix, and NOT a combination of position/rotation/scale/uninitialized data.
        private static final int FLAG_UNKNOWN05 = Constants.BIT_FLAG_5; // This flag is always set, but I've yet to find anywhere it is checked in the executable. It may be unimplemented.
        private static final int DEFAULT_FLAGS = FLAG_UNKNOWN05 | FLAG_HAS_SCALE | FLAG_HAS_ROTATION | FLAG_HAS_POSITION;
        private static final int FLAG_VALIDATION_MASK = FLAG_UNKNOWN05 | FLAG_HAS_SCALE | FLAG_HAS_ROTATION | FLAG_HAS_POSITION;

        public kcNode(kcCResourceSkeleton skeleton, kcNode parent) {
            super(skeleton.getGameInstance());
            this.skeleton = skeleton;
            this.parent = parent;
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
            if (nodeDataLength != kcMatrix.BYTE_SIZE)
                throw new RuntimeException("Expected kcMatrix data size, but got " + nodeDataLength + " instead!");

            this.localPosition.load(reader);
            reader.skipBytes(Constants.FLOAT_SIZE);
            this.localRotation.load(reader);
            this.localScale.load(reader);
            reader.skipBytes(Constants.FLOAT_SIZE);
            reader.skipBytes(4 * Constants.FLOAT_SIZE);

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

            // Write positional data.
            markHierarchyMatrixDirty();
            this.localPosition.save(writer);
            writer.writeFloat(1); // Default value of Vector4
            this.localRotation.save(writer);
            this.localScale.save(writer);
            writer.writeFloat(1); // Default value of Vector4
            writer.writeFloat(1); // Unallocated Vector4
            writer.writeFloat(2); // Unallocated Vector4
            writer.writeFloat(3); // Unallocated Vector4
            writer.writeFloat(4); // Unallocated Vector4

            // Write slots for child node offsets.
            int childNodeOffsetList = writer.getIndex();
            for (int i = 0; i < this.children.size(); i++)
                writer.writeNullPointer();

            // Write child nodes.
            for (int i = 0; i < this.children.size(); i++) {
                writer.writeIntAtPos(childNodeOffsetList + (i * Constants.INTEGER_SIZE), writer.getIndex() - nodeBaseAddress);
                this.children.get(i).save(writer);
            }
        }

        @Override
        public void writeMultiLineInfo(StringBuilder builder, String padding) {
            builder.append(padding).append("'").append(this.name)
                    .append("'{Tag=").append(this.tag)
                    .append(",Flags=").append(Integer.toHexString(this.flags).toUpperCase())
                    .append(",Children=").append(this.children.size())
                    .append(",Position=").append(this.localPosition)
                    .append(",Rotation=").append(Quaternion.toEulerAngles(getLocalRotation()))
                    .append(",Scale=").append(this.localScale).append('}');
            if (this.children.size() > 0) {
                builder.append(':').append(Constants.NEWLINE);

                String newPadding = padding + " ";
                for (int i = 0; i < this.children.size(); i++)
                    this.children.get(i).writeMultiLineInfo(builder, newPadding);
            } else {
                builder.append(Constants.NEWLINE);
            }
        }

        /**
         * Returns true iff the position vector is present.
         */
        public boolean hasPosition() {
            return (this.flags & FLAG_HAS_POSITION) == FLAG_HAS_POSITION;
        }

        /**
         * Gets the position as a Vector.
         */
        public Vector3f getLocalPosition() {
            if (hasPosition()) {
                return this.localPosition;
            } else {
                return this.localPosition.setXYZ(0, 0, 0); // Default as seen in BoneSetup() and kcSkeletonExecute()
            }
        }

        /**
         * Returns true iff the rotation vector is present.
         */
        public boolean hasRotation() {
            return (this.flags & FLAG_HAS_ROTATION) == FLAG_HAS_ROTATION;
        }

        /**
         * Gets the rotation as a quaternion.
         */
        public Vector4f getLocalRotation() {
            if (hasRotation()) {
                return this.localRotation;
            } else {
                return Quaternion.newIdentity(); // Unit quaternion as seen in BoneSetup() and kcSkeletonExecute()
            }
        }

        /**
         * Returns true iff the scale vector is present.
         */
        public boolean hasScale() {
            return (this.flags & FLAG_HAS_SCALE) == FLAG_HAS_SCALE;
        }

        /**
         * Gets the scale as a Vector.
         */
        public Vector3f getLocalScale() {
            if (hasScale()) {
                return this.localScale;
            } else {
                return this.localScale.setXYZ(1, 1, 1); // Default as seen in BoneSetup() and kcSkeletonExecute()
            }
        }

        /**
         * Mark the hierarchy matrix as dirty. (Also recursively updates any child nodes which don't have it marked as dirty, as they will have matrices impacted by this one)
         */
        public void markHierarchyMatrixDirty() {
            if (this.hierarchyMatrixDirty)
                return; // Don't update it again.

            this.inverseMatrixDirty = this.hierarchyMatrixDirty = true;
            for (int i = 0; i < this.children.size(); i++)
                this.children.get(i).markHierarchyMatrixDirty();
        }

        /**
         * Gets the local offset matrix usable for matrix calculations.
         * @return localOffsetMatrix
         */
        public Matrix4x4f getLocalOffsetMatrix() {
            return getLocalOffsetMatrix(new Matrix4x4f());
        }

        /**
         * Gets the local offset matrix usable for matrix calculations.
         * @param result the matrix to store the results within
         * @return localOffsetMatrix
         */
        public Matrix4x4f getLocalOffsetMatrix(Matrix4x4f result) {
            kcMatrix.kcQuatToMatrix(getLocalRotation(), result);
            if (this.tag != 0) { // Seen in EnumEval()
                result.setTranslation(getLocalPosition());
            } else { // Seen in EnumEval()
                result.setTranslation(0, 0, 0);
            }

            return result;
        }

        /**
         * Gets an updated hierarchy matrix causing which can be used to transform from model space (relative to 0, 0, 0) to bone space (relative to the bone).
         * Based on the 'ComputeWtoLInit()' method.
         */
        public Matrix4x4f getBoneToModelMatrix() {
            if (!this.hierarchyMatrixDirty)
                return this.hierarchyMatrix;

            // Reimplementation of ComputeWToLInit().
            // To ensure consistency with the original function, ignore the flags indicating whether the position & scale are valid.

            // 1) Get the quaternion (rotation) as a rotation matrix, and apply the current position as the translation.
            // This matches the behavior in ComputeWtoLInit, NOT the behavior in EnumEval() or elsewhere.
            Matrix4x4f quatMatrix = kcMatrix.kcQuatToMatrix(this.localRotation, this.hierarchyMatrix);
            quatMatrix.setTranslation(this.localPosition);

            // 2) Multiply against the matrix from the parent bone. (If there is no parent bone, the game multiplies it against an identity matrix, but we can just skip that.)
            if (this.parent != null)
                this.hierarchyMatrix.multiply(this.parent.getBoneToModelMatrix(), this.hierarchyMatrix);

            this.hierarchyMatrixDirty = false;
            return this.hierarchyMatrix;
        }

        /**
         * Gets an updated hierarchy matrix causing which can be used to transform from model space (relative to 0, 0, 0) to bone space (relative to the bone).
         * Based on the 'ComputeWtoLInit()' method calling kcMatrixInvert() to store its result.
         */
        public Matrix4x4f getModelToBoneMatrix() {
            if (!this.inverseMatrixDirty)
                return this.inverseHierarchyMatrix;

            this.inverseHierarchyMatrix.set(getBoneToModelMatrix());
            kcMatrix.kcMatrixInvert(this.inverseHierarchyMatrix, this.inverseHierarchyMatrix);
            this.inverseMatrixDirty = false;
            return this.inverseHierarchyMatrix;
        }
    }
}