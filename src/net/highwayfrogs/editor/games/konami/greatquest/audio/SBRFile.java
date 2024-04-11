package net.highwayfrogs.editor.games.konami.greatquest.audio;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.psx.VAGUtil;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.kcPlatform;
import net.highwayfrogs.editor.utils.Utils;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Represents .SBR sound bank files.
 * Created by Kneesnap on 8/17/2023.
 */
@Getter
public class SBRFile extends GameObject {
    private final kcPlatform platform;
    private int sfxBankId;
    private final List<SfxEntry> soundEffects = new ArrayList<>();
    private final List<SfxWave> waves = new ArrayList<>();

    private static final int SIGNATURE = 0x42584653; // 'SFXB'
    private static final int SUPPORTED_VERSION = 0x100;

    public SBRFile(kcPlatform platform) {
        this.platform = platform;
    }

    @Override
    public void load(DataReader reader) {
        int signature = reader.readInt();
        if (signature != SIGNATURE)
            throw new RuntimeException("Expected signature '" + Utils.to0PrefixedHexString(SIGNATURE) + "' for SFX Bank but got '" + Utils.to0PrefixedHexString(signature) + "' instead.");

        int version = reader.readInt();
        if (version != SUPPORTED_VERSION)
            throw new RuntimeException("Expected SFX Bank Version " + SUPPORTED_VERSION + ", but got version " + version + " instead.");

        this.sfxBankId = reader.readInt();
        int sfxCount = reader.readInt();
        int sfxAttrOffset = reader.readInt();
        @SuppressWarnings("unused") int sfxAttrSize = reader.readInt();
        int numWaves = reader.readInt();
        int waveAttrOffset = reader.readInt();
        int waveAttrSize = reader.readInt();
        int waveDataOffset = reader.readInt();
        @SuppressWarnings("unused") int waveDataSize = reader.readInt();

        // Read sfx attributes.
        this.soundEffects.clear();
        for (int i = 0; i < sfxCount; i++) {
            int sfxId = reader.readInt();
            if (sfxId == 0xCDCDCDCD)
                break; // Uninitialized memory.... This seems like the file has invalid data to be honest... Perhaps since the data isn't actually parsed until it gets used, since this bad data is never used it never gets parsed by the game, and thus never has a chance to break stuff.

            int currentSfxAttrOffset = reader.readInt();

            reader.jumpTemp(sfxAttrOffset + currentSfxAttrOffset);
            SfxAttributes attributes = SfxAttributes.readAttributes(reader);
            reader.jumpReturn();

            this.soundEffects.add(new SfxEntry(sfxId, attributes));
        }

        // Read wav attributes.
        this.waves.clear();
        reader.setIndex(waveAttrOffset);
        for (int i = 0; i < numWaves; i++) {
            SfxWave wave = createNewAttributes();
            wave.load(reader, waveDataOffset);
            this.waves.add(wave);
        }

        int actualWaveAttrSize = reader.getIndex() - waveAttrOffset;
        if (actualWaveAttrSize != waveAttrSize)
            throw new RuntimeException("Read " + actualWaveAttrSize + " bytes of wave attribute data, but the file said there were supposed to be " + waveAttrSize + " bytes.");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(SIGNATURE);
        writer.writeInt(SUPPORTED_VERSION);
        writer.writeInt(this.sfxBankId);
        writer.writeInt(this.soundEffects.size());
        int sfxAttrOffset = writer.writeNullPointer();
        int sfxAttrSize = writer.writeNullPointer();
        writer.writeInt(this.waves.size());
        int waveAttrOffset = writer.writeNullPointer();
        int waveAttrSize = writer.writeNullPointer();
        int waveDataOffset = writer.writeNullPointer();
        int waveDataSize = writer.writeNullPointer();

        // Write sfx attributes.
        int[] sfxAttrOffsets = new int[this.soundEffects.size()];
        for (int i = 0; i < this.soundEffects.size(); i++) {
            writer.writeInt(this.soundEffects.get(i).getSfxId());
            sfxAttrOffsets[i] = writer.writeNullPointer();
        }

        int sfxAttributeDataStart = writer.getIndex();
        writer.writeAddressTo(sfxAttrOffset);
        for (int i = 0; i < this.soundEffects.size(); i++) {
            writer.writeAddressAt(sfxAttrOffsets[i], writer.getIndex() - sfxAttributeDataStart);
            this.soundEffects.get(i).getAttributes().save(writer);
        }
        writer.writeAddressAt(sfxAttrSize, writer.getIndex() - sfxAttributeDataStart);

        // Write wav.
        int writtenWaveDataBytes = 0;
        writer.writeAddressTo(waveAttrOffset);
        for (int i = 0; i < this.waves.size(); i++) {
            SfxWave wave = this.waves.get(i);
            wave.save(writer, writtenWaveDataBytes);
            writtenWaveDataBytes += wave.getWaveSize();
        }

        writer.writeAddressAt(waveAttrSize, writtenWaveDataBytes);

        // Write wav data.
        int waveDataStartsAt = writer.getIndex();
        writer.writeAddressTo(waveDataOffset);
        for (int i = 0; i < this.waves.size(); i++)
            this.waves.get(i).saveADPCM(writer);

        writer.writeAddressAt(waveDataSize, writer.getIndex() - waveDataStartsAt);
    }

    /**
     * Creates a new sfx wave object.
     */
    public SfxWave createNewAttributes() {
        switch (this.platform) {
            case PC:
                return new SfxWavePC();
            case PS2:
                return new SfxWavePS2();
            default:
                throw new RuntimeException("Cannot create new wave attributes for the " + this.platform + " platform.");
        }
    }

    /**
     * Plays the audio SFX blocking. Primary purpose is for debugging.
     * @param wave The sfx to play.
     * @throws LineUnavailableException Thrown if the clip cannot be created.
     * @throws InterruptedException     Thrown if waiting for the audio clip to finish is interrupted.
     */
    @SuppressWarnings("unused")
    public static void playSfxBlocking(SfxWave wave) throws LineUnavailableException, InterruptedException {
        Clip clip = wave.getClip();
        clip.setMicrosecondPosition(0);
        CountDownLatch latch = new CountDownLatch(1);
        clip.addLineListener(listener -> {
            if (listener.getType() == javax.sound.sampled.LineEvent.Type.STOP)
                latch.countDown();
        });
        clip.start();
        latch.await();
        clip.close();
    }

    public interface SfxWave {
        /**
         * Loads data from the reader.
         * @param reader The reader to read data from.
         */
        void load(DataReader reader, int ADPCMStartOffset);

        /**
         * Saves data to the writer.
         * @param writer        The writer to save data to.
         * @param wavDataOffset The offset which audio data is written at.
         */
        void save(DataWriter writer, int wavDataOffset);

        /**
         * Writes the ADPCM data to the writer.
         * @param writer The writer to write the data to.
         */
        void saveADPCM(DataWriter writer);

        /**
         * Gets an audio clip with the audio data playable.
         * @throws LineUnavailableException Thrown if the clip cannot be created.
         */
        Clip getClip() throws LineUnavailableException;

        /**
         * Save as a .wav file.
         * @param file The file to save to.
         * @throws IOException If it was impossible to write the file.
         */
        void exportToWav(File file) throws IOException;

        /**
         * Returns the Wave ID used by this sound effect.
         */
        @SuppressWarnings("unused")
        int getWaveID();

        /**
         * Gets the bit flags applied for this sound effect.
         */
        int getFlags();

        /**
         * The amount of bytes the wave data takes up when saved to the file.
         * This is not always the same as the ADPCM data size.
         */
        int getWaveSize();
    }

    @Getter
    public static class SfxWavePS2 implements SfxWave {
        private int waveID;
        private int flags;
        private int sampleRate;
        private byte[] ADPCMData;

        @Override
        public void load(DataReader reader, int wavDataOffset) {
            this.waveID = reader.readInt();
            this.flags = reader.readInt();
            this.sampleRate = reader.readInt();
            int waveDataOffset = reader.readInt();
            int waveDataSize = reader.readInt();

            // The data here follows the WAVEFORMATEX struct as defined at
            // https://learn.microsoft.com/en-us/previous-versions/ms713497(v=vs.85)

            reader.jumpTemp(wavDataOffset + waveDataOffset);
            this.ADPCMData = reader.readBytes(waveDataSize);
            reader.jumpReturn();
        }

        @Override
        public void save(DataWriter writer, int wavDataOffset) {
            writer.writeInt(this.waveID);
            writer.writeInt(this.flags);
            writer.writeInt(this.sampleRate);
            writer.writeInt(wavDataOffset);
            writer.writeInt(getWaveSize());
        }

        @Override
        public void saveADPCM(DataWriter writer) {
            // TODO: We have an audio pop problem. Perhaps a few bytes need to be removed.
            if (this.ADPCMData != null)
                writer.writeBytes(this.ADPCMData);
        }

        @Override
        public int getWaveSize() {
            return this.ADPCMData != null ? this.ADPCMData.length : 0;
        }

        @Override
        public Clip getClip() throws LineUnavailableException {
            Clip clip = AudioSystem.getClip();
            AudioFormat format = new AudioFormat(getSampleRate(), 16, 1, true, false);

            byte[] convertedAudioData = VAGUtil.rawVagToWav(this.ADPCMData);
            clip.open(format, convertedAudioData, 0, convertedAudioData.length);
            return clip;
        }

        @Override
        public void exportToWav(File file) throws IOException {
            Files.write(file.toPath(), VAGUtil.rawVagToWav(this.ADPCMData, getSampleRate()));
        }
    }

    @Getter
    public static class SfxWavePC implements SfxWave {
        private int waveID;
        private int flags;
        private int unknownValue;
        private byte[] waveFormatEx; // Modelled by the 'WAVEFORMATEX' struct. https://learn.microsoft.com/en-us/windows/win32/api/mmreg/ns-mmreg-waveformatex
        private byte[] ADPCMData;

        private static final String RIFF_SIGNATURE = "RIFF";
        private static final String WAV_SIGNATURE = "WAVE";
        private static final String DATA_CHUNK_SIGNATURE = "data";

        @Override
        public void load(DataReader reader, int wavDataOffset) {
            this.waveID = reader.readInt();
            this.flags = reader.readInt();
            this.unknownValue = reader.readInt();
            int waveDataStartOffset = reader.readInt();
            int waveDataSize = reader.readInt();

            reader.jumpTemp(wavDataOffset + waveDataStartOffset);

            // Read WAVEFORMATEX.
            int waveFormatExSize = 18; // Default Size of 'WAVEFORMATEX'.
            reader.jumpTemp(reader.getIndex() + waveFormatExSize - Constants.SHORT_SIZE);
            waveFormatExSize += reader.readShort();
            reader.jumpReturn();

            this.waveFormatEx = reader.readBytes(waveFormatExSize);

            // Read ADPCM data.
            this.ADPCMData = reader.readBytes(waveDataSize - waveFormatExSize);
            reader.jumpReturn();
        }

        @Override
        public void save(DataWriter writer, int wavDataOffset) {
            writer.writeInt(this.waveID);
            writer.writeInt(this.flags);
            writer.writeInt(this.unknownValue);
            writer.writeInt(wavDataOffset);
            writer.writeInt(getWaveSize());
        }

        @Override
        public void saveADPCM(DataWriter writer) {
            if (this.waveFormatEx != null)
                writer.writeBytes(this.waveFormatEx);
            if (this.ADPCMData != null)
                writer.writeBytes(this.ADPCMData);
        }

        @Override
        public Clip getClip() throws LineUnavailableException {
            Clip clip = AudioSystem.getClip();

            byte[] wavFileContents = toWavFileBytes();

            AudioInputStream audioStream;
            try {
                audioStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavFileContents));
                clip.open(audioStream);
            } catch (UnsupportedAudioFileException ex) {
                throw new RuntimeException("The file was in an unsupported audio format.", ex);
            } catch (IOException ex) {
                throw new RuntimeException("An IOException happened somehow?", ex);
            }

            return clip;
        }

        /**
         * Gets the sound as a .wav file.
         */
        public byte[] toWavFileBytes() {
            ArrayReceiver receiver = new ArrayReceiver();
            DataWriter writer = new DataWriter(receiver);

            writer.writeStringBytes(RIFF_SIGNATURE);
            int fileSizeAddress = writer.writeNullPointer();
            writer.writeStringBytes(WAV_SIGNATURE);

            writer.writeStringBytes("fmt ");
            writer.writeInt(this.waveFormatEx.length); // Write chunk 1 size.
            writer.writeBytes(this.waveFormatEx);
            writer.writeStringBytes(DATA_CHUNK_SIGNATURE);
            writer.writeInt(this.ADPCMData.length);
            writer.writeBytes(this.ADPCMData); // TODO: We have an audio pop problem. Perhaps a few bytes need to be removed.
            writer.writeAddressAt(fileSizeAddress, writer.getIndex() - (fileSizeAddress + Constants.INTEGER_SIZE));

            writer.closeReceiver();
            return receiver.toArray();

        }

        @Override
        public void exportToWav(File file) throws IOException {
            Files.write(file.toPath(), toWavFileBytes());
        }

        @Override
        public int getWaveSize() {
            return (this.waveFormatEx != null ? this.waveFormatEx.length : 0)
                    + (this.ADPCMData != null ? this.ADPCMData.length : 0);
        }
    }

    @Getter
    public static abstract class SfxAttributes extends GameObject {
        private final byte type;
        private short flags;
        private short priority;

        protected SfxAttributes(byte typeOpcode) {
            this.type = typeOpcode;
        }

        @Override
        public void load(DataReader reader) {
            this.flags = reader.readUnsignedByteAsShort();
            this.priority = reader.readUnsignedByteAsShort();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeByte(this.type);
            writer.writeUnsignedByte(this.flags);
            writer.writeUnsignedByte(this.priority);
        }

        /**
         * Read sound effect attributes from the reader.
         * @param reader The reader to read the data from.
         * @return attributes
         */
        public static SfxAttributes readAttributes(DataReader reader) {
            byte type = reader.readByte();
            SfxAttributes attributes;
            if (type == SfxEntrySimpleAttributes.TYPE_OPCODE) {
                attributes = new SfxEntrySimpleAttributes();
            } else if (type == SfxEntryStreamAttributes.TYPE_OPCODE) {
                attributes = new SfxEntryStreamAttributes();
            } else {
                throw new RuntimeException("Don't know what SfxAttributes type " + type + " is.");
            }

            attributes.load(reader);
            return attributes;
        }

    }

    /**
     * Represents attributes for a simple sound effect.
     */
    @Getter
    public static class SfxEntrySimpleAttributes extends SfxAttributes {
        private short instanceLimit;
        private short volume;
        private short pan;
        private int pitch;
        private long wave;

        private static final byte TYPE_OPCODE = 0;

        protected SfxEntrySimpleAttributes() {
            super(TYPE_OPCODE);
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.instanceLimit = reader.readUnsignedByteAsShort();
            this.volume = reader.readUnsignedByteAsShort();
            this.pan = reader.readUnsignedByteAsShort();
            this.pitch = reader.readUnsignedShortAsInt();
            this.wave = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedByte(this.instanceLimit);
            writer.writeUnsignedByte(this.volume);
            writer.writeUnsignedByte(this.pan);
            writer.writeUnsignedShort(this.pitch);
            writer.writeUnsignedInt(this.wave);
        }
    }

    /**
     * Represents attributes for a stream sound effect.
     */
    @Getter
    public static class SfxEntryStreamAttributes extends SfxAttributes {
        private short volume;

        private static final byte TYPE_OPCODE = 1;

        protected SfxEntryStreamAttributes() {
            super(TYPE_OPCODE);
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.volume = reader.readUnsignedByteAsShort();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedByte(this.volume);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class SfxEntry {
        private final int sfxId;
        private final SfxAttributes attributes;
    }
}