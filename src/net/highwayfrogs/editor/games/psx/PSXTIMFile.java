package net.highwayfrogs.editor.games.psx;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXClutColor;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.MainController;
import net.highwayfrogs.editor.gui.editor.TIMController;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a PSX .TIM image.
 * References:
 * - http://fileformats.archiveteam.org/wiki/TIM_(PlayStation_graphics)
 * - http://formats.kaitai.io/psx_tim/
 * - https://github.com/rickomax/psxprev/blob/no-deps/Common/Parsers/TIMParser.cs
 * Created by Kneesnap on 9/10/2023.
 */
@Getter
public class PSXTIMFile extends SCSharedGameFile {
    private int flags;
    private int clutX;
    private int clutY;
    private PSXClutColor[][] palettes;
    private int imageX;
    private int imageY;
    private int imageStride;
    private int imageHeight;
    private byte[] rawImageBytes;

    private static final int SIGNATURE = 0x00000010;
    private static final byte[] SIGNATURE_BYTES = {0x10, 0x00, 0x00, 0x00};
    private static final int HEADER_SIZE = 12; // 12 bytes.
    private static final int MAX_IMAGE_DIMENSION = 1024;
    private static final int TEXTURE_PAGE_BPP4_WIDTH = 256;
    private static final int TEXTURE_PAGE_BPP8_WIDTH = 128;
    private static final int TEXTURE_PAGE_BPP16_WIDTH = 64;
    private static final int TEXTURE_PAGE_HEIGHT = 256;

    private static final int FLAG_HAS_CLUT = Constants.BIT_FLAG_3; // 0b1000

    public PSXTIMFile(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        int readSignature = reader.readInt();
        if (readSignature != SIGNATURE)
            throw new RuntimeException("There was no .TIM signature present. (Got: " + Utils.toHexString(readSignature) + ")");

        this.flags = reader.readInt();
        if (hasClut()) {
            int clutStartPos = reader.getIndex();
            int clutSize = reader.readInt();
            this.clutX = reader.readUnsignedShortAsInt();
            this.clutY = reader.readUnsignedShortAsInt();
            int clutWidth = reader.readUnsignedShortAsInt();
            int clutHeight = reader.readUnsignedShortAsInt();
            if (clutSize < HEADER_SIZE + (clutHeight * clutWidth) * PSXClutColor.BYTE_SIZE) // 2 bytes per pixel.
                throw new RuntimeException("Invalid CLUT Size Read [Size: " + clutSize + ", Width: " + clutWidth + ", Height: " + clutHeight + "]");

            // Noted in PSXPrev/TIMParser/ReadTim and jpsxdec/CreateTim that some files can claim an unpaletted pmode but still use a palette.
            this.palettes = new PSXClutColor[clutHeight][]; // Do this immediately because getClutHeight() will error if accessed before this is set. It so happens that getClutHeight() is used in getBPPType() on the next line.
            this.readPalettes(reader, getBPPType(), clutWidth, this.palettes);

            // Ensure the reader position is in the expected spot.
            int clutEndPos = clutStartPos + clutSize;
            if (reader.getIndex() != clutEndPos) {
                System.out.println("CLUT Position Mismatch for " + getFileDisplayName() + ", expected " + Utils.toHexString(clutEndPos) + ", but got " + Utils.toHexString(reader.getIndex()) + ".");
                reader.setIndex(clutEndPos);
            }
        }

        if (getBPPType().hasPalette() && this.palettes == null)
            throw new RuntimeException("The BPPType " + getBPPType() + " is expected to have a palette, but none was read.");

        int imageStartPos = reader.getIndex();
        int imageSize = reader.readInt(); // Size of image data starting at this field
        this.imageX = reader.readUnsignedShortAsInt(); // Frame buffer coordinates
        this.imageY = reader.readUnsignedShortAsInt();
        this.imageStride = reader.readUnsignedShortAsInt(); // Stride in units of 2 bytes
        this.imageHeight = reader.readUnsignedShortAsInt();
        if (imageSize < HEADER_SIZE + this.imageHeight * this.imageStride * 2)
            throw new RuntimeException("Invalid Image Size Read [Size: " + imageSize + ", Stride: " + this.imageStride + ", Height: " + this.imageHeight + "]");

        // Verify image stuff.
        int texturePageX = this.imageX / 64;
        if (texturePageX > 16)
            throw new RuntimeException("The x texture page value was invalid: " + texturePageX);

        int texturePageY = this.imageY / 256; // Changed from 255
        if (texturePageY > 2)
            throw new RuntimeException("The y texture page value was invalid: " + texturePageY);

        // Read image.
        int imageEndPos = imageStartPos + imageSize;
        this.rawImageBytes = reader.readBytes(imageEndPos - reader.getIndex());
    }

    private PSXClutColor[][] readPalettes(DataReader reader, BPPType bppType, int clutWidth, PSXClutColor[][] palettes) {
        int clutHeight = palettes.length;
        if (clutWidth == 0 || clutHeight == 0 || clutWidth > 256 || clutHeight > 256)
            return null;

        if (!bppType.hasPalette())
            return null; // Not a clut format

        // HMD: Support models with invalid image data, but valid model data.
        int clutDataSize = (clutHeight * clutWidth * PSXClutColor.BYTE_SIZE);
        if (clutDataSize > reader.getRemaining())
            return null;

        for (int i = 0; i < palettes.length; i++)
            palettes[i] = this.readPalette(reader, bppType, clutWidth);

        return palettes;
    }

    private PSXClutColor[] readPalette(DataReader reader, BPPType bppType, int clutWidth) {
        if (!bppType.hasPalette())
            return null; // Not a clut format
        if (clutWidth != bppType.getPaletteSize())
            throw new RuntimeException("Palette size mismatch! Read Width: " + clutWidth + ", Expected: " + bppType.getPaletteSize() + ", " + bppType);

        // HMD: Support models with invalid image data, but valid model data.
        int clutDataSize = (clutWidth * PSXClutColor.BYTE_SIZE);
        if (clutDataSize > reader.getRemaining())
            return null;

        PSXClutColor[] palette = new PSXClutColor[bppType.getPaletteSize()];
        for (int i = 0; i < palette.length; i++) {
            PSXClutColor color = new PSXClutColor(); // Default color is black.
            color.load(reader); // Load color data.
            palette[i] = color;
        }

        return palette;
    }

    /**
     * Get the image data parsed into a BufferedImage.
     * @param enableTransparency If the PSX would have "transparency processing" enabled on the draw primitive.
     * @return bufferedImage
     */
    public BufferedImage toBufferedImage(boolean enableTransparency) {
        return toBufferedImage(enableTransparency, 0);
    }

    /**
     * Get the image data parsed into a BufferedImage.
     * @param enableTransparency If the PSX would have "transparency processing" enabled on the draw primitive.
     * @return bufferedImage
     */
    public BufferedImage toBufferedImage(boolean enableTransparency, int paletteId) {
        DataReader reader = new DataReader(new ArraySource(this.rawImageBytes));
        BufferedImage image = readTexture(reader, getBPPType(), this.imageStride, getImageWidth(), this.imageHeight, enableTransparency, this.palettes != null && this.palettes.length > paletteId ? this.palettes[paletteId] : null);
        if (image == null)
            throw new RuntimeException("The image was not loaded successfully.");

        return image;
    }

    private static BufferedImage readTexture(DataReader reader, BPPType bpp, int stride, int width, int height, boolean enableTransparency, PSXClutColor[] palette) {
        if (stride <= 0 || width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION)
            return null;

        // HMD: Support models with invalid image data, but valid model data.
        int textureDataSize = (height * stride * 2);
        if (textureDataSize > reader.getRemaining())
            return null;

        BufferedImage texture = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        switch (bpp) {
            case BPP_4:
                read4BppTexture(reader, texture, enableTransparency, palette);
                break;
            case BPP_8:
                read8BppTexture(reader, texture, enableTransparency, palette);
                break;
            case BPP_16:
                read16BppTexture(reader, texture, enableTransparency);
                break;
            case BPP_24:
                read24BppTexture(reader, texture);
                break;
            default:
                throw new RuntimeException("Unsupported BPPType: " + bpp);
        }

        return texture;
    }

    private static void read4BppTexture(DataReader reader, BufferedImage image, boolean enableTransparency, PSXClutColor[] palette) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                short value = reader.readUnsignedByteAsShort();
                int first = (value & 0x0F);
                int second = value >> 4;

                image.setRGB(x++, y, palette[first].toFullARGB(enableTransparency));
                image.setRGB(x, y, palette[second].toFullARGB(enableTransparency));
            }
        }
    }

    private static void read8BppTexture(DataReader reader, BufferedImage image, boolean enableTransparency, PSXClutColor[] palette) {
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++)
                image.setRGB(x, y, palette[reader.readUnsignedByteAsShort()].toFullARGB(enableTransparency));
    }

    private static void read16BppTexture(DataReader reader, BufferedImage image, boolean enableTransparency) {
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++)
                image.setRGB(x, y, PSXClutColor.readARGBColorFromShort(reader.readShort(), enableTransparency));
    }

    private static void read24BppTexture(DataReader reader, BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                byte red = reader.readByte();
                byte green = reader.readByte();
                byte blue = reader.readByte();

                // Alpha is always 0xFF I think.
                image.setRGB(x, y, Utils.toARGB(red, green, blue, (byte) 0xFF));
            }
        }
    }

    /**
     * Load the image data from an image into the TIM file.
     * @param image              The image to load.
     * @param enableTransparency If the PSX would have "transparency processing" enabled on the draw primitive.
     */
    public void fromBufferedImage(BufferedImage image, boolean enableTransparency) {
        if (image.getWidth() > MAX_IMAGE_DIMENSION || image.getHeight() > MAX_IMAGE_DIMENSION)
            throw new RuntimeException("Image dimensions cannot exceed " + MAX_IMAGE_DIMENSION + "x" + image.getHeight() + ", but this image was " + image.getWidth() + "x" + image.getHeight());

        // Generate palette.
        Map<PSXClutColor, Integer> colorPalette = new HashMap<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argbColor = image.getRGB(x, y); // This will always return the value in ARGB form.
                PSXClutColor clutColor = PSXClutColor.fromARGB(argbColor, enableTransparency);
                colorPalette.putIfAbsent(clutColor, colorPalette.size());
            }
        }

        BPPType bppType = getBPPType();
        if (bppType.hasPalette() && bppType.getPaletteSize() < colorPalette.size())
            throw new RuntimeException("The TIM only accepts " + bppType.getPaletteSize() + " unique colors, but " + colorPalette.size() + " were seen.");

        ArrayReceiver newReceiver = new ArrayReceiver();
        DataWriter writer = new DataWriter(newReceiver);
        switch (bppType) {
            case BPP_4:
                write4BppTexture(writer, image, enableTransparency, colorPalette);
                break;
            case BPP_8:
                write8BppTexture(writer, image, enableTransparency, colorPalette);
                break;
            case BPP_16:
                write16BppTexture(writer, image, enableTransparency);
                break;
            case BPP_24:
                write24BppTexture(writer, image);
                break;
            default:
                throw new RuntimeException("The BPP Type " + bppType + " is unsupported for image importing.");
        }

        writer.closeReceiver();
        this.rawImageBytes = newReceiver.toArray();
    }

    private static void write4BppTexture(DataWriter writer, BufferedImage image, boolean enableTransparency, Map<PSXClutColor, Integer> palette) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argbPixel1 = image.getRGB(x++, y);
                int argbPixel2 = image.getRGB(x, y);
                PSXClutColor clutColor1 = PSXClutColor.fromARGB(argbPixel1, enableTransparency);
                PSXClutColor clutColor2 = PSXClutColor.fromARGB(argbPixel2, enableTransparency);
                int clutIndex1 = palette.get(clutColor1);
                int clutIndex2 = palette.get(clutColor2);

                short value = (short) ((clutIndex1 & 0x0F) | ((clutIndex2 & 0x0F) << 4));
                writer.writeUnsignedByte(value);
            }
        }
    }

    private static void write8BppTexture(DataWriter writer, BufferedImage image, boolean enableTransparency, Map<PSXClutColor, Integer> palette) {
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++)
                writer.writeUnsignedByte((short) (palette.get(PSXClutColor.fromARGB(image.getRGB(x, y), enableTransparency)) & 0xFF));
    }

    private static void write16BppTexture(DataWriter writer, BufferedImage image, boolean enableTransparency) {
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++)
                PSXClutColor.fromARGB(image.getRGB(x, y), enableTransparency).save(writer);
    }

    private static void write24BppTexture(DataWriter writer, BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argbColor = image.getRGB(x, y);
                writer.writeByte(Utils.getRed(argbColor));
                writer.writeByte(Utils.getGreen(argbColor));
                writer.writeByte(Utils.getBlue(argbColor));
            }
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(SIGNATURE);
        writer.writeInt(this.flags);

        if (hasClut()) {
            int clutStartPos = writer.getIndex();
            int clutSizePos = writer.writeNullPointer();
            writer.writeUnsignedShort(this.clutX);
            writer.writeUnsignedShort(this.clutY);
            writer.writeUnsignedShort(getClutWidth());
            writer.writeUnsignedShort(getClutHeight());

            // Noted in PSXPrev/TIMParser/ReadTim and jpsxdec/CreateTim that some files can claim an unpaletted pmode but still use a palette.
            this.writePalettes(writer, this.palettes);

            int clutSize = writer.getIndex() - clutStartPos;
            writer.writeAddressAt(clutSizePos, clutSize);
        }

        // Write image header.
        int imageStartPos = writer.getIndex();
        int imageSizePos = writer.writeNullPointer(); // Size of image data starting at this field
        writer.writeUnsignedShort(this.imageX);
        writer.writeUnsignedShort(this.imageY);
        writer.writeUnsignedShort(this.imageStride);
        writer.writeUnsignedShort(this.imageHeight);

        // Write image data.
        if (this.rawImageBytes == null)
            throw new RuntimeException("Cannot write null rawImageBytes to writer.");
        writer.writeBytes(this.rawImageBytes);

        // Write image size.
        int imageSize = writer.getIndex() - imageStartPos;
        writer.writeAddressAt(imageSizePos, imageSize);
    }

    private void writePalettes(DataWriter writer, PSXClutColor[][] palettes) {
        if (palettes == null)
            throw new RuntimeException("The BPPType " + getBPPType() + " is expected to have a palette, but none was available to write.");

        for (int i = 0; i < palettes.length; i++)
            this.writePalette(writer, palettes[i]);
    }

    private void writePalette(DataWriter writer, PSXClutColor[] palette) {
        for (int i = 0; i < palette.length; i++) {
            PSXClutColor color = palette[i];
            if (color == null) // Default color is black.
                color = new PSXClutColor();

            color.save(writer);
        }
    }

    @Override
    public Node makeEditor() {
        return loadEditor(getGameInstance(), new TIMController(getGameInstance()), "edit-file-tim", this);
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        MainController.MAIN_WINDOW.openEditor(MainController.MAIN_WINDOW.getCurrentFilesList(), this);
        ((TIMController) MainController.getCurrentController()).setParentWad(parent);
    }

    @Override
    public List<Tuple2<String, Object>> createPropertyList() {
        List<Tuple2<String, Object>> list = super.createPropertyList();
        list.add(new Tuple2<>("Flags", Utils.toHexString(this.flags)));
        list.add(new Tuple2<>("Palette Count", this.palettes != null ? this.palettes.length : 0));
        list.add(new Tuple2<>("Image Dimensions", getImageWidth() + "x" + getImageHeight()));
        list.add(new Tuple2<>("Image Position", "X: " + this.imageX + ", Y: " + this.imageHeight));
        if (hasClut()) {
            list.add(new Tuple2<>("CLUT Dimensions", getClutWidth() + "x" + getClutHeight()));
            list.add(new Tuple2<>("CLUT Position", "X: " + this.clutX + ", Y: " + this.clutY));
        }

        return list;
    }

    @Override
    public Image getIcon() {
        return VLOArchive.ICON;
    }

    /**
     * Gets the width of the image.
     */
    public int getImageWidth() {
        return (this.imageStride * 16) / getBitsPerPixel();
    }

    /**
     * Test if there's a color lookup table enabled for this image.
     */
    public boolean hasClut() {
        return (this.flags & FLAG_HAS_CLUT) == FLAG_HAS_CLUT;
    }

    /**
     * Get the number of pixels a CLUT takes up.
     * @return clutWidth
     */
    public int getClutWidth() {
        return getBPPType().getBitsPerPixel();
    }

    /**
     * Get the height of the clut.
     * @return clutHeight
     */
    public int getClutHeight() {
        if (this.palettes == null) {
            if (hasClut())
                throw new RuntimeException("Tried to access the CLUT Height before it was created.");

            return 0;
        }

        return this.palettes.length;
    }

    /**
     * Get the number of bits per pixel for the image data.
     */
    public int getBitsPerPixel() {
        return getBPPType().getBitsPerPixel();
    }

    /**
     * Gets the BPPType for the image.
     */
    public BPPType getBPPType() {
        if (hasClut()) {
            // Noted in PSXPrev/TIMParser/InferBppFromClut jpsxdec/CreateTim that some files can claim an unpaletted mode but still use a palette.
            switch (this.flags & 0b11) {
                case 2:
                    // NOTE: Width always seems to be 16 or 256.
                    //       Specifically width was 16 or 256 and height was 1.
                    //       With that, it's safe to assume the dimensions tell us the color count.
                    //       Because this data could potentially give us something other than 16 or 256,
                    //       assume anything greater than 16 will allocate a 256clut and only read w colors.

                    // Note that height is different, and is used to count the number of cluts.
                    // This check may not be perfectly correct.
                    return (getClutWidth() < 256 ? BPPType.BPP_4 : BPPType.BPP_8);
                case 3: // Yeah idk but PSXPrev does it this way.
                    return BPPType.BPP_8;
            }

            // If we haven't returned a value, do the default stuff below.
        }

        switch (this.flags & 0b11) {
            case 0:
                return BPPType.BPP_4;
            case 1:
                return BPPType.BPP_8;
            case 2:
                return BPPType.BPP_16;
            case 3:
                return BPPType.BPP_24;
            default:
                throw new RuntimeException("Should be impossible.");
        }
    }

    /**
     * Test if the given data is a .TIM file.
     * @param instance The game instance to use for more intensive file testing. (Optional)
     * @param data     The data to test.
     * @return isTimFile
     */
    public static boolean isTIMFile(SCGameInstance instance, byte[] data) {
        if (!Utils.testSignature(data, SIGNATURE_BYTES))
            return false;

        // Because the signature of a .TIM file is not very unique, false-positives may occur with the above check.
        // So, we take a further look at the data to see if there's any other data.
        if (instance != null) {
            DataReader reader = new DataReader(new ArraySource(data));
            try {
                new PSXTIMFile(instance).load(reader);
                return true;
            } catch (Throwable th) {
                th.printStackTrace();
                return false;
            }
        }

        return true;
    }

    @Getter
    @AllArgsConstructor
    public enum BPPType {
        BPP_4(4, 16),
        BPP_8(8, 256),
        BPP_16(16, -1), // ARGB1555 5/5/5/stp
        BPP_24(24, -1); // RGB888

        private final int bitsPerPixel;
        private final int paletteSize;

        /**
         * Test if palettes are enabled.
         */
        public boolean hasPalette() {
            return this.paletteSize > 0;
        }
    }
}