package net.highwayfrogs.editor.games.konami.greatquest;

import javafx.scene.image.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestImageController;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Frogger - The Great Quest
 * File: .img
 * Contents: A single image.
 * The slowness comes from file reading. It may be faster once we're reading from the main game archive.
 * Seems to be compatible with both PC and PS2 releases.
 * Created by Kneesnap on 8/17/2019.
 */
@Getter
public class GreatQuestImageFile extends GreatQuestArchiveFile implements IFileExport, ITextureSource {
    private final List<Consumer<BufferedImage>> imageChangeListeners = new ArrayList<>();
    private boolean hasHeader;
    private BufferedImage image;

    private static final int HEADER_SIZE = 0x20;
    private static final byte TYPE_CODE_HAS_COLOR_TABLE = (byte) 1;
    private static final byte TYPE_CODE_NO_COLOR_TABLE = (byte) 2;
    private static final byte TYPE_CODE_GRAYSCALE_TABLE = (byte) 3;
    private static final int[] GRAYSCALE_COLOR_MODEL = new int[256];
    public static final Image IMAGE_ICON = loadIcon("image");

    public static final int SIGNATURE = 0x64474D49; // 'IMGd'
    public static final String SIGNATURE_STR = "IMGd"; // 'IMGd'

    public GreatQuestImageFile(GreatQuestInstance instance) {
        super(instance);
    }

    /**
     * Gets the format of the image.
     */
    public kcImageFormat getFormat() {
        return kcImageFormat.getFormatFromBufferedImage(this.image);
    }

    /**
     * Get the number of bits used by each pixel.
     */
    public int getBitsPerPixel() {
        return getFormat().getBitsPerPixel();
    }

    @Override
    public void load(DataReader reader) {
        int startIndex = reader.getIndex();
        this.hasHeader = (reader.readInt() == SIGNATURE);
        reader.skipInt(); // Skip read size.

        if (this.hasHeader) {
            int width = reader.readInt();
            int height = reader.readInt();
            int bitsPerPixel = reader.readInt();
            if (getGameInstance().isPC()) // TODO: Is this safe? Eg: If we do this, and then save files, will the game still load images?
                bitsPerPixel = 32;

            int mipLod = reader.readInt(); // Always 1?
            if (mipLod != 1)
                System.out.println("The image '" + getDebugName() + "'was read with an LOD of " + mipLod + ", but 1 was expected!");

            // TODO: Is it always 8 or 16 bytes here? Let's read the code to figure this poop out.

            // Read extra data at end of the header that we don't fully understand yet.
            int imageBytes = ((bitsPerPixel * width * height) / Constants.BITS_PER_BYTE);
            int skipData = reader.getRemaining() - imageBytes;
            if (skipData != 16) {
                System.out.println(" - The image " + getDebugName() + " skipped " + skipData + " bytes, with the image being " + imageBytes + " bytes...");
                // TODO: On PC this seems to include two images. But, I suspect there's more to it, as skycloud.img doesn't look right.
                // TODO: This only triggers for 26 bytes on PS2 version.
            }

            reader.skipBytes(skipData);

            // Load image.
            loadImage(reader, width, height, bitsPerPixel, null);
        } else {
            reader.setIndex(startIndex);
            kcLoad8BitImageHeader(reader);
        }

        if (reader.getRemaining() > 0) // Test we're not skipping any data.
            System.out.println(" - The image '" + getDebugName() + "' has " + reader.getRemaining() + " unread bytes.");
    }

    private void kcLoad8BitImageHeader(DataReader reader) {
        byte idSize = reader.readByte();
        byte colMapType = reader.readByte();
        byte typeCode = reader.readByte();
        byte[] colMap = reader.readBytes(5);
        short xOrigin = reader.readShort();
        short yOrigin = reader.readShort();
        short width = reader.readShort();
        short height = reader.readShort();
        byte bitsPerPixel = reader.readByte();
        byte descriptor = reader.readByte();

        /*System.out.println("IMG [" + getDebugName() + ", " + width + "x" + height + "]: idsize=" + idSize + ", colMapType=" + colMapType + ", typeCode=" + typeCode
                + ", colMap=" + Utils.toByteString(colMap) + ", origin=[" + xOrigin + "," + yOrigin + "], bpp=" + bitsPerPixel + ", Descriptor=" + descriptor);*/

        int paletteSizeInBytes = 0;
        int imageSizeInBytes = (bitsPerPixel * width * height) / Constants.BITS_PER_BYTE;
        if (typeCode == TYPE_CODE_HAS_COLOR_TABLE) {
            if (colMap[4] == 24 && bitsPerPixel == 8) {
                paletteSizeInBytes = 0x300;
            } else if (colMap[4] == 32 && bitsPerPixel == 8) {
                paletteSizeInBytes = 0x400;
            } else {
                throw new RuntimeException("Encountered situation for image '" + getDebugName() + "' with unknown image parameters: " + colMap[4] + ", " + bitsPerPixel);
            }
        }

        byte[] bitmapData = reader.readBytes(imageSizeInBytes + paletteSizeInBytes); // Aligned by 0x40?

        int max = 0;
        if (typeCode == TYPE_CODE_NO_COLOR_TABLE) {
            // Each pixel has a color value, so apply the fix to all the remaining data.
            max = imageSizeInBytes / (bitsPerPixel == 24 ? 3 : 4);
        } else if (typeCode == TYPE_CODE_HAS_COLOR_TABLE) {
            // Each pixel is an index into the palette / color lookup table, which is hardcoded as having 256 colors.
            // Only read up til there.
            max = 256;
        }

        if ((typeCode == TYPE_CODE_NO_COLOR_TABLE) && (bitsPerPixel == 32)) {
            for (int index = 0, i = 0; i < max; i++, index += 4) {
                byte temp = bitmapData[index];
                bitmapData[index] = bitmapData[index + 2];
                bitmapData[index + 2] = temp;
                if (bitmapData[index + 3] != 0)
                    bitmapData[index + 3] >>= 1;
            }
        } else {
            int bytesPerPixel = 3;
            if (typeCode != TYPE_CODE_NO_COLOR_TABLE && colMap[4] == 32)
                bytesPerPixel = 4;

            for (int index = 0, i = 0; i < max; i++, index += bytesPerPixel) {
                byte temp = bitmapData[index];
                bitmapData[index] = bitmapData[index + 2];
                bitmapData[index + 2] = temp;
            }
        }

        // Setup palette.
        IndexColorModel colorPalette = null;
        if (typeCode == TYPE_CODE_HAS_COLOR_TABLE) {
            // Separate color lookup table from pixel data.
            byte[] paletteBytes = new byte[paletteSizeInBytes];
            System.arraycopy(bitmapData, 0, paletteBytes, 0, paletteBytes.length);
            byte[] pixelData = new byte[bitmapData.length - paletteSizeInBytes];
            System.arraycopy(bitmapData, paletteSizeInBytes, pixelData, 0, pixelData.length);
            bitmapData = pixelData;

            // Setup palette.
            colorPalette = new IndexColorModel(bitsPerPixel, 256, paletteBytes, 0, (colMap[4] == 32));
        } else if (typeCode == TYPE_CODE_GRAYSCALE_TABLE) {
            colorPalette = new IndexColorModel(bitsPerPixel, GRAYSCALE_COLOR_MODEL.length, GRAYSCALE_COLOR_MODEL, 0, false, -1, DataBuffer.TYPE_BYTE);
        }

        DataReader newDataReader = new DataReader(new ArraySource(bitmapData));
        loadImage(newDataReader, width, height, bitsPerPixel, colorPalette);
    }

    private void loadImage(DataReader reader, int width, int height, int bitsPerPixel, IndexColorModel colorModel) {
        kcImageFormat format = kcImageFormat.getFormatFromBitsPerPixel(bitsPerPixel);

        if (format == kcImageFormat.INDEXED8) {
            if (colorModel == null)
                throw new RuntimeException("The image format for " + getDebugName() + " was " + format + ", but there was no color lookup table.");
            this.image = new BufferedImage(width, height, format.getBufferedImageType(), colorModel);
        } else {
            if (colorModel != null)
                throw new RuntimeException("The image format for " + getDebugName() + " was " + format + ", but there was a color lookup table??");

            this.image = new BufferedImage(width, height, format.getBufferedImageType());
        }

        switch (format) {
            case A8R8G8B8:
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        this.image.setRGB(x, height - y - 1, reader.readInt());
                break;
            case R8G8B8:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        byte red = reader.readByte();
                        byte green = reader.readByte();
                        byte blue = reader.readByte();
                        this.image.setRGB(x, height - y - 1, Utils.toRGB(red, green, blue));
                    }
                }
                break;
            case INDEXED8:
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        this.image.setRGB(x, height - y - 1, colorModel.getRGB(reader.readByte()));
                break;
            default:
                throw new RuntimeException("Cannot load unsupported from image format " + format + ".");
        }

    }

    @Override
    public void save(DataWriter writer) {
        // TODO: Verify this method works.
        int headerSizeAddress = -1;

        if (this.hasHeader) {
            writer.writeInt(SIGNATURE);
            headerSizeAddress = writer.writeNullPointer();
            writer.writeInt(this.image.getWidth());
            writer.writeInt(this.image.getHeight());
            writer.writeInt(getBitsPerPixel());
            writer.writeInt(1); // Always 1.
            // TODO: May need to write additional data.
        } else {
            // TODO: Write 8 bit header.
            throw new RuntimeException("We don't yet support writing this kind of image for '" + getDebugName() + "'.");
        }

        // TODO: Save image function call.

        if (this.hasHeader)
            writer.writeAddressAt(headerSizeAddress, writer.getIndex() - headerSizeAddress - Constants.INTEGER_SIZE);
    }
    @Override
    public String getExtension() {
        return "img";
    }

    /**
     * Exports this image to a file, as a png.
     * @param saveTo The file to save the image to.
     */
    public void saveImageToFile(File saveTo) throws IOException {
        ImageIO.write(this.image, "png", saveTo);
    }

    @Override
    public String getDefaultFolderName() {
        return "Images";
    }

    @Override
    public void exportToFolder(File folder) throws IOException {
        File imageFile = new File(folder, Utils.stripExtension(getExportName()) + ".png");
        if (!imageFile.exists())
            saveImageToFile(imageFile);
    }

    @Override
    public Image getCollectionViewIcon() {
        return IMAGE_ICON;
    }

    @Override
    public GreatQuestImageController makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-img", new GreatQuestImageController(getGameInstance()), this);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Image Dimensions", getWidth() + " x " + getHeight());
        propertyList.add("Has Header?", this.hasHeader);
        return propertyList;
    }

    @Override
    public BufferedImage makeImage() {
        return this.image;
    }

    @Override
    public int getWidth() {
        return this.image != null ? this.image.getWidth() : 0;
    }

    @Override
    public int getHeight() {
        return this.image != null ? this.image.getHeight() : 0;
    }

    @Override
    public int getUpPadding() {
        return 0;
    }

    @Override
    public int getDownPadding() {
        return 0;
    }

    @Override
    public int getLeftPadding() {
        return 0;
    }

    @Override
    public int getRightPadding() {
        return 0;
    }

    @Override
    public void fireChangeEvent(BufferedImage newImage) {
        fireChangeEvent0(newImage);
    }

    /**
     * Registry of the different supported image formats.
     * Created from 'kcFormat', but only including supported image formats.
     */
    @Getter
    @AllArgsConstructor
    public enum kcImageFormat {
        R8G8B8((byte) 0x14, 24, BufferedImage.TYPE_INT_RGB),
        A8R8G8B8((byte) 0x15, 32, BufferedImage.TYPE_INT_ARGB),
        INDEXED8((byte) 0x67, 8, BufferedImage.TYPE_BYTE_INDEXED);

        private final byte typeCode;
        private final int bitsPerPixel;
        private final int bufferedImageType;

        /**
         * Gets the format from the bits per pixel, as seen in 'kcTextureCreateFromImages' from the PS2 PAL version.
         * @param bitsPerPixel The number of bits per pixel.
         * @return imageFormat
         */
        public static kcImageFormat getFormatFromBitsPerPixel(int bitsPerPixel) {
            switch (bitsPerPixel) {
                case 32:
                    return kcImageFormat.A8R8G8B8;
                case 24:
                    return kcImageFormat.R8G8B8;
                case 8:
                    return kcImageFormat.INDEXED8;
                default:
                    // PS2 PAL will also fail in these situations.
                    throw new RuntimeException("Images with " + bitsPerPixel + " bits per pixel are not supported by kcGameSystem.");
            }
        }

        /**
         * Gets the kcImageFormat from the image's format.
         * @param image The image to determine the format from.
         * @return imageFormat
         */
        public static kcImageFormat getFormatFromBufferedImage(BufferedImage image) {
            switch (image.getType()) {
                case BufferedImage.TYPE_INT_ARGB:
                    return kcImageFormat.A8R8G8B8;
                case BufferedImage.TYPE_INT_RGB:
                    return kcImageFormat.R8G8B8;
                case BufferedImage.TYPE_BYTE_INDEXED:
                    return kcImageFormat.INDEXED8;
                default:
                    throw new RuntimeException("Images of format type " + image.getType() + " do not have a corresponding/valid kcFormatImage type.");
            }
        }
    }

    static {
        for (int i = 0; i < GRAYSCALE_COLOR_MODEL.length; i++)
            GRAYSCALE_COLOR_MODEL[i] = (i << 16) | (i << 8) | i;
    }
}