package net.highwayfrogs.editor.file.config;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Information about a specific frogger.exe file.
 * Created by Kneesnap on 8/18/2018.
 */
@Getter
public class FroggerEXEInfo extends Config {
    private static final String NO_REMAP_DATA = "PLACEHOLDER";

    public FroggerEXEInfo(InputStream inputStream) throws IOException {
        super(inputStream);
    }

    /**
     * Gets the byte-location of this executable's MWI.
     * @return mwiOffset
     */
    public int getMWIOffset() {
        return getInt("mwiOffset");
    }

    /**
     * Get the byte-length of this executable's MWI.
     * @return mwiLength
     */
    public int getMWILength() {
        return getInt("mwiLength");
    }

    /**
     * Loads the remap table from the Frogger EXE.
     * @param levelName  The name of the level.
     * @param executable The Frogger executable to read remap information from.
     * @return remapTable
     */
    public List<Short> getRemapTable(String levelName, File executable) {
        List<Short> remapTable = new ArrayList<>();

        String remapData = getChild("Remaps").getString(levelName);
        Utils.verify(remapData != null && !remapData.equalsIgnoreCase(NO_REMAP_DATA) && remapData.contains("|"), "There is no remap data for %s.", levelName);
        String[] split = remapData.split("\\|");
        int remapAddress = Integer.decode(split[0]);
        int remapCount = Integer.parseInt(split[1]); // Amount of texture remaps. (Bytes / SHORT_SIZE)

        DataReader reader;
        try {
            reader = new DataReader(new FileSource(executable));
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            return remapTable;
        }

        reader.setIndex(remapAddress);
        for (int i = 0; i < remapCount; i++)
            remapTable.add(reader.readShort());

        return remapTable;
    }

    /**
     * Patch Frogger.exe to use a modded MWI.
     * @param froggerEXE The file to modify.
     * @param mwiFile    The MWI file to save.
     */
    public void patchEXE(File froggerEXE, MWIFile mwiFile) {
        DataWriter writer;

        try {
            writer = new DataWriter(new FileReceiver(froggerEXE));
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            return;
        }

        int mwiOffset = getMWIOffset();
        writer.setIndex(mwiOffset);
        mwiFile.save(writer);
        writer.closeReceiver();

        int bytesWritten = writer.getIndex() - mwiOffset;
        Utils.verify(bytesWritten == getMWILength(), "MWI Patching Failed. The size of the written MWI does not match the correct MWI size! [%d/%d]", bytesWritten, getMWILength());
    }

}
