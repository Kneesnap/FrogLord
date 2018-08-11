package net.highwayfrogs.editor;

import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;

import java.io.File;

/**
 * The main class which starts the entire editor.
 * Created by Kneesnap on 8/10/2018.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        File file = new File("./debug/VANILLA.MWI");
        MWIFile mwi = new MWIFile();
        mwi.load(new DataReader(new FileSource(file)));
    }
}
