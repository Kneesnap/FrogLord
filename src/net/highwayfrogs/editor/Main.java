package net.highwayfrogs.editor;

import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.PP20Packer.BitWriter;
import net.highwayfrogs.editor.file.PP20Unpacker.BitReader;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;

import java.io.File;

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

        BitWriter writer = new BitWriter();
        for (int i = 0; i < 4; i++) {
            writer.writeBit(1);
            writer.writeBit(0);
        }

        writer.writeBits(new int[] {1, 0, 1, 0, 1, 0, 1, 0});

        byte[] array = writer.toArray();

        BitReader reader1 = new BitReader(array, array.length - 1);
        BitReader reader2 = new BitReader(array, array.length - 1);

        for (int i = 0; i < array.length * 8; i++)
            System.out.println("Bit #" + i + ": " + reader1.readBit());

        for (int i = 0; i < array.length; i++)
            System.out.println("Read Byte #" + i + ": " + (byte) reader2.readBits(8));
    }
}
