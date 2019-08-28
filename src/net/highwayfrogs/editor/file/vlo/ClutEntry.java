package net.highwayfrogs.editor.file.vlo;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXClutColor;
import net.highwayfrogs.editor.file.standard.psx.PSXRect;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.image.BufferedImage;
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
    private transient int tempSaveColorsPointer;

    private static final int BYTE_SIZE = PSXRect.BYTE_SIZE + Constants.INTEGER_SIZE;

    @Override
    public void load(DataReader reader) {
        this.clutRect.load(reader);
        int clutOffset = reader.readInt();

        reader.jumpTemp(clutOffset);

        // Read clut.
        for (int i = 0; i < calculateColorCount(); i++) {
            PSXClutColor color = new PSXClutColor();
            color.load(reader);
            colors.add(color);
        }

        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        Utils.verify(getClutRect().getX() % 16 == 0, "Clut VRAM X must be a multiple of 16!"); // According to http://www.psxdev.net/forum/viewtopic.php?t=109
        Utils.verify(getClutRect().getY() >= 0 && getClutRect().getY() <= 511, "Invalid CLUT VRAM Y!");

        this.clutRect.save(writer);
        this.tempSaveColorsPointer = writer.writeNullPointer();
    }

    /**
     * Save color data.
     * @param writer The writer to save to.
     */
    public void saveExtra(DataWriter writer) {
        Utils.verify(calculateColorCount() == colors.size(), "CLUT Information says there should be %d colors, however we tried to save %d!", calculateColorCount(), colors.size());
        writer.writeAddressTo(this.tempSaveColorsPointer);
        this.tempSaveColorsPointer = 0;
        this.colors.forEach(color -> color.save(writer));
    }

    /**
     * Calculate the number of colors this entry holds.
     * @return colorCount
     */
    public int calculateColorCount() {
        return getClutRect().getWidth() * getClutRect().getHeight();
    }

    /**
     * Creates an image with clut colors.
     * @return clutImage
     */
    public BufferedImage makeImage() {
        BufferedImage clutImage = new BufferedImage(getClutRect().getWidth(), getClutRect().getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < getClutRect().getWidth(); x++)
            for (int y = 0; y < getClutRect().getHeight(); y++)
                clutImage.setRGB(x, y, getColors().get((y * getClutRect().getWidth()) + x).toBGRA());
        return clutImage;
    }
}