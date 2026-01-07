package net.highwayfrogs.editor.games.renderware.chunks;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.chunks.RwPlatformIndependentTextureDictionaryChunk.IRwPlatformIndependentTexturePrefix;
import net.highwayfrogs.editor.games.renderware.struct.types.RwImage;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.image.ImageUtils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents the image chunk from baimage.c/RwImageStreamRead.
 * Created by Kneesnap on 6/9/2020.
 */
@Getter
public class RwImageChunk extends RwStreamChunk implements ITextureSource {
    private final IRwPlatformIndependentTexturePrefix texturePrefix;
    private final List<Consumer<BufferedImage>> imageChangeListeners = new ArrayList<>();
    private final int mipMapLevelId;
    private BufferedImage image = DEFAULT_IMAGE;

    private static final byte PADDING_BYTE = (byte) 0xCD;
    private static final int COLOR_SIZE_IN_BYTES = Constants.INTEGER_SIZE; // ABGR
    private static final BufferedImage DEFAULT_IMAGE = UnknownTextureSource.MAGENTA_INSTANCE.makeImage();

    public RwImageChunk(RwStreamFile streamFile, int renderwareVersion, RwStreamChunk parentChunk) {
        this(streamFile, renderwareVersion, parentChunk, null, -1);
    }

    public RwImageChunk(RwStreamFile streamFile, int renderwareVersion, RwStreamChunk parentChunk, IRwPlatformIndependentTexturePrefix texturePrefix, int mipMapLevelId) {
        super(streamFile, RwStreamChunkType.IMAGE, renderwareVersion, parentChunk);
        this.texturePrefix = texturePrefix;
        this.mipMapLevelId = mipMapLevelId;
    }

    @Override
    public void loadChunkData(DataReader reader, int dataLength, int version) {
        int chunkStartIndex = reader.getIndex();

        RwImage imageDef = readStruct(reader, RwImage.class);
        int bitDepth = imageDef.getDepth();
        int imageWidth = imageDef.getWidth();
        int imageHeight = imageDef.getHeight();
        int stride = imageDef.getStride();

        // Any corrective calculations should use unmodified values.
        // The following scenarios with values as zero have no explanation for why they are this way.
        // Further reverse engineering could help shed light on why this might occur sometimes.
        // Either
        if (imageHeight == 0) { // Seen in Frogger Rescue, but not Frogger Beyond.
            if (imageDef.getDepth() != 0 && imageDef.getWidth() != 0) {
                int paletteSize = getPaletteSize(imageDef.getDepth());
                int imageDataSizeInBytes = dataLength - (reader.getIndex() - chunkStartIndex) - (paletteSize * COLOR_SIZE_IN_BYTES);
                if ((imageDataSizeInBytes % imageDef.getWidth()) == 0) {
                    imageHeight = (imageDataSizeInBytes / imageDef.getWidth()) / Math.max(1, imageDef.getDepth() / Constants.BITS_PER_BYTE);
                    //getLogger().info("Calculated missing image height to be " + imageHeight + ". " + imageDef);
                } else {
                    //noinspection ReassignedVariable,SuspiciousNameCombination
                    imageHeight = imageWidth; // This is definitely wrong, but we're unable to calculate the correct answer, so this will just have to do.
                    getLogger().warning("Failed to calculate missing image height! %s", imageDef);
                }
            } else {
                //noinspection ReassignedVariable,SuspiciousNameCombination
                imageHeight = imageWidth; // This is definitely wrong, but we're unable to calculate the correct answer, so this will just have to do.
                getLogger().warning("Image definition had height of zero! %s", imageDef);
            }
        }

        if (bitDepth == 0) { // Seen in Frogger Rescue, but not Frogger Beyond.
            int remainingDataSizeInBytes = dataLength - (reader.getIndex() - chunkStartIndex);

            if (imageDef.getWidth() != 0 && imageDef.getHeight() != 0) {
                int pixelCount = imageWidth * imageHeight;
                if ((pixelCount * COLOR_SIZE_IN_BYTES) == remainingDataSizeInBytes) {
                    bitDepth = 32;
                    //getLogger().info("Calculated missing image bitDepth to be " + bitDepth + ". " + imageDef);
                } else if (pixelCount + (getPaletteSize(4) * COLOR_SIZE_IN_BYTES) == remainingDataSizeInBytes) {
                    bitDepth = 4;
                    //getLogger().info("Calculated missing image bitDepth to be " + bitDepth + ". " + imageDef);
                } else if (pixelCount + (getPaletteSize(8) * COLOR_SIZE_IN_BYTES) == remainingDataSizeInBytes) {
                    bitDepth = 8;
                    //getLogger().info("Calculated missing image bitDepth to be " + bitDepth + ". " + imageDef);
                } else {
                    getLogger().warning("Failed to calculate missing image bitDepth! Defaulting to four! %s", imageDef);
                    bitDepth = 4;
                }
            } else {
                getLogger().warning("Image definition had a bit-depth of zero! Defaulting to four! %s", imageDef);
                bitDepth = 4;
            }
        }

        int calculatedStride = calculateStride(imageWidth, bitDepth);
        if (stride == 0) { // Seen in Frogger Rescue, but not Frogger Beyond.
            //getLogger().info("Calculated missing stride and got " + calculatedStride + ". " + imageDef);
            stride = calculatedStride;
        } else if (calculatedStride != stride) {
            getLogger().warning("Incorrectly calculated missing stride as %d. (%s)", calculatedStride, imageDef);
        }

        if (bitDepth != 4 && bitDepth != 8 && bitDepth != 32) // Asserted in RwImageCreate, these appear to be the only supported bit depths.
            throw new UnsupportedOperationException("The RwImageChunk reported an unsupported bit-depth! (" + imageDef + ")");

        IndexColorModel palette = null;
        if (bitDepth <= 8) {
            reader.jumpTemp(reader.getIndex() + (stride * imageHeight));
            palette = readPalette(reader, bitDepth);
            reader.jumpReturn();
            if (palette == null)
                throw new IllegalStateException("Read empty palette.");

            this.image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_INDEXED, palette);
        } else {
            this.image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        }

        int pixelBufStartIndex = reader.getIndex();
        int pixelBufEndIndex = reader.getIndex() + (stride * imageHeight);

        while (pixelBufEndIndex > reader.getIndex()) {
            int bytePos = (reader.getIndex() - pixelBufStartIndex);

            switch (bitDepth) {
                case 4:
                    byte value = reader.readByte();
                    setPixel(this.image, bytePos, stride, bitDepth, palette.getRGB(value & 0x0F), value & 0xFF);
                    break;
                case 8:
                    short value2 = reader.readUnsignedByteAsShort();
                    setPixel(this.image, bytePos, stride, bitDepth, palette.getRGB(value2), value2);
                    break;
                case 32:
                    int value3 = reader.readInt();
                    setPixel(this.image, bytePos / COLOR_SIZE_IN_BYTES, stride / COLOR_SIZE_IN_BYTES, bitDepth, ColorUtils.swapRedBlue(value3), value3);
                    break;
            }
        }

        reader.skipBytes(getPaletteSize(bitDepth) * COLOR_SIZE_IN_BYTES); // Skip the palette.

        // Calculation of bitDepth should happen last, since it relies upon the image.
        int calculatedBitDepth = getBitDepth();
        if (calculatedBitDepth != bitDepth)
            getLogger().warning("Calculated incorrect bitDepth! Real: %d, Calculated: %d", bitDepth, calculatedBitDepth);
    }

    private IndexColorModel readPalette(DataReader reader, int bitDepth) {
        int paletteSize = getPaletteSize(bitDepth);
        if (paletteSize <= 0)
            return null;

        int transPixelIndex = -1;
        int[] colors = new int[paletteSize];
        for (int i = 0; i < colors.length; i++) {
            int color = ColorUtils.swapRedBlue(reader.readInt());
            colors[i] = color;
            if ((color & 0xFF000000) == 0 && (transPixelIndex < 0 || colors[transPixelIndex] > color))
                transPixelIndex = i;
        }

        return new IndexColorModel(bitDepth, paletteSize, colors, 0, true, transPixelIndex, DataBuffer.TYPE_BYTE);
    }

    @Override
    public void saveChunkData(DataWriter writer) {
        int imageWidth = this.image.getWidth();
        int imageHeight = this.image.getHeight();
        int bitDepth = getBitDepth();
        int stride = calculateStride(imageWidth, bitDepth);
        writeStruct(writer, new RwImage(getGameInstance(), imageWidth, imageHeight, bitDepth, stride));

        IndexColorModel palette = this.image.getColorModel() instanceof IndexColorModel ? ((IndexColorModel) this.image.getColorModel()) : null;
        if (bitDepth <= 8 && palette == null)
            throw new IllegalStateException("Expected " + this + " to have an indexed color palette, but it did not.");

        // Write pixel data.
        for (int y = 0; y < imageHeight; y++) {
            int pixelLineStartIndex = writer.getIndex();
            for (int x = 0; x < imageWidth; x++) {
                switch (bitDepth) {
                    case 4:
                    case 8:
                        writer.writeUnsignedByte((short) this.image.getRaster().getDataElements(x, y, null));
                        break;
                    case 32:
                        writer.writeInt(ColorUtils.swapRedBlue(this.image.getRGB(x, y)));
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported bitDepth: " + bitDepth);
                }
            }

            writer.writeTo(pixelLineStartIndex + stride, PADDING_BYTE);
        }

        // Write palette.
        if (bitDepth <= 8) {
            int paletteSize = getPaletteSize(bitDepth);
            for (int i = 0; i < paletteSize; i++)
                writer.writeInt(ColorUtils.swapRedBlue(palette.getRGB(i)));
        }
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        if (this.texturePrefix != null)
            this.texturePrefix.addToPropertyList(propertyList);

        propertyList.add("Image Dimensions", this.image.getWidth() + " x " + this.image.getHeight());
        propertyList.add("Bit Depth", getBitDepth());
        propertyList.add("Stride", getStride());
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return new RwImageViewUIController(this);
    }

    @Override
    public String getCollectionViewDisplayName() {
        String displayInfo = this.texturePrefix != null ? this.texturePrefix.getDisplayInfo(this.mipMapLevelId) : null;
        return super.getCollectionViewDisplayName() + (displayInfo != null ? " " + displayInfo : "");
    }

    @Override
    public String getLoggerInfo() {
        if (this.image != null && this.image != DEFAULT_IMAGE) {
            int bitDepth = getBitDepth();
            return super.getLoggerInfo() + ",dimensions=" + this.image.getWidth() + "x" + this.image.getHeight()
                    + ",bitDepth=" + bitDepth + ",stride=" + calculateStride(this.image.getWidth(), bitDepth);
        } else {
            return super.getLoggerInfo() + ",null image";
        }
    }

    /**
     * Calculates the image bit-depth.
     */
    public int getBitDepth() {
        return calculateBitDepth(this.image);
    }

    /**
     * Calculates the image pixel stride. (Number of bytes used per row of pixels)
     */
    public int getStride() {
        return calculateStride(this.image.getWidth(), getBitDepth());
    }

    /**
     * Replaces the image contents with a new image.
     * @param newImage the new image to apply
     */
    public void setImage(BufferedImage newImage) {
        if (newImage == null)
            throw new NullPointerException("newImage");
        if (newImage == this.image)
            return; // Same image as before.

        // Ensure supported type.
        if (newImage.getType() != BufferedImage.TYPE_BYTE_INDEXED)
            newImage = ImageUtils.convertBufferedImageToFormat(newImage, BufferedImage.TYPE_INT_ARGB);

        // TODO: Consider trying to create the smallest palette for the image.

        try {
            calculateBitDepth(newImage); // Ensures the new image has an appropriate bit-depth.
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, true, "The image appears to be in the wrong format.");
        }

        this.image = newImage;
        fireChangeEvent(newImage);
    }

    private static int calculateStride(int width, int bitDepth) {
        int stride = ((width * bitDepth) + (Constants.BITS_PER_BYTE - 1)) / Constants.BITS_PER_BYTE;
        if (bitDepth == 4) // RenderWare doesn't support packed pixel formats, so there's still one pixel per-byte, even if only 4 bits are used.
            stride <<= 1;
        if ((stride % Constants.INTEGER_SIZE) != 0) // alignment
            stride += Constants.INTEGER_SIZE - (stride % Constants.INTEGER_SIZE);

        return stride;
    }

    private static int calculateBitDepth(BufferedImage image) {
        if (image == null)
            throw new NullPointerException("Cannot calculate bitDepth with null image.");

        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            return 32;
        } else if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
            int bitsPerPixel = image.getColorModel().getPixelSize();
            if (bitsPerPixel > 8 || bitsPerPixel < 4) {
                throw new IllegalStateException("Unsupported bitsPerPixel of " + bitsPerPixel);
            } else if (bitsPerPixel > 4) {
                return 8;
            } else {
                return 4;
            }
        } else {
            throw new IllegalStateException("Unsupported BufferedImage type: " + image.getType());
        }
    }

    private void setPixel(BufferedImage image, int bytePos, int stride, int bitDepth, int color, int value) {
        int x = bytePos % stride;
        int y = bytePos / stride;
        if (x >= image.getWidth()) {
            if (((bitDepth == 32) || (bitDepth == 4 && (value & 0xF0) != 0) || (bitDepth == 8 && value != (PADDING_BYTE & 0xFF))) && (image.getWidth() > 4 || image.getHeight() > 4)) // Frogger's Adventures The Rescue seems to have garbage data for extremely tiny images. No clue why.
                getLogger().warning("Unusual padding value at [%d, %d]. Value: 0x%X", x, y, value);
            return;
        }

        image.setRGB(x, y, color);
    }

    private static int getPaletteSize(int bitDepth) {
        switch (bitDepth) {
            case 4:
                return 16;
            case 8:
                return 256;
            case 32:
                return 0; // No palette
            default:
                throw new IllegalArgumentException("Unsupported bit-depth: " + bitDepth);
        }
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

    @SuppressWarnings("FieldCanBeLocal")
    public static class RwImageViewUIController extends GameUIController<GameInstance> {
        private final ImageView imageView;
        private final HBox buttonGroup;
        private final Button importButton;
        private final Button exportButton;
        private final RwImageChunk imageChunk;

        public RwImageViewUIController(RwImageChunk imageChunk) {
            super(imageChunk.getGameInstance());
            this.imageChunk = imageChunk;
            this.imageView = new ImageView(FXUtils.toFXImage(imageChunk.getImage(), false)); // Must be assigned here, or it will be null in onControllerLoad().
            this.importButton = new Button("Import");
            this.importButton.setOnAction(evt -> importImage());
            this.exportButton = new Button("Export");
            this.importButton.setOnAction(evt -> exportImage());
            this.buttonGroup = new HBox(5, this.importButton, this.exportButton);
            this.buttonGroup.setAlignment(Pos.CENTER);
            loadController(new VBox(3, this.imageView, this.buttonGroup));
        }

        @Override
        public VBox getRootNode() {
            return (VBox) super.getRootNode();
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            getRootNode().setAlignment(Pos.CENTER);
        }

        /**
         * Prompts the user to export the active image.
         */
        public void exportImage() {
            String suggestedTextureName = this.imageChunk.texturePrefix != null ? this.imageChunk.getTexturePrefix().getName() : null;
            if (suggestedTextureName == null || suggestedTextureName.trim().isEmpty())
                suggestedTextureName = "unknown";

            FileUtils.askUserToSaveImageFile(getLogger(), getGameInstance(), this.imageChunk.getImage(), suggestedTextureName);
        }

        /**
         * Prompts the user to overwrite the active image.
         */
        public void importImage() {
            BufferedImage image = FileUtils.askUserToOpenImageFile(getLogger(), getGameInstance());
            if (image != null)
                this.imageChunk.setImage(image);
        }
    }
}