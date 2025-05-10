package net.highwayfrogs.editor.games.psx.sound;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.ArrayReceiver;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Utilities for the VAG file format.
 * References:
 * - https://github.com/simias/psxsdk/blob/master/tools/vag2wav.c
 * - https://github.com/simias/psxsdk/blob/master/tools/wav2vag.c
 * - https://github.com/losnoco/vgmstream/blob/2125433cd5fc362e679373cce7777110f7b5fba0/src/coding/psx_decoder.c
 * Created by Kneesnap on 11/30/2019.
 */
public class VAGUtil {
    private static final String RIFF_SIGNATURE = "RIFF";
    private static final String WAV_SIGNATURE = "WAVE";
    private static final String DATA_CHUNK_SIGNATURE = "data";
    private static final int BUFFER_SIZE = 128 * 8;
    private static final double[][] TABLE = new double[][]{{0.0, 0.0},
            {60D / 64D, 0D},
            {115D / 64D, -52D / 64D},
            {98D / 64D, -55D / 64D},
            {122D / 64D, -60D / 64D}};

    /**
     * Converts a playstation VAG sound to raw PCM.
     * @param vagAudioData The vag file to convert.
     * @return rawPcmData
     */
    public static byte[] rawVagToWav(byte[] vagAudioData) {
        return rawVagToWav(vagAudioData, -1);
    }

    /**
     * Converts a playstation VAG sound to .WAV
     * @param vagAudioData The vag file to convert.
     * @param sampleRate   The sample rate of the audio. If value is <= 0, raw PCM will be returned, without the .wav header.
     * @return wavBytes
     */
    public static byte[] rawVagToWav(byte[] vagAudioData, int sampleRate) {
        boolean includeWavHeader = (sampleRate > 0);
        double[] samples = new double[28];
        double s1 = 0;
        double s2 = 0;

        DataReader reader = new DataReader(new ArraySource(vagAudioData));

        ArrayReceiver dataReceiver = new ArrayReceiver();
        DataWriter writer = new DataWriter(dataReceiver);

        // Write wav.
        int fileSizeAddress = -1;
        int subChunk2SizeAddress = -1;
        if (includeWavHeader) {
            writer.writeStringBytes(RIFF_SIGNATURE);
            fileSizeAddress = writer.writeNullPointer();
            writer.writeStringBytes(WAV_SIGNATURE);
            writer.writeStringBytes("fmt ");
            writer.writeInt(16); // Write chunk 1 size.
            writer.writeShort((short) 1); // Writes audio format. 1 = PCM.
            writer.writeShort((short) 1); // Number of channels.
            writer.writeInt(sampleRate);
            writer.writeInt(sampleRate * 2); // (SampleRate * NumChannels * BitsPerSample/8). That would be 44100*1*(16/8), thus 88200.
            writer.writeShort((short) 2); // Block align: (NumChannels * BitsPerSample/8), thus 2
            writer.writeShort((short) 16); // 16 bits per sample.

            writer.writeStringBytes(DATA_CHUNK_SIGNATURE);
            subChunk2SizeAddress = writer.writeNullPointer();
        }

        while (reader.hasMore()) {
            byte predictNr = reader.readByte();
            int shiftFactor = (predictNr & 0b1111);
            predictNr >>= 4;
            byte flags = reader.readByte();

            if (flags == 7)
                break; // End.

            for (int i = 0; i < 28; i += 2) {
                byte d = reader.readByte();
                int s = (d & 0xF) << 12;
                if ((s & 0x8000) == 0x8000)
                    s |= 0xFFFF0000;
                samples[i] = (s >> shiftFactor);
                s = (d & 0xF0) << 8;
                if ((s & 0x8000) == 0x8000)
                    s |= 0xFFFF0000;
                samples[i + 1] = (s >> shiftFactor);
            }

            for (int i = 0; i < 28; i++) {
                samples[i] += (s1 * TABLE[predictNr][0]) + (s2 * TABLE[predictNr][1]);
                s2 = s1;
                s1 = samples[i];
                int d = (int) (samples[i] + .5);
                writer.writeByte((byte) (d & 0xFF));
                writer.writeByte((byte) ((d >> 8) & 0xFF));
            }
        }

        // Write sizes.
        if (includeWavHeader) {
            writer.writeIntAtPos(fileSizeAddress, writer.getIndex() - (fileSizeAddress + Constants.INTEGER_SIZE)); // Write file size.
            writer.writeIntAtPos(subChunk2SizeAddress, writer.getIndex() - (subChunk2SizeAddress + Constants.INTEGER_SIZE)); // Write chunk size.
        }

        writer.closeReceiver();
        return dataReceiver.toArray();
    }

    /**
     * Converts a .wav file into VAG audio data.
     * @param wavBytes The file data to convert.
     * @return vagAudio
     */
    public static byte[] wavToVag(byte[] wavBytes) {
        double[] samples = new double[28];
        short[] fourBit = new short[28];
        short[] wave = new short[BUFFER_SIZE + 28];
        double s1 = 0;
        double s2 = 0;
        double oldS1 = 0;
        double oldS2 = 0;

        DataReader reader = new DataReader(new ArraySource(wavBytes));

        ArrayReceiver dataReceiver = new ArrayReceiver();
        DataWriter writer = new DataWriter(dataReceiver);

        // Read wav file.
        reader.verifyString(RIFF_SIGNATURE);
        reader.skipInt(); // File size address.
        reader.verifyString(WAV_SIGNATURE);
        reader.verifyString("fmt "); // Well, I'm actually not 100% sure this is always present, I don't know how loose the .wav format is. For now it doesn't matter, but if it causes issues we'll change it.
        int chunkData = reader.readInt() - 16; // How much data until we reach the pcm data.
        int audioFormat = reader.readShort(); // 1 = PCM.
        int channelCount = reader.readShort();
        reader.skipInt(); // Sample Rate.
        reader.skipInt();
        reader.skipShort();
        int bitsPerSample = reader.readShort();
        reader.skipBytes(chunkData);

        if (audioFormat != 1) // Unsure in what circumstances this won't be 1.
            throw new RuntimeException("WAV did not have PCM encoded audio. (Mode: " + audioFormat + ")");

        if (bitsPerSample != 16) // Frogger limitation, I think.
            throw new RuntimeException("The supplied WAV file did not use 16 bits per sample. (Used: " + bitsPerSample + ")");

        if (channelCount != 1) // VAG Limitation.
            throw new RuntimeException("VAG Audio only supports one channel, but the supplied audio had " + channelCount + ".");


        reader.verifyString(DATA_CHUNK_SIGNATURE);
        int dataChunkSize = reader.readInt();

        int sampleLen = (dataChunkSize / (bitsPerSample / Constants.BITS_PER_BYTE));
        int flags = 0;

        int predictNr = 0;
        int shiftFactor = 0;
        while (sampleLen > 0) {
            int size = Math.min(BUFFER_SIZE, sampleLen);
            for (int i = 0; i < size; i++)
                wave[i] = reader.readShort();

            int i = size / 28;
            if ((size % 28) != 0) {
                for (int j = size % 28; j < 28; j++)
                    wave[(28 * i) + j] = 0;
                i++;
            }

            for (int j = 0; j < i; j++) { // Pack 28 samples.
                PredictResult pResult = findPredict(wave, j * 28, samples, predictNr, oldS1, oldS2);
                oldS1 = pResult.getS1();
                oldS2 = pResult.getS2();
                predictNr = pResult.getPredictNr();
                shiftFactor = pResult.getShiftFactor();

                PackResult result = pack(samples, fourBit, predictNr, shiftFactor, s1, s2);
                s1 = result.getS1();
                s2 = result.getS2();

                writer.writeByte((byte) ((predictNr << 4) | shiftFactor));
                writer.writeByte((byte) flags);
                for (int k = 0; k < 28; k += 2)
                    writer.writeByte((byte) (((fourBit[k + 1] >> 8) & 0xF0) | ((fourBit[k] >> 12) & 0xF)));

                sampleLen -= 28;
                if (sampleLen < 28)
                    flags = 1;
            }
        }

        writer.writeByte((byte) ((predictNr << 4) | shiftFactor));
        writer.writeByte((byte) 7); // End flag.
        writer.writeNull(14);
        writer.closeReceiver();
        return dataReceiver.toArray();
    }

    private static PredictResult findPredict(short[] useSamples, int startIndex, double[] samples, int predictNr, double oldS1, double oldS2) {
        double[][] buffer = new double[28][5];
        double min = 10000000000D;
        double s1 = oldS1;
        double s2 = oldS2;

        for (int i = 0; i < 5; i++) {
            double max = 0;
            s1 = oldS1;
            s2 = oldS2;
            for (int j = 0; j < 28; j++) {
                double s0 = Math.min(30720D, Math.max(-30719D, useSamples[startIndex + j])); // s[t-0]
                double ds = s0 + s1 * -TABLE[i][0] + s2 * -TABLE[i][1];
                buffer[j][i] = ds;
                if (fabs(ds) > max)
                    max = fabs(ds);

                s2 = s1; // new s[t-2]
                s1 = s0; // new s[t-1]
            }

            if (max < min) {
                min = max;
                predictNr = i;
            }
            if (min <= 7) {
                predictNr = 0;
                break;
            }
        }

        for (int i = 0; i < 28; i++)
            samples[i] = buffer[i][predictNr];

        int min2 = (int) min;
        int shiftMask = 0x4000;

        int shiftFactor;
        for (shiftFactor = 0; shiftFactor < 12; shiftFactor++) {
            if ((shiftMask & (min2 + (shiftMask >> 3))) != 0)
                break;
            shiftMask = shiftMask >> 1;
        }

        return new PredictResult(predictNr, shiftFactor, s1, s2);
    }

    @Getter
    @AllArgsConstructor
    private static class PredictResult {
        private int predictNr;
        private int shiftFactor;
        private double s1;
        private double s2;
    }

    private static PackResult pack(double[] samples, short[] fourBit, int predictNr, int shiftFactor, double s1, double s2) {
        for (int i = 0; i < 28; i++) {
            double s0 = samples[i] + s1 * -TABLE[predictNr][0] + s2 * -TABLE[predictNr][1];
            double ds = s0 * (double) (1 << shiftFactor);
            int di = Math.max(-32768, Math.min(32767, (((int) ds + 0x800) & 0xfffff000)));

            fourBit[i] = (short) di;

            di >>= shiftFactor;
            s2 = s1;
            s1 = (double) di - s0;
        }

        return new PackResult(s1, s2);
    }

    @Getter
    @AllArgsConstructor
    private static class PackResult {
        private double s1;
        private double s2;
    }

    private static double fabs(double val) {
        return (val >= 0 ? val : -val);
    }
}