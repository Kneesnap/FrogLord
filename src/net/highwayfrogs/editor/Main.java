package net.highwayfrogs.editor;

import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.PP20Packer;
import net.highwayfrogs.editor.file.PP20Packer.BitWriter;
import net.highwayfrogs.editor.file.PP20Unpacker;
import net.highwayfrogs.editor.file.PP20Unpacker.BitReader;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;

import java.io.File;
import java.util.Arrays;

/**
 * The main class which starts the entire editor.
 * Created by Kneesnap on 8/10/2018.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        MWIFile mwi = new MWIFile();
        mwi.load(new DataReader(new FileSource(new File("./debug/VANILLA.MWI"))));

        MWDFile mwd = new MWDFile(mwi);
        mwd.load(new DataReader(new FileSource(new File("./debug/VANILLA.MWD"))));
        mwd.save(new DataWriter(new FileReceiver(new File("./debug/MODDED.MWD"))));

        //testStr("AAAABBBBAADBBAAACBBBAAAABBBB");
        //testStr("ALERTALERTALERT");
        PP20Unpacker.OUTPUT = true;
        testStr("ALERTLERTERT");
    }

    private static void testStr(String test) {
        System.out.println("Testing: " + test);
        byte[] start = test.getBytes();
        byte[] compressed = PP20Packer.packData(start);
        System.out.println(Utils.toByteString(compressed) + " -> (" + compressed.length + ") " + new String(compressed).replace("\n", "\\n").replace("\r", "\\r"));
        byte[] decompressed = PP20Unpacker.unpackData(compressed);
        System.out.println(Utils.toByteString(decompressed) + " -> (" + decompressed.length + ") " + new String(decompressed));
    }
}
