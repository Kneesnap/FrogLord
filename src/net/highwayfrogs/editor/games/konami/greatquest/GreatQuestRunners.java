package net.highwayfrogs.editor.games.konami.greatquest;

import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.DataSizeUnit;
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
    public static void applyUnusedLevel(String[] args) {
        File binFile = getBinFile(args);
        if (binFile == null)
            return;

        // Load main bin.
        System.out.println("Loading file...");
        GreatQuestInstance instance = new GreatQuestInstance();
        instance.loadGame("pc-retail", new Config("FakeConfig"), binFile, null);
        System.out.println("Loaded.");

        // Switch the level hashes, so it loads it.
        GreatQuestArchiveFile theGoblinFort = instance.getMainArchive().getFiles().get(31);
        GreatQuestArchiveFile ruinsOfJoyTown = instance.getMainArchive().getFiles().get(32);
        String goblinFortFilePath = theGoblinFort.getFilePath();
        boolean goblinFortCompressed = theGoblinFort.isCompressed();
        int goblinFortHash = theGoblinFort.getHash();
        byte[] goblinFortRawData = theGoblinFort.getRawData();
        instance.getMainArchive().removeFile(theGoblinFort);
        instance.getMainArchive().removeFile(ruinsOfJoyTown);
        theGoblinFort.init(ruinsOfJoyTown.getFilePath(), ruinsOfJoyTown.isCompressed(), ruinsOfJoyTown.getHash(), ruinsOfJoyTown.getRawData());
        ruinsOfJoyTown.init(goblinFortFilePath, goblinFortCompressed, goblinFortHash, goblinFortRawData);
        instance.getMainArchive().addFile(theGoblinFort);
        instance.getMainArchive().addFile(ruinsOfJoyTown);

        System.out.println("Saving...");
        DataWriter writer = new DataWriter(new FileReceiver(new File(binFile.getParentFile(), "export.bin"), 256 * (int) DataSizeUnit.MEGABYTE.getIncrement()));
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