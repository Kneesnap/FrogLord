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
import java.util.List;

/**
 * Represents a game using the Hudson/RenderWare engine seen in Frogger's Adventures: The Rescue and Frogger Ancient Shadow.
 * Created by Kneesnap on 8/8/2024.
 */
@Getter
public abstract class HudsonGameInstance extends GameInstance {
    private IHudsonFileSystem mainHfs;
    private File mainHfsFile;

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
        if (this.mainHfs != null)
            throw new RuntimeException("The game instance has already been loaded.");

        if (mainFile == null || !mainFile.exists())
            throw new RuntimeException("The main archive file '" + mainFile + "' does not exist.");

        this.mainHfsFile = mainFile;
        loadGameConfig(gameVersionConfigName);

        // Load the main file.
        try {
            DataReader reader = new DataReader(new FileSource(mainFile));
            this.mainHfs = createHfsFile(new HudsonFileUserFSDefinition(this, mainFile));
            this.mainHfs.load(reader, progressBar);
        } catch (IOException ex) {
            Utils.handleError(getLogger(), ex, true, "Failed to load the bin file.");
        }
    }

    @Override
    protected MainMenuController<?, ?> makeMainMenuController() {
        return new HudsonMainMenuUIController<>(this);
    }

    @Override
    public File getMainGameFolder() {
        if (this.mainHfsFile != null) {
            return this.mainHfsFile.getParentFile();
        } else {
            throw new IllegalStateException("The folder is not known since the game has not been loaded yet.");
        }
    }

    /**
     * Gets all game files tracked for the game instance.
     */
    public List<HudsonGameFile> getAllGameFiles() {
        return this.mainHfs.getGameFiles();
    }

    /**
     * Creates an HFS file for the current game instance based on the given definition.
     * @param fileDefinition the definition to create the hfs file for
     * @return newHfsFile
     */
    protected abstract IHudsonFileSystem createHfsFile(IHudsonFileDefinition fileDefinition);

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