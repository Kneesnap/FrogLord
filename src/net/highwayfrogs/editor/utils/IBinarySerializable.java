package net.highwayfrogs.editor.utils;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

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
}