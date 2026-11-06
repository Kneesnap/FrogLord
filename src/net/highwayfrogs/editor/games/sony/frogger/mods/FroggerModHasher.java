package net.highwayfrogs.editor.games.sony.frogger.mods;

import net.highwayfrogs.editor.file.patch.PatchWriteRecorder.PatchWriteRange;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

/**
 * Builds the SHA-1 hashes and keys that the base fingerprint and the exporter must produce identically.
 * Created by jimbolina on 11/6/2026.
 */
class FroggerModHasher {
    private final MessageDigest digest;

    /**
     * Creates a hasher with an empty SHA-1 digest.
     */
    FroggerModHasher() {
        try {
            this.digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 is not available.", ex);
        }
    }

    /**
     * Adds an integer as four big-endian bytes.
     * @param value the value to add
     */
    void putInt(int value) {
        this.digest.update((byte) (value >>> 24));
        this.digest.update((byte) (value >>> 16));
        this.digest.update((byte) (value >>> 8));
        this.digest.update((byte) value);
    }

    /**
     * Adds a boolean as one byte.
     * @param value the value to add
     */
    void putBoolean(boolean value) {
        this.digest.update(value ? (byte) 1 : (byte) 0);
    }

    /**
     * Adds a string as its UTF-8 length and bytes, or the length -1 for null.
     * @param value the value to add
     */
    void putString(String value) {
        byte[] bytes = value != null ? value.getBytes(StandardCharsets.UTF_8) : new byte[0];
        putInt(value != null ? bytes.length : -1);
        this.digest.update(bytes);
    }

    /**
     * Adds a byte array as its length and bytes.
     * @param value the value to add
     */
    void putBytes(byte[] value) {
        putInt(value.length);
        this.digest.update(value);
    }

    /**
     * Finishes the digest.
     * @return the hash as 40 lowercase hexadecimal digits
     */
    String finish() {
        byte[] hash = this.digest.digest();
        StringBuilder builder = new StringBuilder(hash.length * 2);
        for (byte b : hash)
            builder.append(String.format("%02x", b & 0xFF));
        return builder.toString();
    }

    /**
     * Hashes raw bytes.
     * @param data the bytes to hash
     * @return the hash as 40 lowercase hexadecimal digits
     */
    static String sha1(byte[] data) {
        FroggerModHasher hasher = new FroggerModHasher();
        hasher.digest.update(data);
        return hasher.finish();
    }

    /**
     * Makes a key unique among existing keys by appending the suffix, then a counter.
     * @param existing the keys already in use
     * @param key the preferred key
     * @param suffix the suffix to append when the key is taken
     * @return the unique key
     */
    static String makeUniqueKey(Set<String> existing, String key, String suffix) {
        if (!existing.contains(key))
            return key;

        String newKey = key + suffix;
        int counter = 2;
        while (existing.contains(newKey))
            newKey = key + suffix + "." + counter++;
        return newKey;
    }

    /**
     * Gets the name a level-select row field records for an image pointer.
     * @param resolver the name resolver of the game
     * @param pointer the image pointer
     * @return the image name, or the pointer as hexadecimal when it names no image
     */
    static String pointerName(FroggerModNameResolver resolver, long pointer) {
        String name = pointer != 0 ? resolver.imageNameForPointer(pointer) : null;
        return name != null ? name : String.format("0x%08X", pointer);
    }

    /**
     * Formats an executable range as it is written in fingerprints and manifests.
     * @param range the range to format
     * @return the range as "0xADDRESS+LENGTH"
     */
    static String formatRange(PatchWriteRange range) {
        return String.format("0x%08X+%d", range.getAddress(), range.getLength());
    }

    /**
     * Marks the bytes of a range inside a mask, clipped to the mask.
     * @param mask the mask, one entry per executable byte
     * @param range the range to mark
     */
    static void markRange(boolean[] mask, PatchWriteRange range) {
        int start = Math.max(0, range.getAddress());
        int end = Math.min(mask.length, range.getEndAddress());
        for (int i = start; i < end; i++)
            mask[i] = true;
    }
}
