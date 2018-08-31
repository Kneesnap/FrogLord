package net.highwayfrogs.editor.file;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXClutColor;
import net.highwayfrogs.editor.file.standard.psx.PSXRect;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * VLOArchive - Image archive format created by VorgPC/Vorg2.
 * ABGR 8888 = 24 + 8.
 * TODO: Is clutId still zero on PSX vlos?
 * Created by Kneesnap on 8/17/2018.
 */
@Getter
public class VLOArchive extends GameFile {
    private List<GameImage> images = new ArrayList<>();
    private List<ClutEntry> clutEntries = new ArrayList<>();
    private boolean psxMode;

    public static final int TYPE_ID = 1;
    public static final int FLAG_TRANSLUCENT = 1;
    public static final int FLAG_ROTATED = 2; // Unused.
    public static final int FLAG_HIT_X = 4; //Appears to decrease width by 1?
    public static final int FLAG_HIT_Y = 8; //Appears to decrease height by 1?
    public static final int FLAG_REFERENCED_BY_NAME = 16; // Unsure.
    public static final int FLAG_BLACK_IS_TRANSPARENT = 32; // Seems like it may not be used. Would be weird if that were the case.
    public static final int FLAG_2D_SPRITE = 32768;

    private static final String PC_SIGNATURE = "2GRP";
    private static final String PSX_SIGNATURE = "2GRV";
    private static final int SIGNATURE_LENGTH = 4;

    private static final int IMAGE_INFO_BYTES = 24;
    private static final int HEADER_SIZE = SIGNATURE_LENGTH + (2 * Constants.INTEGER_SIZE);
    private static final int PIXEL_BYTES = 4;

    @Override
    public void load(DataReader reader) {
        String readSignature = reader.readString(SIGNATURE_LENGTH);
        if (readSignature.equals(PSX_SIGNATURE)) {
            this.psxMode = true;
        } else {
            Utils.verify(readSignature.equals(PC_SIGNATURE), "Invalid VLO signature: %s.", readSignature);
        }

        int fileCount = reader.readInt();
        int textureOffset = reader.readInt();

        // Load image data.
        reader.jumpTemp(textureOffset);
        for (int i = 0; i < fileCount; i++) {
            GameImage image = new GameImage(this);
            image.load(reader);
            this.images.add(image);
        }
        reader.jumpReturn();

        // Load clut data.
        if (isPsxMode()) { // GRV file has clut data.
            int clutCount = reader.readInt();
            int clutOffset = reader.readInt();

            reader.jumpTemp(clutOffset);
            for (int i = 0; i < clutCount; i++) {
                ClutEntry clut = new ClutEntry();
                clut.load(reader);
                clutEntries.add(clut);
            }
            reader.jumpReturn();
        }
    }

    @Override
    public void save(DataWriter writer) {
        int imageCount = getImages().size();
        writer.writeStringBytes(isPsxMode() ? PSX_SIGNATURE : PC_SIGNATURE);
        writer.writeInt(imageCount);
        writer.writeInt(writer.getIndex() + Constants.INTEGER_SIZE); // Offset to the VLO table info.

        int clutSetupStart = 0; //TODO: Calculate this right.
        if (isPsxMode()) {
            writer.writeInt(clutEntries.size());
            writer.writeInt(clutSetupStart);
        }

        int offset = IMAGE_INFO_BYTES * imageCount + HEADER_SIZE;
        for (GameImage image : getImages()) {
            writer.writeShort(image.getVramX());
            writer.writeShort(image.getVramY());
            writer.writeShort(image.getFullWidth());
            writer.writeShort(image.getFullHeight());
            writer.writeInt(offset);
            writer.writeShort(image.getTextureId());
            writer.writeShort(image.getTexturePage());
            writer.writeShort(image.getFlags());
            writer.writeShort(image.getClutId());
            writer.writeByte(image.getU());
            writer.writeByte(image.getV());
            writer.writeByte(image.ingameWidth);
            writer.writeByte(image.ingameHeight);
            offset += image.getImageBytes().length;
        }

        getImages().stream()
                .map(GameImage::getImageBytes)
                .forEach(writer::writeBytes);


        if (isPsxMode()) { // Save CLUT data.
            int clutOffset = writer.getIndex() + (clutEntries.size() * ClutEntry.BYTE_SIZE);
            for (ClutEntry entry : clutEntries)
                clutOffset += entry.save(writer, clutOffset);
        }
    }


    @Getter
    @Setter
    public static class ClutEntry extends GameObject {
        private PSXRect clutRect = new PSXRect();
        private List<PSXClutColor> colors = new ArrayList<>();
        private int suppliedClutOffset = 0;

        public static final int BYTE_SIZE = PSXRect.BYTE_SIZE + Constants.INTEGER_SIZE;

        @Override
        public void load(DataReader reader) {
            this.clutRect.load(reader);
            int clutOffset = reader.readInt();

            reader.jumpTemp(clutOffset);

            // Read clut.
            int colorCount = getClutRect().getWidth() * getClutRect().getHeight();
            for (int i = 0; i < colorCount; i++) {
                PSXClutColor color = new PSXClutColor();
                color.load(reader);
                colors.add(color);
            }

            reader.jumpReturn();
        }

        @Override
        public void save(DataWriter writer) {
            Utils.verify(suppliedClutOffset != 0, "Clut offset was not supplied!"); // Call save(DataWriter, int) instead.
            Utils.verify(getClutRect().getX() % 16 == 0, "Clut VRAM X must be a multiple of 16!"); // According to http://www.psxdev.net/forum/viewtopic.php?t=109
            Utils.verify(getClutRect().getY() >= 0 && getClutRect().getY() <= 511, "Invalid CLUT VRAM Y!");

            this.clutRect.save(writer);
            writer.writeInt(suppliedClutOffset);

            //TODO: Write CLUT data at the right offset. [Goes after txsetup data.]
        }

        public int save(DataWriter writer, int clutOffset) {
            this.suppliedClutOffset = clutOffset;
            save(writer);
            this.suppliedClutOffset = 0;
            return getClutRect().getWidth() * getClutRect().getHeight() * PSXClutColor.BYTE_SIZE;
        }
    }

    @Getter
    @Setter
    @SuppressWarnings("unused")
    public static class GameImage extends GameObject {
        private VLOArchive parent;
        private short vramX;
        private short vramY;
        private short fullWidth;
        private short fullHeight;
        private short textureId;
        private short texturePage;
        private short flags;
        private short clutId; // Believed to always be zero.
        private byte u; // Unsure. Texture orientation?
        private byte v;
        private byte ingameWidth; // In-game texture width, used to remove texture padding.
        private byte ingameHeight;
        private byte[] imageBytes;

        private static final int MAX_DIMENSION = 256;

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
            this.flags = reader.readShort();
            this.clutId = reader.readShort();
            this.u = reader.readByte();
            this.v = reader.readByte();
            this.ingameWidth = reader.readByte();
            this.ingameHeight = reader.readByte();

            reader.jumpTemp(offset);

            if (getParent().isPsxMode()) {
                //TODO.
            } else {
                this.imageBytes = reader.readBytes(getFullWidth() * getFullHeight() * PIXEL_BYTES);
            }

            reader.jumpReturn();
        }

        @Override
        public void save(DataWriter writer) {

        }

        /**
         * Replace this texture with a new one.
         * @param image The new image to use.
         */
        public void replaceImage(BufferedImage image) { //TODO: PSX.
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
            for (int i = 0; i < bytes.length; i += PIXEL_BYTES) { // Load image bytes.
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
        public BufferedImage toBufferedImage(boolean trimEdges) { //TODO: Actually make trimming work. {TODO: PSX}
            int height = trimEdges ? getIngameHeight() : getFullHeight();
            int width = trimEdges ? getIngameWidth() : getFullWidth();

            byte[] cloneBytes = new byte[imageBytes.length]; // We don't want to make any changes to the original array.
            System.arraycopy(imageBytes, 0, cloneBytes, 0, cloneBytes.length);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            //ABGR -> BGRA
            for (int temp = 0; temp < cloneBytes.length; temp += PIXEL_BYTES) {
                byte alpha = cloneBytes[temp];
                int alphaIndex = temp + PIXEL_BYTES - 1;
                System.arraycopy(cloneBytes, temp + 1, cloneBytes, temp, alphaIndex - temp);
                cloneBytes[alphaIndex] = (byte) (0xFF - alpha); // Alpha needs to be flipped.
            }

            IntBuffer buffer = ByteBuffer.wrap(cloneBytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asIntBuffer();

            int[] array = new int[buffer.remaining()];
            buffer.get(array);
            image.setRGB(0, 0, width, height, array, 0, width);
            return image;
        }

        /**
         * Gets the in-game height of this image.
         * @return ingameHeight
         */
        public int getIngameHeight() {
            return this.ingameHeight == 0 ? MAX_DIMENSION : this.ingameHeight;
        }

        /**
         * Gets the in-game width of this image.
         * @return ingameWidth
         */
        public int getIngameWidth() {
            return this.ingameWidth == 0 ? MAX_DIMENSION : this.ingameWidth;
        }

        /**
         * Set the in-game height of this image.
         * @param height The in-game height.
         */
        public void setIngameHeight(int height) {
            Utils.verify(height >= 0 && height <= MAX_DIMENSION, "Image height is not in the required range (0,%d].", MAX_DIMENSION);
            this.ingameHeight = (byte) (height == MAX_DIMENSION ? 0 : height);
        }

        /**
         * Set the in-game width of this image.
         * @param width The in-game width.
         */
        public void setIngameWidth(int width) {
            Utils.verify(width >= 0 && width <= MAX_DIMENSION, "Image width is not in the required range: (0,%d].", MAX_DIMENSION);
            this.ingameWidth = (byte) (width == MAX_DIMENSION ? 0 : width);
        }
    }
}
