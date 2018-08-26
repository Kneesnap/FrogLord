package net.highwayfrogs.editor.file.mof.animation;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "MR_ANIM_CEL_SET" struct.
 * Created by Kneesnap on 8/25/2018.
 */
public class MOFAnimationCelSet extends GameObject {
    private List<MOFAnimationCelPart> parts = new ArrayList<>();

    @Override
    public void load(DataReader reader) {
        short count = reader.readShort();
        reader.readShort(); // Padding.

        for (int i = 0; i < count; i++) {
            MOFAnimationCelPart part = new MOFAnimationCelPart();
            part.load(reader);
            parts.add(part);
        }

        //TODO
    }

    @Override
    public void save(DataWriter writer) {
        //TODO
    }
}
