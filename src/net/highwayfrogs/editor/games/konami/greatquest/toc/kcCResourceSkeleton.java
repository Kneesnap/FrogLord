package net.highwayfrogs.editor.games.konami.greatquest.toc;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.utils.Utils;

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
    }

    @Override
    public void save(DataWriter writer) {
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
        builder.append("kcCResourceSkeleton['").append(getName()).append("'/").append(Utils.to0PrefixedHexString(getHash())).append("]:").append(Constants.NEWLINE).append(padding);
        this.rootNode.writeMultiLineInfo(builder, padding + " ");
    }


    public static class kcNode extends GameData<GreatQuestInstance> implements IMultiLineInfoWriter {
        @Getter private final kcCResourceSkeleton skeleton;
        @Getter private final kcNode parent;
        @Getter private final List<kcNode> children = new ArrayList<>();
        @Getter private String name;
        @Getter private int tag;
        @Getter private int flags;
        @Getter private byte[] nodeData; // TODO: Parse this later.
        private transient int loadEndPosition = -1;

        public kcNode(kcCResourceSkeleton skeleton, kcNode parent) {
            super(skeleton.getGameInstance());
            this.skeleton = skeleton;
            this.parent = parent;
        }

        @Override
        public void load(DataReader reader) {
            int nodeBaseAddress = reader.getIndex();
            this.name = reader.readTerminatedStringOfLength(32);
            this.tag = reader.readInt();
            this.flags = reader.readInt();
            reader.skipPointer(); // Runtime pointer. (There does seem to be a pointer here, but I think it's overwritten at runtime)
            reader.skipPointer(); // Runtime pointer. (There does seem to be a pointer here, but I think it's overwritten at runtime)
            int childCount = reader.readInt();
            int nodeDataLength = reader.readInt();
            reader.skipInt(); // Pointer to the node data.
            this.nodeData = reader.readBytes(nodeDataLength);

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
            int nodeBaseAddress = writer.getIndex();
            writer.writeTerminatedStringOfLength(this.name, 32);
            writer.writeInt(this.tag);
            writer.writeInt(this.flags);
            writer.writeNullPointer(); // Runtime pointer. (There does seem to be a pointer here, but I think it's overwritten at runtime)
            writer.writeNullPointer(); // Runtime pointer. (There does seem to be a pointer here, but I think it's overwritten at runtime)
            writer.writeInt(this.children.size());
            writer.writeInt(this.nodeData != null ? this.nodeData.length : 0);
            writer.writeNullPointer(); // Pointer to the node data.
            if (this.nodeData != null)
                writer.writeBytes(this.nodeData);

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
            builder.append("'").append(this.name).append("'{Tag=").append(this.tag).append(",Flags=").append(this.flags).append(",Data=").append(this.nodeData.length).append(" Bytes Children=").append(this.children.size()).append('}');
            if (this.children.size() > 0) {
                builder.append(':');

                String newPadding = padding + " ";
                for (int i = 0; i < this.children.size(); i++) {
                    builder.append(Constants.NEWLINE).append(padding).append(' ');
                    this.children.get(i).writeMultiLineInfo(builder, newPadding);
                }
            }
        }
    }
}