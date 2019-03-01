package net.highwayfrogs.editor.system.mm3d;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Type B data block.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMDataBlockHeader<T extends MMDataBlockBody> extends GameObject {
    private List<T> dataBlockBodies = new ArrayList<>();
    private int invalidBodies;

    private transient OffsetType offsetType;
    private transient MisfitModel3DObject parent;

    private static final short FLAGS = 0x00;

    public MMDataBlockHeader(OffsetType type, MisfitModel3DObject parent) {
        this.offsetType = type;
        this.parent = parent;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load(DataReader reader) {
        reader.skipShort();
        long elementCount = reader.readUnsignedIntAsLong();

        if (getOffsetType().isTypeA()) {
            for (int i = 0; i < elementCount; i++) {
                int elementSize = reader.readInt(); // Keep this.
                int readGoal = (reader.getIndex() + elementSize);
                MMDataBlockBody body = getOffsetType().makeNew(getParent());
                body.load(reader);
                if (reader.getIndex() != readGoal) {
                    System.out.println("[A/" + this.dataBlockBodies.size() + "] " + getOffsetType() + ": Expected " + readGoal + ", Actual: " + reader.getIndex() + ", (" + elementSize + ", " + elementCount + ")");
                    this.invalidBodies++;
                    continue;
                }
                this.dataBlockBodies.add((T) body);
            }
        } else if (getOffsetType().isTypeB()) {
            int elementSize = reader.readInt(); // Keep this.
            for (int i = 0; i < elementCount; i++) {
                int readGoal = (reader.getIndex() + elementSize);
                MMDataBlockBody body = getOffsetType().makeNew(getParent());
                body.load(reader);
                if (reader.getIndex() != readGoal) {
                    System.out.println("[B/" + this.dataBlockBodies.size() + "] " + getOffsetType() + ": Expected " + readGoal + ", Actual: " + reader.getIndex() + ", (" + elementCount + ")");
                    this.invalidBodies++;
                    continue;
                }
                this.dataBlockBodies.add((T) body);
            }
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(FLAGS);
        writer.writeUnsignedInt(dataBlockBodies.size() + this.invalidBodies);

        if (getOffsetType().isTypeA()) {
            for (MMDataBlockBody body : dataBlockBodies) {
                int writeSizeTo = writer.writeNullPointer();
                int structStart = writer.getIndex();
                body.save(writer);
                writer.writeAddressAt(writeSizeTo, (writer.getIndex() - structStart));
            }
        } else if (getOffsetType().isTypeB()) {
            int writeSizeTo = writer.writeNullPointer();
            int structStart = writer.getIndex();
            this.dataBlockBodies.forEach(block -> block.save(writer));
            writer.writeAddressAt(writeSizeTo, (writer.getIndex() - structStart) / this.dataBlockBodies.size());
        }
    }

    /**
     * Get the amount of blocks this is holding on to.
     * @return size
     */
    public int size() {
        return getDataBlockBodies().size();
    }
}
