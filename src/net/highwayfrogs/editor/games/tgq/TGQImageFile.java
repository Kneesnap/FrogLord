package net.highwayfrogs.editor.games.tgq;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Frogger - The Great Quest
 * File: .img
 * Contents: A single image.
 * The slowness comes from file reading. It may be faster once we're reading from the main game archive.
 * Created by Kneesnap on 8/17/2019.
 */
@Getter
public class TGQImageFile extends TGQFile {
    private BufferedImage image;
    private short unknown1; // Usually is 32, but sometimes has some wild number like 16416.
    private short unknown2 = 4; // Values seen: 0, 1, 2, 4. Could this be a bit flag field? Could it be a TYPE? (Like, maybe map texture vs character map vs menu item, etc)
    private int unknown3 = 32; // Seems to always be 32. (Bit Depth?)
    private int unknown4 = 1; // Seems to always be 1.
    private short unknown5; // ?
    private short unknown6; // ?
    private int unknown7 = 0; // May always be zero. NOTE: It appears under certain circumstances this is not the case!
    private int unknown8 = 0; // May always be zero.
    private int unknown9 = 0; // May always be zero.

    public static final String SIGNATURE = "IMGd";

    public TGQImageFile(TGQBinFile mainArchive) {
        super(mainArchive);
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);
        this.unknown1 = reader.readShort();
        this.unknown2 = reader.readShort();
        int width = reader.readInt();
        int height = reader.readInt();
        this.unknown3 = reader.readInt();
        this.unknown4 = reader.readInt();
        this.unknown5 = reader.readShort();
        this.unknown6 = reader.readShort();
        this.unknown7 = reader.readInt();
        this.unknown8 = reader.readInt();
        this.unknown9 = reader.readInt();

        // Read Image.
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                this.image.setRGB(x, height - y - 1, reader.hasMore() ? reader.readInt() : 0);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE);
        writer.writeShort(this.unknown1);
        writer.writeShort(this.unknown2);
        writer.writeInt(this.image.getWidth());
        writer.writeInt(this.image.getHeight());
        writer.writeInt(this.unknown3);
        writer.writeInt(this.unknown4);
        writer.writeShort(this.unknown5);
        writer.writeShort(this.unknown6);
        writer.writeInt(this.unknown7);
        writer.writeInt(this.unknown8);
        writer.writeInt(this.unknown9);

        // Write image.
        for (int y = 0; y < this.image.getHeight(); y++)
            for (int x = 0; x < this.image.getWidth(); x++)
                this.image.setRGB(x, y, this.image.getRGB(x, this.image.getHeight() - y - 1));
    }

    @Override
    public String getExtension() {
        return "img";
    }

    /**
     * Exports this image to a file, as a png.
     * @param saveTo The file to save the image to.
     */
    public void saveImageToFile(File saveTo) throws IOException {
        ImageIO.write(this.image, "png", saveTo);
    }
}
