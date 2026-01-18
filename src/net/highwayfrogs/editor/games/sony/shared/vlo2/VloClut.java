package net.highwayfrogs.editor.games.sony.shared.vlo2;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.psx.PSXClutColor;
import net.highwayfrogs.editor.games.psx.image.PsxVram;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.image.ImageUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a single CLUT in a VLO file.
 * 'CLUT' stands for 'Color lookup table', and is the official name given for a color palette used by PlayStation games.
 * Cluts are often re-used across different images.
 * Created by Kneesnap on 11/26/2025.
 */
public class VloClut extends SCSharedGameData implements ITextureSource {
    @Getter private final VloFile vloFile;
    @Getter private final List<Consumer<BufferedImage>> imageChangeListeners = new ArrayList<>();
    private final List<VloImage> images = new ArrayList<>();
    private final List<VloImage> immutableImages = Collections.unmodifiableList(this.images);
    @Getter boolean registered;
    @Getter private short x = -1; // Always in unit form, since the clut colors are always 16 bits each.
    @Getter private short y = -1;
    private PSXClutColor[][] colors;
    transient int tempColorsPointer = -1;

    public static final int X_POSITION_MODULO = 16;

    public static final int CLUT4_COLOR_COUNT = 16;
    public static final int CLUT8_COLOR_COUNT = 256;

    // Clut ID format: YYYYYYYYYYXXXXXX (10 Y bits, 6 X bits, shifted left by 4)
    public static final int CLUT_SHIFT_X = 4;
    public static final int MAX_CLUT_X = 64 << CLUT_SHIFT_X; // 1024
    public static final int MAX_CLUT_Y = 1024;

    public VloClut(@NonNull VloFile vloFile) {
        super(vloFile.getGameInstance());
        this.vloFile = vloFile;
    }

    /**
     * Gets a list of images which use this clut.
     */
    public List<VloImage> getImages() {
        return this.immutableImages;
    }

    /**
     * Track an image as using this clut.
     * @param image the image to track.
     */
    void addImage(VloImage image) {
        if (this.images.contains(image))
            throw new RuntimeException(image + " already registered.");
        if (image.getParent() != this.vloFile)
            throw new IllegalArgumentException("Cannot add " + image + ", because it is registered to the wrong parent VloFile.");

        this.images.add(image);
        if (this.images.size() == 1 && !this.registered)
            this.vloFile.getClutList().addClut(this);
    }

    /**
     * Stop tracking an image as using this clut.
     * @param image the image to stop tracking.
     */
    void removeImage(VloImage image) {
        if (this.images.remove(image) && this.images.isEmpty() && this.registered)
            this.vloFile.getClutList().removeClut(this);
    }

    /**
     * Loads up the clut colors at the given position.
     * @param width the new width of the clut
     * @param height the new height of the clut
     * @param colors the colors to apply to the clut
     */
    public void loadColors(int width, int height, List<PSXClutColor> colors) {
        if (colors != null && colors.size() != (width * height))
            throw new IllegalArgumentException("Expected " + (width * height) + " colors, but actually got " + colors.size() + ".");

        setDimensions(width, height);

        // Loads the colors.
        if (colors != null)
            for (int i = 0; i < colors.size(); i++)
                this.colors[i / width][i % width].fromShort(colors.get(i).toShort());
    }

    /**
     * Sets up the clut at the given position.
     * @param x the x position to place the clut
     * @param y the y position to place the clut
     * @param width the width of the grid containing the colors
     * @param height the height of the grid containing the colors
     */
    public void setup(int x, int y, int width, int height) {
        if (width != CLUT4_COLOR_COUNT && width != CLUT8_COLOR_COUNT)
            throw new IllegalArgumentException("Invalid width value, must be either " + CLUT4_COLOR_COUNT + " or " + CLUT8_COLOR_COUNT + "! (" + getDebugText(x, y, width, height) + ")");
        if (height < 0)
            throw new IllegalArgumentException("Invalid height value, must be at least zero! (" + getDebugText(x, y, width, height) + ")");

        if (x < 0 || x + width > PsxVram.PSX_VRAM_MAX_POSITION_X)
            throw new IllegalArgumentException("The provided x coordinate would result in one or more colors placed outside of VRAM! (" + getDebugText(x, y, width, height) + ")");
        if ((x % X_POSITION_MODULO) != 0) // According to http://www.psxdev.net/forum/viewtopic.php?t=109
            throw new IllegalArgumentException("The provided x coordinate was not a multiple of " + X_POSITION_MODULO + "! (" + getDebugText(x, y, width, height) + ")");

        if (y < 0 || y + height > PsxVram.PSX_VRAM_MAX_POSITION_Y)
            throw new IllegalArgumentException("The provided y coordinate would result in colors placed outside of VRAM! (" + getDebugText(x, y, width, height) + ")");

        this.x = (short) x;
        this.y = (short) y;
        setDimensions(width, height);
    }

    private void setDimensions(int width, int height) {
        if (width != CLUT4_COLOR_COUNT && width != CLUT8_COLOR_COUNT)
            throw new IllegalArgumentException("Invalid width value, must be either " + CLUT4_COLOR_COUNT + " or " + CLUT8_COLOR_COUNT + "! (" + getDebugText(x, y, width, height) + ")");
        if (height < 0)
            throw new IllegalArgumentException("Invalid height value, must be at least zero! (" + getDebugText(x, y, width, height) + ")");

        // Set up a new color array.
        if (this.colors == null || this.colors.length != height || (height > 0 && this.colors[0].length != width)) {
            this.colors = new PSXClutColor[height][width];
            for (int i = 0; i < this.colors.length; i++) {
                PSXClutColor[] colors = this.colors[i];
                for (int j = 0; j < colors.length; j++)
                    colors[j] = new PSXClutColor();
            }
        }
    }

    private static String getDebugText(int x, int y, int width, int height) {
        return "Position: [" + x + ", " + y + "], Dimensions: [" + width + ", " + height + "]";
    }

    @Override
    public void load(DataReader reader) {
        // This is the PSX 'RECT' struct.
        short x = reader.readShort();
        short y = reader.readShort();
        short width = reader.readShort();
        short height = reader.readShort();
        this.tempColorsPointer = reader.readInt();
        this.setup(x, y, width, height);
    }

    @Override
    public void save(DataWriter writer) {
        // This is the PSX 'RECT' struct.
        writer.writeShort(this.x);
        writer.writeShort(this.y);
        writer.writeShort((short) getWidth());
        writer.writeShort((short) getHeight());
        this.tempColorsPointer = writer.writeNullPointer();
    }

    /**
     * Load color data from the data stream.
     * @param reader The reader to read from
     */
    int readColors(DataReader reader) {
        if (this.tempColorsPointer < 0)
            throw new RuntimeException("Cannot read VloClut color data, the color data pointer was not set.");

        requireReaderIndex(reader, this.tempColorsPointer, "Expected CLUT color data");
        this.tempColorsPointer = -1;

        // Read clut.
        for (int y = 0; y < this.colors.length; y++) {
            PSXClutColor[] colors = this.colors[y];
            for (int x = 0; x < colors.length; x++) {
                PSXClutColor color = new PSXClutColor();
                color.load(reader);
                colors[x] = color;
            }
        }

        return reader.getIndex();
    }

    /**
     * Save color data to the data stream.
     * @param writer The writer to save to
     */
    void writeColors(DataWriter writer) {
        if (this.tempColorsPointer < 0)
            throw new RuntimeException("Cannot write VloClut color data, the color data pointer was not set.");
        if (this.colors == null)
            throw new RuntimeException("Cannot write VloClut color data, there are no colors to write.");

        writer.writeAddressTo(this.tempColorsPointer);
        this.tempColorsPointer = -1;

        for (int y = 0; y < this.colors.length; y++) {
            PSXClutColor[] colors = this.colors[y];
            for (int x = 0; x < colors.length; x++)
                colors[x].save(writer);
        }
    }

    @Override
    public BufferedImage makeImage() {
        final int clutPixelWidth = PsxVram.PSX_VRAM_MAX_PIXELS_PER_UNIT; // Each clut color is 2 bytes, or 1 unit. But we should cover the maximum number of pixels per unit.

        int width = getWidth();
        BufferedImage clutImage = new BufferedImage(width * clutPixelWidth, getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] pixelArray = ImageUtils.getWritablePixelIntegerArray(clutImage);
        for (int i = 0; i < pixelArray.length / clutPixelWidth; i++) {
            int color = this.colors[i / width][i % width].toARGB(true, null);
            Arrays.fill(pixelArray, i * clutPixelWidth, ((i + 1) * clutPixelWidth), color);
        }

        return clutImage;
    }

    /**
     * Gets the Clut ID usable to index the clut position.
     */
    public short getClutID() {
        return getClutID(this.x, this.y);
    }

    /**
     * Gets the number of colors tracked in this clut.
     * @return colorCount
     */
    public int getColorCount() {
        if (this.colors.length == 0)
            return 0;

        return this.colors.length * this.colors[0].length;
    }

    /**
     * Gets the clutColor from the given index
     * @param colorIndex the index of the color to obtain
     * @return color
     */
    public PSXClutColor getColor(int colorIndex) {
        requireClutInitialized();

        int clutHeight = this.colors.length;
        int clutWidth = this.colors.length > 0 ? this.colors[0].length : 0;
        int x = colorIndex % clutWidth;
        int y = colorIndex / clutWidth;
        if (colorIndex < 0 || y >= clutHeight)
            throw new IllegalArgumentException("colorIndex: " + colorIndex + "(X: " + x + ", Y: " + y + ") is outside of the clut! (Width: " + clutWidth + ", Height: " + clutHeight + ")");

        return this.colors[y][x];
    }

    /**
     * Gets the index of the color within the clut
     * @param color the color to find
     * @param errorIfNotFound if true, and the color is not found, an error will be thrown
     * @return colorIndex
     */
    public int getColorIndex(PSXClutColor color, boolean errorIfNotFound) {
        return getColorIndex(color.toShort(), errorIfNotFound);
    }

    /**
     * Gets the index of the color within the clut
     * @param color the color to find
     * @param errorIfNotFound if true, and the color is not found, an error will be thrown
     * @return colorIndex
     */
    public int getColorIndex(short color, boolean errorIfNotFound) {
        requireClutInitialized();

        for (int y = 0; y < this.colors.length; y++) {
            for (int x = 0; x < this.colors[0].length; x++) {
                PSXClutColor clutColor = this.colors[y][x];
                if (clutColor.toShort() == color)
                    return (y * this.colors[0].length) + x;
            }
        }

        if (errorIfNotFound)
            throw new RuntimeException(String.format("No clutColor could be found represented as %04X in %s.", color, this));

        return -1;
    }

    private void requireClutInitialized() {
        if (this.colors == null)
            throw new UnsupportedOperationException("The clut is uninitialized.");
    }

    /**
     * Returns true if this clut overlaps with the other clut.
     * @param otherClut the other clut to test
     * @return true or false
     */
    public boolean overlaps(VloClut otherClut) {
        if (otherClut == null)
            throw new NullPointerException("otherClut");

        return ((otherClut.x < (this.x + getWidth())) && ((otherClut.x + otherClut.getWidth()) > this.x))
                && ((otherClut.y < (this.y + getHeight())) && ((otherClut.y + otherClut.getHeight()) > this.y));
    }

    @Override // Width is always in unit form, since the clut colors are always 16 bits each.
    public int getWidth() {
        return this.colors != null && this.colors.length > 0 ? this.colors[0].length : 0;
    }

    @Override
    public int getHeight() {
        return this.colors != null ? this.colors.length : 0;
    }

    @Override
    public int getUpPadding() {
        return 0; // Clut images do not have padding, as they are always used with pixel-perfect accuracy.
    }

    @Override
    public int getDownPadding() {
        return 0; // Clut images do not have padding, as they are always used with pixel-perfect accuracy.
    }

    @Override
    public int getLeftPadding() {
        return 0; // Clut images do not have padding, as they are always used with pixel-perfect accuracy.
    }

    @Override
    public int getRightPadding() {
        return 0; // Clut images do not have padding, as they are always used with pixel-perfect accuracy.
    }

    @Override
    public void fireChangeEvent(BufferedImage newImage) {
        fireChangeEvent0(newImage);
    }

    @Override
    public String toString() {
        return "VloClut{pos=[" + this.x + "," + this.y + "],size=[" + getWidth() + "," + getHeight() + "]}";
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(this.colors);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VloClut && Arrays.deepEquals(((VloClut) obj).colors, this.colors);
    }

    /**
     * Gets the clut X position from the clut ID.
     * @param clutId the clut ID to get the x position from
     * @return clutX
     */
    public static int getClutX(short clutId) {
        return ((clutId & 0x3F) << 4);
    }

    /**
     * Gets the clut Y position from the clut ID.
     * @param clutId the clut ID to get the y position from
     * @return clutY
     */
    public static int getClutY(short clutId) {
        return (clutId >>> 6);
    }

    /**
     * Creates a 16-bit clut ID representing the clut coordinates.
     * @param x the x coordinate
     * @param y the y coordinate
     * @return clutId
     */
    public static short getClutID(int x, int y) {
        if (x < 0 || x >= MAX_CLUT_X)
            throw new IllegalArgumentException("Invalid clutX coordinate: " + x);
        if (y < 0 || y >= MAX_CLUT_Y)
            throw new IllegalArgumentException("Invalid clutY coordinate: " + y);
        if ((x % (1 << CLUT_SHIFT_X)) != 0)
            throw new IllegalArgumentException("clutX value of " + x + " is not divisible by " + (1 << CLUT_SHIFT_X) + "!");

        return (short) ((y << 6) | ((x >>> CLUT_SHIFT_X) & 0x3F));
    }
}
