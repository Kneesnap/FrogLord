package net.highwayfrogs.editor.games.tgq;

import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Contains TGQUtils
 * Created by Kneesnap on 8/17/2019.
 */
public class TGQUtils {

    /**
     * Compress data with zlib compression.
     * @param data The data to compress.
     * @return compressedData
     */
    @SneakyThrows
    public static byte[] zlibCompress(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // returns the generated code... index
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        return outputStream.toByteArray();
    }

    /**
     * Decompress zlib data.
     * @param data The zlib data to decompress.
     * @return decompressedData
     */
    @SneakyThrows
    public static byte[] zlibDecompress(byte[] data, int resultSize) {
        byte[] result = new byte[resultSize];
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        inflater.inflate(result);
        inflater.end();
        return result;
    }
}
