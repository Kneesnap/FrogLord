package net.highwayfrogs.editor.file;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

@Getter
public class VHFile extends GameObject {
    private List<FileEntry> entries = new ArrayList<>();
    private int fileSize;

    private static final int ENTRY_LENGTH = 28;

    @Override
    public void load(DataReader reader) {
        this.fileSize = reader.getSize();

        int numEntries = reader.readInt();

        for (int i = 0; i < numEntries; i++) {
            FileEntry entry = new FileEntry();

            entry.setChannels(reader.readInt());
            entry.setDataStartOffset(reader.readInt());
            entry.setDataSize(reader.readInt());
            entry.setUnknown1(reader.readInt());
            entry.setUnknown2(reader.readInt());
            entry.setSampleRate(reader.readInt());
            entry.setBitWidth(reader.readInt());

            getEntries().add(entry);
            
            if((i * ENTRY_LENGTH) < fileSize) {
                throw new RuntimeException("Invalid number of entries in VH file!");
            }
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getEntries().size());
        for (FileEntry entry : getEntries()) {
            writer.writeInt(entry.getChannels());
            writer.writeInt(entry.getDataStartOffset());
            writer.writeInt(entry.getDataSize());
            writer.writeInt(entry.getUnknown1());
            writer.writeInt(entry.getUnknown2());
            writer.writeInt(entry.getSampleRate());
            writer.writeInt(entry.getBitWidth());
        }
    }

    @Setter
    @Getter
    public static class FileEntry {
        private int channels;
        private int dataStartOffset;
        private int dataSize;
        private int unknown1;
        private int unknown2;
        private int sampleRate;
        private int bitWidth;
    }
}
