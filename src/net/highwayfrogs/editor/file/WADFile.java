package net.highwayfrogs.editor.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a WAD file.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
public class WADFile extends GameFile {
    private List<WADEntry> files = new ArrayList<>();

    public static final int TYPE_ID = -1;
    private static final int TERMINATOR = -1;

    @Override
    public void load(DataReader reader) {
        while (true) {
            int resourceId = reader.readInt();
            if (resourceId == TERMINATOR)
                break; // There are no more files.

            int fileType = reader.readInt();
            int size = reader.readInt();
            reader.readInt(); // Padding.
            byte[] data = reader.readBytes(size);

            Utils.verify(fileType == MOFFile.MOF_ID || fileType == MOFFile.MAP_MOF_ID, "Unexpected WAD file-type: %d.", fileType);

            GameFile file = new MOFFile();
            file.load(new DataReader(new ArraySource(data)));
            files.add(new WADEntry(resourceId, fileType, file));
        }
    }

    @Override
    public void save(DataWriter writer) {
        for (WADEntry entry : getFiles()) {
            writer.writeInt(entry.getResourceId());
            writer.writeInt(entry.getFileType());

            ArrayReceiver receiver = new ArrayReceiver();
            entry.getFile().save(new DataWriter(receiver));
            byte[] fileBytes = receiver.toArray();

            writer.writeInt(fileBytes.length); // File length.
            writer.writeNull(Constants.INTEGER_SIZE); // Padding
            writer.writeBytes(fileBytes); // Write file contents.
        }

        writer.writeInt(TERMINATOR);
        writer.writeNull(Constants.INTEGER_SIZE * 3);
    }

    @Getter
    @AllArgsConstructor
    private static class WADEntry {
        private int resourceId;
        private int fileType;
        private GameFile file;
    }
}
