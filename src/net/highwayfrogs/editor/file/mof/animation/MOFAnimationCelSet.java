package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "MR_ANIM_CEL_SET" struct.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimationCelSet extends GameObject {
    private List<MOFAnimationCels> parts = new ArrayList<>();
    private transient int dataPointer;

    @Override
    public void load(DataReader reader) {
        this.dataPointer = reader.getIndex();

        int count = reader.readUnsignedShortAsInt();
        reader.readShort(); // Padding.

        reader.setIndex(reader.readInt()); // Points to literally the exact index after reading.
        for (int i = 0; i < count; i++) {
            MOFAnimationCels part = new MOFAnimationCels();
            part.load(reader);
            parts.add(part);
        }
    }

    @Override
    public void save(DataWriter writer) {
        this.dataPointer = writer.getIndex();

        writer.writeUnsignedShort(parts.size());
        writer.writeShort((short) 0); // Padding
        writer.writeInt(writer.getIndex() + Constants.POINTER_SIZE);
        parts.forEach(part -> part.save(writer));
    }
}
