package net.highwayfrogs.editor.file.vlo;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.psx.PSXClutColor;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse.BlackFilter;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse.TransparencyFilter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * A singular game image. MR_TXSETUP struct.
 * Created by Kneesnap on 8/30/2018.
 */
@SuppressWarnings({"unused", "lossy-conversions"})
public class GameImage extends SCSharedGameData implements Cloneable, ITextureSource {
    @Getter private final List<Consumer<BufferedImage>> imageChangeListeners;
    @Getter private final VLOArchive parent;
    @Getter @Setter private short vramX;
    @Getter @Setter private short vramY;
    @Getter @Setter private short fullWidth;
    @Getter @Setter private short fullHeight;
    @Getter @Setter private short textureId;
    @Getter private short flags;
    @Getter private short clutId;
    private byte ingameWidth; // In-game texture width, used to remove texture padding.
    private byte ingameHeight;
    private byte[] imageBytes;
    @Getter private ImageClutMode clutMode; // TPF
    @Getter private int abr; // ABR.

    private transient int tempImageDataPointer = -1;
    private transient BufferedImage cachedImage;

    public static final int MAX_DIMENSION = 256;
    private static final int PC_BYTES_PER_PIXEL = 4;
    public static final int PC_PAGE_WIDTH = 256;
    public static final int PC_PAGE_HEIGHT = 256;
    public static final int PSX_PAGE_WIDTH = 64;
    public static final int PSX_FULL_PAGE_WIDTH = 256;
    public static final int PSX_PAGE_HEIGHT = 256;
    public static final int PSX_X_PAGES = 16;
    public static final int PSX_Y_PAGES = 2;
    public static final int TOTAL_PAGES = 32; // It seems to be 32 on both PC and PS1. We can't go higher than this because it encodes only 5 bits for the page id. It appears the PC version rendering dlls only create 14 pages though.

    public static final int FLAG_TRANSLUCENT = Constants.BIT_FLAG_0; // Used by sprites + MOFs in the MR API to enable semi-transparent rendering mode.
    //public static final int FLAG_ROTATED = Constants.BIT_FLAG_1; // Unused in MR API + Frogger, does not appear to be set.
    public static final int FLAG_HIT_X = Constants.BIT_FLAG_2; // Appears to decrease width by 1?
    public static final int FLAG_HIT_Y = Constants.BIT_FLAG_3; // Appears to decrease height by 1?
    public static final int FLAG_REFERENCED_BY_NAME = Constants.BIT_FLAG_4; // It means it has an entry in bmp_pointers. Images without this flag can be dynamically loaded/unloaded without having a fixed memory location which the code can access the texture info from.
    public static final int FLAG_BLACK_IS_TRANSPARENT = Constants.BIT_FLAG_5; // Seems like it may not be used. Would be weird if that were the case.
    public static final int FLAG_2D_SPRITE = Constants.BIT_FLAG_15; // Indicates that an animation list should be used when the image is used to create a sprite. I dunno, it seems like every single texture in frogger has this flag set. (Though this is not confirmed, let alone confirmed for all versions)
    private static final int VALIDATION_FLAGS = FLAG_2D_SPRITE | FLAG_BLACK_IS_TRANSPARENT | FLAG_REFERENCED_BY_NAME | FLAG_HIT_Y | FLAG_HIT_X | FLAG_TRANSLUCENT;

    public GameImage(VLOArchive parent) {
        super(parent != null ? parent.getGameInstance() : null);
        this.imageChangeListeners = new ArrayList<>();
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        this.vramX = reader.readShort();
        this.vramY = reader.readShort();
        this.fullWidth = reader.readShort();
        this.fullHeight = reader.readShort();

        this.tempImageDataPointer = reader.readInt();
        this.textureId = reader.readShort();

        short readPage = reader.readShort();
        this.clutMode = ImageClutMode.values()[(readPage & 0b110000000) >> 7];
        this.abr = (readPage & 0b1100000) >> 5;
        if (getTexturePageShort() != readPage) // Verify this is both read and calculated properly.
            throw new RuntimeException("Calculated tpage short as " + getTexturePageShort() + ", Real: " + readPage + "!");

        // Can do this after texturePage is set.
        this.vramX *= getWidthMultiplier();
        this.fullWidth *= getWidthMultiplier();

        if (getParent().isPsxMode()) {
            this.clutId = reader.readShort();
            this.flags = reader.readShort();
        } else {
            this.flags = reader.readShort();
            this.clutId = reader.readShort();
        }

        if (getGameInstance().getGameType().isAtOrBefore(SCGameType.FROGGER))
            warnAboutInvalidBitFlags(this.flags & 0xFFFF, VALIDATION_FLAGS, toString());

        short readU = reader.readUnsignedByteAsShort();
        short readV = reader.readUnsignedByteAsShort();
        this.ingameWidth = reader.readByte();
        this.ingameHeight = reader.readByte();

        if (readU != getU() || readV != getV())
            getLogger().warning("UV Mismatch at image %d! [%d,%d] [%d,%d] -> %dx%d, %dx%d, %04X [%d, %d]",
                    Utils.getLoadingIndex(this.parent.getImages(), this), readU, readV, getU(), getV(),
                    getIngameWidth(), getIngameHeight(), getFullWidth(), getFullHeight(), getFlags(),
                    (getFullWidth() - getIngameWidth()), (getFullHeight() - getIngameHeight()));
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort((short) (this.vramX / getWidthMultiplier()));
        writer.writeShort(this.vramY);

        writer.writeShort((short) (this.fullWidth / getWidthMultiplier()));
        writer.writeShort(this.fullHeight);
        this.tempImageDataPointer = writer.writeNullPointer();
        writer.writeShort(this.textureId);

        short oldVramX = this.vramX;
        this.vramX /= getWidthMultiplier();
        writer.writeShort(getTexturePageShort());
        this.vramX = oldVramX;

        if (getParent().isPsxMode()) {
            writer.writeShort(this.clutId);
            writer.writeShort(this.flags);
        } else {
            writer.writeShort(this.flags);
            writer.writeShort(this.clutId);
        }

        writer.writeUnsignedByte(getU());
        writer.writeUnsignedByte(getV());
        writer.writeByte(this.ingameWidth);
        writer.writeByte(this.ingameHeight);
    }

    @Override
    public ILogger getLogger() {
        return this.parent != null ? this.parent.getLogger() : super.getLogger();
    }

    @Override
    public String toString() {
        return "GameImage{id=" + this.textureId + (this.parent != null ? "@" + this.parent.getFileDisplayName() : "") + "}";
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

        // Read image.
        int pixelCount = getFullWidth() * getFullHeight();
        if (getParent().isPsxMode()) {
            ByteBuffer buffer = ByteBuffer.allocate(PC_BYTES_PER_PIXEL * pixelCount);

            if (getClutMode() == ImageClutMode.MODE_15BIT_NO_CLUT) { // Used in PS1 demo. Example: Frogger's eye, VOL@35 (The fireball texture)
                for (int i = 0; i < pixelCount; i++)
                    buffer.putInt(PSXClutColor.readBGRAColorFromShort(reader.readShort(), false));
            } else if (getClutMode() == ImageClutMode.MODE_8BIT) { // Used in PS1 release. Example: STARTNTSC.VLO
                ClutEntry clut = getClut();
                for (int i = 0; i < pixelCount; i++)
                    readPSXPixel(reader.readUnsignedByteAsShort(), clut, buffer);
            } else { // 4bit (normal) mode.
                ClutEntry clut = getClut();
                for (int i = 0; i < pixelCount / 2; i++) { // We read two pixels per iteration.
                    short value = reader.readUnsignedByteAsShort();
                    int low = value & 0x0F;
                    int high = value >> 4;

                    readPSXPixel(low, clut, buffer);
                    readPSXPixel(high, clut, buffer);
                }
            }

            this.imageBytes = buffer.array();
        } else {
            this.imageBytes = reader.readBytes(pixelCount * PC_BYTES_PER_PIXEL);
        }
    }

    /**
     * Save extra data.
     * @param writer The writer to save data to.
     */
    void writeImageData(DataWriter writer) {
        if (this.tempImageDataPointer < 0)
            throw new RuntimeException("Cannot write image data, the image data pointer is invalid.");

        writer.writeAddressTo(this.tempImageDataPointer);
        writeImageBytes(writer);

        this.tempImageDataPointer = -1;
    }

    private void writeImageBytes(DataWriter writer) {
        if (!getParent().isPsxMode()) {
            writer.writeBytes(this.imageBytes); // The image bytes as they are loaded are already as they should be when saved.
            return;
        }

        if (getClutMode() == ImageClutMode.MODE_15BIT_NO_CLUT) {
            for (int i = 0; i < this.imageBytes.length; i += PC_BYTES_PER_PIXEL)
                PSXClutColor.fromRGBA(this.imageBytes, i).save(writer);
            return;
        }

        ClutEntry clut = getClut();
        clut.getColors().clear(); // Generate a new clut.
        int maxColors = getClut().calculateColorCount();

        for (int i = 0; i < this.imageBytes.length; i += PC_BYTES_PER_PIXEL) {
            PSXClutColor color = PSXClutColor.fromRGBA(this.imageBytes, i);
            if (!clut.getColors().contains(color))
                clut.getColors().add(color);
        }

        if (clut.getColors().size() > maxColors)
            throw new RuntimeException("Tried to save a PSX image with too many colors. [Max: " + maxColors + ", Colors: " + clut.getColors().size() + "]");

        clut.getColors().sort(Comparator.comparingInt(PSXClutColor::toRGBA));

        if (getClutMode() == ImageClutMode.MODE_8BIT) {
            for (int i = 0; i < this.imageBytes.length; i += PC_BYTES_PER_PIXEL)
                writer.writeByte((byte) clut.getColors().indexOf(PSXClutColor.fromRGBA(this.imageBytes, i)));
        } else if (getClutMode() == ImageClutMode.MODE_4BIT) {
            for (int i = 0; i < this.imageBytes.length; i += (PC_BYTES_PER_PIXEL * 2)) {
                PSXClutColor color1 = PSXClutColor.fromRGBA(this.imageBytes, i);
                PSXClutColor color2 = PSXClutColor.fromRGBA(this.imageBytes, i + PC_BYTES_PER_PIXEL);
                writer.writeByte((byte) (clut.getColors().indexOf(color1) | (clut.getColors().indexOf(color2) << 4)));
            }
        } else {
            throw new RuntimeException("Could not handle clut mode: " + getClutMode());
        }

        // For any unfilled part of the clut, fill it with black.
        PSXClutColor unused = new PSXClutColor();
        while (maxColors > clut.getColors().size())
            clut.getColors().add(unused);
    }

    /**
     * Calculates the page this image lies in.
     * @return page
     */
    public short getPage() {
        return getPage(getVramX(), getVramY());
    }

    public short getMultiplierPage() {
        return getPage(getVramX() / getWidthMultiplier(), getVramY());
    }

    /**
     * Gets the page the end of this image lies on.
     * @return endPage
     */
    public short getEndPage() {
        return getPage(getVramX() / getWidthMultiplier() + ((getFullWidth() - 1) / getWidthMultiplier()), getVramY() + getFullHeight() - 1);
    }

    private short getPage(int vramX, int vramY) {
        if (getParent().isPsxMode()) {
            return (short) (((vramY / PSX_PAGE_HEIGHT) * PSX_X_PAGES) + (vramX / PSX_PAGE_WIDTH));
        } else if (getGameInstance().getGameType().isAtLeast(SCGameType.FROGGER)) {
            return (short) (vramY / PC_PAGE_HEIGHT);
        } else {
            // Old Frogger PSX Milestone 3 does this.
            return (short) (vramX / PC_PAGE_WIDTH);
        }
    }

    /**
     * Gets the tpage short for this image.
     * This information seems very similar to what's found on:
     * http://wiki.xentax.com/index.php/Playstation_TMD
     * @return tpageShort
     */
    public short getTexturePageShort() {
        return (short) ((getPage() & 0b11111) | (this.abr << 5) | (getClutMode().ordinal() << 7));
    }

    /**
     * Gets the width multiplier for the image.
     */
    public int getWidthMultiplier() {
        return getParent().isPsxMode() ? getClutMode().getMultiplier() : 1;
    }

    /**
     * Generates the U value used by this image.
     * @return uValue
     */
    public short getU() {
        short u = (short) (getVramX() % (getParent().isPsxMode() ? PSX_PAGE_WIDTH * getWidthMultiplier() : PC_PAGE_WIDTH));

        if (getParent().isPsxMode()) { // PS1 logic.
            short fullHeight = getFullHeight();
            short ingameHeight = getIngameHeight();

            // The purpose here is not fully understood, but seems to accurately reflect the original behavior in all games.
            if (fullHeight != ingameHeight && fullHeight != ingameHeight + 1)
                u++;
            return u;
        }

        // PC logic.
        return (short) (u + ((getFullWidth() - getIngameWidth()) / 2));
    }

    /**
     * Generates the V value used by this image.
     * @return vValue
     */
    public short getV() {
        return (short) ((getVramY() % (getParent().isPsxMode() ? PSX_PAGE_HEIGHT : PC_PAGE_HEIGHT)) + ((getFullHeight() - getIngameHeight()) / 2));
    }

    private void readPSXPixel(int clutIndex, ClutEntry clut, ByteBuffer buffer) {
        buffer.putInt(clut.getColors().get(clutIndex).toRGBA());
    }

    private ClutEntry getClut() {
        int clutX = ((clutId & 0x3F) << 4);
        int clutY = (clutId >> 6);

        for (ClutEntry testEntry : getParent().getClutEntries())
            if (testEntry.getClutRect().getX() == clutX && testEntry.getClutRect().getY() == clutY)
                return testEntry;

        throw new RuntimeException("FAiled to find clut for coordinates [" + clutX + ", " + clutY + "].");
    }

    /**
     * Set a flag state for this image.
     * @param flag     The flag to set.
     * @param newState The new flag state.
     * @return invalidateImage
     */
    public boolean setFlag(int flag, boolean newState) {
        boolean oldState = testFlag(flag);
        if (oldState == newState)
            return false;

        if (oldState) {
            this.flags ^= flag;
        } else {
            this.flags |= flag;
        }

        return flag == FLAG_BLACK_IS_TRANSPARENT || flag == FLAG_HIT_X || flag == FLAG_HIT_Y;
    }

    /**
     * Replace this texture with a new one.
     * @param image The new image to use.
     */
    public void replaceImage(BufferedImage image, ProblemResponse response) {
        // We can only parse TYPE_INT_ARGB, so if it's not that, we must convert the image to that, so it can be parsed properly.
        image = ImageWorkHorse.convertBufferedImageToFormat(image, BufferedImage.TYPE_INT_ARGB);

        // Ensure transparent pixels are now black.
        image = ImageWorkHorse.applyFilter(image, new BlackFilter());

        // Automatically generate padding if necessary.
        /*if (getIngameWidth() == image.getWidth() && getIngameHeight() == image.getHeight()) {
            // TODO: Automatically generate padding, and update dimensions.

        }*/

        // Now that the dimensions are finalized, grant easy access to them.
        short imageWidth = (short) image.getWidth();
        short imageHeight = (short) image.getHeight();

        // Ensure it's not too large.
        if (imageWidth > MAX_DIMENSION || imageHeight > MAX_DIMENSION) {
            Utils.handleProblem(response, getLogger(), Level.SEVERE, "The imported image is too big. The maximum image dimensions supported are %dx%d.", MAX_DIMENSION, MAX_DIMENSION);
            return;
        }

        // TODO: warn if VRAM overlap occurs, or page boundary is crossed.
        /*if ((imageWidth / getWidthMultiplier()) + getVramX() > MAX_DIMENSION)
            Utils.makePopUp("This image does not fit horizontally in VRAM. Use the VRAM editor to make it fit.", AlertType.WARNING);
         */


        if (getFullWidth() != imageWidth || getFullHeight() != imageHeight) {
            // If it's not the expected dimensions, use a default padding scheme.
            this.fullWidth = imageWidth;
            this.fullHeight = imageHeight;
            setIngameWidth((short) (imageWidth - 2));
            setIngameHeight((short) (imageHeight - 2));
        }

        // Read image rgba data.
        int[] array = new int[getFullHeight() * getFullWidth()];
        image.getRGB(0, 0, getFullWidth(), getFullHeight(), array, 0, getFullWidth());

        // Convert int array into byte array.
        ByteBuffer buffer = ByteBuffer.allocate(array.length * Constants.INTEGER_SIZE);
        buffer.asIntBuffer().put(array);
        byte[] bytes = buffer.array();

        // Convert BGRA -> ABGR, and write the new image bytes.
        this.imageBytes = bytes; // Override existing image.
        for (int i = 0; i < bytes.length; i += PC_BYTES_PER_PIXEL) { // Load image bytes.
            this.imageBytes[i] = (byte) (0xFF - this.imageBytes[i]); // Flip alpha.
            byte temp = this.imageBytes[i + 1];
            this.imageBytes[i + 1] = this.imageBytes[i + 3];
            this.imageBytes[i + 3] = temp;
        }

        invalidateCache();
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

        return this.cachedImage = makeUnmodifiedImage();
    }

    /**
     * Export this game image as a BufferedImage.
     * @param settings The settings to export this image with.
     * @return bufferedImage
     */
    public BufferedImage toBufferedImage(ImageFilterSettings settings) {
        return applyFilters(toBufferedImage(), settings);
    }

    /**
     * Export this game image as a JavaFX image.
     * @param settings The settings to export this image with.
     * @return fxImage
     */
    public Image toFXImage(ImageFilterSettings settings) {
        return FXUtils.toFXImage(toBufferedImage(settings), false);
    }

    /**
     * Export this game image as a JavaFX image.
     * @return fxImage
     */
    public Image toFXImage() {
        return FXUtils.toFXImage(toBufferedImage(), false);
    }

    /**
     * Apply filters to an image export.
     * @return newImage
     */
    public BufferedImage applyFilters(BufferedImage image, ImageFilterSettings setting) {
        return setting.applyFilters(this, image);
    }

    /**
     * Test if a flag is present.
     * @param test The flag to test.
     * @return hasFlag
     */
    public boolean testFlag(int test) {
        return (getFlags() & test) == test;
    }

    /**
     * Gets the in-game width of this image.
     * @return ingameWidth
     */
    public short getIngameWidth() {
        return (short) ((this.ingameWidth == 0 ? MAX_DIMENSION : DataUtils.byteToUnsignedShort(this.ingameWidth)) - (testFlag(FLAG_HIT_X) ? 1 : 0));
    }

    /**
     * Set the in-game width of this image.
     * @param width The in-game width.
     */
    public void setIngameWidth(short width) {
        Utils.verify(width >= 0 && width <= MAX_DIMENSION, "Image width is not in the required range: (0,%d].", MAX_DIMENSION);
        this.ingameWidth = (width == MAX_DIMENSION ? 0 : DataUtils.unsignedShortToByte(width));
        setFlag(FLAG_HIT_X, (width % 2) > 0);
    }

    /**
     * Gets the in-game height of this image.
     * @return ingameHeight
     */
    public short getIngameHeight() {
        return (short) ((this.ingameHeight == 0 ? MAX_DIMENSION : DataUtils.byteToUnsignedShort(this.ingameHeight)) - (testFlag(FLAG_HIT_Y) ? 1 : 0));
    }

    /**
     * Set the in-game height of this image.
     * @param height The in-game height.
     */
    public void setIngameHeight(short height) {
        Utils.verify(height >= 0 && height <= MAX_DIMENSION, "Image height is not in the required range (0,%d].", MAX_DIMENSION);
        this.ingameHeight = (height == MAX_DIMENSION ? 0 : DataUtils.unsignedShortToByte(height));
        setFlag(FLAG_HIT_Y, (height % 2) > 0);
    }

    /**
     * Test whether this image contains a certain coordinate in VRAM.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return contains
     */
    public boolean contains(double x, double y) {
        return x >= getVramX() && x <= (getVramX() + getFullWidth())
                && y >= getVramY() && y <= (getVramY() + getFullHeight());
    }

    /**
     * Gets the name configured as the original name for this image.
     */
    public String getOriginalName() {
        return getConfig().getImageNames().get(this.textureId);
    }

    @Override
    @SneakyThrows
    public GameImage clone() {
        return (GameImage) super.clone();
    }

    /**
     * Gets the local VLO image id.
     */
    public int getLocalImageID() {
        return getParent().getImages().indexOf(this);
    }

    private BufferedImage makeUnmodifiedImage() {
        int height = getFullHeight();
        int width = getFullWidth();

        // Create image.
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final int[] imageDataBuffer = ImageWorkHorse.getPixelIntegerArray(image);

        // Convert (Big Endian: ABGR, Little Endian: RGBA) -> (Big Endian: BGRA, Little Endian: ARGB), and store in the array.
        for (int i = 0; i < this.imageBytes.length; i += PC_BYTES_PER_PIXEL) {
            byte alpha = (byte) (0xFF - this.imageBytes[i]); // Alpha needs to be flipped.
            byte blue = this.imageBytes[i + 1];
            byte green = this.imageBytes[i + 2];
            byte red = this.imageBytes[i + 3];
            imageDataBuffer[i / PC_BYTES_PER_PIXEL] = ColorUtils.toARGB(red, green, blue, alpha);
        }

        return image;
    }

    @Override
    public boolean hasAnyTransparentPixels(BufferedImage image) {
        return testFlag(FLAG_TRANSLUCENT) || testFlag(FLAG_BLACK_IS_TRANSPARENT);
    }

    @Override
    public BufferedImage makeImage() {
        BufferedImage image = makeUnmodifiedImage();
        if (testFlag(FLAG_BLACK_IS_TRANSPARENT))
            image = ImageWorkHorse.applyFilter(image, new TransparencyFilter());

        return image;
    }

    @Override
    public int getWidth() {
        return getFullWidth();
    }

    @Override
    public int getHeight() {
        return getFullHeight();
    }

    @Override
    public int getLeftPadding() {
        if (getParent().isPsxMode()) {
            if (getFullHeight() != getIngameHeight()) // TODO: This is broken when getU() is broken.
                return 1;

            return 0;
        }

        // PC logic.
        return ((getFullWidth() - getIngameWidth()) / 2);
    }

    @Override
    public int getRightPadding() {
        return (getFullWidth() - getLeftPadding() - getIngameWidth());
    }

    @Override
    public int getUpPadding() {
        return (getFullHeight() - getIngameHeight()) / 2;
    }

    @Override
    public int getDownPadding() {
        return (getFullHeight() - getUpPadding() - getIngameHeight());
    }

    @Override
    public void fireChangeEvent(BufferedImage newImage) {
        invalidateCache();
        fireChangeEvent0(newImage);
    }
}