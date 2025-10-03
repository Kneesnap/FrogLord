package net.highwayfrogs.editor.games.konami.greatquest;

import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.reader.FileSource;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.data.writer.FileReceiver;

import java.io.File;
import java.util.Scanner;

/**
 * Contains various runners.
 * Created by Kneesnap on 4/22/2020.
 */
public class GreatQuestRunners {

    // This runner will replace the "Goblin Fort" level with the unused level on the PC version.
    public static void applyUnusedLevel(String[] args) throws Exception {
        File binFile = getBinFile(args);
        if (binFile == null)
            return;

        // Load main bin.
        System.out.println("Loading file...");
        DataReader reader = new DataReader(new FileSource(binFile));
        GreatQuestInstance instance = new GreatQuestInstance();
        instance.loadGame("pc-retail", new Config("FakeConfig"), binFile, null);
        instance.getMainArchive().load(reader);
        System.out.println("Loaded.");

        // Switch the level hashes, so it loads it.
        GreatQuestArchiveFile theGoblinFort = instance.getMainArchive().getFiles().get(31);
        GreatQuestArchiveFile ruinsOfJoyTown = instance.getMainArchive().getFiles().get(32);
        int goblinFortHash = theGoblinFort.getHash();
        boolean goblinFortCollision = theGoblinFort.isCollision();
        theGoblinFort.init(ruinsOfJoyTown.getFilePath(), theGoblinFort.isCompressed(), ruinsOfJoyTown.getHash(), theGoblinFort.getRawData(), ruinsOfJoyTown.isCollision());
        ruinsOfJoyTown.init(theGoblinFort.getFilePath(), ruinsOfJoyTown.isCompressed(), goblinFortHash, ruinsOfJoyTown.getRawData(), goblinFortCollision);

        System.out.println("Saving.");
        DataWriter writer = new DataWriter(new FileReceiver(new File(binFile.getParentFile(), "export.bin"), 300 * (1024 * 1024)));
        instance.getMainArchive().save(writer);
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