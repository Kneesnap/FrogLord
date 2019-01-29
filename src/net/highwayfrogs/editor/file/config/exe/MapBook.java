package net.highwayfrogs.editor.file.config.exe;

import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * A general mapbook struct.
 * Created by Kneesnap on 1/27/2019.
 */
public abstract class MapBook extends ExeStruct {

    private static final short TERMINATOR = (short) 0;

    /**
     * Reads remap data into a config.
     */
    public abstract void readRemapData(FroggerEXEInfo config);

    /**
     * Save remap data into the exe.
     * @param config the config to save it from.
     */
    public abstract void saveRemapData(FroggerEXEInfo config);

    /**
     * Read a remap.
     * @param config       The config to keep the results in.
     * @param resourceId   The map's resource id.
     * @param remapAddress The address the remap starts at.
     */
    protected void readRemap(FroggerEXEInfo config, int resourceId, long remapAddress) {
        if (remapAddress == 0 || resourceId == 0)
            return; // Dummied out book.

        DataReader reader = config.getReader();
        int fileAddress = (int) (remapAddress - config.getRamPointerOffset());
        reader.setIndex(fileAddress);

        List<Short> shortList = new ArrayList<>();

        short tempShort;
        while ((tempShort = reader.readShort()) != TERMINATOR)
            shortList.add(tempShort);

        config.getRemapTable().put(config.getMWI().getEntries().get(resourceId), shortList);
    }

    /**
     * Save a remap.
     * @param config       The config to keep the results in.
     * @param resourceId   The map's resource id.
     * @param remapAddress The address the remap starts at.
     */
    protected void saveRemap(FroggerEXEInfo config, int resourceId, long remapAddress) {
        if (remapAddress == 0 || resourceId == 0)
            return; // Dummied out book.

        DataWriter exeWriter = config.getWriter();
        int fileAddress = (int) (remapAddress - config.getRamPointerOffset());
        exeWriter.setIndex(fileAddress);

        List<Short> entries = config.getRemapTable(config.getMWI().getEntries().get(resourceId));
        entries.forEach(exeWriter::writeShort);
        exeWriter.writeShort(TERMINATOR);

        exeWriter.closeReceiver();
    }

    /**
     * Test if a file entry is held by this struct.
     * @param test The entry to test.
     * @return isEntry
     */
    public abstract boolean isEntry(FileEntry test);
}