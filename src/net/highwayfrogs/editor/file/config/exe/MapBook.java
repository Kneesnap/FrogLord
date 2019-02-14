package net.highwayfrogs.editor.file.config.exe;

import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.exe.pc.PCMapBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXMapBook;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A general mapbook struct.
 * Created by Kneesnap on 1/27/2019.
 */
public abstract class MapBook extends ExeStruct {

    public static final short REMAP_TERMINATOR = (short) 0;

    /**
     * Reads remap data into a config.
     */
    public abstract void readRemapData(FroggerEXEInfo config);

    /**
     * Save remap data into the exe.
     * @param writer The writer to save data to.
     * @param config the config to save it from.
     */
    public abstract void saveRemapData(DataWriter writer, FroggerEXEInfo config);

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
        while ((tempShort = reader.readShort()) != REMAP_TERMINATOR)
            shortList.add(tempShort);

        config.getRemapTable().put(config.getResourceEntry(resourceId), shortList);
    }

    /**
     * Save a remap.
     * @param config       The config to keep the results in.
     * @param resourceId   The map's resource id.
     * @param remapAddress The address the remap starts at.
     */
    protected void saveRemap(DataWriter writer, FroggerEXEInfo config, int resourceId, long remapAddress) {
        if (remapAddress == 0 || resourceId == 0)
            return; // Dummied out book.

        int fileAddress = (int) (remapAddress - config.getRamPointerOffset());
        writer.setIndex(fileAddress);

        List<Short> entries = config.getRemapTable(config.getResourceEntry(resourceId));
        entries.forEach(writer::writeShort);
        writer.writeShort(REMAP_TERMINATOR);
    }

    /**
     * Check if this map book is dummied.
     * @return isDummy
     */
    public abstract boolean isDummy();

    /**
     * Execute something depending on which MapBook type this is.
     * @param pcHandler  The PC handler.
     * @param psxHandler The psx handler.
     * @return result
     */
    public abstract <T> T execute(Function<PCMapBook, T> pcHandler, Function<PSXMapBook, T> psxHandler);
}