package net.highwayfrogs.editor.file.vlo;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXClutColor;
import net.highwayfrogs.editor.file.standard.psx.PSXRect;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MR_CLUTSETUP struct. Holds Clut information.
 * Created by Kneesnap on 8/30/2018.
 */
@Getter
public class ClutEntry extends GameObject {
    private PSXRect clutRect = new PSXRect();
    private List<PSXClutColor> colors = new ArrayList<>();
    private int suppliedClutOffset = 0;

    public static final int BYTE_SIZE = PSXRect.BYTE_SIZE + Constants.INTEGER_SIZE;

    @Override
    public void load(DataReader reader) {
        this.clutRect.load(reader);
        int clutOffset = reader.readInt();

        reader.jumpTemp(clutOffset);

        // Read clut.
        int colorCount = getClutRect().getWidth() * getClutRect().getHeight();
        for (int i = 0; i < colorCount; i++) {
            PSXClutColor color = new PSXClutColor();
            color.load(reader);
            colors.add(color);
        }

        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        Utils.verify(suppliedClutOffset != 0, "Clut offset was not supplied!"); // Call save(DataWriter, int) instead.
        Utils.verify(getClutRect().getX() % 16 == 0, "Clut VRAM X must be a multiple of 16!"); // According to http://www.psxdev.net/forum/viewtopic.php?t=109
        Utils.verify(getClutRect().getY() >= 0 && getClutRect().getY() <= 511, "Invalid CLUT VRAM Y!");

        this.clutRect.save(writer);
        writer.writeInt(suppliedClutOffset);

        //TODO: Write CLUT data at the right offset. [Goes after txsetup data.]
    }

    public int save(DataWriter writer, int clutOffset) {
        this.suppliedClutOffset = clutOffset;
        save(writer);
        this.suppliedClutOffset = 0;
        return getClutRect().getWidth() * getClutRect().getHeight() * PSXClutColor.BYTE_SIZE;
    }
}