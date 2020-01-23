package net.highwayfrogs.editor.games.frogger2;

import java.io.File;
import java.nio.file.Files;
import java.util.Scanner;

/**
 * A temporary runner for Frogger 2 utilities.
 * Created by Kneesnap on 1/18/2020.
 */
public class Frogger2Main {

    public static void main(String[] args) throws Exception {
        System.out.print("FLA Unpacker, File: ");

        Scanner scanner = new Scanner(System.in);
        File file = new File(scanner.nextLine());

        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        File outputFile = new File("./" + file.getName() + ".unpacked");

        byte[] packed = Files.readAllBytes(file.toPath());
        byte[] unpacked = FLA2Unpacker.unpackData(packed);

        Files.write(outputFile.toPath(), unpacked);
        System.out.println("Saved to: " + outputFile);
    }
}
