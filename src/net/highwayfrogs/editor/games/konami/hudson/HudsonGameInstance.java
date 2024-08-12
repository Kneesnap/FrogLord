package net.highwayfrogs.editor.games.konami.hudson;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.games.konami.hudson.file.HudsonDummyFile;
import net.highwayfrogs.editor.games.konami.hudson.file.HudsonRwStreamFile;
import net.highwayfrogs.editor.games.konami.hudson.ui.HudsonMainMenuUIController;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkTypeRegistry;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.gui.MainMenuController;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a game using the Hudson/RenderWare engine seen in Frogger's Adventures: The Rescue and Frogger Ancient Shadow.
 * Created by Kneesnap on 8/8/2024.
 */
@Getter
public abstract class HudsonGameInstance extends GameInstance {
    private final List<HudsonGameFile> files = new ArrayList<>();
    private final List<HudsonGameFile> allFiles = new ArrayList<>(); // Includes nested files.
    private File rootFolder;

    protected HudsonGameInstance(IGameType gameType) {
        super(gameType);
    }

    /**
     * Load and setup all instance data relating to the game such as version configuration and game files.
     * @param gameVersionConfigName The name of the version configuration file to load.
     * @param mainFile the main archive file to read
     * @param progressBar the progress bar to display load progress on, if it exists
     */
    public void loadGame(String gameVersionConfigName, File mainFile, ProgressBarComponent progressBar) {
        if (this.rootFolder != null)
            throw new RuntimeException("The game instance has already been loaded.");

        if (mainFile == null || !mainFile.exists())
            throw new RuntimeException("The main file '" + mainFile + "' does not exist.");

        if (mainFile.isDirectory()) {
            this.rootFolder = mainFile;
            loadGameConfig(gameVersionConfigName);

            loadFilesFromFolderRecursive(mainFile, progressBar);
        } else if (mainFile.isFile()) {
            this.rootFolder = mainFile.getParentFile();
            loadGameConfig(gameVersionConfigName);
            loadFile(mainFile, true, progressBar);
        } else {
            throw new RuntimeException("Don't know how to load '" + mainFile + "', which reported as neither a directory nor a file!");
        }
    }

    /**
     * Tests if the given file should be loaded as a game file for the game.
     * @param file the file to load
     * @param fileName the name of the file
     * @param extension the file extension, if there is one
     * @return should the file be loaded as a game file?
     */
    protected boolean shouldLoadAsGameFile(File file, String fileName, String extension) {
        return "hfs".equalsIgnoreCase(extension);
    }

    private void loadFilesFromFolderRecursive(File folder, ProgressBarComponent progressBar) {
        List<File> files = new ArrayList<>();
        files.add(folder);

        List<File> loadFiles = new ArrayList<>();
        while (files.size() > 0) {
            File file = files.remove(0);
            if (file.isFile()) {
                String fileName = file.getName();
                String extension = Utils.getFileNameExtension(fileName);
                if (shouldLoadAsGameFile(file, fileName, extension))
                    loadFiles.add(file);
            } else if (file.isDirectory()) {
                File[] directoryFiles = file.listFiles();
                if (directoryFiles != null)
                    Collections.addAll(files, directoryFiles);
            }
        }

        if (progressBar != null)
            progressBar.update(0, loadFiles.size(), "");

        for (File file : loadFiles)
            loadFile(file, false, progressBar);
    }

    private void loadFile(File file, boolean showError, ProgressBarComponent progressBar) {
        IHudsonFileDefinition fileDefinition = new HudsonFileUserFSDefinition(this, file);

        if (progressBar != null)
            progressBar.setStatusMessage("Loading '" + file.getName() + "'...");

        try {
            FileSource fileSource = new FileSource(file);
            DataReader reader = new DataReader(fileSource);

            HudsonGameFile gameFile = createGameFile(fileDefinition, fileSource.getFileData());
            gameFile.load(reader);
            this.files.add(gameFile);
            this.allFiles.add(gameFile);
            if (gameFile instanceof IHudsonFileSystem)
                this.allFiles.addAll(((IHudsonFileSystem) gameFile).getGameFiles());
        } catch (IOException ex) {
            Utils.handleError(getLogger(), ex, showError, "Failed to load file '%s'.", Utils.toLocalPath(this.rootFolder, file, true));
        }

        if (progressBar != null)
            progressBar.addCompletedProgress(1);
    }

    @Override
    protected MainMenuController<?, ?> makeMainMenuController() {
        return new HudsonMainMenuUIController<>(this);
    }

    @Override
    public File getMainGameFolder() {
        if (this.rootFolder != null) {
            return this.rootFolder;
        } else {
            throw new IllegalStateException("The folder is not known since the game has not been loaded yet.");
        }
    }

    /**
     * Gets the RenderWare stream chunk type registry to use to load files.
     */
    public abstract RwStreamChunkTypeRegistry getRwStreamChunkTypeRegistry();

    /**
     * Reads the contents of a game file from the reader.
     * @param reader the reader to read the game file from
     * @param fileEntry the file entry containing information about the file
     * @param fileDefinition the file definition to apply
     * @param progressBar the progress bar to update, if there is one
     * @return newGameFile
     */
    public HudsonGameFile readGameFile(DataReader reader, HFSHeaderFileEntry fileEntry, IHudsonFileDefinition fileDefinition, ProgressBarComponent progressBar) {
        byte[] rawFileData = reader.readBytes(fileEntry.getFileDataLength());
        byte[] readFileData = fileEntry.isCompressed() ? PRS1Unpacker.decompressPRS1(rawFileData) : rawFileData;

        // Setup new file.
        HudsonGameFile newGameFile = createGameFile(fileDefinition, readFileData);
        newGameFile.setRawData(readFileData);
        newGameFile.setCompressionEnabled(fileEntry.isCompressed());
        if (progressBar != null)
            progressBar.setStatusMessage("Reading '" + newGameFile.getDisplayName() + "'...");

        // Load file.
        DataReader fileReader = new DataReader(new ArraySource(readFileData));
        try {
            newGameFile.load(fileReader);
        } catch (Exception ex) {
            Utils.handleError(getLogger(), ex, true, "Failed to load '%s'.", newGameFile.getDisplayName());

            // Setup dummy instead.
            newGameFile = new HudsonDummyFile(fileDefinition);
            newGameFile.setRawData(readFileData);
            newGameFile.setCompressionEnabled(fileEntry.isCompressed());
            fileReader.setIndex(0);
            newGameFile.load(fileReader);
        }

        if (progressBar != null)
            progressBar.addCompletedProgress(1);

        this.allFiles.add(newGameFile);
        return newGameFile;
    }

    /**
     * Saves the contents of a game file to the writer.
     * @param writer the writer to write the game file data to
     * @param gameFile the game file to save
     * @param fileEntry the file entry containing information about the file
     * @param progressBar the progress bar to update, if there is one
     * @return true iff the file was saved successfully
     */
    public boolean saveGameFile(DataWriter writer, HudsonGameFile gameFile, HFSHeaderFileEntry fileEntry, ProgressBarComponent progressBar) {
        if (progressBar != null)
            progressBar.setStatusMessage("Writing '" + gameFile.getDisplayName() + "'...");

        ArrayReceiver fileByteArray = new ArrayReceiver();
        DataWriter fileWriter = new DataWriter(fileByteArray);

        try {
            gameFile.save(fileWriter);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, true, "Failed to save file '%s' to HFS.", gameFile.getDisplayName());
            return false;
        }

        byte[] writtenFileBytes = fileByteArray.toArray();
        if (gameFile.isCompressionEnabled()) {
            // TODO: Add compression behavior, and apply the flag.
            if (PRS1Unpacker.isCompressedPRS1(writtenFileBytes) && fileEntry != null)
                fileEntry.cdSectorWithFlags |= HFSHeaderFileEntry.FLAG_IS_COMPRESSED;
        }

        if (fileEntry != null)
            fileEntry.fileDataLength = writtenFileBytes.length;
        writer.writeBytes(writtenFileBytes);

        if (progressBar != null)
            progressBar.addCompletedProgress(1);

        return true;
    }

    /**
     * Creates a game file.
     * @param fileDefinition the definition to create the file with.
     * @param rawData the raw file bytes to test file signatures with
     * @return gameFile
     */
    public HudsonGameFile createGameFile(IHudsonFileDefinition fileDefinition, byte[] rawData) {
        if (RwStreamFile.isRwStreamFile(rawData)) {
            return new HudsonRwStreamFile(fileDefinition);
        } else {
            return new HudsonDummyFile(fileDefinition);
        }
    }
}