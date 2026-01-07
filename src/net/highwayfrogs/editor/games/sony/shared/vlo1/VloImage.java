package net.highwayfrogs.editor.games.sony.shared.vlo1;

import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.psx.image.PsxVram;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents an image within a .vlo file, in the original (pre-1997) VLO format.
 * Created by Kneesnap on 12/13/2025.
 */
public class VloImage extends SCSharedGameData implements Cloneable, ITextureSource {
    @Getter private final List<Consumer<BufferedImage>> imageChangeListeners;
    @Getter private final VloFile parent;
    @Getter private short vramX;
    @Getter private short vramY;
    private short width;
    private short height;
    private byte[] imageBytes;

    private transient int tempImageDataPointer = -1;
    private transient BufferedImage cachedImage;

    public static final int BYTES_PER_PIXEL = PsxVram.PSX_VRAM_LOAD_FORMAT_BYTES_PER_PIXEL;

    public VloImage(VloFile parentFile) {
        super(parentFile != null ? parentFile.getGameInstance() : null);
        this.imageChangeListeners = new ArrayList<>();
        this.parent = parentFile;
    }

    @Override
    public void load(DataReader reader) {
        this.vramX = reader.readShort();
        this.vramY = reader.readShort();
        this.width = reader.readShort();
        this.height = reader.readShort();
        this.tempImageDataPointer = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.vramX);
        writer.writeShort(this.vramY);
        writer.writeShort(this.width);
        writer.writeShort(this.height);
        this.tempImageDataPointer = writer.writeNullPointer();
    }

    /**
     * Load image data.
     * @param reader The reader to read image data from.
     */
    void readImageData(DataReader reader) {
        if (this.tempImageDataPointer < 0)
            throw new RuntimeException("Cannot read image data, the image data pointer is invalid.");

        requireReaderIndex(reader, this.tempImageDataPointer, "Expected image data");
        this.tempImageDataPointer = -1;

        int pixelCount = this.width * this.height;
        this.imageBytes = reader.readBytes(pixelCount * BYTES_PER_PIXEL);
    }

    /**
     * Save extra data.
     * @param writer The writer to save data to.
     */
    void writeImageData(DataWriter writer) {
        if (this.tempImageDataPointer < 0)
            throw new RuntimeException("Cannot write image data, the image data pointer is invalid.");
        int actualLength = this.imageBytes != null ? this.imageBytes.length : 0;
        int expectedLength = this.width * this.height * BYTES_PER_PIXEL;
        if (actualLength != expectedLength)
            throw new IllegalStateException("The image was expected to have " + expectedLength + " bytes, but actually had " + actualLength + " bytes of data.");

        writer.writeAddressTo(this.tempImageDataPointer);
        this.tempImageDataPointer = -1;
        if (this.imageBytes != null)
            writer.writeBytes(this.imageBytes);
    }

    /**
     * Invalidate the cached image.
     */
    public void invalidateCache() {
        this.cachedImage = null;
    }

    /**
     * Export this image exactly how it is saved in the database.
     * @return bufferedImage
     */
    public BufferedImage toBufferedImage() {
        if (this.cachedImage != null)
            return this.cachedImage;

        return this.cachedImage = makeImage();
    }

    /**
     * Test whether this image contains a certain coordinate in VRAM.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return contains
     */
    public boolean contains(double x, double y) {
        return x >= getVramX() && x <= (getVramX() + getWidth())
                && y >= getVramY() && y <= (getVramY() + getHeight());
    }

    @Override
    @SneakyThrows
    public VloImage clone() {
        return (VloImage) super.clone();
    }

    /**
     * Gets the local VLO image id.
     */
    public int getLocalImageID() {
        return getParent().getImages().indexOf(this);
    }

    @Override
    public BufferedImage makeImage() {
        return null; // TODO: Implement.
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
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
    public int getUpPadding() {
        return 0;
    }

    @Override
    public int getDownPadding() {
        return 0;
    }

    @Override
    public void fireChangeEvent(BufferedImage newImage) {
        invalidateCache();
        fireChangeEvent0(newImage);
    }
}
