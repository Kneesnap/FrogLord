package net.highwayfrogs.editor.games.konami.greatquest.file;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.generic.GamePlatform;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IFileExport;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestImageController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.ArrayReceiver;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.image.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Frogger - The Great Quest Image Files
 * These files are labelled .img by the game, but on closer inspection, this file actually has two different file formats.
 * One format is a proprietary engine format, the other is .TGA. <a href="https://en.wikipedia.org/wiki/Truevision_TGA"/>.
 * This is confirmed by the "TRUEVISION-XFILE" signature in some of the files.
 * The proprietary format appears to be used when the game has a non-indexed image, while .tga is used when the game wants to use an indexed image.
 * As the PC version does not use indexed colors in any of its images, the PC version exclusively uses the proprietary engine format.
 * Created by Kneesnap on 8/17/2019.
 */
@Getter
public class GreatQuestImageFile extends GreatQuestArchiveFile implements IFileExport, ITextureSource {
    private final List<Consumer<BufferedImage>> imageChangeListeners = new ArrayList<>();
    // Always true for PC. I believe images where this is false are likely some kind of legacy format, since the PC version came afterward without them.
    // Also, the game has no code to save this format, only code to read it.
    // It's also possible saving this format was exclusive to the debug build, but I am not certain.
    @NonNull private GreatQuestImageFile.GreatQuestImageFileFormat fileFormat = GreatQuestImageFileFormat.ENGINE;
    @NonNull private BufferedImage image = DEFAULT_IMAGE;

    private static final int HEADER_SIZE = 0x20;
    private static final byte TYPE_CODE_HAS_COLOR_TABLE = (byte) 1;
    private static final byte TYPE_CODE_NO_COLOR_TABLE = (byte) 2;
    private static final byte TYPE_CODE_GRAYSCALE_TABLE = (byte) 3; // We don't really support saving this, as it's extremely uncommon. Only appearing once in PS2 NTSC, and none on PC. We do support reading it though.
    private static final int[] GRAYSCALE_COLOR_MODEL = new int[256];
    private static final int EXPECTED_MIP_LOD = 1; // kcImageLoad8BitHeader() sets mipLod to 1 regardless.

    public static final int SIGNATURE = 0x64474D49; // 'IMGd'
    public static final String SIGNATURE_STR = "IMGd"; // 'IMGd'
    private static final String TGA_FILE_SIGNATURE = "\0\0\0\0\0\0\0\0TRUEVISION-XFILE.\0";
    private static final BufferedImage DEFAULT_IMAGE = UnknownTextureSource.LIGHT_PURPLE_INSTANCE.makeImage();

    // The PC version has images which appear to have data allocated beyond the end of the "official" image.
    // Eg: The image files used to be larger, but then smaller images were imported, and the size of the files were never reduced.
    // So, to avoid warnings for these files, we've included the names of impacted files.
    private static final List<String> PC_IGNORED_IMAGES_WITH_EXTRA_DATA = Arrays.asList("skycloud.img", "stmcloud.img", "ground04.img", "momring.img", "firems01.img");

    public GreatQuestImageFile(GreatQuestInstance instance) {
        super(instance, GreatQuestArchiveFileType.IMAGE);
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
        // kcCResourceTexture::Prepare() -> -> Chunk file texture loading.
        // LoadInterfaceTexture() -> kcImportTextureDynamic() -> CreateTextureFromStream() -> Hardcoded menu textures.
        // kcImportMaterialTexture() -> kcImportTexture() -> CreateTextureFromStream()
        //  -> Appends ".img" to the texName of a material, then tries to find the resource .
        //  -> If it's not found, it will run kcImportTexture() to create a new texture reference chunk.

        // CreateTextureFromStream()
        int startIndex = reader.getIndex();
        this.fileFormat = (reader.readInt() == SIGNATURE) ? GreatQuestImageFileFormat.ENGINE : GreatQuestImageFileFormat.TGA;
        reader.skipInt(); // Skip read size.

        if (this.fileFormat == GreatQuestImageFileFormat.ENGINE) {
            int width = reader.readInt();
            int height = reader.readInt();
            int bitsPerPixel = reader.readInt();
            if (getGameInstance().isPC() && bitsPerPixel != 32) {
                if ("fuelIcon_old.img".equalsIgnoreCase(getFileName())) {
                    bitsPerPixel = 32; // The PC version has a broken image. Every single other image in the game uses 32 BPP, and processing this image as 32 BPP does seem to mostly render correctly, so likely this was some kind of bugged export. To avoid FrogLord errors, we'll just treat it as 32BPP.
                } else {
                    getLogger().warning("The image reported as having %d bits per pixel, but the PC version only supports 32 bits per pixel!", bitsPerPixel);
                }
            }

            int mipLod = reader.readInt();
            if (mipLod != EXPECTED_MIP_LOD)
                getLogger().warning("The image '%s' was read with an LOD of %d, but %d was expected!", getDebugName(), mipLod, EXPECTED_MIP_LOD);

            // kcImagePrepare()/kcImageSave() has some weird behavior here, this recreates it.
            reader.skipPointer(); // p->data. Assigned by kcImagePrepare()
            int paddedMipLod = ((mipLod + 4) & 0xFFFFFFFC) - 1;
            reader.skipBytes(paddedMipLod * Constants.INTEGER_SIZE); // Area for pointers to each of the mip levels? (It seems to mostly just be padding though.)

            // Load image.
            loadImage(reader, width, height, bitsPerPixel, null);
        } else if (this.fileFormat == GreatQuestImageFileFormat.TGA) {
            reader.setIndex(startIndex); // Restore to the start of the file, so we can read it.
            kcLoad8BitImageHeader(reader);
        } else {
            throw new RuntimeException("The file format '" + this.fileFormat + "' is not supported.");
        }

        // Warn about any unexpectedly large files.
        if (reader.getRemaining() > 0 && !(getGameInstance().isPC() && PC_IGNORED_IMAGES_WITH_EXTRA_DATA.contains(getFileName())))
            getLogger().warning(" - The image '%s' (Format: %s) has %d unread byte(s).", getDebugName(), this.fileFormat, reader.getRemaining());
    }

    private void kcLoad8BitImageHeader(DataReader reader) { // TGA File.
        byte idSize = reader.readByte(); // Always zero.
        byte colMapType = reader.readByte(); // Always either zero or one. Always one if typeCode=1, otherwise zero.
        byte typeCode = reader.readByte(); // See the TYPE_CODE_ defines.
        byte[] colMap = reader.readBytes(5); // {00 00 00 01 18} for BPP 8, or {00 00 00 00 00} for the others.
        short xOrigin = reader.readShort(); // Always zero.
        short yOrigin = reader.readShort(); // Always zero.
        short width = reader.readShort();
        short height = reader.readShort();
        byte bitsPerPixel = reader.readByte();
        byte descriptor = reader.readByte(); // Either 0 or 8, there isn't a clear pattern atm.

        kcImageFormat format = kcImageFormat.getFormatFromBitsPerPixel(bitsPerPixel);
        if (idSize != 0)
            throw new RuntimeException("Expected idSize to be zero for '" + getExportName() + "', but it was " + idSize + "!");
        if (typeCode != TYPE_CODE_GRAYSCALE_TABLE && colMapType != format.getColMapType())
            throw new RuntimeException("Unexpected colMapType " + colMapType + " for format " + format + ". (Expected: " + format.getColMapType() + ")");
        if (typeCode != TYPE_CODE_GRAYSCALE_TABLE && typeCode != format.getTypeCode())
            throw new RuntimeException("Unexpected typeCode " + typeCode + " for format " + format + ".");
        if (typeCode != TYPE_CODE_GRAYSCALE_TABLE && !Arrays.equals(colMap, format.getColMap()))
            throw new RuntimeException("Unexpected colMap " + DataUtils.toByteString(colMap) + " for format " + format + ", " + DataUtils.toByteString(format.getColMap()) + " was expected.");
        if (xOrigin != 0)
            throw new RuntimeException("Expected xOrigin to be zero for '" + getExportName() + "', but it was " + xOrigin + "!");
        if (yOrigin != 0)
            throw new RuntimeException("Expected yOrigin to be zero for '" + getExportName() + "', but it was " + yOrigin + "!");
        if (descriptor != 0 && descriptor != 8)
            throw new RuntimeException("Unexpected descriptor value " + descriptor + ". (Expected either 0 or 8.)");

        // Observed in the PS2 NTSC version.
        // bpp -> 8?
        //  -> ALL MATCH 'colMapType=1, typeCode=1, colMap={00 00 00 01 18}, origin=[0,0], bpp=8'
        //  -> EXCEPT    'idSize=0, colMapType=0, typeCode=3, colMap={00 00 00 00 00}, origin=[0,0], bpp=8, Descriptor=0'
        // bpp -> 24?
        //  -> ALL MATCH 'idSize=0, colMapType=0, typeCode=2, colMap={00 00 00 00 00}, origin=[0,0], bpp=24, Descriptor=0'
        // bpp -> 32?
        //  -> ALL MATCH 'idSize=0, colMapType=0, typeCode=2, colMap={00 00 00 00 00}, origin=[0,0], bpp=32, Descriptor=8'

        //getLogger().info("IMG [" + getDebugName() + ", " + width + "x" + height + "]: idSize=" + idSize + ", colMapType=" + colMapType + ", typeCode=" + typeCode
        //        + ", colMap=" + DataUtils.toByteString(colMap) + ", origin=[" + xOrigin + "," + yOrigin + "], bpp=" + bitsPerPixel + ", Descriptor=" + descriptor);*/

        int paletteSizeInBytes = 0;
        int imageSizeInBytes = (bitsPerPixel * width * height) / Constants.BITS_PER_BYTE;
        if (typeCode == TYPE_CODE_HAS_COLOR_TABLE) {
            if (colMap[4] == 24 && bitsPerPixel == 8) {
                paletteSizeInBytes = 0x300;
            } else if (colMap[4] == 32 && bitsPerPixel == 8) { // Not seen.
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
                // kcLoad8BitImage does this, but I'm not sure why or if we'd even benefit from this, so in the interest of having as little data loss as possible, we're keeping it commented out.
                //if (bitmapData[index + 3] != 0)
                //    bitmapData[index + 3] >>= 1;*/
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

        if (reader.getRemaining() == TGA_FILE_SIGNATURE.length())
            reader.verifyString(TGA_FILE_SIGNATURE);
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
                int[] rawPixelBufferArgb = ImageUtils.getWritablePixelIntegerArray(this.image);
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        rawPixelBufferArgb[((height - y - 1) * width) + x] = reader.hasMore() ? reader.readInt() : 0; // Faster version of this.image.setRGB(x, height - y - 1,  ...)
                break;
            case R8G8B8:
                int[] rawPixelBufferRgb = ImageUtils.getWritablePixelIntegerArray(this.image);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        byte red = reader.readByte();
                        byte green = reader.readByte();
                        byte blue = reader.readByte();
                        rawPixelBufferRgb[((height - y - 1) * width) + x] = ColorUtils.toRGB(red, green, blue); // Faster version of this.image.setRGB(x, height - y - 1, ...)
                    }
                }
                break;
            case INDEXED8:
                byte[] pixelBufferIndex8 = new byte[width];
                for (int y = 0; y < height; y++)
                    this.image.getRaster().setDataElements(0, height - y - 1, width, 1, reader.readBytes(pixelBufferIndex8)); // Faster version of this.image.setRGB(x, height - y - 1, colorModel.getRGB(reader.readUnsignedByte()) (Order of magnitude faster)
                break;
            default:
                throw new RuntimeException("Cannot load unsupported from image format " + format + ".");
        }
    }

    @Override
    public void save(DataWriter writer) {
        if (this.fileFormat == GreatQuestImageFileFormat.ENGINE) { // Based on kcImageSave(_kcImage p, int handle)
            writer.writeInt(SIGNATURE);
            int headerSizeAddress = writer.writeNullPointer();
            writer.writeInt(this.image.getWidth()); // width
            writer.writeInt(this.image.getHeight()); // height
            writer.writeInt(getBitsPerPixel()); // bits per pixel

            // kcImagePrepare()/kcImageSave() has some weird behavior here, this recreates it.
            int mipLod = EXPECTED_MIP_LOD;
            writer.writeInt(mipLod); // mipLod
            int paddedMipLod = ((mipLod + 4) & 0xFFFFFFFC);
            writer.writeNull(paddedMipLod * Constants.INTEGER_SIZE);

            // Write the image.
            saveImage(writer);
            writer.writeIntAtPos(headerSizeAddress, writer.getIndex() - headerSizeAddress - Constants.INTEGER_SIZE);
        } else {
            kcSave8BitImageHeader(writer);
        }
    }

    @SuppressWarnings("ExtractMethodRecommender")
    private void kcSave8BitImageHeader(DataWriter writer) {
        kcImageFormat format = getFormat();
        int bitsPerPixel = format.getBitsPerPixel();
        writer.writeByte(Constants.NULL_BYTE); // idSize - Always zero.
        writer.writeByte(format.getColMapType()); // Always either zero or one.
        writer.writeByte(format.getTypeCode()); // 1, 2, or 3. 3 is only seen once in PS2 NTSC and appears unimplemented potentially.
        writer.writeBytes(format.getColMap());
        writer.writeShort((short) 0); // xOrigin - Always zero.
        writer.writeShort((short) 0); // yOrigin - Always zero.
        writer.writeUnsignedShort(this.image.getWidth()); // image width
        writer.writeUnsignedShort(this.image.getHeight()); // image height
        writer.writeByte((byte) format.getBitsPerPixel());
        writer.writeByte(format.getDescriptor());

        int max = 0;
        if (format.getTypeCode() == TYPE_CODE_NO_COLOR_TABLE) {
            // Each pixel has a color value, so apply the fix to all the remaining data.
            max = ((this.image.getWidth() * this.image.getHeight() * format.getBitsPerPixel()) >> 3) / (bitsPerPixel == 24 ? 3 : 4);
        } else if (format.getTypeCode() == TYPE_CODE_HAS_COLOR_TABLE) {
            // Each pixel is an index into the palette / color lookup table, which is hardcoded as having 256 colors.
            // Only read up til there.
            max = 256;
        }

        // Save palette & image.
        if ((format.getTypeCode() == TYPE_CODE_NO_COLOR_TABLE) && (bitsPerPixel == 32)) {
            ArrayReceiver imageBytesReceiver = new ArrayReceiver();
            DataWriter imageWriter = new DataWriter(imageBytesReceiver);
            saveImage(imageWriter);
            imageWriter.closeReceiver();
            byte[] bitmapData = imageBytesReceiver.toArray();

            // Each pixel has a color value, so apply the fix to all the remaining data.
            for (int index = 0, i = 0; i < max; i++, index += 4) {
                byte temp = bitmapData[index];
                bitmapData[index] = bitmapData[index + 2];
                bitmapData[index + 2] = temp;
                // kcLoad8BitImage does this, but I'm not sure why or if we'd even benefit from this, so in the interest of having as little data loss as possible, we're keeping it commented out.
                // if (bitmapData[index + 3] != 0)
                //    bitmapData[index + 3] <<= 1;*/
            }

            writer.writeBytes(bitmapData);
        } else if (format.getTypeCode() == TYPE_CODE_HAS_COLOR_TABLE) {
            IndexColorModel colorModel = ((IndexColorModel) this.image.getColorModel());

            int bytesPerPixel = 3;
            if (format.getColMap()[4] == 32)
                bytesPerPixel = 4;

            // Each pixel is an index into the palette / color lookup table, which is hardcoded as having 256 colors.
            for (int index = 0, i = 0; i < max; i++, index += bytesPerPixel) {
                int color = colorModel.getRGB(i);
                writer.writeByte(ColorUtils.getBlue(color));
                writer.writeByte(ColorUtils.getGreen(color));
                writer.writeByte(ColorUtils.getRed(color));
                if (bytesPerPixel == 4)
                    writer.writeByte(ColorUtils.getAlpha(color));
            }

            saveImage(writer);
        } else {
            saveImage(writer);
        }
    }


    private void saveImage(DataWriter writer) {
        kcImageFormat format = getFormat();
        int width = this.image.getWidth();
        int height = this.image.getHeight();
        switch (format) {
            case A8R8G8B8:
                int[] rawImageDataArgb = ImageUtils.getReadOnlyPixelIntegerArray(this.image);
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        writer.writeInt(rawImageDataArgb[((height - y - 1) * width) + x]); // Faster version of this.image.getRGB(x, height - y - 1)
                break;
            case R8G8B8:
                int[] rawImageDataRgb = ImageUtils.getReadOnlyPixelIntegerArray(this.image);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int rgb = rawImageDataRgb[((height - y - 1) * width) + x]; // Faster version of this.image.getRGB(x, height - y - 1);
                        writer.writeByte(ColorUtils.getRed(rgb));
                        writer.writeByte(ColorUtils.getGreen(rgb));
                        writer.writeByte(ColorUtils.getBlue(rgb));
                    }
                }
                break;
            case INDEXED8:
                byte[] scanlineBufferIndex8 = new byte[width];
                for (int y = 0; y < height; y++)
                    writer.writeBytes((byte[]) this.image.getRaster().getDataElements(0, height - y - 1, width, 1, scanlineBufferIndex8)); // Faster version of: writer.writeByte(((byte[]) this.image.getRaster().getDataElements(x, height - y - 1, null))[0]);
                break;
            default:
                throw new RuntimeException("Cannot save unsupported from image format " + format + ".");
        }
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

    /**
     * Applies the provided image to this file.
     * @param image The image to apply.
     */
    public void setImage(BufferedImage image) {
        if (image == null)
            throw new NullPointerException("image");

        BufferedImage tempImage;
        if (getGameInstance().isPS2() && (tempImage = ImageUtils.tryConvertTo8BitIndexedBufferedImage(image)) != null) {
            // TODO: Allow quantization if the user wishes.
            image = tempImage;
        } else if (getGameInstance().isPS2() && (tempImage = ImageUtils.tryConvertToRgb888Image(image)) != null) {
            image = tempImage;
        } else {
            image = ImageUtils.convertBufferedImageToFormat(image, BufferedImage.TYPE_INT_ARGB);
        }

        kcImageFormat format = kcImageFormat.getFormatFromBufferedImage(image);
        if (!Utils.contains(format.getSupportedPlatforms(), getGameInstance().getPlatform()))
            throw new RuntimeException("The kcImageFormat '" + format + "' does not support the '" + getGameInstance().getPlatform() + "' game platform!");

        this.image = image;

        // Automatically update the file format to match the necessary option for the newly applied image format.
        GreatQuestImageFileFormat oldFormat = this.fileFormat;
        GreatQuestImageFileFormat newFormat;
        if (format == kcImageFormat.A8R8G8B8) {
            newFormat = GreatQuestImageFileFormat.ENGINE;
        } else {
            // Both R8G8B8 and INDEXED8 are stored as .tga.
            newFormat = GreatQuestImageFileFormat.TGA;
        }

        // If the format changes, this impacts the file ordering, so re-register the file.
        if (oldFormat != newFormat) {
            if (getMainArchive().removeFile(this)) {
                this.fileFormat = newFormat; // Must change only after removal, but BEFORE adding.
                getMainArchive().addFile(this);
            } else {
                this.fileFormat = newFormat; // Must change only after removal, so this if statement is necessary.
            }
        }
    }

    @Override
    public String getDefaultFolderName() {
        return "Images";
    }

    @Override
    public void exportToFolder(File folder) throws IOException {
        File imageFile = new File(folder, FileUtils.stripExtension(getExportName()) + ".png");
        if (!imageFile.exists())
            saveImageToFile(imageFile);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.PHOTO_ALBUM_16.getFxImage();
    }

    @Override
    public GreatQuestImageController makeEditorUI() {
        return loadEditor(getGameInstance(), new GreatQuestImageController(getGameInstance()), this);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("Image Dimensions", getWidth() + " x " + getHeight());
        propertyList.add("Image File Format", this.fileFormat.getDisplayName()); // No need to allow for manual changes of this, as importing an image will do everything necessary.
        propertyList.add("Pixel Format", getFormat());
    }

    @Override
    public BufferedImage makeImage() {
        return this.image;
    }

    @Override
    public int getWidth() {
        return this.image.getWidth();
    }

    @Override
    public int getHeight() {
        return this.image.getHeight();
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

    private static final byte[] COL_MAP_BPP8 = {0x00, 0x00, 0x00, 0x01, 0x18};
    private static final byte[] COL_MAP_DEFAULT = {0x00, 0x00, 0x00, 0x00, 0x00};

    /**
     * Registry of the different supported image formats.
     * Created from 'kcFormat', but only including supported image formats.
     */
    @Getter
    public enum kcImageFormat {
        R8G8B8((byte) 0x14, 24, BufferedImage.TYPE_INT_RGB, 0, TYPE_CODE_NO_COLOR_TABLE, COL_MAP_DEFAULT, 8, GamePlatform.PLAYSTATION_2),
        A8R8G8B8((byte) 0x15, 32, BufferedImage.TYPE_INT_ARGB, 0, TYPE_CODE_NO_COLOR_TABLE, COL_MAP_DEFAULT, 0, GamePlatform.PLAYSTATION_2, GamePlatform.WINDOWS),
        INDEXED8((byte) 0x67, 8, BufferedImage.TYPE_BYTE_INDEXED, 1, TYPE_CODE_HAS_COLOR_TABLE, COL_MAP_BPP8, 0, GamePlatform.PLAYSTATION_2); // NOTE: 0 appears a lot more common than 8 for the descriptor, but there are some images which have 8 for the descriptor. As this value is unused, it seems OK to write 0.

        private final byte enumValue;
        private final int bitsPerPixel;
        private final int bufferedImageType;
        private final byte colMapType;
        private final byte typeCode;
        private final byte[] colMap;
        private final byte descriptor;
        private final GamePlatform[] supportedPlatforms;

        kcImageFormat(byte enumValue, int bitsPerPixel, int bufferedImageType, int colMapType, int typeCode, byte[] colMap, int descriptor, GamePlatform... supportedPlatforms) {
            this.enumValue = enumValue;
            this.bitsPerPixel = bitsPerPixel;
            this.bufferedImageType = bufferedImageType;
            this.colMapType = (byte) colMapType;
            this.typeCode = (byte) typeCode;
            this.colMap = colMap;
            this.descriptor = (byte) descriptor;
            this.supportedPlatforms = supportedPlatforms;
        }

        /**
         * Gets the format from the bits per pixel, as seen in 'kcTextureCreateFromImages' from the PS2 PAL version.
         * Based on what's seen in kcTextureCreateFromImages()
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

    @Getter
    @RequiredArgsConstructor
    public enum GreatQuestImageFileFormat {
        ENGINE("kcCResourceImage"),
        TGA(".tga (TrueVision)");

        private final String displayName;
    }
}