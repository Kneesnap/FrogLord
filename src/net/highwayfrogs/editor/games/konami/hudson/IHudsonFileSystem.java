package net.highwayfrogs.editor.games.konami.hudson;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.IGameObject;

import java.io.File;
import java.util.List;

/**
 * Represents a hudson file system across different versions of the format.
 * Created by Kneesnap on 8/8/2024.
 */
public interface IHudsonFileSystem extends IGameObject {
    /**
     * Gets the game instance associated with the file-system.
     */
    HudsonGameInstance getGameInstance();

    /**
     * Gets a list of all game files available within the hfs.
     */
    List<HudsonGameFile> getGameFiles();

    /**
     * Loads the contents of the HFS file while updating the progress bar.
     * @param reader the reader to read data from
     * @param progressBar the progress bar to update, if not null
     */
    void load(DataReader reader, ProgressBarComponent progressBar);

    /**
     * Saves the contents of the HFS file while updating the progress bar.
     * @param writer writer to write data to
     * @param progressBar the progress bar to update, if not null
     */
    void save(DataWriter writer, ProgressBarComponent progressBar);

    /**
     * Export the game files from this virtual file-system into the user's real file-system.
     * @param exportFolder the folder chosen for the file exportation
     */
    void export(File exportFolder);

    /**
     * Gets the name associated with the file-system. Usually the hfs file name.
     */
    String getDisplayName();

    /**
     * Gets the name associated with the file-system. Usually the hfs file path.
     */
    String getFullDisplayName();
}