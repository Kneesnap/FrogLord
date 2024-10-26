package net.highwayfrogs.editor.games.shared.basic;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.games.shared.basic.file.BasicGameFile;
import net.highwayfrogs.editor.games.shared.basic.file.IVirtualFileSystem;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.games.shared.basic.file.definition.PhysicalFileDefinition;
import net.highwayfrogs.editor.games.shared.basic.ui.BasicMainMenuUIController;
import net.highwayfrogs.editor.gui.MainMenuController;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a game instance which has loose files.
 * Created by Kneesnap on 8/12/2024.
 */
public abstract class BasicGameInstance extends GameInstance {
    @Getter protected final List<BasicGameFile<?>> files = new ArrayList<>();
    @Getter protected final List<BasicGameFile<?>> allFiles = new ArrayList<>(); // Includes nested files.
    protected File rootFolder;

    public BasicGameInstance(IGameType gameType) {
        super(gameType);
    }

    /**
     * Load and setup all instance data relating to the game such as version configuration and game files.
     * @param gameVersionConfigName The name of the version configuration file to load.
     * @param mainFolder the main folder file to read game files from
     * @param progressBar the progress bar to display load progress on, if it exists
     */
    public void loadGame(String gameVersionConfigName, File mainFolder, ProgressBarComponent progressBar) {
        if (this.rootFolder != null)
            throw new RuntimeException("The game instance has already been loaded.");

        if (mainFolder == null || !mainFolder.exists())
            throw new RuntimeException("The folder '" + mainFolder + "' does not exist.");

        if (mainFolder.isDirectory()) {
            this.rootFolder = mainFolder;
            loadGameConfig(gameVersionConfigName);
            loadFilesFromFolderRecursive(mainFolder, progressBar);
        } else {
            throw new RuntimeException("Don't know how to load '" + mainFolder + "', which reported as not a directory!");
        }
    }

    /**
     * Tests if the given file should be loaded as a game file for the game.
     * @param file the file to load
     * @param fileName the name of the file
     * @param extension the file extension, if there is one
     * @return should the file be loaded as a game file?
     */
    protected abstract boolean shouldLoadAsGameFile(File file, String fileName, String extension);

    private void loadFilesFromFolderRecursive(File folder, ProgressBarComponent progressBar) {
        List<File> files = new ArrayList<>();
        files.add(folder);

        List<File> loadFiles = new ArrayList<>();
        while (files.size() > 0) {
            File file = files.remove(0);
            if (file.isFile()) {
                String fileName = file.getName();
                String extension = FileUtils.getFileNameExtension(fileName);
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

    /**
     * Loads the file data from the given file.
     * @param file the file to read data from
     * @param singleGameDataFile whether this is a single file holding all game data
     * @param progressBar the progress bar to update
     */
    protected void loadFile(File file, boolean singleGameDataFile, ProgressBarComponent progressBar) {
        PhysicalFileDefinition fileDefinition = new PhysicalFileDefinition(this, file);

        if (progressBar != null && !singleGameDataFile)
            progressBar.setStatusMessage("Loading '" + file.getName() + "'...");

        BasicGameFile<?> gameFile = null;
        try {
            FileSource fileSource = new FileSource(file);
            DataReader reader = new DataReader(fileSource);

            gameFile = createGameFile(fileDefinition, fileSource.getFileData());
            gameFile.setRawData(fileSource.getFileData());
            if (singleGameDataFile && gameFile instanceof IVirtualFileSystem) {
                ((IVirtualFileSystem) gameFile).load(reader, progressBar);
            } else {
                gameFile.load(reader);
            }
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, singleGameDataFile, "Failed to load file '%s'.", FileUtils.toLocalPath(this.rootFolder, file, true));
        }

        if (gameFile != null) {
            this.files.add(gameFile);
            this.allFiles.add(gameFile);
            if (gameFile instanceof IVirtualFileSystem)
                this.allFiles.addAll(((IVirtualFileSystem) gameFile).getGameFiles());
        }

        if (progressBar != null && !singleGameDataFile)
            progressBar.addCompletedProgress(1);
    }

    @Override
    protected MainMenuController<?, ?> makeMainMenuController() {
        return new BasicMainMenuUIController<>(this);
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
     * Creates a game file.
     * @param fileDefinition the definition to create the file with.
     * @param rawData the raw file bytes to test file signatures with
     * @return gameFile
     */
    public abstract BasicGameFile<?> createGameFile(IGameFileDefinition fileDefinition, byte[] rawData);
}