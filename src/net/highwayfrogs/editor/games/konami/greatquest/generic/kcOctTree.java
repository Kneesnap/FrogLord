package net.highwayfrogs.editor.games.konami.greatquest.generic;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector3;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents OctTree data.
 * Created by Kneesnap on 4/20/2024.
 */
@Getter
@Setter
public class kcOctTree extends GameData<GreatQuestInstance> implements IMultiLineInfoWriter {
    private int maxDimension; // TODO: Figure this one out.
    private int maxDimensionE; // TODO: Figure this one out.
    private int maxResolution; // TODO: Figure this one out.
    private int maxResolutionE; // TODO: Figure this one out.
    private final kcVector3 offset;
    private int root;
    private int flags;
    private final List<kcOctBranch> branches = new ArrayList<>();
    private final List<kcOctLeaf> leaves = new ArrayList<>();
    private final List<kcQuadBranch> quadBranches = new ArrayList<>();

    private static final int RUNTIME_VALUE_COUNT = 6;

    public kcOctTree(GreatQuestInstance instance) {
        super(instance);
        this.offset = new kcVector3();
    }

    @Override
    public void load(DataReader reader) {
        this.maxDimension = reader.readInt();
        this.maxDimensionE = reader.readInt();
        this.maxResolution = reader.readInt();
        this.maxResolutionE = reader.readInt();
        reader.skipPointer(); // mpRaycastCallback
        int octantBranchCount = reader.readInt();
        reader.skipPointer(); // Runtime (octantBranchArray)
        int octantLeafCount = reader.readInt();
        reader.skipPointer(); // Runtime (octantLeafArray)
        int quadBranchCount = reader.readInt();
        reader.skipPointer(); // Runtime (quadBranchArray)
        this.root = reader.readInt();
        this.offset.load(reader);
        this.flags = reader.readInt();
        reader.skipBytes(RUNTIME_VALUE_COUNT * Constants.INTEGER_SIZE);

        // Alignment padding
        reader.alignRequireByte(GreatQuestInstance.PADDING_BYTE_DEFAULT, 16);

        // Read branches.
        this.branches.clear();
        for (int i = 0; i < octantBranchCount; i++) {
            kcOctBranch newBranch = new kcOctBranch(getGameInstance());
            newBranch.load(reader);
            this.branches.add(newBranch);
        }

        // Align to 16 byte boundary
        reader.alignRequireEmpty(16);

        // Read leaves.
        this.leaves.clear();
        for (int i = 0; i < octantLeafCount; i++) {
            kcOctLeaf newLeaf = new kcOctLeaf(getGameInstance());
            newLeaf.load(reader);
            this.leaves.add(newLeaf);
        }

        // Read quad branches.
        this.quadBranches.clear();
        for (int i = 0; i < quadBranchCount; i++) {
            kcQuadBranch newBranch = new kcQuadBranch(getGameInstance());
            newBranch.load(reader);
            this.quadBranches.add(newBranch);
        }
    }


    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.maxDimension);
        writer.writeInt(this.maxDimensionE);
        writer.writeInt(this.maxResolution);
        writer.writeInt(this.maxResolutionE);
        writer.writeNullPointer(); // mpRaycastCallback
        writer.writeInt(this.branches.size()); // octantBranchCount
        writer.writeNullPointer(); // Runtime (octantBranchArray)
        writer.writeInt(this.leaves.size()); // octantLeafCount
        writer.writeNullPointer(); // Runtime (octantLeafArray)
        writer.writeInt(this.quadBranches.size()); // quadBranchCount
        writer.writeNullPointer(); // Runtime (quadBranchArray)
        writer.writeInt(this.root);
        this.offset.save(writer);
        writer.writeInt(this.flags);
        writer.writeNull(RUNTIME_VALUE_COUNT * Constants.INTEGER_SIZE);

        // Alignment padding
        writer.align(16, GreatQuestInstance.PADDING_BYTE_DEFAULT);

        // Read branches.
        for (int i = 0; i < this.branches.size(); i++)
            this.branches.get(i).save(writer);

        // Align to 16 byte boundary
        writer.align(16);

        // Read leaves.
        for (int i = 0; i < this.leaves.size(); i++)
            this.leaves.get(i).save(writer);

        // Read quad branches.
        for (int i = 0; i < this.quadBranches.size(); i++)
            this.quadBranches.get(i).save(writer);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Max Dimension: ").append(this.maxDimension).append(Constants.NEWLINE);
        builder.append(padding).append("Max Dimension E: ").append(this.maxDimensionE).append(Constants.NEWLINE);
        builder.append(padding).append("Max Resolution: ").append(this.maxResolution).append(Constants.NEWLINE);
        builder.append(padding).append("Max Resolution E: ").append(this.maxResolutionE).append(Constants.NEWLINE);
        this.offset.writePrefixedInfoLine(builder, "Offset", padding);
        builder.append(padding).append("Root: ").append(this.root).append(Constants.NEWLINE);
        builder.append(padding).append("Flags: ").append(Utils.toHexString(this.flags)).append(Constants.NEWLINE);
        builder.append(padding).append("Branches: ").append(Utils.toHexString(this.branches.size())).append(Constants.NEWLINE);
        builder.append(padding).append("Leaves: ").append(Utils.toHexString(this.leaves.size())).append(Constants.NEWLINE);
        builder.append(padding).append("Quad Branches: ").append(Utils.toHexString(this.quadBranches.size())).append(Constants.NEWLINE);
    }

    @Getter
    public static class kcOctBranch extends GameData<GreatQuestInstance> {
        private final int[] childNumber = new int[8];
        private int parent;
        private byte childIndexFromParent;

        public kcOctBranch(GreatQuestInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            for (int i = 0; i < this.childNumber.length; i++)
                this.childNumber[i] = reader.readUnsignedShortAsInt();
            this.parent = reader.readUnsignedShortAsInt();
            reader.skipBytesRequireEmpty(1); // unused
            this.childIndexFromParent = reader.readByte();
        }

        @Override
        public void save(DataWriter writer) {
            for (int i = 0; i < this.childNumber.length; i++)
                writer.writeUnsignedShort(this.childNumber[i]);
            writer.writeUnsignedShort(this.parent);
            writer.writeByte(Constants.NULL_BYTE); // unused
            writer.writeByte(this.childIndexFromParent);
        }
    }

    @Getter
    @Setter
    public static class kcOctLeaf extends GameData<GreatQuestInstance> {
        private int nodeDimension;
        private int nodeX;
        private int nodeY;
        private int nodeZ;
        private final int[] sideNumber = new int[6];
        private int parent;
        private byte flags;
        private byte childIndexFromParent;
        private final byte[] context = new byte[8];

        public kcOctLeaf(GreatQuestInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            this.nodeDimension = reader.readUnsignedShortAsInt();
            this.nodeX = reader.readUnsignedShortAsInt();
            this.nodeY = reader.readUnsignedShortAsInt();
            this.nodeZ = reader.readUnsignedShortAsInt();
            for (int i = 0; i < this.sideNumber.length; i++)
                this.sideNumber[i] = reader.readUnsignedShortAsInt();
            this.parent = reader.readUnsignedShortAsInt();
            this.flags = reader.readByte();
            this.childIndexFromParent = reader.readByte();
            for (int i = 0; i < this.context.length; i++)
                this.context[i] = reader.readByte();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.nodeDimension);
            writer.writeUnsignedShort(this.nodeX);
            writer.writeUnsignedShort(this.nodeY);
            writer.writeUnsignedShort(this.nodeZ);
            for (int i = 0; i < this.sideNumber.length; i++)
                writer.writeUnsignedShort(this.sideNumber[i]);
            writer.writeUnsignedShort(this.parent);
            writer.writeByte(this.flags);
            writer.writeByte(this.childIndexFromParent);
            for (int i = 0; i < this.context.length; i++)
                writer.writeByte(this.context[i]);
        }
    }

    @Getter
    public static class kcQuadBranch extends GameData<GreatQuestInstance> {
        private final int[] childNodes = new int[4]; // TODO: This is a guess.

        public kcQuadBranch(GreatQuestInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            for (int i = 0; i < this.childNodes.length; i++)
                this.childNodes[i] = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            for (int i = 0; i < this.childNodes.length; i++)
                writer.writeUnsignedShort(this.childNodes[i]);
        }
    }
}