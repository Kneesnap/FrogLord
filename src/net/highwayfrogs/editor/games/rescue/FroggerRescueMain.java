package net.highwayfrogs.editor.games.rescue;


import net.highwayfrogs.editor.file.DummyFile;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;

import java.io.File;
import java.nio.file.Files;
import java.util.Scanner;

/**
 * A temporary driver which runs "Frogger's Adventures: The Rescue" utilities.
 * Created by Kneesnap on 6/7/2020.
 */
public class FroggerRescueMain {

    public static void main(String[] args) throws Exception {
        System.out.print("HFS Extractor, File: "); // C:\Program Files (x86)\Konami\Frogger's Adventures\dvd\win\area00.hfs

        Scanner scanner = new Scanner(System.in);
        File file = new File(scanner.nextLine());

        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        byte[] packed = Files.readAllBytes(file.toPath());
        HFSFile hfsFile = new HFSFile();
        hfsFile.load(new DataReader(new ArraySource(packed)));

        // Save data.
        int id = 0;
        for (DummyFile dummyFile : hfsFile.getFileData()) {
            boolean compressed = PRS1Unpacker.isCompressedPRS1(dummyFile.getArray());
            File outputFile = new File("./" + id++ + "-" + (compressed ? "UNPACKED" : "RAW"));

            byte[] data = (compressed ? PRS1Unpacker.decompressPRS1(dummyFile.getArray()) : dummyFile.getArray());
            Files.write(outputFile.toPath(), data);
            System.out.println("Saved to: " + outputFile);
        }

        System.out.println("Done.");
    }
}
