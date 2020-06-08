package net.highwayfrogs.editor.file.sound.psx;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.GameSound;
import net.highwayfrogs.editor.file.sound.psx.PSXVBFile.PSXSound;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Reads a PSX vab header file.
 * Resources: LIBSND.H (PsyQ)
 * Created by Kneesnap on 11/30/2019.
 */
@Getter
public class PSXVHFile extends GameFile {
    private int fileVersion = 7;
    private int vabBankId;
    private int reserved0;
    private int programCount;
    private int toneCount;
    private int vagCount;
    private short masterVolume;
    private short panLevel;
    private short attr1;
    private short attr2;
    private long reserved1;
    private VABProgram[] programs = new VABProgram[128];
    private VABTone[] tones;
    @Setter private transient PSXVBFile VB;
    private transient int[] loadedSampleAddresses;

    private static final int TONES_PER_PROGRAM = 16;
    public static final int TYPE_ID = 2;
    public static final Image ICON = loadIcon("sound");
    public static final int CHANNEL_COUNT = 1;
    private static final String SIGNATURE = "pBAV"; // VABp

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);
        this.fileVersion = reader.readInt();
        if (this.fileVersion != 7 && this.fileVersion != 6)
            throw new RuntimeException("Unknown Vab Header Version: " + this.fileVersion + "!");

        this.vabBankId = reader.readInt();
        reader.readUnsignedIntAsLong(); // Seems like it's the size of the VH + VB combined.
        this.reserved0 = reader.readUnsignedShortAsInt();
        this.programCount = reader.readUnsignedShortAsInt();
        this.toneCount = reader.readUnsignedShortAsInt();
        this.vagCount = reader.readUnsignedShortAsInt();
        this.masterVolume = reader.readUnsignedByteAsShort();
        this.panLevel = reader.readUnsignedByteAsShort();
        this.attr1 = reader.readUnsignedByteAsShort();
        this.attr2 = reader.readUnsignedByteAsShort();
        this.reserved1 = reader.readUnsignedIntAsLong(); // Header size = 32 bytes.

        // Read programs.
        for (int i = 0; i < this.programs.length; i++) {
            this.programs[i] = new VABProgram();
            this.programs[i].load(reader);
        }

        // Read Tones.
        int totalToneCount = (TONES_PER_PROGRAM * this.programCount);
        this.tones = new VABTone[totalToneCount];
        for (int i = 0; i < totalToneCount; i++) {
            this.tones[i] = new VABTone();
            this.tones[i].load(reader);
        }

        // Read Samples.
        this.loadedSampleAddresses = new int[256];
        for (int i = 0; i < this.loadedSampleAddresses.length; i++)
            this.loadedSampleAddresses[i] = (reader.readUnsignedShortAsInt() << 3);

        this.loadedSampleAddresses = null;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE);
        writer.writeInt(this.fileVersion);
        writer.writeInt(this.vabBankId);
        int sizeAddress = writer.writeNullPointer();
        writer.writeUnsignedShort(this.reserved0);
        writer.writeUnsignedShort(this.programCount);
        writer.writeUnsignedShort(this.toneCount);
        writer.writeUnsignedShort(this.vagCount);
        writer.writeUnsignedByte(this.masterVolume);
        writer.writeUnsignedByte(this.panLevel);
        writer.writeUnsignedByte(this.attr1);
        writer.writeUnsignedByte(this.attr2);
        writer.writeUnsignedInt(this.reserved1);

        // Write programs.
        for (VABProgram program : getPrograms())
            program.save(writer);

        // Write Tones.
        for (VABTone tone : getTones())
            tone.save(writer);

        // Write Sample Addresses.
        for (GameSound sound : getVB().getAudioEntries())
            writer.writeUnsignedShort(((PSXSound) sound).getAddressWrittenTo() >> 3);

        // Write the final size.
        int headerSize = writer.getIndex();
        writer.jumpTemp(sizeAddress);
        writer.writeInt(headerSize + getVB().getSavedTotalSize());
        writer.jumpReturn();
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

    @Getter
    @Setter // LIBSND.H: VagAtr "VAG Tone Headdings"
    public static class VABProgram extends GameObject {
        private short toneCount;
        private short volume;
        private short priority;
        private short mode;
        private short panning;
        private short reserved1;
        private short attribute;
        private int reserved2;
        private int reserved3;

        @Override
        public void load(DataReader reader) {
            this.toneCount = reader.readUnsignedByteAsShort();
            this.volume = reader.readUnsignedByteAsShort();
            this.priority = reader.readUnsignedByteAsShort();
            this.mode = reader.readUnsignedByteAsShort();
            this.panning = reader.readUnsignedByteAsShort();
            this.reserved1 = reader.readUnsignedByteAsShort();
            this.attribute = reader.readShort();
            this.reserved2 = reader.readInt();
            this.reserved3 = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedByte(this.toneCount);
            writer.writeUnsignedByte(this.volume);
            writer.writeUnsignedByte(this.priority);
            writer.writeUnsignedByte(this.mode);
            writer.writeUnsignedByte(this.panning);
            writer.writeUnsignedByte(this.reserved1);
            writer.writeShort(this.attribute);
            writer.writeInt(this.reserved2);
            writer.writeInt(this.reserved3);
        }
    }

    @Getter
    @Setter
    public static class VABTone extends GameObject {
        private short priority;
        private short mode;
        private short volume = 80;
        private short panning = 64;
        private short centerNote = 64;
        private short pitchShift;
        private short minNote = 64;
        private short maxNote = 64;
        private short vibratoWidth;
        private short vibratoTime;
        private short portamentoWidth; // Pitch sliding from one note to another.
        private short portamentoTime;
        private short pitchbendMin;
        private short pitchbendMax;
        private short reserved1;
        private short reserved2;
        private short adsr1 = (short) 0x80FF; // What are these?
        private short adsr2 = 0x5FC0;
        private short parentProgram;
        private short wave;
        private short reserved3;
        private short reserved4;
        private short reserved5;
        private short reserved6;

        @Override
        public void load(DataReader reader) {
            this.priority = reader.readUnsignedByteAsShort();
            this.mode = reader.readUnsignedByteAsShort();
            this.volume = reader.readUnsignedByteAsShort();
            this.panning = reader.readUnsignedByteAsShort();
            this.centerNote = reader.readUnsignedByteAsShort();
            this.pitchShift = reader.readUnsignedByteAsShort();
            this.minNote = reader.readUnsignedByteAsShort();
            this.maxNote = reader.readUnsignedByteAsShort();
            this.vibratoWidth = reader.readUnsignedByteAsShort();
            this.vibratoTime = reader.readUnsignedByteAsShort();
            this.portamentoWidth = reader.readUnsignedByteAsShort();
            this.portamentoTime = reader.readUnsignedByteAsShort();
            this.pitchbendMin = reader.readUnsignedByteAsShort();
            this.pitchbendMax = reader.readUnsignedByteAsShort();
            this.reserved1 = reader.readUnsignedByteAsShort();
            this.reserved2 = reader.readUnsignedByteAsShort();
            this.adsr1 = reader.readShort();
            this.adsr2 = reader.readShort();
            this.parentProgram = reader.readShort(); // Might be run-time only? Unsure.
            this.wave = reader.readShort();
            this.reserved3 = reader.readShort();
            this.reserved4 = reader.readShort();
            this.reserved5 = reader.readShort();
            this.reserved6 = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedByte(this.priority);
            writer.writeUnsignedByte(this.mode);
            writer.writeUnsignedByte(this.volume);
            writer.writeUnsignedByte(this.panning);
            writer.writeUnsignedByte(this.centerNote);
            writer.writeUnsignedByte(this.pitchShift);
            writer.writeUnsignedByte(this.minNote);
            writer.writeUnsignedByte(this.maxNote);
            writer.writeUnsignedByte(this.vibratoWidth);
            writer.writeUnsignedByte(this.vibratoTime);
            writer.writeUnsignedByte(this.portamentoWidth);
            writer.writeUnsignedByte(this.portamentoTime);
            writer.writeUnsignedByte(this.pitchbendMin);
            writer.writeUnsignedByte(this.pitchbendMax);
            writer.writeUnsignedByte(this.reserved1);
            writer.writeUnsignedByte(this.reserved2);
            writer.writeShort(this.adsr1);
            writer.writeShort(this.adsr2);
            writer.writeShort(this.parentProgram);
            writer.writeShort(this.wave);
            writer.writeShort(this.reserved3);
            writer.writeShort(this.reserved4);
            writer.writeShort(this.reserved5);
            writer.writeShort(this.reserved6);
        }
    }
}