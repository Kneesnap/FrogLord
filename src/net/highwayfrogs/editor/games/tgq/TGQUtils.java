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

    /**
     * Creates a file ID from a file path.
     * Example File Path: \\Netapp1\PD\Frogger1\PC\KatWorking\GameSource\Level01RollingRapids\Props\WalTreGL\WALTREGL.VTX
     * Example File ID: S01sPWalTreGL\WALTREGLX
     * The real game does not have any kind of error checking on file paths, for instance reading parts of the name which are not part of the string.
     * We may want to implement exceptions for this at a later date, even though the original game does not.
     * It also could stand to use some cleanup, but I want to keep this functionally identical for now.
     * Notes: The PC version is different from the PS2 version. This function was reversed from PS2, and modified for PC.
     * TODO: In the future, allow an option for both PS2 and PC mode.
     * @param filePath The path to generate a file ID from.
     * @return fileId
     */
    public static String getFileIdFromPath(String filePath) { // Reversed from the 'Hash' function, in the global namespace. (To make it distinct from the other hash method)
        int gameIndex = filePath.indexOf("\\Game");
        if (gameIndex == -1)
            gameIndex = filePath.indexOf("\\game");
        if (gameIndex == -1)
            gameIndex = filePath.indexOf("\\GAME");

        StringBuilder fileId = new StringBuilder();
        if (gameIndex != -1) { // If it was found.
            String cutPath = filePath.substring(gameIndex + 6); // Cut out \\game and everything before it.
            fileId.append(filePath.charAt(gameIndex + 5));
            int levelIndex = cutPath.indexOf("\\Level");
            if (levelIndex == -1)
                levelIndex = cutPath.indexOf("\\level");
            if (levelIndex == -1)
                levelIndex = cutPath.indexOf("\\LEVEL");

            if (levelIndex != -1) {
                fileId.append(cutPath.charAt(levelIndex + 6));
                fileId.append(cutPath.charAt(levelIndex + 7));

                String cutSepString = cutPath.substring(levelIndex + 8);
                int sepIndex = cutSepString.indexOf('\\');
                if (sepIndex != -1) {
                    fileId.append(cutPath.charAt(levelIndex + 7 + sepIndex)); // Yes, this can result in getting data that is not in cutSepString, which is why it's getting it from cutPath.
                    fileId.append(cutSepString.charAt(sepIndex + 1));

                    String remaining = cutSepString.substring(sepIndex + 2);
                    int nextDirIndex = remaining.indexOf('\\');
                    if (nextDirIndex != -1) {
                        String fileName = remaining.substring(nextDirIndex + 1);
                        int extensionIndex = fileName.indexOf('.');
                        if (extensionIndex != -1) {
                            fileId.append(fileName, 0, extensionIndex);
                            fileId.append(fileName, fileName.length() - 1, fileName.length());
                            return fileId.toString();
                        }
                        return fileId.append(fileName).toString();
                    }
                    //return fileId.append(remaining).toString(); // PSX Mode.
                    return remaining;
                }
                return fileId.append(cutSepString).toString();
            }
            return fileId.append(cutPath).toString();
        }

        return fileId.append(filePath).toString();
    }

    /**
     * Calculates the checksum / hash of a string.
     * This value is directly what is used in the Table of contents chunk.
     * @param str        The hash to use.
     * @param ignoreCase Whether or not case should be considered when hashing. Usually true.
     * @return hash
     */
    public static int hash(String str, boolean ignoreCase) { // Reverse engineered the "Hash" function, in the kcHash (Hash table) namespace.
        if (str == null || str.isEmpty())
            return 0;

        int hash = str.length();
        for (int i = 0; i < str.length(); i++) {
            char tempChar = str.charAt(i);
            if (ignoreCase && (tempChar >= 'A') && (tempChar <= 'Z')) // If the letter is upper-case.
                tempChar = Character.toLowerCase(tempChar);

            //System.out.println((ignoreCase ? "Ignore Case " : "") + "Step #" + i + ": " + hash + "/" + Utils.toHexString(hash));
            hash = ((hash << 4) ^ ((hash >> 0x1c) & 0x0F)) ^ (int) tempChar; // I'm pretty sure it's closer to the >>> behavior, but this works properly right now.
        }

        //System.out.println("Result: " + Utils.toHexString(hash));
        return hash;
    }
}
