package net.highwayfrogs.editor.file.vlo;

import lombok.Getter;
import net.highwayfrogs.editor.file.standard.psx.PSXClutColor;
import net.highwayfrogs.editor.file.standard.psx.PSXRect;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MR_CLUTSETUP struct. Holds Clut information.
 * Created by Kneesnap on 8/30/2018.
 */
@Getter
public class ClutEntry extends SCSharedGameData {
    private final PSXRect clutRect = new PSXRect();
    private final List<PSXClutColor> colors = new ArrayList<>();
    private transient int tempColorsPointer = -1;

    public ClutEntry(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.clutRect.load(reader);
        this.tempColorsPointer = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        Utils.verify(getClutRect().getX() % 16 == 0, "Clut VRAM X must be a multiple of 16!"); // According to http://www.psxdev.net/forum/viewtopic.php?t=109
        Utils.verify(getClutRect().getY() >= 0 && getClutRect().getY() <= 511, "Invalid CLUT VRAM Y!");

        this.clutRect.save(writer);
        this.tempColorsPointer = writer.writeNullPointer();
    }

    /**
     * Load color data.
     * @param reader The reader to read from
     */
    int readClutColors(DataReader reader) {
        if (this.tempColorsPointer < 0)
            throw new RuntimeException("Cannot read ClutEntry data, the data pointer is invalid.");

        requireReaderIndex(reader, this.tempColorsPointer, "Expected CLUT color data");
        this.tempColorsPointer = -1;

        // Read clut.
        this.colors.clear();
        for (int i = 0; i < calculateColorCount(); i++) {
            PSXClutColor color = new PSXClutColor();
            color.load(reader);
            this.colors.add(color);
        }

        return reader.getIndex();
    }

    /**
     * Save color data.
     * @param writer The writer to save to
     */
    void writeClutColors(DataWriter writer) {
        if (this.tempColorsPointer < 0)
            throw new RuntimeException("Cannot write ClutEntry data, the data pointer is invalid.");

        Utils.verify(calculateColorCount() == this.colors.size(), "CLUT Information says there should be %d colors, however we tried to save %d!", calculateColorCount(), colors.size());
        writer.writeAddressTo(this.tempColorsPointer);
        this.tempColorsPointer = -1;
        for (int i = 0; i < this.colors.size(); i++)
            this.colors.get(i).save(writer);
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