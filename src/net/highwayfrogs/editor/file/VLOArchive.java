package net.highwayfrogs.editor.file;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * VLOArchive - Image archive format created by VorgPC/Vorg2.
 * MR_TXSETUP
 * ABGR 8888 = 24 + 8.
 * Created by Kneesnap on 8/17/2018.
 */
public class VLOArchive extends GameFile {

    public static final int TYPE_ID = 1;
    public static final int FLAG_TRANSLUCENT = 1;
    public static final int FLAG_ROTATED = 2; // Unused.
    public static final int FLAG_HIT_X = 4; //Appears to decrease width by 1?
    public static final int FLAG_HIT_Y = 8; //Appears to decrease height by 1?
    public static final int FLAG_REFERENCED_BY_NAME = 16; // TODO: Wat
    public static final int FLAG_BLACK_IS_TRANSPARENT = 32; // Seems like it may not be used. Would be weird if that were the case.
    public static final int FLAG_2D_SPRITE = 32768;


    private static final String SIGNATURE = "2GRP";
    private static final int PIXEL_BYTES = 4;

    @Override
    public void load(DataReader reader) {
        Utils.verify(reader.readString(SIGNATURE.length()).equals(SIGNATURE), "Invalid VLO signature.");
        int fileCount = reader.readInt();

        reader.setIndex(reader.readInt());

        for (int i = 0; i < fileCount; i++) {

            GameImage image = new GameImage();
            image.setVramX(reader.readShort());
            image.setVramY(reader.readShort());
            image.setFullWidth(reader.readShort());
            image.setFullHeight(reader.readShort());

            int offset = reader.readInt();

            image.setTextureId(reader.readShort());
            image.setTexturePage(reader.readShort());
            image.setFlags(reader.readShort());
            image.setClutId(reader.readShort());
            image.setU(reader.readByte());
            image.setV(reader.readByte());
            image.setIngameWidth(reader.readByte());
            image.setIngameHeight(reader.readByte());

            reader.jumpTemp(offset);
            image.setImageBytes(reader.readBytes(image.getFullWidth() * image.getFullHeight() * PIXEL_BYTES));
            reader.jumpReturn();
        }
    }

    @Override
    public void save(DataWriter writer) {

    }

    @Getter
    @Setter
    public static class GameImage {
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

        /**
         * Export this game image as a BufferedImage.
         * @param trimEdges Should edges be trimmed so the textures are exactly how they appear in-game?
         * @return bufferedImage
         */
        public BufferedImage toBufferedImage(boolean trimEdges) { //TODO: Actually make trimming work.
            int height = trimEdges ? getIngameHeight() : getFullHeight();
            int width = trimEdges ? getIngameWidth() : getFullWidth();

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            //ABGR -> BGRA
            for (int temp = 0; temp < imageBytes.length; temp += PIXEL_BYTES) {
                byte alpha = imageBytes[temp];
                int alphaIndex = temp + PIXEL_BYTES - 1;
                System.arraycopy(imageBytes, temp + 1, imageBytes, temp, alphaIndex - temp);
                imageBytes[alphaIndex] = (byte) (0xFF - alpha); // Alpha needs to be flipped.
            }

            IntBuffer buffer = ByteBuffer.wrap(imageBytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asIntBuffer();

            int[] array = new int[buffer.remaining()];
            buffer.get(array);
            image.setRGB(0, 0, width, height, array, 0, width);
            return image;
        }

        public int getIngameHeight() {
            return this.ingameHeight == 0 ? 256 : this.ingameHeight;
        }

        public int getIngameWidth() {
            return this.ingameWidth == 0 ? 256 : this.ingameWidth;
        }
    }
}
