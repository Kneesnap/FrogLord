package net.highwayfrogs.editor.file;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a WAD file.
 * Created by Kneesnap on 8/24/2018.
 */
public class WADFile extends GameFile {
    public static final int TYPE_ID = -1;

    @Override
    public void load(DataReader reader) {
        while (true) {
            int id = reader.readInt();
            if (id == 0xFFFFFFFF)
                break; // There are no moer files.

            int unknown1 = reader.readInt();
            int size = reader.readInt();
            int zero = reader.readInt();
            byte[] data = reader.readBytes(size);
            //TODO
        }
    }

    @Override
    public void save(DataWriter writer) {
        //TODO
    }
}
