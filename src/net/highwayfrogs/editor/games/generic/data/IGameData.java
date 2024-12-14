package net.highwayfrogs.editor.games.generic.data;

import net.highwayfrogs.editor.file.reader.DataReader;

import java.io.File;

/**
 * Represents the functionality seen in GameData.
 * Created by Kneesnap on 10/26/2024.
 */
public interface IGameData extends IGameObject, IBinarySerializable {
    /**
     * Imports the contents of this object from a file.
     * This is NOT the primary method of loading data, this is intended primarily for importing files from the user.
     * If an error occurs while loading the data, we will attempt to restore the state of the object by loading the data which had previously been supplied.
     * If this fails, the object will be left in an undefined state.
     * @param inputFile The file to load the data from
     * @param showPopupOnError Whether to show the popup on error.
     * @return true iff the import occurs successfully.
     */
    boolean importDataFromFile(File inputFile, boolean showPopupOnError);

    /**
     * Imports the contents of this object from a reader.
     * This is NOT the primary method of loading data, this is intended primarily for importing files from the user.
     * If an error occurs while loading the data, we will attempt to restore the state of the object by loading the data which had previously been supplied.
     * If this fails, the object will be left in an undefined state.
     * @param reader The reader to read data with
     * @param showPopupOnError Whether to show the popup on error.
     * @return true iff the import occurs successfully.
     */
    boolean importDataFromReader(DataReader reader, boolean showPopupOnError);

    /**
     * Serializes the data in this object, saving it to a file.
     * @param outputFile the file to write the data to
     * @param showPopupOnError when an error occurs, this allows specifying if a popup should be shown
     */
    boolean writeDataToFile(File outputFile, boolean showPopupOnError);
}
