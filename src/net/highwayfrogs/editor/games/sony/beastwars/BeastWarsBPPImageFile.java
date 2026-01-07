package net.highwayfrogs.editor.games.sony.beastwars;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.psx.math.vector.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.beastwars.ui.BeastWarsBppFileUIController;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygon;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.image.ImageUtils;
import net.highwayfrogs.editor.utils.image.quantization.octree.OctreeQuantizer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

/**
 * Represents a .BPP (8 bit indexed image) file as seen in Beast Wars.
 * Created by Kneesnap on 3/3/2025.
 */
@Getter
public class BeastWarsBPPImageFile extends SCGameFile<BeastWarsInstance> {
    @NonNull private BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_INDEXED);
    private static final int COLOR_PALETTE_SIZE = 256;
    private static final int BITS_PER_PIXEL = 8;

    public BeastWarsBPPImageFile(BeastWarsInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        int imageDataPointer = reader.readInt();
        int imageWidth = reader.readUnsignedShortAsInt();
        int imageHeight = reader.readUnsignedShortAsInt();
        int clutDataPointer = reader.readInt();
        requireReaderIndex(reader, imageDataPointer, "Expected BPP Image Data");

        // Read palettes first.
        reader.jumpTemp(clutDataPointer + imageDataPointer);
        int[] colorPaletteInts = new int[COLOR_PALETTE_SIZE];
        for (int i = 0; i < colorPaletteInts.length; i++)
            colorPaletteInts[i] = fromPackedShortToARGB(reader.readShort());

        int paletteDataEndIndex = reader.getIndex();
        reader.jumpReturn();

        // Read image pixels.
        IndexColorModel colorPalette = new IndexColorModel(BITS_PER_PIXEL, colorPaletteInts.length, colorPaletteInts, 0, false, -1, DataBuffer.TYPE_BYTE);
        this.image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_INDEXED, colorPalette);
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                short pixelIndex = reader.readUnsignedByteAsShort();
                this.image.setRGB(x, y, colorPalette.getRGB(pixelIndex));
            }
        }

        // Reached end of image data.
        requireReaderIndex(reader, clutDataPointer + imageDataPointer, "Expected Clut Palette Data");
        reader.setIndex(paletteDataEndIndex); // Ensure at end of file.
    }

    @Override
    public void save(DataWriter writer) {
        int imageDataPointer = writer.writeNullPointer();
        writer.writeUnsignedShort(this.image.getWidth());
        writer.writeUnsignedShort(this.image.getHeight());
        int clutDataPointer = writer.writeNullPointer();

        // Write image data pointer.
        writer.writeAddressTo(imageDataPointer);
        imageDataPointer = writer.getIndex();

        // Write image data.
        for (int y = 0; y < this.image.getHeight(); y++)
            for (int x = 0; x < this.image.getWidth(); x++)
                writer.writeUnsignedByte((short) this.image.getRaster().getDataElements(x, y, null));

        // Write image palette.
        writer.writeIntAtPos(clutDataPointer, writer.getIndex() - imageDataPointer);

        IndexColorModel palette = this.image.getColorModel() instanceof IndexColorModel ? ((IndexColorModel) this.image.getColorModel()) : null;
        if (palette == null)
            throw new RuntimeException("Expected " + this + " to have an indexed color palette, but it did not.");

        for (int i = 0; i < COLOR_PALETTE_SIZE; i++) {
            int rgbValue = palette.getMapSize() > i ? palette.getRGB(i) : 0;
            writer.writeShort(toPackedShort(rgbValue));
        }
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.PHOTO_ALBUM_32.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-bpp", new BeastWarsBppFileUIController(getGameInstance()), this);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("BPP (Bits Per Pixel)", BITS_PER_PIXEL);
        propertyList.add("Width", this.image.getWidth());
        propertyList.add("Height", this.image.getHeight());
    }

    /**
     * Sets the image currently active.
     * @param newImage The new image to apply
     */
    public void setImage(BufferedImage newImage) {
        if (newImage == null)
            throw new NullPointerException("newImage");
        if (newImage.getWidth() == 0 || newImage.getHeight() == 0)
            throw new IllegalArgumentException("Cannot accept newImage with a width/height of zero!");

        newImage = OctreeQuantizer.quantizeImage(newImage, 256); // Quantize down to 256 colors.
        BufferedImage indexedImage = ImageUtils.tryConvertTo8BitIndexedBufferedImage(newImage);
        if (indexedImage == null) // This should not happen after quantization occurs.
            throw new IllegalArgumentException("Could not convert the newImage to an 8-bit indexed color mode! Reduce the number of unique colors in the image and try again!");

        this.image = newImage;
    }

    private static int fromPackedShortToARGB(short packedColor) {
        // Swap bytes... This is what the game does. PSX PAL has the swap loop at 0x80035C64.
        short swappedColor = DataUtils.swapShortByteOrder(packedColor);
        return SCMapPolygon.fromPackedShort(swappedColor, PSXPolygonType.POLY_FT4, false, false).toARGB();
    }

    private static short toPackedShort(int rgbColor) {
        return DataUtils.swapShortByteOrder(SCMapPolygon.toPackedShort(CVector.makeColorFromRGB(rgbColor)));
    }
}
