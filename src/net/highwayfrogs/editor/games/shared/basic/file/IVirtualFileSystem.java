package net.highwayfrogs.editor.games.shared.basic.file;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;

import java.util.List;

/**
 * Represents a virtual file system, a part of another file.
 * Created by Kneesnap on 8/12/2024.
 */
public interface IVirtualFileSystem extends IBasicGameFile {
    /**
     * Gets a list of all game files available within the file-system.
     */
    List<? extends BasicGameFile<?>> getGameFiles();

    /**
     * Loads the contents of the virtual file-system while updating the progress bar.
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
}