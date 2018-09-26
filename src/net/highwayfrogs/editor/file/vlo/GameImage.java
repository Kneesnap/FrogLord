package net.highwayfrogs.editor.file.vlo;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXClutColor;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A singular game image. MR_TXSETUP struct.
 * Created by Kneesnap on 8/30/2018.
 */
@Getter
@Setter
@SuppressWarnings("unused")
public class GameImage extends GameObject {
    private VLOArchive parent;
    private short vramX;
    private short vramY;
    private short fullWidth;
    private short fullHeight;
    private short textureId;
    private short texturePage;
    private short flags;
    private short clutId;
    private byte u; // Unsure. Texture orientation?
    private byte v;
    private byte ingameWidth; // In-game texture width, used to remove texture padding.
    private byte ingameHeight;
    private byte[] imageBytes;

    private AtomicInteger suppliedTextureOffset;

    private static final int MAX_DIMENSION = 256;
    private static final int PC_BYTES_PER_PIXEL = 4;
    private static final int PSX_PIXELS_PER_PC = 2;
    private static final int PSX_WIDTH_MODIFIER = 4;

    public static final int FLAG_TRANSLUCENT = 1;
    public static final int FLAG_ROTATED = 2; // Unused.
    public static final int FLAG_HIT_X = 4; //Appears to decrease width by 1?
    public static final int FLAG_HIT_Y = 8; //Appears to decrease height by 1?
    public static final int FLAG_REFERENCED_BY_NAME = 16; // Unsure.
    public static final int FLAG_BLACK_IS_TRANSPARENT = 32; // Seems like it may not be used. Would be weird if that were the case.
    public static final int FLAG_2D_SPRITE = 32768;

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
        this.texturePage = reader.readShort();

        if (getParent().isPsxMode()) {
            this.clutId = reader.readShort();
            this.flags = reader.readShort();
        } else {
            this.flags = reader.readShort();
            this.clutId = reader.readShort();
        }

        this.u = reader.readByte();
        this.v = reader.readByte();
        this.ingameWidth = reader.readByte();
        this.ingameHeight = reader.readByte();

        reader.jumpTemp(offset);

        int pixelCount = getFullWidth() * getFullHeight();
        if (getParent().isPsxMode()) {
            this.fullWidth *= PSX_WIDTH_MODIFIER;
            pixelCount *= PSX_WIDTH_MODIFIER;

            ByteBuffer buffer = ByteBuffer.allocate(PC_BYTES_PER_PIXEL * pixelCount);
            ClutEntry clut = getClut();

            for (int i = 0; i < pixelCount / PSX_PIXELS_PER_PC; i++) { // We read two pixels per iteration.
                short value = reader.readUnsignedByteAsShort();
                int low = value & 0x0F;
                int high = value >> 4;

                readPSXPixel(low, clut, buffer);
                readPSXPixel(high, clut, buffer);
            }

            this.imageBytes = buffer.array();
        } else {
            this.imageBytes = reader.readBytes(pixelCount * PC_BYTES_PER_PIXEL);
        }

        reader.jumpReturn();
    }

    private void readPSXPixel(int clutIndex, ClutEntry clut, ByteBuffer buffer) {
        PSXClutColor color = clut.getColors().get(clutIndex);

        byte[] arr = new byte[4]; //RGBA
        arr[0] = Utils.unsignedShortToByte(color.getUnsignedScaledRed());
        arr[1] = Utils.unsignedShortToByte(color.getUnsignedScaledGreen());
        arr[2] = Utils.unsignedShortToByte(color.getUnsignedScaledBlue());
        arr[3] = (byte) (0xFF - color.getAlpha(false));
        buffer.putInt(Utils.readNumberFromBytes(arr));
    }

    private ClutEntry getClut() {
        int clutX = ((clutId & 0x3F) << 4);
        int clutY = (clutId >> 6);

        ClutEntry clut = getParent().getClutEntries().stream()
                .filter(entry -> entry.getClutRect().getX() == clutX)
                .filter(entry -> entry.getClutRect().getY() == clutY)
                .findAny().orElse(null);

        Utils.verify(clut != null, "Failed to find clut for coordinates [%d,%d].", clutX, clutY);
        return clut;
    }

    @Override
    public void save(DataWriter writer) {
        Utils.verify(this.suppliedTextureOffset != null, "Image data offset was not specified.");

        writer.writeShort(this.vramX);
        writer.writeShort(this.vramY);

        short width = this.fullWidth;
        if (getParent().isPsxMode())
            width /= PSX_WIDTH_MODIFIER;
        writer.writeShort(width);

        writer.writeShort(this.fullHeight);
        writer.writeInt(this.suppliedTextureOffset.get());
        writer.writeShort(this.textureId);
        writer.writeShort(this.texturePage);

        if (getParent().isPsxMode()) {
            writer.writeShort(this.clutId);
            writer.writeShort(this.flags);
        } else {
            writer.writeShort(this.flags);
            writer.writeShort(this.clutId);
        }

        writer.writeByte(this.u);
        writer.writeByte(this.v);
        writer.writeByte(this.ingameWidth);
        writer.writeByte(this.ingameHeight);

        byte[] savedImageBytes = getSavedImageBytes();
        int writeOffset = this.suppliedTextureOffset.getAndAdd(savedImageBytes.length); // Add offset.

        writer.jumpTemp(writeOffset);
        writer.writeBytes(savedImageBytes);
        writer.jumpReturn();
    }

    public void save(DataWriter writer, AtomicInteger textureOffset) {
        this.suppliedTextureOffset = textureOffset;
        save(writer);
        this.suppliedTextureOffset = null;
    }

    /**
     * Get the byte[] which will be saved to the VLO file.
     * @return vloImageByteArray
     */
    public byte[] getSavedImageBytes() {
        if (!getParent().isPsxMode())
            return getImageBytes(); // The image bytes as they are loaded are already as they should be when saved.

        ClutEntry clut = getClut();
        clut.getColors().clear(); // Generate a new clut.

        ByteBuffer buffer = ByteBuffer.allocate((getImageBytes().length / PC_BYTES_PER_PIXEL) * PSX_PIXELS_PER_PC);
        int maxColors = getClut().getClutRect().getWidth() * clut.getClutRect().getHeight();

        for (int i = 0; i < getImageBytes().length; i += PC_BYTES_PER_PIXEL * PSX_PIXELS_PER_PC) {

            // RGBA -> ClutColor
            PSXClutColor color1 = PSXClutColor.fromRGBA(this.imageBytes, i);
            PSXClutColor color2 = PSXClutColor.fromRGBA(this.imageBytes, i + PC_BYTES_PER_PIXEL);

            int color1Index = clut.getColors().indexOf(color1);
            if (color1Index == -1) {
                color1Index = clut.getColors().size();
                clut.getColors().add(color1);
            }

            int color2Index = clut.getColors().indexOf(color2);
            if (color2Index == -1) {
                color2Index = clut.getColors().size();
                clut.getColors().add(color2);
            }

            Utils.verify(maxColors >= clut.getColors().size(), "Tried to import a PSX image with too many colors.");
            buffer.putInt(color2Index | (color1Index << 4));
        }

        // For any unfilled part of the clut, fill it with black.
        PSXClutColor unused = new PSXClutColor();
        while (maxColors > clut.getColors().size())
            clut.getColors().add(unused);

        return buffer.array();
    }

    /**
     * Replace this texture with a new one.
     * @param image The new image to use.
     */
    public void replaceImage(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) { // We can only parse TYPE_INT_ARGB, so if it's not that, we must convert the image to that, so it can be parsed properly.
            BufferedImage sourceImage = image;
            image = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics graphics = image.getGraphics();
            graphics.drawImage(sourceImage, 0, 0, null);
            graphics.dispose();
        }

        this.fullWidth = (short) image.getWidth();
        this.fullHeight = (short) image.getHeight();
        setIngameWidth(this.fullWidth);
        setIngameHeight(this.fullHeight);

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
    }

    /**
     * Export this game image as a BufferedImage.
     * @param trimEdges Should edges be trimmed so the textures are exactly how they appear in-game?
     * @return bufferedImage
     */
    public BufferedImage toBufferedImage(boolean trimEdges, boolean allowTransparency) {
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

        BufferedImage returnImage = image;

        if (trimEdges) {
            int xTrim = getFullWidth() - getIngameWidth();
            int yTrim = getFullHeight() - getIngameHeight();

            BufferedImage trimImage = new BufferedImage(getIngameWidth(), getIngameHeight(), returnImage.getType());
            Graphics2D graphics = trimImage.createGraphics();
            graphics.drawImage(returnImage, -xTrim / 2, -yTrim / 2, getFullWidth(), getFullHeight(), null);
            graphics.dispose();

            returnImage = trimImage;
        }

        boolean transparencyGoal = allowTransparency && testFlag(FLAG_BLACK_IS_TRANSPARENT);
        boolean transparencyState = getParent().isPsxMode();

        if (transparencyGoal != transparencyState) {
            Image write = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(returnImage.getSource(), transparencyState ? new BlackFilter() : new TransparencyFilter())); // Make it transparent if marked.
            BufferedImage newImage = new BufferedImage(returnImage.getWidth(), returnImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

            // Make transparent.
            Graphics2D graphics = newImage.createGraphics();
            graphics.drawImage(write, 0, 0, returnImage.getWidth(), returnImage.getHeight(), null); // Draw the image not flipped.
            graphics.dispose();

            returnImage = newImage;
        }

        return returnImage;
    }

    private static class TransparencyFilter extends RGBImageFilter {
        @Override
        public int filterRGB(int x, int y, int rgb) {
            int colorWOAlpha = rgb & 0xFFFFFF;
            return colorWOAlpha == 0x000000 ? colorWOAlpha : rgb;
        }
    }

    private static class BlackFilter extends RGBImageFilter {
        @Override
        @SuppressWarnings("NumericOverflow")
        public int filterRGB(int x, int y, int rgb) {
            int alpha = rgb >>> (3 * Constants.BITS_PER_BYTE);
            return alpha == 0 ? 0xFF000000 : rgb;
        }
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
}
