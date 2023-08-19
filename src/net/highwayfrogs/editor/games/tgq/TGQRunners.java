package net.highwayfrogs.editor.games.tgq;

import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.games.tgq.audio.IDXFile;
import net.highwayfrogs.editor.games.tgq.audio.IDXFile.kcStreamIndexEntry;
import net.highwayfrogs.editor.games.tgq.audio.SBRFile;
import net.highwayfrogs.editor.games.tgq.audio.SBRFile.SfxEntry;
import net.highwayfrogs.editor.games.tgq.audio.SBRFile.SfxEntrySimpleAttributes;
import net.highwayfrogs.editor.games.tgq.audio.SBRFile.SfxWave;
import net.highwayfrogs.editor.utils.Utils;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
        TGQBinFile mainFile = new TGQBinFile(kcPlatform.PC);
        mainFile.load(reader);
        System.out.println("Loaded.");

        // TODO: Slight problem at the moment, which is that it doesn't save a 1:1 copy. We need to either get it so it saves each file correctly, so it produces a valid .bin. If we make every file a dummy file, this works.

        // Switch the level hashes, so it loads it.
        TGQFile theGoblinFort = mainFile.getFiles().get(31);
        TGQFile ruinsOfJoyTown = mainFile.getFiles().get(32);
        int goblinFortHash = theGoblinFort.getNameHash();
        boolean goblinFortCollision = theGoblinFort.isCollision();
        theGoblinFort.init(ruinsOfJoyTown.getFilePath(), theGoblinFort.isCompressed(), ruinsOfJoyTown.getNameHash(), theGoblinFort.getRawData(), ruinsOfJoyTown.isCollision());
        ruinsOfJoyTown.init(theGoblinFort.getFilePath(), ruinsOfJoyTown.isCompressed(), goblinFortHash, ruinsOfJoyTown.getRawData(), goblinFortCollision);

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

    /**
     * Get the platform.
     * @param file    The file in question, if there is one.
     * @param scanner An optional scanner to read data from.
     * @return kcPlatform or null if one was not given.
     */
    public static kcPlatform getPlatform(File file, Scanner scanner) {
        kcPlatform platform = null;

        if (file != null) {
            String filePath = file.getPath();
            if (filePath.contains("PC") || filePath.contains("Windows") || filePath.contains(":\\Program Files"))
                platform = kcPlatform.PC;
            if (filePath.contains("PS2") || filePath.contains("Play"))
                platform = kcPlatform.PS2;
        }

        if (platform == null) {
            boolean makeScanner = (scanner == null);
            if (makeScanner)
                scanner = new Scanner(System.in);

            System.out.print("Please enter the platform the files came from (PC, PS2): ");
            String platformName = scanner.nextLine();

            try {
                platform = kcPlatform.valueOf(platformName.toUpperCase());
            } catch (Throwable th) {
                // We don't care. This is some temporary development CLI mode for devs.
            }

            if (makeScanner)
                scanner.close();
        }

        // Verify platform.
        if (platform == null)
            System.out.println("That is not a valid platform.");

        return platform;
    }

    /**
     * Runs a program which will export sound files.
     * @param args The arguments to the program.
     */
    @SneakyThrows
    @SuppressWarnings("unused")
    public static void exportSoundFiles(String[] args) {
        String filePath;
        if (args.length > 0) {
            filePath = String.join(" ", args);
        } else {
            System.out.print("Please enter the file path to the sound folder: ");
            Scanner scanner = new Scanner(System.in);
            filePath = scanner.nextLine();
            scanner.close();
        }

        File soundFolder = new File(filePath);
        if (!soundFolder.exists() || !soundFolder.isDirectory()) {
            System.out.println("That is not a valid folder!");
            return;
        }

        kcPlatform platform = getPlatform(soundFolder, null);
        if (platform == null)
            return;

        List<File> sbrFiles = new ArrayList<>();
        List<SBRFile> sbrLoadedFiles = new ArrayList<>();
        IDXFile idxFile = null;
        File sckFile = null;
        for (File file : Utils.listFiles(soundFolder)) {
            if (!file.isFile())
                continue;

            String fileNameLowerCase = file.getName().toLowerCase();

            if (fileNameLowerCase.endsWith(".sck")) {
                if (sckFile != null)
                    throw new RuntimeException("Found multiple .SCK files! (" + file.getName() + ")");

                System.out.println("Found '" + file.getName() + "'...");
                sckFile = file;
            } else if (fileNameLowerCase.endsWith(".idx")) {
                if (idxFile != null)
                    throw new RuntimeException("Found multiple .IDX files! (" + file.getName() + ")");

                System.out.println("Reading '" + file.getName() + "'...");
                idxFile = new IDXFile();
                idxFile.load(new DataReader(new FileSource(file)));
            } else if (fileNameLowerCase.endsWith(".sbr")) {
                System.out.println("Reading '" + file.getName() + "'...");
                SBRFile sbrFile = new SBRFile(platform);
                sbrFile.load(new DataReader(new FileSource(file)));
                sbrFiles.add(file);
                sbrLoadedFiles.add(sbrFile);
            }
        }

        if (idxFile == null && sckFile != null) {
            System.out.println("Couldn't find the SNDCHUNK.IDX file in this folder.");
            return;
        }

        if (sckFile == null && idxFile != null) {
            System.out.println("Couldn't find the SNDCHUNK.SCK file in this folder.");
            return;
        }

        File exportFolder = new File(soundFolder, "Export/");
        Utils.makeDirectory(exportFolder);

        if (sckFile != null) {
            File chunkFolder = new File(exportFolder, Utils.stripExtension(sckFile.getName()));
            Utils.makeDirectory(chunkFolder);
            AudioFormat sckAudioFormat = new AudioFormat(24000, 16, 1, true, false);

            DataReader sckReader = new DataReader(new FileSource(sckFile));
            for (int i = 0; i < idxFile.getIndexEntries().size(); i++) {
                kcStreamIndexEntry entry = idxFile.getIndexEntries().get(i);
                int stopReadingAt = idxFile.getIndexEntries().size() > i + 1 ? idxFile.getIndexEntries().get(i + 1).getOffset() : sckReader.getSize();
                byte[] rawAudioData = sckReader.readBytes(stopReadingAt - sckReader.getIndex());

                String fileName = Utils.padNumberString(entry.getSfxId(), 4) + ".wav";
                File wavFile = new File(chunkFolder, fileName);
                if (wavFile.exists())
                    continue;

                Clip clip = AudioSystem.getClip();
                clip.open(sckAudioFormat, rawAudioData, 0, rawAudioData.length);
                AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(rawAudioData), clip.getFormat(), clip.getFrameLength());
                AudioSystem.write(inputStream, Type.WAVE, wavFile);
            }
        } else {
            System.out.println("No .SCK file found.");
        }

        for (int i = 0; i < sbrFiles.size(); i++) {
            File file = sbrFiles.get(i);
            SBRFile sbrFile = sbrLoadedFiles.get(i);
            File sbrFolder = new File(exportFolder, Utils.stripExtension(file.getName()));
            Utils.makeDirectory(sbrFolder);

            for (SfxEntry entry : sbrFile.getSoundEffects()) {
                if (!(entry.getAttributes() instanceof SfxEntrySimpleAttributes))
                    continue;

                String fileName = Utils.padNumberString(entry.getSfxId(), 4) + ".wav";
                File targetFile = new File(sbrFolder, fileName);
                if (targetFile.exists())
                    continue;

                int waveId = (int) ((SfxEntrySimpleAttributes) entry.getAttributes()).getWave();
                SfxWave wave = sbrFile.getWaves().get(waveId);
                wave.exportToWav(targetFile);
            }
        }
    }
}
