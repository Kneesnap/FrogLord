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

        if (getConfig().isAtOrBeforeBuild30()) {
            // TODO: Properly support the format. (This doesn't work on PC...)
            long nextTexture = reader.readUnsignedIntAsLong();
            while (reader.hasMore() && !getConfig().getBmpTexturePointers().contains(nextTexture))
                nextTexture = reader.readUnsignedIntAsLong();

            reader.setIndex(reader.getIndex() - Constants.INTEGER_SIZE);
        }

        // Read image pointers.
        long imagePointer;
        while (reader.hasMore() && (imagePointer = reader.readUnsignedIntAsLong()) != 0)
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