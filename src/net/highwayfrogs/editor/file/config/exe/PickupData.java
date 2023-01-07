package net.highwayfrogs.editor.file.config.exe;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents pickup data.
 * Created by Kneesnap on 3/26/2019.
 */
@Getter
public class PickupData extends GameObject {
    private int unknown1;
    private int unknown2;
    private final List<Long> imagePointers = new ArrayList<>();

    @Override
    public void load(DataReader reader) {
        this.unknown1 = reader.readInt();
        this.unknown2 = reader.readInt();

        if (getConfig().isAtOrBeforeBuild20()) {
            // TODO: Properly support the format.
            reader.skipBytes(Constants.INTEGER_SIZE - 1);
            short nextNum = reader.readUnsignedByteAsShort();
            while ((nextNum != 0x80)) {
                if (!reader.hasMore())
                    return;

                reader.skipBytes(Constants.INTEGER_SIZE - 1);
                nextNum = reader.readUnsignedByteAsShort();
            }
            reader.setIndex(reader.getIndex() - Constants.INTEGER_SIZE);
        }

        // Read image pointers.
        long imagePointer;
        while ((imagePointer = reader.readUnsignedIntAsLong()) != 0)
            imagePointers.add(imagePointer);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.unknown1);
        writer.writeInt(this.unknown2);
        for (long imagePointer : imagePointers)
            writer.writeUnsignedInt(imagePointer);
        writer.writeNullPointer();
    }
}