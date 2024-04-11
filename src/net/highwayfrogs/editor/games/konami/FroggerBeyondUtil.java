package net.highwayfrogs.editor.games.konami;

import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.utils.Utils;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

/**
 * Created by Kneesnap on 10/10/2020.
 */
public class FroggerBeyondUtil {
    public enum FroggerBeyondPlatform {
        WINDOWS,
        GAMECUBE,
        XBOX,
        PS2
    }

    /**
     * Exports the music from a MCP file.
     * @param mcpFile   The file to export music from.
     * @param exportDir The directory to save music to.
     * @param platform  The platform the music is being exported from.
     */
    @SneakyThrows
    public static void exportMusic(File mcpFile, File exportDir, FroggerBeyondPlatform platform) {
        if (platform == FroggerBeyondPlatform.GAMECUBE || platform == FroggerBeyondPlatform.PS2 || platform == null)
            throw new UnsupportedOperationException("Platform not supported yet: '" + platform + "'.");

        DataReader reader = new DataReader(new FileSource(mcpFile));
        Utils.makeDirectory(exportDir);

        int count = 0;
        boolean isPC = (platform == FroggerBeyondPlatform.WINDOWS);
        boolean isXbox = (platform == FroggerBeyondPlatform.XBOX);
        AudioFormat pcFormat = new AudioFormat(22050, 16, 2, true, false);
        while (reader.hasMore()) {
            int startAt = reader.readInt();
            int size = reader.readInt();

            reader.jumpTemp(startAt);
            if (size > reader.getRemaining()) {
                reader.jumpReturn();
                break;
            }

            byte[] byteData = reader.readBytes(size);
            reader.jumpReturn();

            if (isXbox) {
                Files.write(new File(exportDir, (count++) + ".wavm").toPath(), byteData);
            } else if (isPC) {
                Clip clip = AudioSystem.getClip();
                clip.open(pcFormat, byteData, 0, byteData.length);

                AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(byteData), clip.getFormat(), clip.getFrameLength());
                AudioSystem.write(inputStream, Type.WAVE, new File(exportDir, (count++) + ".wav"));
            }
        }
    }

    /**
     * Exports the voice files from a voice file.
     * @param mcpFile   The file to export voice sounds from.
     * @param exportDir The directory to export to.
     * @param platform  The platform the audio is being exported from.
     */
    @SneakyThrows
    public static void exportVoices(File mcpFile, File exportDir, FroggerBeyondPlatform platform) {
        if (platform == FroggerBeyondPlatform.GAMECUBE || platform == FroggerBeyondPlatform.PS2 || platform == null)
            throw new UnsupportedOperationException("Platform not supported yet: '" + platform + "'.");

        DataReader reader = new DataReader(new FileSource(mcpFile));
        reader.setIndex(0x1000);
        Utils.makeDirectory(exportDir);

        int count = 0;
        boolean isPC = (platform == FroggerBeyondPlatform.WINDOWS);
        boolean isXbox = (platform == FroggerBeyondPlatform.XBOX);
        AudioFormat pcFormat = new AudioFormat(22050, 16, 1, true, false);

        while (reader.hasMore()) {
            reader.jumpTemp(reader.getIndex());
            int lastIndex = reader.getIndex();
            while (reader.hasMore()) {
                byte[] bytes = reader.readBytes(Math.min(0x800, reader.getRemaining()));
                if (!reader.hasMore()) {
                    lastIndex = reader.getIndex();
                    break;
                }

                int nullCount = 0;
                for (int i = bytes.length - 1; i >= 0; i--, nullCount++)
                    if (bytes[i] != Constants.NULL_BYTE)
                        break;

                if (nullCount >= 8) {
                    System.out.println("NEXT! " + (count + 1) + " = " + Utils.toHexString(reader.getIndex()) + " - " + nullCount);
                    lastIndex = reader.getIndex();
                    break;
                }

                System.out.println(count + " = " + Utils.toHexString(reader.getIndex()) + " - " + nullCount);
            }
            reader.jumpReturn();

            byte[] byteData = reader.readBytes(lastIndex - reader.getIndex());
            if (byteData.length <= 2048)
                continue;

            /*if (isGameCube) {
                Files.write(new File(exportDir, (count++) + ".dsp").toPath(), byteData);
            } else*/
            if (isXbox || isPC) {
                Clip clip = AudioSystem.getClip();
                clip.open(pcFormat, byteData, 0, byteData.length);
                AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(byteData), clip.getFormat(), clip.getFrameLength());
                AudioSystem.write(inputStream, Type.WAVE, new File(exportDir, (count++) + ".wav"));
            }
        }
    }
}