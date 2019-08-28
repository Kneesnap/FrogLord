package net.highwayfrogs.editor.file.vlo;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXClutColor;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse.BlackFilter;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Comparator;

/**
 * A singular game image. MR_TXSETUP struct.
 * Created by Kneesnap on 8/30/2018.
 */
@Getter
@Setter
@SuppressWarnings("unused")
public class GameImage extends GameObject implements Cloneable {
    private VLOArchive parent;
    private short vramX;
    private short vramY;
    private short fullWidth;
    private short fullHeight;
    private short textureId;
    private short flags;
    private short clutId;
    private byte ingameWidth; // In-game texture width, used to remove texture padding.
    private byte ingameHeight;
    private byte[] imageBytes;
    private ImageClutMode clutMode; // TPF
    private int abr; // ABR.

    private transient int tempSaveImageDataPointer;
    private transient BufferedImage cachedImage;

    public static final int MAX_DIMENSION = 256;
    private static final int PC_BYTES_PER_PIXEL = 4;
    public static final int PC_PAGE_WIDTH = 256;
    public static final int PC_PAGE_HEIGHT = 256;
    public static final int PSX_PAGE_WIDTH = 64;
    public static final int PSX_PAGE_HEIGHT = 256;
    public static final int PSX_X_PAGES = 16;
    public static final int PSX_Y_PAGES = 2;

    public static final int FLAG_TRANSLUCENT = Constants.BIT_FLAG_0;
    public static final int FLAG_ROTATED = Constants.BIT_FLAG_1; // Unused.
    public static final int FLAG_HIT_X = Constants.BIT_FLAG_2; //Appears to decrease width by 1?
    public static final int FLAG_HIT_Y = Constants.BIT_FLAG_3; //Appears to decrease height by 1?
    public static final int FLAG_REFERENCED_BY_NAME = Constants.BIT_FLAG_4; // All images have this. It means it has an entry in bmp_pointers.
    public static final int FLAG_BLACK_IS_TRANSPARENT = Constants.BIT_FLAG_5; // Seems like it may not be used. Would be weird if that were the case.
    public static final int FLAG_2D_SPRITE = Constants.BIT_FLAG_15;

    public GameImage(VLOArchive parent) {
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        this.vramX = reader.readShort();
        this.vramY = reader.readShort();
        this.fullWidth = reader.readShort();
        this.fullHeight = reader.readShort();

        int offset = reader.readInt();
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

        short readU = reader.readUnsignedByteAsShort();
        short readV = reader.readUnsignedByteAsShort();
        this.ingameWidth = reader.readByte();
        this.ingameHeight = reader.readByte();

        reader.jumpTemp(offset);

        int pixelCount = getFullWidth() * getFullHeight();
        if (getParent().isPsxMode()) {
            ByteBuffer buffer = ByteBuffer.allocate(PC_BYTES_PER_PIXEL * pixelCount);

            if (getClutMode() == ImageClutMode.MODE_15BIT_NO_CLUT) { // Used in PS1 demo. Example: Frogger's eye, VOL@35 (The fireball texture)
                for (int i = 0; i < pixelCount; i++)
                    buffer.putInt(PSXClutColor.readColorFromShort(reader.readShort()));
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

        reader.jumpReturn();
        //Utils.verify(getParent().isPsxMode() || (readU == getU() && readV == getV()), "Image UV does not match the calculated one! [%d,%d] [%d, %d]", readU, readV, getU(), getV()); // Psx mode has this disabled because there are lots of problems with saving PSX VLOs right now. //TODO: Fix this check.
        if (readU != getU() || readV != getV()) //TODO
            System.out.println(getParent().getFileEntry().getDisplayName() + "@" + getParent().getImages().size() + " Mismatch! [" + readU + "," + readV + "] [" + getU() + "," + getV() + "]");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort((short) (this.vramX / getWidthMultiplier()));
        writer.writeShort(this.vramY);

        writer.writeShort((short) (this.fullWidth / getWidthMultiplier()));
        writer.writeShort(this.fullHeight);
        this.tempSaveImageDataPointer = writer.writeNullPointer();
        writer.writeShort(this.textureId);
        writer.writeShort(getTexturePageShort());

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

    /**
     * Save extra data.
     * @param writer The writer to save data to.
     */
    public void saveExtra(DataWriter writer) {
        writer.writeAddressTo(this.tempSaveImageDataPointer);
        writeImageBytes(writer);
    }

    private void writeImageBytes(DataWriter writer) {
        if (!getParent().isPsxMode()) {
            writer.writeBytes(getImageBytes()); // The image bytes as they are loaded are already as they should be when saved.
            return;
        }

        if (getClutMode() == ImageClutMode.MODE_15BIT_NO_CLUT) {
            for (int i = 0; i < getImageBytes().length; i += PC_BYTES_PER_PIXEL)
                PSXClutColor.fromRGBA(this.imageBytes, i).save(writer);
            return;
        }

        ClutEntry clut = getClut();
        clut.getColors().clear(); // Generate a new clut.
        int maxColors = getClut().calculateColorCount();

        for (int i = 0; i < getImageBytes().length; i += PC_BYTES_PER_PIXEL) {
            PSXClutColor color = PSXClutColor.fromRGBA(this.imageBytes, i);
            if (!clut.getColors().contains(color))
                clut.getColors().add(color);
        }

        if (clut.getColors().size() > maxColors)
            throw new RuntimeException("Tried to save a PSX image with too many colors. [Max: " + maxColors + ", Colors: " + clut.getColors().size() + "]");

        clut.getColors().sort(Comparator.comparingInt(PSXClutColor::toRGBA));

        if (getClutMode() == ImageClutMode.MODE_8BIT) {
            for (int i = 0; i < getImageBytes().length; i += PC_BYTES_PER_PIXEL)
                writer.writeByte((byte) clut.getColors().indexOf(PSXClutColor.fromRGBA(this.imageBytes, i)));
        } else if (getClutMode() == ImageClutMode.MODE_4BIT) {
            for (int i = 0; i < getImageBytes().length; i += (PC_BYTES_PER_PIXEL * 2)) {
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
        return getParent().isPsxMode()
                ? (short) (((getVramY() / PSX_PAGE_HEIGHT) * PSX_X_PAGES) + (getVramX() / PSX_PAGE_WIDTH))
                : (short) (getVramY() / PC_PAGE_HEIGHT);
    }

    /**
     * Gets the tpage short for this image.
     * This information seems very similar to what's found on:
     * http://wiki.xentax.com/index.php/Playstation_TMD
     * @return tpageShort
     */
    public short getTexturePageShort() {
        return (short) (getPage() | (this.abr << 5) | (getClutMode().ordinal() << 7));
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
        return (short) ((getVramX() % MAX_DIMENSION) + ((getFullWidth() - getIngameWidth()) / 2));
    }

    /**
     * Generates the V value used by this image.
     * @return vValue
     */
    public short getV() {
        return (short) ((getVramY() % MAX_DIMENSION) + ((getFullHeight() - getIngameHeight()) / 2));
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

        return flag == FLAG_BLACK_IS_TRANSPARENT || flag == FLAG_HIT_X;
    }

    /**
     * Replace this texture with a new one.
     * @param image The new image to use.
     */
    public void replaceImage(BufferedImage image) {
        image = ImageWorkHorse.applyFilter(image, new BlackFilter());

        if (image.getType() != BufferedImage.TYPE_INT_ARGB) { // We can only parse TYPE_INT_ARGB, so if it's not that, we must convert the image to that, so it can be parsed properly.
            BufferedImage sourceImage = image;
            image = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics graphics = image.getGraphics();
            graphics.drawImage(sourceImage, 0, 0, null);
            graphics.dispose();
        }

        short imageWidth = (short) image.getWidth();
        short imageHeight = (short) image.getHeight();
        Utils.verify(imageWidth <= MAX_DIMENSION && imageHeight <= MAX_DIMENSION, "The imported image is too big. Frogger's engine only supports up to %dx%d.", MAX_DIMENSION, MAX_DIMENSION);

        if ((imageWidth / getWidthMultiplier()) + getVramX() > MAX_DIMENSION)
            Utils.makePopUp("This image does not fit horizontally in VRAM. Use the VRAM editor to make it fit.", AlertType.WARNING);

        if (imageWidth > getFullWidth() || imageHeight > getFullHeight())
            Utils.makePopUp("The image you have imported is larger than the image it replaced.\nThis may cause problems if it overlaps with another texture. Click on the 'VRAM' option to make sure the texture is ok.", AlertType.WARNING);

        if (getFullWidth() != imageWidth || getFullHeight() != imageHeight) {
            this.fullWidth = imageWidth;
            this.fullHeight = imageHeight;
            setIngameWidth((short) (imageWidth - 2));
            setIngameHeight((short) (imageHeight - 2));
        }

        // Read image rgba data.
        int[] array = new int[getFullHeight() * getFullWidth()];
        image.getRGB(0, 0, getFullWidth(), getFullHeight(), array, 0, getFullWidth());

        //Convert int array into byte array.
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

        int height = getFullHeight();
        int width = getFullWidth();

        byte[] cloneBytes = Arrays.copyOf(getImageBytes(), getImageBytes().length); // We don't want to make changes to the original array.
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        //ABGR -> BGRA
        for (int temp = 0; temp < cloneBytes.length; temp += PC_BYTES_PER_PIXEL) {
            byte alpha = cloneBytes[temp];
            int alphaIndex = temp + PC_BYTES_PER_PIXEL - 1;
            System.arraycopy(cloneBytes, temp + 1, cloneBytes, temp, alphaIndex - temp);
            cloneBytes[alphaIndex] = (byte) (0xFF - alpha); // Alpha needs to be flipped.
        }

        IntBuffer buffer = ByteBuffer.wrap(cloneBytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asIntBuffer();

        int[] array = new int[buffer.remaining()];
        buffer.get(array);
        image.setRGB(0, 0, image.getWidth(), image.getHeight(), array, 0, image.getWidth());
        return this.cachedImage = image;
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
        return Utils.toFXImage(toBufferedImage(settings), true);
    }

    /**
     * Export this game image as a JavaFX image.
     * @return fxImage
     */
    public Image toFXImage() {
        return Utils.toFXImage(toBufferedImage(), true);
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
     * Gets the in-game height of this image.
     * @return ingameHeight
     */
    public short getIngameHeight() {
        return this.ingameHeight == 0 ? MAX_DIMENSION : Utils.byteToUnsignedShort(this.ingameHeight);
    }

    /**
     * Gets the in-game width of this image.
     * @return ingameWidth
     */
    public short getIngameWidth() {
        return this.ingameWidth == 0 ? MAX_DIMENSION : Utils.byteToUnsignedShort(this.ingameWidth);
    }

    /**
     * Set the in-game height of this image.
     * @param height The in-game height.
     */
    public void setIngameHeight(short height) {
        Utils.verify(height >= 0 && height <= MAX_DIMENSION, "Image height is not in the required range (0,%d].", MAX_DIMENSION);
        this.ingameHeight = (height == MAX_DIMENSION ? 0 : Utils.unsignedShortToByte(height));
    }

    /**
     * Set the in-game width of this image.
     * @param width The in-game width.
     */
    public void setIngameWidth(short width) {
        Utils.verify(width >= 0 && width <= MAX_DIMENSION, "Image width is not in the required range: (0,%d].", MAX_DIMENSION);
        this.ingameWidth = (width == MAX_DIMENSION ? 0 : Utils.unsignedShortToByte(width));
    }

    /**
     * Test whether or not this image contains a certain coordinate in VRAM.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return contains
     */
    public boolean contains(double x, double y) {
        return x >= getVramX() && x <= (getVramX() + getFullWidth())
                && y >= getVramY() && y <= (getVramY() + getFullHeight());
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
}
