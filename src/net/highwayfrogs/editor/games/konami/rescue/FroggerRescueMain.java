package net.highwayfrogs.editor.games.konami.rescue;


import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.renderware.RWSChunk;
import net.highwayfrogs.editor.games.renderware.RWSFile;
import net.highwayfrogs.editor.games.renderware.chunks.RWPlatformIndependentTextureDictionaryChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RWPlatformIndependentTextureDictionaryChunk.RWPlatformIndependentTextureEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.DummyFile;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A temporary driver which runs "Frogger's Adventures: The Rescue" utilities.
 * Created by Kneesnap on 6/7/2020.
 */
public class FroggerRescueMain {

    public static void main(String[] args) throws Exception {
        /*System.out.print("HFS Extractor, File: "); // C:\Program Files (x86)\Konami\Frogger's Adventures\dvd\win\area00.hfs

        Scanner scanner = new Scanner(System.in);
        File file = new File(scanner.nextLine());

        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        HFSFile hfsFile = loadHfsFile(file);
        File outputFolder = new File("RescueOutput/" + Utils.stripExtension(file.getName()) + "/");
        Utils.makeDirectory(outputFolder);
         */

        // C:\Program Files (x86)\Konami\Frogger's Adventures\dvd\win\area07.hfs

        for (File dirFile : new File("C:\\Program Files (x86)\\Konami\\Frogger's Adventures\\dvd\\win\\").listFiles()) {
            if (!dirFile.isFile() || dirFile.getName().contains("dummy"))
                continue;

            File outputFolder = new File("RescueOutput/" + Utils.stripExtension(dirFile.getName()) + "/");
            Utils.makeDirectory(outputFolder);

            HFSFile hfsFile = loadHfsFile(dirFile);
            extractHfsFile(outputFolder, hfsFile);
            extractTextures(outputFolder, hfsFile);
        }

        // Save data.
        // extractHfsFile(outputFolder, hfsFile);
        // extractTextures(outputFolder, hfsFile);


        System.out.println("Done.");
    }

    private static HFSFile loadHfsFile(File file) throws IOException {
        byte[] packed = Files.readAllBytes(file.toPath());
        HFSFile hfsFile = new HFSFile();
        hfsFile.load(new DataReader(new ArraySource(packed)));
        return hfsFile;
    }

    private static void extractHfsFile(File outputFolder, HFSFile hfsFile) throws IOException {
        File extractFolder = new File(outputFolder, "RawExtracts/");
        Utils.makeDirectory(extractFolder);

        int id = 0;
        for (DummyFile dummyFile : hfsFile.getFileData()) {
            boolean compressed = PRS1Unpacker.isCompressedPRS1(dummyFile.getArray());
            File outputFile = new File(extractFolder, "./" + id++ + "-" + (compressed ? "UNPACKED" : "RAW"));

            byte[] data = (compressed ? PRS1Unpacker.decompressPRS1(dummyFile.getArray()) : dummyFile.getArray());
            Files.write(outputFile.toPath(), data);
        }
    }

    private static void extractTextures(File outputFolder, HFSFile hfsFile) throws IOException {
        File textureFolder = new File(outputFolder, "Textures/");
        Utils.makeDirectory(textureFolder);

        System.out.println("Extracting textures for file: " + outputFolder.getName());

        int file = 0;
        Map<String, AtomicInteger> fileNames = new HashMap<>();
        for (DummyFile dummyFile : hfsFile.getFileData()) {
            System.out.println("File Entry #" + (file++));
            boolean compressed = PRS1Unpacker.isCompressedPRS1(dummyFile.getArray());
            byte[] data = (compressed ? PRS1Unpacker.decompressPRS1(dummyFile.getArray()) : dummyFile.getArray());

            RWSFile rwsFile = new RWSFile();
            try {
                rwsFile.load(new DataReader(new ArraySource(data)));
            } catch (Exception ex) {
                if (!tryExportProprietaryImage(textureFolder, data, file))
                    System.err.println("Skipping due to error!");
                continue;
            }

            exportChunk(textureFolder, rwsFile, fileNames);
        }
    }

    private static boolean tryExportProprietaryImage(File textureFolder, byte[] data, int id) throws IOException {
        DataReader reader = new DataReader(new ArraySource(data));
        short val1 = reader.readShort();
        short val2 = reader.readShort();
        if (val1 != val2 || reader.readInt() != 1) {
            System.err.println("Fail #1");
            return false;
        }

        reader.skipBytes(8);
        reader.setIndex(reader.readInt() + 4);
        int width = reader.readShort();
        int height = reader.readShort();
        int unknown = reader.readInt();
        int paletteStart = reader.readInt();
        int imageDataStart = reader.readInt();

        if (width == 0 && height > 0)
            width = ((reader.getSize() - imageDataStart)) / height;
        if (height == 0 && width > 0)
            height = ((reader.getSize() - imageDataStart)) / width;

        boolean hasPalette = (paletteStart < imageDataStart && imageDataStart != 0x88888888);

        // TODO: Need to determine pixel format, but for now this works as a test.
        reader.setIndex(imageDataStart);
        if (reader.getRemaining() < width * height) {
            System.err.println("Fail #2");
            return false;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                byte colorByte = reader.readByte();
                image.setRGB(x, y, (0xFF << 24) | (colorByte << 16) | (colorByte << 8) | colorByte);
            }
        }

        File proprietaryFolder = new File(textureFolder, "Proprietary/");
        if (!proprietaryFolder.exists())
            proprietaryFolder.mkdirs();
        ImageIO.write(image, "png", new File(proprietaryFolder, id + ".png"));
        return true;
    }

    private static void exportChunk(File textureFolder, RWSFile rwsFile, Map<String, AtomicInteger> fileNames) throws IOException {
        for (RWSChunk chunk : rwsFile.getChunks()) {
            if (!(chunk instanceof RWPlatformIndependentTextureDictionaryChunk))
                continue;

            RWPlatformIndependentTextureDictionaryChunk textureDictionaryChunk = (RWPlatformIndependentTextureDictionaryChunk) chunk;
            for (RWPlatformIndependentTextureEntry entry : textureDictionaryChunk.getEntries()) {
                for (int i = 0; i < entry.getMipLevelImages().size(); i++) {
                    String baseName = entry.makeFileName(i);
                    int num = fileNames.computeIfAbsent(baseName, key -> new AtomicInteger()).getAndIncrement();

                    File imageOutputFile = new File(textureFolder, baseName + "_" + Utils.padNumberString(num, 2) + ".png");
                    ImageIO.write(entry.getMipLevelImages().get(i).getImage(), "png", imageOutputFile);
                }
            }
        }
    }
}