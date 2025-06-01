package net.highwayfrogs.editor.games.sony.shared.sound.header;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankHeaderEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCPlayStationSoundBankBody.SCPlayStationVabSound;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationVabSoundBankHeader.SCPlayStationVabHeaderEntry;

/**
 * Represents the PSX sound bank header file format. (VAB Header)
 * This is shared between PSX games, so at some point we could split the game-agnostic stuff into its own object, but keep this object for SC compatibility.
 * Resources: LIBSND.H (PsyQ)
 * Created by Kneesnap on 5/13/2024.
 */
@Getter
public class SCPlayStationVabSoundBankHeader extends SCSplitSoundBankHeader<SCPlayStationVabHeaderEntry, SCPlayStationVabSound> {
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
    private final VABProgram[] programs = new VABProgram[128];
    private VABTone[] tones;
    private int[] loadedSampleAddresses;
    @Setter private int savedBodyTotalSize;

    private static final int TONES_PER_PROGRAM = 16;
    public static final int TYPE_ID = 2;
    public static final int CHANNEL_COUNT = 1;
    public static final String PSX_SIGNATURE = "pBAV"; // VABp

    public SCPlayStationVabSoundBankHeader(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public boolean load(DataReader reader, SCSplitSoundBankBody<SCPlayStationVabHeaderEntry, SCPlayStationVabSound> other) {
        int headerStartIndex = reader.getIndex();
        reader.verifyString(PSX_SIGNATURE);
        this.fileVersion = reader.readInt();
        if (this.fileVersion != 7 && this.fileVersion != 6 && this.fileVersion != 5)
            throw new RuntimeException("Unknown Vab Header Version: " + this.fileVersion + "!");

        this.vabBankId = reader.readInt();
        int savedBodyHeaderSize = reader.readInt(); // Seems like it's the size of the VH + VB combined.
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
            this.programs[i] = new VABProgram(getGameInstance());
            this.programs[i].load(reader);
        }

        // Read Tones.
        int totalToneCount = (TONES_PER_PROGRAM * this.programCount);
        this.tones = new VABTone[totalToneCount];
        for (int i = 0; i < totalToneCount; i++) {
            this.tones[i] = new VABTone(getGameInstance());
            this.tones[i].load(reader);
        }

        // Read Samples.
        this.loadedSampleAddresses = new int[256];
        for (int i = 0; i < this.loadedSampleAddresses.length; i++)
            this.loadedSampleAddresses[i] = (reader.readUnsignedShortAsInt() << 3);

        this.savedBodyTotalSize = savedBodyHeaderSize - (reader.getIndex() - headerStartIndex); // Get it to be just the size of the body.
        return true;
    }

    @Override
    public void save(DataWriter writer, SCSplitSoundBankBody<SCPlayStationVabHeaderEntry, SCPlayStationVabSound> other) {
        writer.writeStringBytes(PSX_SIGNATURE);
        writer.writeInt(this.fileVersion);
        writer.writeInt(this.vabBankId);
        int bodyAndHeaderSizeAddress = writer.writeNullPointer();
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
        for (int i = 0; i < this.loadedSampleAddresses.length; i++)
            writer.writeUnsignedShort(this.loadedSampleAddresses[i] >> 3);

        // Write the final size.
        int headerSize = writer.getIndex();
        writer.writeIntAtPos(bodyAndHeaderSizeAddress, headerSize + this.savedBodyTotalSize);
    }

    public static class SCPlayStationVabHeaderEntry extends SCSplitSoundBankHeaderEntry {
        public SCPlayStationVabHeaderEntry(SCPlayStationVabSoundBankHeader header, int internalId) {
            super(header, internalId);
        }

        @Override
        public SCPlayStationVabSoundBankHeader getHeader() {
            return (SCPlayStationVabSoundBankHeader) super.getHeader();
        }

        @Override
        public void load(DataReader reader) {
            // Do nothing.
        }

        @Override
        public void save(DataWriter writer) {
            // Do nothing.
        }
    }

    @Getter
    @Setter // LIBSND.H: VagAtr "VAG Tone Headdings"
    public static class VABProgram extends SCSharedGameData {
        private short toneCount;
        private short volume;
        private short priority;
        private short mode;
        private short panning;
        private short reserved1;
        private short attribute;
        private int reserved2;
        private int reserved3;

        public VABProgram(SCGameInstance instance) {
            super(instance);
        }

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
    public static class VABTone extends SCSharedGameData {
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

        public VABTone(SCGameInstance instance) {
            super(instance);
        }

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