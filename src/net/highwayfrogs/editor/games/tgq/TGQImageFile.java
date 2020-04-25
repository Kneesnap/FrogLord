package net.highwayfrogs.editor.games.tgq;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
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
 * Seems to be compatible with both PC and PS2 releases.
 * Created by Kneesnap on 8/17/2019.
 */
@Getter
public class TGQImageFile extends TGQFile {
    private BufferedImage image;
    private int bitsPerPixel = 32; // 32 - RGBA, 24 - BGR (Unused), 16 - RGBA16 (Unused), 8 - Palette (Used once)
    private short unknown5; // ? TODO: This combined with unk6 might be a hash of the filename?
    private short unknown6; // ?
    private int unknown8 = 0; // 0 in all but three cases. If this is not 0, unknown9 also seems to not be zero.
    private int unknown9 = 0; // May always be zero. 3 Cases where it is not. It seems to mirror 8 in two of those cases.
    private int missingBytes; // It's unclear why this exists, but it likely serves a purpose, so we abide by it.

    private static final int HEADER_SIZE = 0x20;

    public static final String SIGNATURE = "IMGd";

    public TGQImageFile(TGQBinFile mainArchive) {
        super(mainArchive);
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);

        int readSize = reader.readInt(); //TODO: Try making this a chunk again.
        int width = reader.readInt();
        int height = reader.readInt();
        this.missingBytes = (((width * height) * (this.bitsPerPixel / Constants.BITS_PER_BYTE)) - readSize);
        this.bitsPerPixel = reader.readInt();

        int unknownPossiblyTextureCount = reader.readInt();
        if (unknownPossiblyTextureCount != 1)
            throw new RuntimeException("Value was expected to always be 1! (" + unknownPossiblyTextureCount + ")");

        reader.jumpTemp(reader.getIndex());
        int tempValue = reader.readInt();
        reader.jumpReturn();

        this.unknown5 = reader.readShort(); //TODO: Could these be a hash? No.
        this.unknown6 = reader.readShort();

        int unknown7AlwaysSeemsToBeZero = reader.readInt();
        if (unknown7AlwaysSeemsToBeZero != 0)
            throw new RuntimeException("Value was expected to always be 0! (" + unknown7AlwaysSeemsToBeZero + ")");

        this.unknown8 = reader.readInt();
        this.unknown9 = reader.readInt();

        //TODO: TOSS (Maybe the unknowns are an offset to the file, where the next data is?)
        System.out.println("5: " + this.unknown5 + ", 6: " + this.unknown6 + " (" + tempValue + "), 8: " + this.unknown8 + ", 9: " + this.unknown9 + ", W: " + width + ", H: " + height + ", A: " + (width * height) + ", ID: " + getMainArchive().getFiles().size());

        // Read Image.
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                this.image.setRGB(x, height - y - 1, reader.hasMore() ? reader.readInt() : 0);

        // TODO: Skybox textures seem to have more data after this point, while most images have their data end here.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE);
        writer.writeInt(((this.image.getWidth() * this.image.getHeight()) * (this.bitsPerPixel / Constants.BITS_PER_BYTE)) + HEADER_SIZE - this.missingBytes);
        writer.writeInt(this.image.getWidth());
        writer.writeInt(this.image.getHeight());
        writer.writeInt(this.bitsPerPixel);
        writer.writeInt(1); // Always 1.
        writer.writeShort(this.unknown5);
        writer.writeShort(this.unknown6);
        writer.writeInt(0);
        writer.writeInt(this.unknown8);
        writer.writeInt(this.unknown9);

        // Write image.
        int startSkip = (this.image.getWidth() - (this.missingBytes / (this.bitsPerPixel / Constants.BITS_PER_BYTE)));
        for (int y = 0; y < this.image.getHeight(); y++)
            for (int x = 0; x < this.image.getWidth(); x++)
                if (y != this.image.getHeight() - 1 || (startSkip > x))
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
