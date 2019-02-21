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
    @Setter private transient AbstractVBFile VB;

    public static final int TYPE_ID = 2;
    public static final Image ICON = loadIcon("sound");
    public static final int CHANNEL_COUNT = 1;

    @Override
    public void load(DataReader reader) {
        int numEntries = reader.readInt();
        for (int i = 0; i < numEntries; i++) {
            AudioHeader entry = new AudioHeader();
            entry.load(reader);
            getEntries().add(entry);
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
        Utils.verify(getVB() != null, "VB sound is null.");
        return getVB().makeEditor(); // Build the editor for the right file.
    }

    @Setter
    @Getter
    public static class AudioHeader extends GameObject {
        private boolean audioPresent;
        private int dataStartOffset;
        private int dataSize;
        private int sampleRate;
        private int bitWidth;

        private static final int HAS_AUDIO = 1;
        private static final int UNKNOWN_VALUE = 1;

        @Override
        public void load(DataReader reader) {
            this.audioPresent = (reader.readInt() == HAS_AUDIO);
            this.dataStartOffset = reader.readInt();
            this.dataSize = reader.readInt();
            Utils.verify(reader.readInt() == UNKNOWN_VALUE, "Unknown Value #1 was not correct.");
            Utils.verify(reader.readInt() == UNKNOWN_VALUE, "Unknown Value #2 was not correct.");
            this.sampleRate = reader.readInt();
            this.bitWidth = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.audioPresent ? HAS_AUDIO : 0);
            writer.writeInt(this.dataStartOffset);
            writer.writeInt(this.dataSize);
            writer.writeInt(UNKNOWN_VALUE);
            writer.writeInt(UNKNOWN_VALUE);
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

        @Override
        public String toString() {
            return "[Data: (" + Utils.toHexString(dataStartOffset) + "->" + Utils.toHexString(dataStartOffset + dataSize)
                    + "), Sample Rate: " + sampleRate + ", Bit-Width: " + this.bitWidth + ", Has Audio: " + audioPresent + "]";
        }

    }
}
