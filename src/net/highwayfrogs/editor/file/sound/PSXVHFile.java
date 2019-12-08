/*package net.highwayfrogs.editor.file.sound;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a PSX vab file.
 * Created by Kneesnap on 11/30/2019.
 */
/*@Getter
public class PSXVHFile extends GameFile {
    private List<PSXAudioHeader> entries = new ArrayList<>();
    @Setter private transient AbstractVBFile VB;

    public static final int TYPE_ID = 2;
    public static final Image ICON = loadIcon("sound");
    public static final int CHANNEL_COUNT = 1;
    private static final String SIGNATURE = "VABp";

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);
        int fileVersion = reader.readInt();
        int vabBankId = reader.readInt();
        long fileSize = reader.readUnsignedIntAsLong();
        int reserved0 = reader.readUnsignedShortAsInt();
        int programCount = reader.readUnsignedShortAsInt();
        int toneCount = reader.readUnsignedShortAsInt();
        int vagCount = reader.readUnsignedShortAsInt();
        short masterVolume = reader.readUnsignedByteAsShort();
        short panLevel = reader.readUnsignedByteAsShort();
        short attr1 = reader.readUnsignedByteAsShort();
        short attr2 = reader.readUnsignedByteAsShort();
        long reserved1 = reader.readUnsignedIntAsLong(); // Header size = 32 bytes.

        //TODO: Read programs. [Always 128 programs]
        //TODO: Read tones. [32 bytes, getting the count unknown.]
        //TODO: Read samples []

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
    public static class PSXAudioHeader extends GameObject {
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
/*        public int getByteWidth() {
            return getBitWidth() / Constants.BITS_PER_BYTE;
        }

        @Override
        public String toString() {
            return "[Data: (" + Utils.toHexString(dataStartOffset) + "->" + Utils.toHexString(dataStartOffset + dataSize)
                    + "), Sample Rate: " + sampleRate + ", Bit-Width: " + this.bitWidth + ", Has Audio: " + audioPresent + "]";
        }

    }
}
*/