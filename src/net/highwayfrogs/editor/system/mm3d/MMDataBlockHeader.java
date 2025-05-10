package net.highwayfrogs.editor.system.mm3d;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Type B data block.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMDataBlockHeader<T extends MMDataBlockBody> implements IBinarySerializable {
    private final List<T> blocks = new ArrayList<>();
    private int invalidBodies;

    private transient final OffsetType offsetType;
    private transient final MisfitModel3DObject parent;

    private static final short FLAGS = 0x00;

    public MMDataBlockHeader(OffsetType type, MisfitModel3DObject parent) {
        this.offsetType = type;
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        reader.skipShort();
        long elementCount = reader.readUnsignedIntAsLong();

        if (getOffsetType().isTypeA()) {
            for (int i = 0; i < elementCount; i++) {
                int elementSize = reader.readInt(); // Keep this.
                int readGoal = (reader.getIndex() + elementSize);
                T body = addNewElement();
                body.load(reader);
                if (reader.getIndex() != readGoal) { // This happens with EXTERNAL_TEXTURES on mm3d v1.3.12, it saves invalid data. However, it can be safely ignored.
                    System.out.println("[A/" + i + "/" + this.blocks.size() + "] " + getOffsetType() + ": Expected " + readGoal + ", Actual: " + reader.getIndex() + ", (" + elementSize + ", " + elementCount + ")");
                    getBlocks().remove(body); // It's invalid.
                    this.invalidBodies++;
                }
            }
        } else if (getOffsetType().isTypeB()) {
            int elementSize = reader.readInt(); // Keep this.
            for (int i = 0; i < elementCount; i++) {
                int readGoal = (reader.getIndex() + elementSize);
                T body = addNewElement();
                body.load(reader);
                if (reader.getIndex() != readGoal) {
                    System.out.println("[B/" + i + "/" + this.blocks.size() + "] " + getOffsetType() + ": Expected " + readGoal + ", Actual: " + reader.getIndex() + ", (" + elementCount + ")");
                    getBlocks().remove(body); // It's invalid.
                    this.invalidBodies++;
                }
            }
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(FLAGS);
        writer.writeUnsignedInt(blocks.size() + this.invalidBodies);

        if (getOffsetType().isTypeA()) {
            for (MMDataBlockBody body : blocks) {
                int writeSizeTo = writer.writeNullPointer();
                int structStart = writer.getIndex();
                body.save(writer);
                writer.writeIntAtPos(writeSizeTo, (writer.getIndex() - structStart));
            }
        } else if (getOffsetType().isTypeB()) {
            int writeSizeTo = writer.writeNullPointer();
            int structStart = writer.getIndex();
            this.blocks.forEach(block -> block.save(writer));
            writer.writeIntAtPos(writeSizeTo, (writer.getIndex() - structStart) / this.blocks.size());
        }
    }

    /**
     * Get the amount of blocks this is holding on to.
     * @return size
     */
    public int size() {
        return getBlocks().size();
    }

    /**
     * Add a new element to this header.
     * @return newElement
     */
    @SuppressWarnings("unchecked")
    public T addNewElement() {
        T newElement = (T) getOffsetType().makeNew(getParent());
        getBlocks().add(newElement);
        return newElement;
    }

    /**
     * Gets the body at the given index.
     * @param index The index to get the body for.
     * @return body or null
     */
    public T getBody(int index) {
        return index >= 0 && size() > index ? getBlocks().get(index) : null;
    }
}
