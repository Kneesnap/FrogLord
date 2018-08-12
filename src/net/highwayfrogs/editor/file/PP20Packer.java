package net.highwayfrogs.editor.file;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.PP20Unpacker.BitReader;

/**
 * Packs a byte array into PP20 compressed data.
 * I was unable to find a compression sub-routine (Even in C) which does this function.
 * So, this will/is being created from the documentation given below, and trying to reverse the unpacker.
 *
 * Work backwards.
 * Useful Links:
 *  - https://en.wikipedia.org/wiki/Lempel–Ziv–Welch
 *  - https://eblong.com/zarf/blorb/mod-spec.txt
 * Created by Kneesnap on 8/11/2018.
 */
public class PP20Packer {

    /**
     * Pack a byte array into PP20 compressed data.
     * @param data The data to compress.
     * @return packedData
     */
    public static byte[] packData(byte[] data) {
        return null; //TODO
    }
}