package net.highwayfrogs.editor.games.tgq;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;

import java.io.File;
import java.util.Scanner;

/**
 * Contains various runners.
 * Created by Kneesnap on 4/22/2020.
 */
public class TGQRunners {

    // This runner will replace the "Goblin Fort" level with the unused level on the PC version.
    public static void applyUnusedLevel(String[] args) throws Exception {
        File binFile = getBinFile(args);
        if (binFile == null)
            return;

        // Load main bin.
        System.out.println("Loading file...");
        DataReader reader = new DataReader(new FileSource(binFile));
        TGQBinFile mainFile = new TGQBinFile();
        mainFile.load(reader);
        System.out.println("Loaded.");

        //TODO: Slight problem at the moment, which is that it doesn't save a 1:1 copy. We need to either get it so it saves each file correctly, so it produces a valid .bin. If we make every file a dummy file, this works.
        // Equivalent to changing

        // Switch the level hashes, so it loads it.
        int levelHash = mainFile.getFiles().get(31).getNameHash();
        mainFile.getFiles().get(31).init(null, mainFile.getFiles().get(31).isCompressed(), mainFile.getFiles().get(32).getNameHash());
        mainFile.getFiles().get(32).init(null, mainFile.getFiles().get(32).isCompressed(), levelHash);

        System.out.println("Saving.");
        DataWriter writer = new DataWriter(new FileReceiver(new File(binFile.getParentFile(), "export.bin"), 300 * (1024 * 1024)));
        mainFile.save(writer);
        writer.closeReceiver();
        System.out.println("Done.");
    }

    private static File getBinFile(String[] args) {
        String fileName;
        if (args.length > 0) {
            fileName = String.join(" ", args);
        } else {
            System.out.print("Please enter the file path to data.bin: ");
            Scanner scanner = new Scanner(System.in);
            fileName = scanner.nextLine();
            scanner.close();
        }

        File binFile = new File(fileName);
        if (!binFile.exists() || !binFile.isFile()) {
            System.out.println("That is not a valid file!");
            return null;
        }

        return binFile;
    }
}
