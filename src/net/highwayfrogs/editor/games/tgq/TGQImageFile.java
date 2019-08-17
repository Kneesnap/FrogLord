package net.highwayfrogs.editor.games.tgq;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Frogger - The Great Quest
 * File: .img
 * Contents: A single image.
 * The slowness comes from file reading. It may be faster once we're reading from the main game archive.
 * Created by Kneesnap on 8/17/2019.
 */
@Getter
public class TGQImageFile extends GameObject {
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

    private static final String SIGNATURE = "IMGd";

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
                this.image.setRGB(x, height - y - 1, reader.readInt());
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

    /**
     * Exports this image to a file, as a png.
     * @param saveTo The file to save the image to.
     */
    public void exportToFile(File saveTo) throws IOException {
        ImageIO.write(this.image, "png", saveTo);
    }

    /**
     * Converts all images in a directory.
     * @param directory The directory of images.
     */
    public static void convertAllImages(File directory) {
        File saveDirectory = new File(directory, "images/");
        Utils.makeDirectory(saveDirectory);

        for (File file : Utils.listFiles(directory)) {
            if (!file.getName().endsWith(".img"))
                continue;

            try {
                long start = System.currentTimeMillis();
                DataReader reader = new DataReader(new FileSource(file));
                TGQImageFile image = new TGQImageFile();
                image.load(reader);
                long read = System.currentTimeMillis();
                image.exportToFile(new File(saveDirectory, file.getName() + ".png"));
                long save = System.currentTimeMillis();
                System.out.println("Exported " + file.getName() + ", [Load: " + (read - start) + "ms] [Save: " + (save - read) + " ms] " + image.getUnknown1() + ", " + image.getUnknown2() + ", " + image.getUnknown3() + ", " + image.getUnknown4() + ", " + image.getUnknown5() + ", " + image.getUnknown6() + ", " + image.getUnknown7() + ", " + image.getUnknown8() + ", " + image.getUnknown9());
            } catch (IOException ex) {
                System.out.println("Failed to save " + file.getName() + "!");
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Please enter the directory to scan: ");
        File directory = new File(scanner.nextLine());

        if (!directory.exists()) {
            System.out.println("This directory does not exist!");
            return;
        }

        if (!directory.isDirectory()) {
            System.out.println("This is not a directory!");
            return;
        }

        convertAllImages(directory);
        System.out.println("Done.");
    }
}
