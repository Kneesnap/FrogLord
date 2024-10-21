package net.highwayfrogs.editor.games.generic;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.utils.IBinarySerializable;

import java.io.File;

/**
 * Represents a game object which has the capability of saving / loading data.
 * @param <TGameInstance> The type of game instance this object corresponds to.
 * Created by Kneesnap on 4/10/2024.
 */
public abstract class GameData<TGameInstance extends GameInstance> extends GameObject<TGameInstance> implements IBinarySerializable {
    public GameData(TGameInstance instance) {
        super(instance);
    }

    /**
     * Imports the contents of this object from a file.
     * This is NOT the primary method of loading data, this is intended primarily for importing files from the user.
     * If an error occurs while loading the data, we will attempt to restore the state of the object by loading the data which had previously been supplied.
     * If this fails, the object will be left in an undefined state.
     * @param inputFile The file to load the data from
     * @param showPopupOnError Whether to show the popup on error.
     * @return true iff the import occurs successfully.
     */
    public final boolean importDataFromFile(File inputFile, boolean showPopupOnError) {
        return this.importDataFromFile(getLogger(), inputFile, showPopupOnError);
    }

    /**
     * Imports the contents of this object from a reader.
     * This is NOT the primary method of loading data, this is intended primarily for importing files from the user.
     * If an error occurs while loading the data, we will attempt to restore the state of the object by loading the data which had previously been supplied.
     * If this fails, the object will be left in an undefined state.
     * @param reader The reader to read data with
     * @param showPopupOnError Whether to show the popup on error.
     * @return true iff the import occurs successfully.
     */
    public final boolean importDataFromReader(DataReader reader, boolean showPopupOnError) {
        return this.importDataFromReader(getLogger(), reader, showPopupOnError);
    }

    /**
     * Serializes the data in this object, saving it to a file.
     * @param outputFile the file to write the data to
     * @param showPopupOnError when an error occurs, this allows specifying if a popup should be shown
     */
    public final boolean writeDataToFile(File outputFile, boolean showPopupOnError) {
        return this.writeDataToFile(getLogger(), outputFile, showPopupOnError);
    }

    /**
     * Represents GameData which can be used by any GameInstance.
     */
    public static abstract class SharedGameData extends GameData<GameInstance> {
        public SharedGameData(GameInstance instance) {
            super(instance);
        }
    }
}