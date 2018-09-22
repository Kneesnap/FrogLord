package net.highwayfrogs.editor.file;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
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

    private static final Image ICON = loadIcon("packed");
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
            boolean compressed = PP20Unpacker.isCompressed(data);
            if (compressed)
                data = PP20Unpacker.unpackData(data);

            GameFile file;
            if (fileType == VLOArchive.WAD_TYPE || fileType == 1) {
                file = new VLOArchive();
            } else if (fileType == MOFFile.MOF_ID || fileType == MOFFile.MAP_MOF_ID) {
                file = new MOFFile();
            } else {
                throw new RuntimeException("Unexpected WAD file-type: " + fileType + ".");
            }

            file.load(new DataReader(new ArraySource(data)));
            files.add(new WADEntry(resourceId, fileType, compressed, file));
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
            if (entry.isCompressed())
                fileBytes = PP20Packer.packData(fileBytes);

            writer.writeInt(fileBytes.length); // File length.
            writer.writeNull(Constants.INTEGER_SIZE); // Padding
            writer.writeBytes(fileBytes); // Write file contents.
        }

        writer.writeInt(TERMINATOR);
        writer.writeNull(Constants.INTEGER_SIZE * 3);
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public Node makeEditor() {
        return null;
    }

    @Getter
    @AllArgsConstructor
    private static class WADEntry {
        private int resourceId;
        private int fileType;
        private boolean compressed;
        private GameFile file;
    }
}
