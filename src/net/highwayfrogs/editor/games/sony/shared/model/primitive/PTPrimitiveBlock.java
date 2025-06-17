package net.highwayfrogs.editor.games.sony.shared.model.primitive;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a block of draw primitives.
 * Created by Kneesnap on 5/17/2024.
 */
@Getter
public class PTPrimitiveBlock extends SCSharedGameData {
    private PTPrimitiveType primitiveType;
    private short flags;
    private final List<IPTPrimitive> primitives = new ArrayList<>();

    private static final int HEADER_SIZE = 4 * Constants.SHORT_SIZE;

    public PTPrimitiveBlock(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        int readStartIndex = reader.getIndex();

        this.primitiveType = PTPrimitiveType.values()[reader.readUnsignedShortAsInt()];
        int primitiveCount = reader.readUnsignedShortAsInt();
        this.flags = reader.readShort();
        int blockSize = reader.readUnsignedShortAsInt();

        // Read primitives.
        this.primitives.clear();
        for (int i = 0; i < primitiveCount; i++) {
            IPTPrimitive newPrimitive = this.primitiveType.newPrimitive(getGameInstance());
            newPrimitive.load(reader);
            this.primitives.add(newPrimitive);
        }

        // Validate final.
        int calculatedBlockSize = calculateBlockSize();
        if (calculatedBlockSize != blockSize)
            getLogger().warning("Calculated the PTPrimitiveBlock as having %d bytes, but the actual amount was %d.", calculatedBlockSize, blockSize);
        int readSize = reader.getIndex() - readStartIndex;
        if (readSize != blockSize)
            getLogger().warning("Read %d byte(s) for a(n) %s PTPrimitiveBlock when there were actually %d bytes.", readSize, this.primitiveType, blockSize);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.primitiveType.ordinal());
        writer.writeUnsignedShort(this.primitives.size());
        writer.writeShort(this.flags);
        writer.writeUnsignedShort(calculateBlockSize());

        // Write primitives.
        for (int i = 0; i < this.primitives.size(); i++)
            this.primitives.get(i).save(writer);
    }

    /**
     * Calculate the size in bytes of the full primitive block.
     * @return blockSizeInBytes
     */
    public int calculateBlockSize() {
        return HEADER_SIZE + (this.primitives.size() * this.primitiveType.getSizeInBytes());
    }
}