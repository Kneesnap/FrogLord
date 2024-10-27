package net.highwayfrogs.editor.games.konami.greatquest;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.spi.CharsetProvider;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a custom Charset mapping onto how Frogger: The Great Quest encodes its strings in the PC port (and presumably also PS2 NTSC)
 * Frogger The Great Quest appears to use a variation of "Extended ASCII", where it has added its own custom symbols.
 * Reference: <a href="http://www.javanio.info/filearea/bookexamples/unpacked/com/ronsoft/books/nio/charset/"/>
 * Created by Kneesnap on 10/26/2024.
 */
public class GreatQuestCharsetProvider extends CharsetProvider {
    public static final String CHARSET_NAME = "FROGGER-TGQ";
    private static final int CHARACTER_MAP_SIZE = 0x100;
    private static final char[] CHARACTER_MAPPINGS = new char[CHARACTER_MAP_SIZE];

    {
        CHARACTER_MAPPINGS[0x00] = '\0'; // Symbol for null is '␀'.
        CHARACTER_MAPPINGS[0x01] = '╳'; // PS2 Cross Button
        CHARACTER_MAPPINGS[0x02] = '○'; // PS2 Circle Button
        CHARACTER_MAPPINGS[0x03] = '⟁'; // PS2 Triangle Button
        CHARACTER_MAPPINGS[0x04] = '□'; // PS2 Square Button
        CHARACTER_MAPPINGS[0x05] = '␡'; // Symbol for delete.
        CHARACTER_MAPPINGS[0x06] = '␃'; // Symbol for "End of Text"
        CHARACTER_MAPPINGS[0x07] = '—'; // "End of Guarded Area" U+0097
        // [0x08, 0x1F] are unused, sporting graphics displaying their hex number.
        // [0x20, 0x7E] perfectly match the 7-bit ASCII specification, so we'll copy them directly.
        for (char i = ' '; i <= '~'; i++)
            CHARACTER_MAPPINGS[i] = i;

        // 0x7F is one of the unused hex graphics.
        // 0x80 onward appears similar to Windows-1252 (CP-1252) (Extended ASCII?)
        // UTF-16 seems to share most of these characters however, so we're going to fill out the rest of this with UTF-16, then replace bad characters with better ones.
        for (int i = 0x80; i < CHARACTER_MAP_SIZE; i++)
            CHARACTER_MAPPINGS[i] = (char) i;

        CHARACTER_MAPPINGS[0x80] = '€'; // Euro Symbol
        // 0x81 is one of the unused hex graphics.
        CHARACTER_MAPPINGS[0x82] = '‚'; // Single low-9 quotation mark.
        CHARACTER_MAPPINGS[0x83] = 'ƒ'; // Latin small letter f with hook
        CHARACTER_MAPPINGS[0x84] = '„'; // Double low-9 quotation mark
        CHARACTER_MAPPINGS[0x85] = '…'; // Horizontal ellipsis
        CHARACTER_MAPPINGS[0x86] = '†'; // Dagger
        CHARACTER_MAPPINGS[0x87] = '‡'; // Double dagger
        CHARACTER_MAPPINGS[0x88] = 'ˆ'; // Modifier letter circumflex accent
        CHARACTER_MAPPINGS[0x89] = '‰'; // Per mille sign
        CHARACTER_MAPPINGS[0x8A] = 'Š'; // Latin capital letter S with caron
        CHARACTER_MAPPINGS[0x8B] = '‹'; // Single left-pointing angle quotation
        CHARACTER_MAPPINGS[0x8C] = 'Œ'; // Latin capital ligature OE
        CHARACTER_MAPPINGS[0x91] = '‘'; // Left single quotation mark
        CHARACTER_MAPPINGS[0x92] = '’'; // Right single quotation mark
        CHARACTER_MAPPINGS[0x93] = '“'; // Left double quotation mark
        CHARACTER_MAPPINGS[0x94] = '”'; // Right double quotation mark
        CHARACTER_MAPPINGS[0x95] = '•'; // Bullet
        CHARACTER_MAPPINGS[0x96] = '–'; // En dash
        CHARACTER_MAPPINGS[0x97] = '—'; // Em dash
        CHARACTER_MAPPINGS[0x98] = '˜'; // Small tilde
        CHARACTER_MAPPINGS[0x99] = '™'; // Trade mark sign
        CHARACTER_MAPPINGS[0x9A] = 'š'; // Latin small letter S with caron
        CHARACTER_MAPPINGS[0x9B] = '›'; // Single right-pointing angle quotation mark
        CHARACTER_MAPPINGS[0x9C] = 'œ'; // Latin small ligature oe
        CHARACTER_MAPPINGS[0x9F] = 'Ÿ'; // Latin capital letter Y with diaeresis
    }

    private static final char[] REVERSE_CHARACTER_MAPPINGS = new char[65536];
    {
        for (int i = 0; i < CHARACTER_MAPPINGS.length; i++)
            if (CHARACTER_MAPPINGS[i] != '\0')
                REVERSE_CHARACTER_MAPPINGS[CHARACTER_MAPPINGS[i]] = (char) i;
    }

    private final List<Charset> charsets = Collections.singletonList(this.charset);
    private final GreatQuestCharset charset = new GreatQuestCharset();

    @Override
    public Iterator<Charset> charsets() {
        return this.charsets.iterator();
    }

    @Override
    public Charset charsetForName(String charsetName) {
        return CHARSET_NAME.equalsIgnoreCase(charsetName) ? this.charset : null;
    }

    /**
     * Gets the FTGQ charset.
     */
    public static Charset getCharset() {
        return Charset.forName(CHARSET_NAME);
    }

    private static class GreatQuestCharsetDecoder extends CharsetDecoder {
        /**
         * Initializes a new decoder.  The new decoder will have the given
         * chars-per-byte values and its replacement will be the
         * string <tt>"&#92;uFFFD"</tt>.
         * @param charset The charset that created this decoder
         */
        protected GreatQuestCharsetDecoder(Charset charset) {
            super(charset, 1, 1);
        }

        @Override
        protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
            while (in.hasRemaining()) {
                if (!out.hasRemaining())
                    return CoderResult.OVERFLOW;

                byte nextByte = in.get();
                out.append(CHARACTER_MAPPINGS[nextByte & 0xFF]);
            }

            return CoderResult.UNDERFLOW;
        }
    }

    private static class GreatQuestCharsetEncoder extends CharsetEncoder {
        protected GreatQuestCharsetEncoder(Charset cs) {
            super(cs, 1, 1);
        }

        @Override
        protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
            while (in.hasRemaining()) {
                if (!out.hasRemaining())
                    return CoderResult.OVERFLOW;

                char next = in.get();
                out.put((byte) REVERSE_CHARACTER_MAPPINGS[next]);
            }

            return CoderResult.UNDERFLOW;
        }
    }

    public static class GreatQuestCharset extends Charset {

        protected GreatQuestCharset() {
            super(CHARSET_NAME, null);
        }

        @Override
        public boolean contains(Charset cs) {
            return false; // Responding false is always safe.
        }

        @Override
        public CharsetDecoder newDecoder() {
            return new GreatQuestCharsetDecoder(this);
        }

        @Override
        public CharsetEncoder newEncoder() {
            return new GreatQuestCharsetEncoder(this);
        }
    }
}
