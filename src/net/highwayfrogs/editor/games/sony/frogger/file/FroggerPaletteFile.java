package net.highwayfrogs.editor.games.sony.frogger.file;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.ui.PaletteController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.data.writer.FileReceiver;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Palette file. PC-Only file. Appears to not actually do anything though. Loading corrupted palettes doesn't seem to break anything, and they are not used on the PSX version. I have tested in both low poly mode and high poly mode.
 * Appears this might be "Windows 3.0 logical palette" or "LOGPALETTE"
 * Created by Kneesnap on 8/14/2018.
 */
@Getter
public class FroggerPaletteFile extends SCSharedGameFile {
    private final List<Color> colors = new ArrayList<>();

    private static final String RIFF_SIGNATURE = "RIFF";
    private static final String PAL_SIGNATURE = "PAL ";
    private static final String DATA_HEADER = "data";
    private static final short PAL_VERSION = 0x300;
    private static final byte FLAG = 0x00;
    private static final int COLOR_SIZE = 4;

    public FroggerPaletteFile(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(RIFF_SIGNATURE);
        reader.skipInt(); // File size. Does not include marker.
        reader.verifyString(PAL_SIGNATURE);
        reader.verifyString(DATA_HEADER);
        reader.skipInt(); // Data chunk size. Excludes the header.
        Utils.verify(reader.readShort() == PAL_VERSION, "Unknown PAL version.");

        short colorCount = reader.readShort();
        for (int i = 0; i < colorCount; i++) {
            byte red = reader.readByte();
            byte green = reader.readByte();
            byte blue = reader.readByte();

            int flag = reader.readUnsignedByte();
            if (flag != FLAG)
                throw new RuntimeException("Unknown flag value: " + flag);

            getColors().add(ColorUtils.fromRGB(ColorUtils.toRGB(red, green, blue)));
        }

        if (reader.hasMore())
            reader.skipBytes(Constants.INTEGER_SIZE);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(RIFF_SIGNATURE);

        int dataChunkSize = getColors().size() * COLOR_SIZE + (2 * Constants.INTEGER_SIZE);
        int fullSize = dataChunkSize + DATA_HEADER.length() + PAL_SIGNATURE.length() + Constants.INTEGER_SIZE + RIFF_SIGNATURE.length();
        writer.writeInt(fullSize);

        writer.writeStringBytes(PAL_SIGNATURE);
        writer.writeStringBytes(DATA_HEADER);
        writer.writeInt(dataChunkSize);
        writer.writeShort(PAL_VERSION);
        writer.writeShort((short) getColors().size());

        for (Color color : colors) {
            int intColor = ColorUtils.toRGB(color);
            writer.writeByte(ColorUtils.getRed(intColor));
            writer.writeByte(ColorUtils.getGreen(intColor));
            writer.writeByte(ColorUtils.getBlue(intColor));
            writer.writeByte(FLAG);
        }

        writer.writeNull(Constants.INTEGER_SIZE); // Unsure what this serves, but it's there.
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.PAINTERS_PALETTE_32.getFxImage();
    }

    @Override
    public PaletteController makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-pal", new PaletteController(getGameInstance()), this);
    }

    /**
     * Makes a full palette image.
     * @return image
     */
    public Image makeImage(int imageSize) {
        double sqrt = Math.sqrt(this.colors.size());
        int colorsPerLine = (int) sqrt;
        int colorSize = (int) (imageSize / sqrt);

        BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        for (int x = 0; x < colorsPerLine; x++) {
            for (int y = 0; y < colorsPerLine; y++) {
                int index = (y * colorsPerLine) + x;
                if (getColors().size() > index)
                    graphics.setColor(ColorUtils.toAWTColor(getColors().get(index)));
                graphics.fillRect(x * colorSize, y * colorSize, colorSize, colorSize);
            }
        }

        graphics.dispose();

        return FXUtils.toFXImage(image, false);
    }

    /**
     * Save palette data to an .act file for processing in Photoshop.
     */
    @Override
    public void exportAlternateFormat() {
        File file = FXUtils.promptFileSave(getGameInstance(), "Save the Color Palette.", FileUtils.stripExtension(getFileDisplayName()), "ACT File", "act");
        if (file != null) {
            final int redMask = 0xFF0000, greenMask = 0xFF00, blueMask = 0xFF;

            DataWriter writer = new DataWriter(new FileReceiver(file));
            for (Color color : colors) {
                final int intColor = ColorUtils.toRGB(color);
                writer.writeByte((byte)((intColor & redMask) >> 16));
                writer.writeByte((byte)((intColor & greenMask) >> 8));
                writer.writeByte((byte)(intColor & blueMask));
            }
            writer.closeReceiver();

            System.out.println("Exported PAL file to '" + file.getName() + "'.");
        }
        else {
            System.out.println("Aborted export of PAL file.");
        }
    }
}