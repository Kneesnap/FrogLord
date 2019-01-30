package net.highwayfrogs.editor.file.sound;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses VH files.
 * Created by rdrpenguin04 on 8/22/2018.
 */
@Getter
public class VHFile extends GameFile {
    private List<AudioHeader> entries = new ArrayList<>();
    @Setter private transient VBFile VB;

    public static final int TYPE_ID = 2;
    public static final Image ICON = loadIcon("sound");

    @Override
    public void load(DataReader reader) {
        int numEntries = reader.readInt();
        for (int i = 0; i < numEntries; i++) {
            AudioHeader entry = new AudioHeader();
            entry.load(reader);
            getEntries().add(entry);
        }

        AudioHeader lastEntry = getEntries().get(0);
        for (int i = 1; i < getEntries().size(); i++) {
            AudioHeader entry = getEntries().get(i);

            boolean isPresent = entry.getDataStartOffset() > lastEntry.getDataStartOffset();
            entry.setAudioPresent(isPresent);
            lastEntry.setAudioPresent(isPresent);
            lastEntry = entry;
        }

        getVB().load(this); // Load the linked body file.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getEntries().size());

        int offset = 0;
        for (AudioHeader entry : getEntries()) {
            entry.setDataStartOffset(offset);
            entry.save(writer);
            if (entry.isAudioPresent())
                offset += entry.getDataSize();
        }
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public Node makeEditor() {
        Utils.verify(getVB() != null, "VB sound was null.");
        return getVB().makeEditor(); // Build the editor for the right file.
    }

    @Setter
    @Getter
    public static class AudioHeader extends GameObject {
        private int channels;
        private int dataStartOffset;
        private int dataSize;
        private int unknown1;
        private int unknown2;
        private int sampleRate;
        private int bitWidth;

        private transient boolean audioPresent;

        @Override
        public void load(DataReader reader) {
            this.channels = reader.readInt();
            this.dataStartOffset = reader.readInt();
            this.dataSize = reader.readInt();
            this.unknown1 = reader.readInt();
            this.unknown2 = reader.readInt();
            this.sampleRate = reader.readInt();
            this.bitWidth = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.channels);
            writer.writeInt(this.dataStartOffset);
            writer.writeInt(this.dataSize);
            writer.writeInt(this.unknown1);
            writer.writeInt(this.unknown2);
            writer.writeInt(this.sampleRate);
            writer.writeInt(this.bitWidth);
        }

        /**
         * Get the byte-width for this sound.
         * @return byteWidth
         */
        public int getByteWidth() {
            return getBitWidth() / Constants.BITS_PER_BYTE;
        }
    }
}
