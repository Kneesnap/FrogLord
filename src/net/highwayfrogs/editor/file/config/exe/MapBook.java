package net.highwayfrogs.editor.file.config.exe;

import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.config.exe.pc.PCMapBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXMapBook;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A general mapbook struct.
 * Created by Kneesnap on 1/27/2019.
 */
public abstract class MapBook extends ExeStruct {

    public static final short REMAP_TERMINATOR = (short) 0;

    public MapBook(FroggerGameInstance instance) {
        super(instance);
    }

    /**
     * Reads remap data into a config.
     */
    public abstract void readRemapData(FroggerGameInstance instance);

    /**
     * Save remap data into the exe.
     * @param writer   The writer to save data to.
     * @param instance The game instance to save data to..
     */
    public abstract void saveRemapData(DataWriter writer, FroggerGameInstance instance);

    /**
     * Read a remap.
     * @param instance     The game instead to read from.
     * @param resourceId   The map's resource id.
     * @param remapAddress The address the remap starts at.
     */
    protected void readRemap(FroggerGameInstance instance, int resourceId, long remapAddress) {
        if (remapAddress == 0 || resourceId == 0)
            return; // Dummied out book.

        DataReader reader = instance.getExecutableReader();
        int fileAddress = (int) (remapAddress - getConfig().getRamPointerOffset());
        reader.setIndex(fileAddress);

        List<Short> remap = new ArrayList<>();
        short textureId;
        while (canContinueReadingRemap(reader, textureId = reader.readShort()))
            remap.add(textureId);

        getGameInstance().setRemap(getGameInstance().getResourceEntryByID(resourceId), remap);

        // Hack to read island remap. Build 20 is the last build with the remap present (it's also the last build with the unique textures present.)
        if ((!getConfig().isAtOrBeforeBuild1() && getConfig().isAtOrBeforeBuild20()) && getGameInstance().getResourceName(resourceId).equals("ARN1.MAP")) {
            while ((textureId = reader.readShort()) == REMAP_TERMINATOR) ;

            do {
                getConfig().getIslandRemap().add(textureId);
                textureId = reader.readShort();
            } while (canContinueReadingRemap(reader, textureId));
        }
    }

    private boolean canContinueReadingRemap(DataReader reader, short textureId) {
        if (textureId == REMAP_TERMINATOR)
            return false;

        // Look for the data which comes after the remap table.
        if (getGameInstance().isPSX() && textureId == 0x80) {
            reader.jumpTemp(reader.getIndex());
            long nextData = reader.readUnsignedIntAsLong();
            reader.jumpReturn();
            return nextData != 0x00100020L; // Confirmed end of data.
        }

        return true;
    }

    /**
     * Save a remap.
     * @param instance     The game instance to save to.
     * @param resourceId   The map's resource id.
     * @param remapAddress The address the remap starts at.
     */
    protected void saveRemap(DataWriter writer, FroggerGameInstance instance, int resourceId, long remapAddress) {
        if (remapAddress == 0 || resourceId == 0)
            return; // Dummied out book.

        int fileAddress = (int) (remapAddress - instance.getConfig().getRamPointerOffset());
        writer.setIndex(fileAddress);

        List<Short> entries = instance.getRemapTable(instance.getResourceEntryByID(resourceId));
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

    /**
     * Gets the wad file for a given map.
     * @param map The map to get the wad file for.
     * @return wadFile
     */
    public abstract WADFile getWad(MAPFile map);
}