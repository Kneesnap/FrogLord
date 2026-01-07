package net.highwayfrogs.editor.games.konami.rescue.file;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.rescue.file.ui.FroggerRescueImageUIController;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.image.ImageUtils;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a standalone image file.
 * TODO: Tile sheet?
 * TODO: Rename this to tile sheet or sprite or something.
 * Created by Kneesnap on 11/14/2025.
 */
@Getter
public class FroggerRescueImage extends HudsonGameFile {
    private final List<DataChunk1a> dataChunk1aList = new ArrayList<>(); // TODO: This could be like a font/tilesheet.
    private final List<DataChunk2> dataChunk2List = new ArrayList<>();
    private final List<DataChunk3> dataChunk3List = new ArrayList<>(); // TODO: Different frames of the image.

    private static final int PADDING_ALIGNMENT = 32;
    private static final byte PADDING = (byte) 0x88;

    public FroggerRescueImage(IGameFileDefinition fileDefinition) {
        super(fileDefinition);
    }

    @Override
    public void load(DataReader reader) {
        short u1 = reader.readShort(); // Number of data chunk 2s
        short u2 = reader.readShort(); // Number of data chunk 1s.
        int u3 = reader.readInt();
        int ptr1 = reader.readInt(); // Data
        int ptr2 = reader.readInt(); // Right after header.
        int ptr3 = reader.readInt();

        // Read ???
        requireReaderIndex(reader, ptr2, "Expected DataChunk1as");
        this.dataChunk1aList.clear();
        for (int i = 0; i < u2; i++) {
            DataChunk1a newChunk = new DataChunk1a();
            newChunk.load(reader);
            this.dataChunk1aList.add(newChunk);
            if (newChunk.unknown2 != newChunk.width / 2 || newChunk.unknown3 != newChunk.height / 2)
                getLogger().warning("Chunk1a[%d] -> [%d, %d] x [%d, %d]", i, newChunk.unknown2, newChunk.unknown3, newChunk.width, newChunk.height);
        }

        // Read ???
        for (int i = 0; i < this.dataChunk1aList.size(); i++) {
            DataChunk1a chunk1a = this.dataChunk1aList.get(i);
            requireReaderIndex(reader, chunk1a.pointerToChunk1b, "Expected chunk1b[" + i + "]");
            for (int j = 0; j < chunk1a.dataChunk1b.size(); j++) {
                try {
                    chunk1a.dataChunk1b.get(j).load(reader);
                } catch (Throwable th) {
                    // TODO: TOSS
                    Utils.handleError(getLogger(), th, false, "Failed to load chunk[%d].dataChunk1b[%d]...", i, j);
                }
            }
        }

        // Read ???
        this.dataChunk2List.clear();
        requireReaderIndex(reader, ptr1, "Expected DataChunk2s");
        for (int i = 0; i < u1; i++) {
            DataChunk2 newChunk = new DataChunk2();
            this.dataChunk2List.add(newChunk);
            newChunk.load(reader);
        }

        // Read ???
        int testId = 0;
        for (int i = 0; i < this.dataChunk2List.size(); i++) {
            DataChunk2 chunk2 = this.dataChunk2List.get(i);
            requireReaderIndex(reader, chunk2.pointerToChunk2b, "Expected chunk2b[" + i + "]");

            for (int j = 0; j < chunk2.unknownOne; j++) {
                DataChunk2b newChunk = new DataChunk2b();
                newChunk.load(reader);
                chunk2.dataChunk2b.add(newChunk);
                if (newChunk.id != testId++) // TODO: This can happen if there are multiple chunk3s, and I assume one of these corresponds to the second chunk
                    getLogger().warning("Expected chunk2b to have ID %d, but it actually reported ID %d.", (testId - 1), newChunk.id);
            }
        }

        // ptr3
        this.dataChunk3List.clear();
        requireReaderIndex(reader, ptr3, "Expected DataChunk3");
        for (int i = 0; i < u3; i++) {
            DataChunk3 newChunk = new DataChunk3();
            newChunk.load(reader);
            this.dataChunk3List.add(newChunk);
        }

        // Read images.
        for (int i = 0; i < this.dataChunk3List.size(); i++) {
            DataChunk3 chunk3 = this.dataChunk3List.get(i);

            // Read palette data.
            reader.alignRequireByte(PADDING, PADDING_ALIGNMENT);
            requireReaderIndex(reader, chunk3.paletteDataStartIndex, "Expected chunk3[" + i + "] palette data");
            if (chunk3.paletteColorCount != 0)
                chunk3.paletteData = reader.readBytes(chunk3.paletteColorCount * chunk3.getImageFormat().getPaletteBytesPerPixel());

            // Read image data.
            reader.alignRequireByte(PADDING, PADDING_ALIGNMENT);
            requireReaderIndex(reader, chunk3.imageDataStartIndex, "Expected chunk3[" + i + "] image data");
            chunk3.imageData = reader.readBytes(chunk3.dataBufferSizeInBytes);
        }

        requireReaderIndex(reader, reader.getSize(), "Expected end of file");
    }

    @Override
    public void save(DataWriter writer) {
        // TODO: Implement.
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addString(this::addDataChunks1, "Data Chunks 1", String.valueOf(this.dataChunk1aList.size()));
        propertyList.addString(this::addDataChunks2, "Data Chunks 2", String.valueOf(this.dataChunk2List.size()));
        propertyList.add("Data Chunks 3", this.dataChunk3List.size());
        for (int i = 0; i < this.dataChunk3List.size(); i++)
            propertyList.addProperties("DataChunks3[" + i + "]", this.dataChunk3List.get(i));
    }

    private void addDataChunks1(PropertyListNode propertyList) {
        for (int i = 0; i < this.dataChunk1aList.size(); i++)
            propertyList.addProperties("DataChunks1[" + i + "]", this.dataChunk1aList.get(i));
    }

    private void addDataChunks2(PropertyListNode propertyList) {
        for (int i = 0; i < this.dataChunk2List.size(); i++)
            propertyList.addProperties("DataChunks2[" + i + "]", this.dataChunk2List.get(i));
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.PHOTO_ALBUM_16.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return DefaultFileUIController.loadEditor(getGameInstance(), new FroggerRescueImageUIController(getGameInstance()), this);
    }

    /**
     * Returns true iff the given file looks like it is probably a Frogger rescue image file.
     * @param fileData the file data to test
     * @return true iff the file looks like it could be a rescue image.
     */
    public static boolean couldBeRescueImage(byte[] fileData) {
        if (fileData == null || fileData.length < 20)
            return false;

        int dataStartPtr = DataUtils.readNumberFromBytes(fileData, Constants.INTEGER_SIZE, 3 * Constants.INTEGER_SIZE);
        if (dataStartPtr != 0x14)
            return false;

        // TODO: If this doesn't work well enough, get all 3 pointers from the header. Then, calculate their positions based on the counts + known byte sizes.
        //  - Then, if the pointers match our calculations, it's probably an image.
        short count1 = (short) DataUtils.readNumberFromBytes(fileData, Constants.SHORT_SIZE, 0);
        short count2 = (short) DataUtils.readNumberFromBytes(fileData, Constants.SHORT_SIZE, Constants.SHORT_SIZE);
        int count3 = DataUtils.readNumberFromBytes(fileData, Constants.INTEGER_SIZE, Constants.INTEGER_SIZE);
        return (count1 >= 0 && count1 <= 1000) && (count2 >= 0 && count2 <= 1000) && (count3 >= 0) && (count3 <= 1000);
    }

    // 48 bytes.
    public static class DataChunk1a implements IBinarySerializable, IPropertyListCreator {
        private short unknown2; // 11 (16) (16) // TODO: width / bytes per pixel? Nope.
        private short unknown3; // 13 (16) (20) // TODO: Height / bytes per pixel?
        private short width; // 22 (32) (32)
        private short height; // 26 (32) (40)
        private int pointerToChunk1b = -1;
        private final List<DataChunk1b> dataChunk1b = new ArrayList<>();

        @Override
        public void load(DataReader reader) {
            int chunkCount = reader.readShort();
            this.unknown2 = reader.readShort();
            this.unknown3 = reader.readShort();
            this.width = reader.readShort();
            this.height = reader.readShort();
            int expectedZeroPadding = reader.readShort();
            this.pointerToChunk1b = reader.readInt();

            if (expectedZeroPadding != 0)
                throw new IllegalStateException("Expected a value of zero, but read " + expectedZeroPadding + " instead!");

            this.dataChunk1b.clear();
            for (int i = 0; i < chunkCount; i++)
                this.dataChunk1b.add(new DataChunk1b());
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.dataChunk1b.size());
            writer.writeShort(this.unknown2);
            writer.writeShort(this.unknown3);
            writer.writeShort(this.width);
            writer.writeShort(this.height);
            writer.writeShort((short) 0);
            this.pointerToChunk1b = writer.writeNullPointer();
        }

        @Override
        public void addToPropertyList(PropertyListNode propertyList) {
            propertyList.add("Unknown 2", this.unknown2);
            propertyList.add("Unknown 3", this.unknown3);
            propertyList.add("Width", this.width);
            propertyList.add("Height", this.height);
            propertyList.addString(this::addExtraData, "Chunks", String.valueOf(this.dataChunk1b.size()));
        }

        private void addExtraData(PropertyListNode propertyList) {
            for (int i = 0; i < this.dataChunk1b.size(); i++)
                propertyList.addProperties("Chunk[" + i + "]", this.dataChunk1b.get(i));
        }
    }

    // 32 bytes.
    public static class DataChunk1b implements IBinarySerializable, IPropertyListCreator {
        private int unknown1;
        private short startX;
        private short startY;
        private short width;
        private short height;
        private int unknown4; // TODO: Usually 0. See menu.hfs{file=4}, dvd\win\menu.hfs{file=11}, dvd\win\menu.hfs{file=12}, dvd\win\menu.hfs{file=14}
        private int unknown5; // TODO: Usually 0. See menu.hfs{file=4}, dvd\win\menu.hfs{file=11}, dvd\win\menu.hfs{file=13}, dvd\win\menu.hfs{file=14}
        private int unknown6; // TODO: 88/width/stride/?
        private short unknown7; // TODO: 88/width/stride/?
        private short unknown8; // TODO: 32/bpp/height/?
        private short unknown9; // TODO: 0?
        private short unknown10; // TODO: 32/bpp/height/?

        @Override
        public void load(DataReader reader) {
            this.unknown1 = reader.readInt();
            this.startX = reader.readShort();
            this.startY = reader.readShort();
            this.width = reader.readShort();
            this.height = reader.readShort();
            this.unknown4 = reader.readInt();
            this.unknown5 = reader.readInt();
            this.unknown6 = reader.readInt();
            this.unknown7 = reader.readShort();
            this.unknown8 = reader.readShort();
            this.unknown9 = reader.readShort();
            this.unknown10 = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            // TODO
        }

        @Override
        public void addToPropertyList(PropertyListNode propertyList) {
            propertyList.add("Unknown 1", this.unknown1);
            propertyList.add("Start X", this.startX);
            propertyList.add("Start Y", this.startY);
            propertyList.add("Width", this.width);
            propertyList.add("Height", this.height);
            propertyList.add("Unknown 4", this.unknown4);
            propertyList.add("Unknown 5", this.unknown5);
            propertyList.add("Unknown 6", this.unknown6);
            propertyList.add("Unknown 7", this.unknown7);
            propertyList.add("Unknown 8", this.unknown8);
            propertyList.add("Unknown 9", this.unknown9);
            propertyList.add("Unknown 10", this.unknown10);
        }
    }

    // 8 bytes.
    public static class DataChunk2 implements IBinarySerializable, IPropertyListCreator {
        // dvd\win\frog.hfs{file=83} = 5
        // dvd\win\menu.hfs{file=1} = 4
        // dvd\win\window.hfs{file=25} = 7
        private int unknownOne = 1; // TODO: Datachunk 2b count.
        private int pointerToChunk2b = -1;
        private final List<DataChunk2b> dataChunk2b = new ArrayList<>();

        @Override
        public void load(DataReader reader) {
            this.unknownOne = reader.readInt();
            this.pointerToChunk2b = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            // TODO
        }

        @Override
        public void addToPropertyList(PropertyListNode propertyList) {
            for (int i = 0; i < this.dataChunk2b.size(); i++)
                propertyList.addProperties("Sub Chunk " + i, this.dataChunk2b.get(i));
        }
    }

    // 12 bytes.
    public static class DataChunk2b implements IBinarySerializable, IPropertyListCreator {
        private short id; // Increases by one.
        // TODO: dvd\win\mini.hfs{file=203} = 2
        // TODO: dvd\win\mini.hfs{file=205} = 2
        // TODO: dvd\win\mini.hfs{file=220} = 2
        // TODO: dvd\win\menu.hfs{file=47} = 10, -1 TODO: Frame count?
        private short one; // Seems to be one. TODO: This is either a player ID, or a frame ID, eg: allowing switching between textures.

        @Override
        public void load(DataReader reader) {
            this.id = reader.readShort();
            this.one = reader.readShort();
            reader.skipBytesRequireEmpty(2 * Constants.INTEGER_SIZE); // Two blank spots. (Probably spots for runtime pointers)
        }

        @Override
        public void save(DataWriter writer) {
            // TODO
        }

        @Override
        public void addToPropertyList(PropertyListNode propertyList) {
            propertyList.add("ID", this.id); // TODO: Validate this is always ordinal based.
            propertyList.add("One", this.one); // TODO: ? Does this have to do with player ID? Yeah, I think so. Check how these textures are used in-game.
        }
    }

    // There's only one of this.
    // 20 bytes
    // TODO: Directly load to image at some point?
    public static class DataChunk3 implements IBinarySerializable, IPropertyListCreator {
        private byte bitDepth;
        private byte flags;
        private short paletteColorCount;
        private short width;
        private short height;
        private int dataBufferSizeInBytes;
        private int paletteDataStartIndex = -1;
        private int imageDataStartIndex = 1;
        private byte[] paletteData;
        private byte[] imageData;

        @Override
        public void load(DataReader reader) {
            this.bitDepth = reader.readByte();
            this.flags = reader.readByte();
            this.paletteColorCount = reader.readShort();
            this.width = reader.readShort();
            this.height = reader.readShort();
            this.dataBufferSizeInBytes = reader.readInt();
            this.paletteDataStartIndex = reader.readInt();
            this.imageDataStartIndex = reader.readInt();

            int calculatedSize = ((this.bitDepth * this.width * this.height) / Constants.BITS_PER_BYTE);
            if (this.dataBufferSizeInBytes != calculatedSize)
                throw new IllegalArgumentException("Calculated size of: " + calculatedSize + ", but real size is: " + this.dataBufferSizeInBytes);
        }

        @Override
        public void save(DataWriter writer) {
            // TODO
        }

        @Override
        public void addToPropertyList(PropertyListNode propertyList) {
            propertyList.add("Format", FroggerRescueImageFormat.getFormat(this.bitDepth, this.flags));
            propertyList.add("Bit Depth", this.bitDepth);
            propertyList.add("Unknown 2", this.flags);
            propertyList.add("Width", this.width);
            propertyList.add("Height", this.height);
            propertyList.add("Palette Length", this.paletteData != null ? this.paletteData.length : "No Palette Data");
            propertyList.add("Image Data Length", this.imageData != null ? this.imageData.length : "No Image Data");
        }

        /**
         * Gets the image format used to save/load this image.
         */
        public FroggerRescueImageFormat getImageFormat() {
            FroggerRescueImageFormat imageFormat = FroggerRescueImageFormat.getFormat(this.bitDepth, this.flags);
            if (imageFormat == null)
                throw new IllegalArgumentException("Could not determined imageFormat from bitDepth: " + this.bitDepth + ", Flags: " + this.flags);

            return imageFormat;
        }

        private IndexColorModel loadColorPalette() {
            FroggerRescueImageFormat imageFormat = getImageFormat();
            if (imageFormat == FroggerRescueImageFormat.INDEXED8_PALETTE16 || imageFormat == FroggerRescueImageFormat.INDEXED4_PALETTE16) {
                byte[] paletteBytes = new byte[this.paletteData.length << 1]; // (palleteSizeInBytes / sizeof(short)) * sizeof(int)
                for (int i = 0, index = 0; i < this.paletteData.length; i += Constants.SHORT_SIZE, index += 4) {
                    short color = getShort(this.paletteData, i);
                    // ABGR1555 -> ABGR8888
                    paletteBytes[index] = (byte) ((color & 0b11111) << 3); // Red
                    paletteBytes[index + 1] = (byte) ((color & 0b1111100000) >> 2); // Green
                    paletteBytes[index + 2] = (byte) ((color & 0b0111110000000000) >> 7); // Blue
                    paletteBytes[index + 3] = ((color & 0x8000) == 0x8000) ? (byte) 0xFF : (byte) 0x00; // Alpha
                }

                // Setup palette.
                return new IndexColorModel(8, this.paletteData.length >> 1, paletteBytes, 0, true);
            } else if (imageFormat == FroggerRescueImageFormat.INDEXED8_PALETTE32) {
                byte[] newPaletteData = new byte[this.paletteData.length];
                for (int i = 0; i < this.paletteData.length; i += Constants.INTEGER_SIZE) {
                    // RGBA8888 -> ABGR8888
                    byte alpha = this.paletteData[i];
                    byte blue = this.paletteData[i + 1];
                    byte green = this.paletteData[i + 2];
                    byte red = this.paletteData[i + 3];
                    newPaletteData[i] = red;
                    newPaletteData[i + 1] = green;
                    newPaletteData[i + 2] = blue;
                    newPaletteData[i + 3] = alpha;
                }

                // Setup palette.
                return new IndexColorModel(8, this.paletteData.length >> 2, newPaletteData, 0, true);
            }

            return null;
        }

        /**
         * Creates a BufferedImage representing this image.
         * @return bufferedImage
         */
        @SuppressWarnings("ExtractMethodRecommender")
        public BufferedImage toBufferedImage() {
            FroggerRescueImageFormat format = getImageFormat();
            IndexColorModel colorModel = loadColorPalette();

            // Create the new image.
            BufferedImage newImage;
            if (format.getBufferedImageType() == BufferedImage.TYPE_BYTE_INDEXED) {
                if (colorModel == null)
                    throw new RuntimeException("The image format was " + format + ", but there was no color lookup table.");
                newImage = new BufferedImage(this.width, this.height, format.getBufferedImageType(), colorModel);
            } else {
                if (colorModel != null)
                    throw new RuntimeException("The image format was " + format + ", but there was a color lookup table??");

                newImage = new BufferedImage(this.width, this.height, format.getBufferedImageType());
            }

            int index = 0;
            switch (format) {
                case ARGB8888:
                    // ARGB8888 -> ABGR8888
                    int[] rawPixelBufferArgb = ImageUtils.getWritablePixelIntegerArray(newImage);
                    for (; index < this.imageData.length; index += Constants.INTEGER_SIZE)
                        rawPixelBufferArgb[index / Constants.INTEGER_SIZE] = ColorUtils.swapRedBlue(DataUtils.readIntFromBytes(this.imageData, index)); // Faster version of this.image.setRGB(x, height - y - 1,  ...)
                    break;
                case ABGR1555:
                    // ABGR1555 -> ARGB1555.
                    short[] rawPixelBufferRgb = ImageUtils.getPixelShortArray(newImage);
                    for (int y = 0; y < this.height; y++)
                        for (int x = 0; x < this.width; x++, index += Constants.SHORT_SIZE)
                            rawPixelBufferRgb[(y * this.width) + x] = swapShort(getShort(this.imageData, index)); // Faster version of this.image.setRGB(x, height - y - 1, ...)
                    break;
                case INDEXED4_PALETTE16:
                    byte[] pixelBufferIndex4 = new byte[this.width]; // One byte per pixel.
                    for (int y = 0; y < this.height; y++) {
                        // One byte from the input buffer contains two pixels
                        for (int i = 0; i < this.width; i++, index++) {
                            byte value = this.imageData[index >> 1];
                            if ((index % 2) > 0) {
                                pixelBufferIndex4[i] = (byte) ((value & 0xF0) >>> 4);
                            } else {
                                pixelBufferIndex4[i] = (byte) (value & 0x0F);
                            }
                        }

                        newImage.getRaster().setDataElements(0, y, this.width, 1, pixelBufferIndex4); // Faster version of this.image.setRGB(x, height - y - 1, colorModel.getRGB(reader.readUnsignedByte()) (Order of magnitude faster)
                    }
                    break;
                case INDEXED8_PALETTE16:
                case INDEXED8_PALETTE32:
                    // Faster version of this.image.setRGB(x, y, colorModel.getRGB(reader.readUnsignedByte()) (Order of magnitude faster)
                    newImage.getRaster().setDataElements(0, 0, this.width, this.height, this.imageData);
                    break;
                default:
                    throw new RuntimeException("Cannot load unsupported from image format: " + format + ".");
            }

            return newImage;
        }
    }

    private static short getShort(byte[] array, int index) {
        return (short) ((array[index + 1] << Constants.BITS_PER_BYTE) | (array[index] & 0xFF));
    }

    private static short swapShort(short inputColor) {
        short strippedValue = (short) (inputColor & 0b1000001111100000);
        int topValue = (inputColor & 0b111110000000000) >>> 10;
        int botValue = (inputColor & 0b11111);
        return (short) (strippedValue | (botValue << 10) | topValue);
    }

    @Getter
    @RequiredArgsConstructor
    public enum FroggerRescueImageFormat {
        ARGB8888(32, 0, BufferedImage.TYPE_INT_ARGB, 0),
        ABGR1555(16, 2, BufferedImage.TYPE_USHORT_555_RGB, 0), // TODO: Do we need to implement a custom image type for the alpha bit? Transparency is not working...
        INDEXED8_PALETTE16(8, 3, BufferedImage.TYPE_BYTE_INDEXED,  Constants.SHORT_SIZE),

        // dvd\win\effect.hfs{file=2}
        // dvd\win\effect.hfs{file=3}
        // dvd\win\effect.hfs{file=165}
        // dvd\win\system.hfs{file=25}
        INDEXED4_PALETTE16(4, 4, BufferedImage.TYPE_BYTE_INDEXED, Constants.SHORT_SIZE),
        INDEXED8_PALETTE32(8, 19, BufferedImage.TYPE_BYTE_INDEXED, Constants.INTEGER_SIZE);

        // dvd\win\system.hfs{file=35}
        // TODO: 32, 16

        private final int bitDepth;
        private final int flags;
        private final int bufferedImageType;
        private final int paletteBytesPerPixel;

        /**
         * Gets the image format used to uniquely identify the load/save behavior for the image.
         * @param bitDepth the bits-per-pixel value
         * @param flags the flags value
         * @return imageFormat, or null
         */
        public static FroggerRescueImageFormat getFormat(int bitDepth, int flags) {
            for (int i = 0; i < values().length; i++) {
                FroggerRescueImageFormat format = values()[i];
                if (format.bitDepth == bitDepth && format.flags == flags)
                    return format;
            }

            return null;
        }
    }
}
