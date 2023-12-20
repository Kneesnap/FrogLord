package net.highwayfrogs.editor.games.sony.beastwars;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXClutColor;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.MainController;
import net.highwayfrogs.editor.gui.editor.TexController;
import net.highwayfrogs.editor.gui.texture.BufferedImageWrapper;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements support for a beast wars image file.
 * Created by Kneesnap on 9/13/2023.
 */
@Getter
public class BeastWarsTexFile extends SCGameFile<BeastWarsInstance> {
    private final List<BufferedImageWrapper> images = new ArrayList<>();

    public static final String SIGNATURE = "TDAT";
    public static final String PALETTE_SIGNATURE = "TPAL";
    public static final int TEXTURE_DIMENSION = 32;
    public static final int MAXIMUM_TEXTURE_COUNT = 255; // The maximum number of textures in the file. 255 is likely a hard limit because more may not fit in vram. It's either that or they wanted texture ids to fit in a single byte. (Perhaps 255 or 0 means "untextured")
    public static final int PALETTE_COLOR_COUNT = 16; // The number of colors per palette.

    public BeastWarsTexFile(BeastWarsInstance instance) {
        super(instance);
    }

    /**
     * Creates the texture map containing all the textures.
     */
    public BufferedImage createTextureMap() {
        TextureAtlas atlas = new SequentialTextureAtlas(64, 64, true);

        // Create texture atlas.
        atlas.startBulkOperations();
        for (int i = 0; i < this.images.size(); i++)
            atlas.addTexture(this.images.get(i));
        atlas.endBulkOperations();

        return atlas.getImage();
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);
        int imageDataByteCount = reader.readInt();

        this.images.clear();
        switch (getGameInstance().getPlatform()) {
            case WINDOWS:
                readPcImages(reader);
                break;
            case PLAYSTATION:
                readPsxImages(reader, reader.getIndex() + imageDataByteCount);
                break;
            default:
                throw new RuntimeException("Unsupported platform: " + getGameInstance().getPlatform());
        }

        // Make sure correct number of palettes loaded.
        if (this.images.size() > MAXIMUM_TEXTURE_COUNT)
            throw new RuntimeException("There were too many textures loaded from file. (" + this.images.size() + ")");

    }

    private void readPcImages(DataReader reader) {
        while (reader.hasMore()) {
            BufferedImage newImage = new BufferedImage(TEXTURE_DIMENSION, TEXTURE_DIMENSION, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < TEXTURE_DIMENSION; y++) {
                for (int x = 0; x < TEXTURE_DIMENSION; x++) {
                    byte alpha = reader.readByte();
                    byte blue = reader.readByte();
                    byte green = reader.readByte();
                    byte red = reader.readByte();
                    newImage.setRGB(x, y, Utils.toARGB(red, green, blue, alpha != 0 ? alpha : (byte) 0xFF));
                }
            }

            this.images.add(new BufferedImageWrapper(newImage));
        }
    }

    private void readPsxImages(DataReader reader, int palettePointer) {
        List<PSXClutColor[]> palettes = new ArrayList<>();
        reader.jumpTemp(palettePointer);
        reader.verifyString(PALETTE_SIGNATURE);
        int paletteDataSizeInBytes = reader.readInt();
        int paletteDataEndsAt = reader.getIndex() + paletteDataSizeInBytes;

        while (reader.getIndex() < paletteDataEndsAt) {
            PSXClutColor[] palette = new PSXClutColor[PALETTE_COLOR_COUNT];
            for (int i = 0; i < palette.length; i++) {
                palette[i] = new PSXClutColor();
                palette[i].load(reader);
            }

            palettes.add(palette);
        }
        reader.jumpReturn();

        // Read images.
        while (reader.getIndex() < palettePointer) {
            PSXClutColor[] palette = palettes.get(this.images.size());

            BufferedImage newImage = new BufferedImage(TEXTURE_DIMENSION, TEXTURE_DIMENSION, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < TEXTURE_DIMENSION; y++) {
                for (int x = 0; x < TEXTURE_DIMENSION; x++) {
                    short value = reader.readUnsignedByteAsShort();
                    int palIndex1 = (value & 0x0F);
                    int palIndex2 = (value >> 4);
                    // TODO: Wrong slightly.
                    newImage.setRGB(x++, y, palette[palIndex1].toFullARGB(false));
                    newImage.setRGB(x, y, palette[palIndex2].toFullARGB(false));
                }
            }

            this.images.add(new BufferedImageWrapper(newImage));
        }
    }

    @Override
    public void save(DataWriter writer) {
        if (this.images.size() > MAXIMUM_TEXTURE_COUNT)
            throw new RuntimeException("There are too many textures to save this file. (" + this.images.size() + ")");

        writer.writeStringBytes(SIGNATURE);
        int sizePtr = writer.writeNullPointer();

        switch (getGameInstance().getPlatform()) {
            case WINDOWS:
                writePcImages(writer);
                break;
            case PLAYSTATION:
                writePsxImages(writer);
                break;
            default:
                throw new RuntimeException("Unsupported platform: " + getGameInstance().getPlatform());
        }

        // Write size.
        writer.writeAddressAt(sizePtr, writer.getIndex() - sizePtr);
    }

    private void writePcImages(DataWriter writer) {
        for (int i = 0; i < this.images.size(); i++) {
            BufferedImage image = this.images.get(i).getImage();
            for (int y = 0; y < TEXTURE_DIMENSION; y++) {
                for (int x = 0; x < TEXTURE_DIMENSION; x++) {
                    int argbColor = image.getRGB(x, y);
                    byte alpha = Utils.getAlpha(argbColor);
                    writer.writeByte(alpha == (byte) 0xFF ? 0x00 : alpha);
                    writer.writeByte(Utils.getBlue(argbColor));
                    writer.writeByte(Utils.getGreen(argbColor));
                    writer.writeByte(Utils.getRed(argbColor));
                }
            }
        }
    }

    private void writePsxImages(DataWriter writer) {
        // TODO: !
    }

    /**
     * Creates a palette for the provided image.
     * @param image The image to create a 16bit palette from.
     * @return palette
     */
    public static PSXClutColor[] createPalette(BufferedImage image) {
        int id = 0;
        PSXClutColor[] palette = new PSXClutColor[PALETTE_COLOR_COUNT];

        for (int y = 0; y < TEXTURE_DIMENSION; y++) {
            for (int x = 0; x < TEXTURE_DIMENSION; x++) {
                int argbColor = image.getRGB(x, y);
                PSXClutColor clutColor = PSXClutColor.fromARGB(argbColor, false);
                if (Utils.indexOf(palette, clutColor) >= 0)
                    continue; // Already registered.

                if (id >= palette.length)
                    throw new RuntimeException("There were more than 16 unique colors in the image, but only 16 are supported.");

                palette[id++] = clutColor;
            }
        }

        return palette;
    }

    /**
     * Replaces an existing image with a new one.
     * @param oldImage The old image to replace.
     * @param newImage the new image to apply.
     */
    public void replaceImage(BufferedImage oldImage, BufferedImage newImage) {
        int index = this.images.indexOf(oldImage);
        if (index == -1)
            throw new RuntimeException("Couldn't find the old image to replace.");

        BufferedImage removedImage = setImage(index, newImage);
        if (removedImage != oldImage)
            throw new RuntimeException("The image we removed was not the one we intended to remove.");
    }

    /**
     * Set an image.
     * @param index The index of the image to apply.
     * @param image The image to apply. If null is provided, the image will be removed.
     * @return The previous image at the index, if it existed.
     */
    public BufferedImage setImage(int index, BufferedImage image) {
        if (index < 0 || index > this.images.size())
            throw new IndexOutOfBoundsException("The index " + index + " is not within the bounds of the image list.");

        if (image == null) { // Remove.
            if (index == this.images.size())
                throw new IndexOutOfBoundsException("The removal index " + index + " is not within the bounds of the image list.");

            return this.images.remove(index).getImage();
        }

        // Verify new image is ok.
        if (image.getWidth() != TEXTURE_DIMENSION || image.getHeight() != TEXTURE_DIMENSION)
            throw new RuntimeException("The new texture must be " + TEXTURE_DIMENSION + "x" + TEXTURE_DIMENSION + ", but was " + image.getWidth() + "x" + image.getHeight() + ".");

        if (getGameInstance().isPSX())
            createPalette(image); // If there are too many colors, it will throw an error.

        if (index == this.images.size()) {
            this.images.add(new BufferedImageWrapper(image));
            return null;
        } else {
            BufferedImageWrapper wrapper = this.images.get(index);
            BufferedImage oldImage = wrapper.getImage();
            wrapper.setImage(image);
            return oldImage;
        }
    }

    @Override
    public Image getIcon() {
        return VLOArchive.ICON;
    }

    @Override
    public Node makeEditor() {
        return loadEditor(new TexController(getGameInstance()), "tex", this);
    }

    @Override
    @SneakyThrows
    public void exportAlternateFormat(FileEntry fileEntry) {
        ImageIO.write(createTextureMap(), "png", new File(GUIMain.getWorkingDirectory(), Utils.stripExtension(fileEntry.getDisplayName()) + ".png"));
        System.out.println("Exported texture map Image.");
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        MainController.MAIN_WINDOW.openEditor(MainController.MAIN_WINDOW.getCurrentFilesList(), this);
        ((TexController) MainController.getCurrentController()).setParentWad(parent);
    }

    @Override
    public List<Tuple2<String, Object>> createPropertyList() {
        List<Tuple2<String, Object>> list = super.createPropertyList();
        list.add(new Tuple2<>("Image Count", this.images.size()));
        return list;
    }
}