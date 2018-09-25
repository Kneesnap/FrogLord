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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a Playstation audio bank.
 * The VAB Body File needs to be handled too.
 * Until then, this functionality is unused.
 * If/When this functionality is enabled, it should be tested.
 * Created by Kneesnap on 9/25/2018.
 */
public class VABHeaderFile extends GameFile {
    private List<VABProgram> programs = new ArrayList<>();
    private List<Integer> vagAddresses = new ArrayList<>();
    private int vabId;
    private int waveformSize;
    private short reserved1;
    private short masterVolume;
    private short masterPan;
    private byte attr1; // "User defined"
    private byte attr2;
    private int reserved2;

    public static final String SIGNATURE = "pBAV";
    private static final int MAX_PROGRAMS = 128;
    private static final int MAX_TONES = 16;
    private static final int MAX_VAGS = 255;

    private static final VABProgram BLANK_PROGRAM = new VABProgram();
    private static final VABTone BLANK_TONE = new VABTone();
    private static final int VERSION = 7;

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);
        int version = reader.readInt();
        this.vabId = reader.readInt();
        this.waveformSize = reader.readInt();
        this.reserved1 = reader.readShort();
        short programCount = reader.readShort();
        short toneCount = reader.readShort();
        short vagCount = reader.readShort(); // Also called waveCount.
        this.masterVolume = reader.readUnsignedByteAsShort();
        this.masterPan = reader.readUnsignedByteAsShort();
        this.attr1 = reader.readByte();
        this.attr2 = reader.readByte();
        this.reserved2 = reader.readInt();

        AtomicInteger toneAddress = new AtomicInteger(reader.getIndex() + (MAX_PROGRAMS * VABProgram.BYTE_SIZE));
        for (int i = 0; i < programCount; i++) {
            VABProgram vabProgram = new VABProgram();
            vabProgram.load(reader, toneAddress);
            programs.add(vabProgram);
        }

        reader.jumpTemp(toneAddress.get());
        reader.readShort(); // Padding.

        for (int i = 0; i < vagCount; i++)
            this.vagAddresses.add(reader.readShort() << 3);

        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE);
        writer.writeInt(VERSION);
        writer.writeInt(this.vabId);
        writer.writeInt(this.waveformSize);
        writer.writeShort(this.reserved1);

        short programCount = (short) this.programs.size();
        short toneCount = (short) this.programs.stream().mapToInt(program -> program.getTones().size()).sum();
        short vagCount = (short) this.vagAddresses.size();
        Utils.verify(programCount <= MAX_PROGRAMS, "Cannot have more than %d VAB Programs. (%d)", MAX_PROGRAMS, programCount);
        Utils.verify(toneCount <= MAX_TONES * MAX_PROGRAMS, "Invalid tone count: %d.", toneCount);
        Utils.verify(vagCount <= MAX_VAGS, "Cannot have more than %d VAGs. (%d)", MAX_VAGS, vagCount);

        writer.writeShort(programCount);
        writer.writeShort(toneCount);
        writer.writeShort(vagCount);

        writer.writeUnsignedByte(this.masterVolume);
        writer.writeUnsignedByte(this.masterPan);
        writer.writeByte(this.attr1);
        writer.writeByte(this.attr2);
        writer.writeInt(this.reserved2);

        // Write program and tones.
        AtomicInteger toneAddress = new AtomicInteger(writer.getIndex() + (MAX_PROGRAMS * VABProgram.BYTE_SIZE));
        for (int i = 0; i < MAX_PROGRAMS; i++) {
            VABProgram program = this.programs.size() > i ? this.programs.get(i) : BLANK_PROGRAM;
            program.save(writer, toneAddress);
        }

        // Write waves / VAG addresses.
        writer.jumpTemp(toneAddress.get());
        writer.writeNull(Constants.SHORT_SIZE); // Padding.

        for (int i = 0; i < MAX_VAGS + 1; i++)
            writer.writeShort((short) (this.vagAddresses.size() > i ? (this.vagAddresses.get(i) >> 3) : 0));

        writer.jumpReturn();
    }

    @Override
    public Image getIcon() {
        return VHFile.ICON;
    }

    @Override
    public Node makeEditor() {
        return null;
        //return loadEditor(new VABController(), "vab", this);
    }

    @Getter
    @Setter
    private static class VABProgram extends GameObject {
        private List<VABTone> tones = new ArrayList<>();
        private short volume;
        private short priority;
        private short mode;
        private short panning;
        private short reserved1;
        private short attribute;
        private int reserved2;
        private int reserved3;

        private AtomicInteger suppliedReadToneAddress;
        private AtomicInteger suppliedWriteToneAddress;

        public static final int BYTE_SIZE = (6 * Constants.BYTE_SIZE) + Constants.SHORT_SIZE + (2 * Constants.INTEGER_SIZE);

        public void load(DataReader reader, AtomicInteger toneAddress) {
            this.suppliedReadToneAddress = toneAddress;
            this.load(reader);
            this.suppliedReadToneAddress = null;
        }

        @Override
        public void load(DataReader reader) {
            Utils.verify(this.suppliedReadToneAddress != null, "Tried to load a VAB Program without a VAB Tone address.");
            short toneCount = reader.readUnsignedByteAsShort();
            this.volume = reader.readUnsignedByteAsShort();
            this.priority = reader.readUnsignedByteAsShort();
            this.mode = reader.readUnsignedByteAsShort();
            this.panning = reader.readUnsignedByteAsShort();
            this.reserved1 = reader.readUnsignedByteAsShort();
            this.attribute = reader.readShort();
            this.reserved2 = reader.readInt();
            this.reserved3 = reader.readInt();

            reader.jumpTemp(this.suppliedReadToneAddress.get());

            for (int i = 0; i < toneCount; i++) {
                VABTone newTone = new VABTone();
                newTone.load(reader);
                this.tones.add(newTone);
            }

            this.suppliedReadToneAddress.set(reader.getIndex()); // Set the index for the next program.
            reader.jumpReturn();
        }

        public void save(DataWriter writer, AtomicInteger toneAddress) {
            this.suppliedWriteToneAddress = toneAddress;
            this.save(writer);
            this.suppliedWriteToneAddress = null;
        }

        @Override
        public void save(DataWriter writer) {
            Utils.verify(this.suppliedWriteToneAddress != null, "Tried to save a VAB Program without a Tone Address.");
            writer.writeUnsignedByte((short) this.tones.size());
            writer.writeUnsignedByte(this.volume);
            writer.writeUnsignedByte(this.priority);
            writer.writeUnsignedByte(this.mode);
            writer.writeUnsignedByte(this.panning);
            writer.writeUnsignedByte(this.reserved1);
            writer.writeShort(this.attribute);
            writer.writeInt(this.reserved2);
            writer.writeInt(this.reserved3);

            writer.jumpTemp(this.suppliedWriteToneAddress.get());
            for (int i = 0; i < MAX_TONES; i++) {
                VABTone tone = this.tones.size() > i ? this.tones.get(i) : BLANK_TONE;
                tone.save(writer);
            }

            this.suppliedWriteToneAddress.set(writer.getIndex()); // Set the next tone address after the current data.
            writer.jumpReturn();
        }
    }

    // LIBSND.H: VagAtr "VAG Tone Headdings"
    @Getter
    @Setter
    private static class VABTone extends GameObject {
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

        public static final int BYTE_SIZE = (16 * Constants.BYTE_SIZE) + (8 * Constants.SHORT_SIZE);

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
