package net.highwayfrogs.editor.utils;

import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents an object that can be saved / loaded from a binary reader/writer without any additional parameters.
 * Created by Kneesnap on 9/8/2023.
 */
public interface IBinarySerializable {
    /**
     * Loads information from the file into this object.
     * @param reader The reader to load information from.
     */
    public abstract void load(DataReader reader);

    /**
     * Saves information from the instance into a DataWriter.
     * @param writer The writer to save information into.
     */
    public abstract void save(DataWriter writer);

    /**
     * Imports the contents of this object from a file.
     * This is NOT the primary method of loading data, this is intended primarily for importing files from the user.
     * If an error occurs while loading the data, we will attempt to restore the state of the object by loading the data which had previously been supplied.
     * If this fails, the object will be left in an undefined state.
     * @param logger The logger to use if an error occurs. Providing null is valid.
     * @param inputFile The file to load the data from
     * @param showPopupOnError Whether to show the popup on error.
     * @return true iff the import occurs successfully.
     */
    default boolean importDataFromFile(Logger logger, File inputFile, boolean showPopupOnError) {
        if (inputFile == null)
            throw new NullPointerException("inputFile");

        if (!inputFile.exists() || !inputFile.isFile())
            throw new IllegalArgumentException("Could not find a file named '" + inputFile.getName() + "' to import data from!");

        FileSource fileSource;
        try {
            fileSource = new FileSource(inputFile);
        } catch (IOException ex) {
            Utils.handleError(logger, ex, showPopupOnError, "Failed to read contents of '%s'.", inputFile.getName());
            return false;
        }

        return importDataFromReader(logger, new DataReader(fileSource), showPopupOnError);
    }

    /**
     * Imports the contents of this object from a reader.
     * This is NOT the primary method of loading data, this is intended primarily for importing files from the user.
     * If an error occurs while loading the data, we will attempt to restore the state of the object by loading the data which had previously been supplied.
     * If this fails, the object will be left in an undefined state.
     * @param logger The logger to use if an error occurs. Providing null is valid.
     * @param reader The reader to read data with
     * @param showPopupOnError Whether to show the popup on error.
     * @return true iff the import occurs successfully.
     */
    default boolean importDataFromReader(Logger logger, DataReader reader, boolean showPopupOnError) {
        if (reader == null)
            throw new NullPointerException("reader");

        // Take a backup of existing data
        ArrayReceiver backupReceiver = new ArrayReceiver();
        DataWriter backupWriter = new DataWriter(backupReceiver);

        try {
            this.save(backupWriter);
            backupWriter.closeReceiver();
        } catch (Throwable th) {
            Utils.handleError(logger, th, showPopupOnError, "Failed to create a backup of %s.", this);
            return false;
        }

        // Attempt to load the new data.
        try {
            this.load(reader);
            return true;
        } catch (Throwable th) {
            Utils.handleError(logger, th, showPopupOnError, "Failed to load the new data.", this);

            // Attempt to restore from backup.
            byte[] backupData = backupReceiver.toArray();
            DataReader backupReader = new DataReader(new ArraySource(backupData));
            try {
                this.load(backupReader);
            } catch (Throwable th2) {
                Utils.handleError(logger, th, showPopupOnError, "Failed restore '%s' from backup data.", this);
            }

            return false;
        }
    }

    /**
     * Serializes the data in this object, saving it to a file.
     * @param logger The logger to write the output data to
     * @param outputFile the file to write the data to
     * @param showPopupOnError when an error occurs, this allows specifying if a popup should be shown
     */
    default boolean writeDataToFile(Logger logger, File outputFile, boolean showPopupOnError) {
        if (outputFile == null)
            throw new NullPointerException("outputFile");

        ArrayReceiver arrayReceiver = new ArrayReceiver();
        DataWriter writer = new DataWriter(arrayReceiver);

        try {
            this.save(writer);
            writer.closeReceiver();
        } catch (Throwable th) {
            Utils.handleError(logger, th, showPopupOnError, "Failed to serialize %s. (For saving as '%s')", this, outputFile.getName());
            return false;
        }

        return Utils.writeBytesToFile(logger, outputFile, arrayReceiver.toArray(), showPopupOnError);
    }
}