package net.highwayfrogs.editor.file;

import net.highwayfrogs.editor.file.reader.DataReader;

/**
 * This represents a single game-file, which can be turned into a byte array.
 * Created by Kneesnap on 8/10/2018.
 */
public abstract class GameFile {

    /**
     * Loads information from the file into this object.
     * @param reader The reader to load information from.
     */
    public abstract void load(DataReader reader);
}
