package net.highwayfrogs.editor.games.sony.frogger.data.demo;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MillenniumWadIndex;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a Frogger demo table.
 * Created by Kneesnap on 11/24/2025.
 */
public class FroggerDemoTable extends SCGameData<FroggerGameInstance> {
    private int executableAddress = Integer.MIN_VALUE; // The address which the table can be found.
    @Getter private boolean lowPolygonEntries;
    private FroggerDemoTableEntry[] demoTableEntries = EMPTY_ARRAY;

    private static final FroggerDemoTableEntry[] EMPTY_ARRAY = new FroggerDemoTableEntry[0];
    private static final String CONFIG_KEY_DEMO_TABLE_ADDRESS = "demoTableAddress";
    private static final String CONFIG_KEY_DEMO_TABLE_LOW_POLY = "lowPolyDemoTable";

    public FroggerDemoTable(FroggerGameInstance instance) {
        super(instance);
    }

    /**
     * Returns the number of entries in the demo table.
     */
    public int size() {
        return this.demoTableEntries.length;
    }

    /**
     * Gets the demo table entry for the given index.
     * An exception is thrown if the index is not valid.
     * @param index the index into the table
     * @return demoTableEntry
     */
    public FroggerDemoTableEntry get(int index) {
        return this.demoTableEntries[index];
    }

    /**
     * Gets the demo table entries as a list.
     */
    public List<FroggerDemoTableEntry> getEntries() {
        return Arrays.asList(this.demoTableEntries);
    }

    /**
     * Resolve the demo table information from the config
     * @param config the config to resolve from
     * @return if the data was successfully resolved
     */
    public boolean resolveFromConfig(Config config) {
        if (config == null)
            return false;

        ConfigValueNode tableAddress = config.getOptionalKeyValueNode(CONFIG_KEY_DEMO_TABLE_ADDRESS);
        if (tableAddress == null)
            return false;

        this.executableAddress = tableAddress.getAsInteger();
        this.lowPolygonEntries = config.getOrDefaultKeyValueNode(CONFIG_KEY_DEMO_TABLE_LOW_POLY).getAsBoolean(false);
        return true;
    }

    /**
     * Writes game demo table information to a config
     * @param config the config object to write to
     */
    public void toConfig(@NonNull Config config) {
        if (this.executableAddress <= 0)
            return;

        config.getOrCreateKeyValueNode(CONFIG_KEY_DEMO_TABLE_ADDRESS).setAsString(NumberUtils.toHexString(this.executableAddress));
        if (this.lowPolygonEntries)
            config.getOrCreateKeyValueNode(CONFIG_KEY_DEMO_TABLE_LOW_POLY).setAsBoolean(true);
    }

    /**
     * Resolves the demo table address.
     * @param logger the logger to write warnings to
     */
    public void resolveDemoTableAddress(ILogger logger) {
        if (this.executableAddress != Integer.MIN_VALUE)
            throw new IllegalStateException("The demoTable address has already been resolved.");

        this.executableAddress = -1; // Didn't find the bytes, ABORT!
        MillenniumWadIndex wadIndex = getGameInstance().getArchiveIndex();
        MWIResourceEntry demoEntry = wadIndex.getResourceEntryByName("WSUB1DEMO.DAT");
        if (demoEntry != null) {
            this.lowPolygonEntries = true;
        } else {
            this.lowPolygonEntries = false;
            demoEntry = wadIndex.getResourceEntryByName("SUB1DEMO.DAT");
        }

        if (demoEntry == null) {
            logger.warning("Failed to find the demo table, is there a demo named SUB1DEMO.DAT?");
            return; // Couldn't find a demo by this name, so... skip.
        }

        byte[] levelId = DataUtils.toByteArray(FroggerMapLevelID.SUBURBIA1.ordinal());
        byte[] demoId = DataUtils.toByteArray(demoEntry.getResourceId());

        byte[] searchFor = new byte[levelId.length + demoId.length];
        System.arraycopy(levelId, 0, searchFor, 0, levelId.length);
        System.arraycopy(demoId, 0, searchFor, levelId.length, demoId.length);

        int findIndex = Utils.indexOf(getGameInstance().getExecutableBytes(), searchFor);
        if (findIndex == -1) {
            logger.warning("Failed to automatically find the demo table via byte-search.");
            return;
        }

        this.executableAddress = findIndex;
        getLogger().info("Found the demo table address at 0x%X", findIndex);
    }

    @Override
    @SuppressWarnings("ExtractMethodRecommender")
    public void load(DataReader reader) {
        if (this.executableAddress == Integer.MIN_VALUE)
            throw new IllegalStateException("The demoTable address has not been resolved yet.");

        this.demoTableEntries = EMPTY_ARRAY;
        if (this.executableAddress < 0)
            return;

        reader.setIndex(this.executableAddress);
        FroggerDemoTableEntry[] newTableEntries;
        if (this.lowPolygonEntries) {
            List<FroggerDemoTableEntry> lowPolyTableEntries = readRawTable(getGameInstance(), reader, false);
            newTableEntries = readRawTable(getGameInstance(), reader, true).toArray(EMPTY_ARRAY);
            if (newTableEntries.length != lowPolyTableEntries.size())
                throw new IllegalStateException("The first demoTable @ " + NumberUtils.to0PrefixedHexString(this.executableAddress) + " had " + lowPolyTableEntries.size() + " entries, but the second one had " + newTableEntries.length + " entries. (These were expected to match)");

            for (int i = 0; i < newTableEntries.length; i++) {
                FroggerDemoTableEntry lowPolyEntry = lowPolyTableEntries.get(i);
                FroggerDemoTableEntry highPolyEntry = newTableEntries[i];
                if (lowPolyEntry.getLevel() != highPolyEntry.getLevel())
                    throw new IllegalStateException("DemoTableEntry[" + i + "] had a level mismatch between low-poly ("
                            + lowPolyEntry.getLevel() + ") and high-poly (" + highPolyEntry.getLevel() + ") tables!");
                if (lowPolyEntry.getUnlockLevel() != highPolyEntry.getUnlockLevel())
                    throw new IllegalStateException("DemoTableEntry[" + i + "] had an unlock level mismatch between low-poly ("
                            + lowPolyEntry.getUnlockLevel() + ") and high-poly (" + highPolyEntry.getUnlockLevel() + ") tables!");

                ((FroggerPCDemoTableEntry) highPolyEntry).setLowPolyDemoResourceId(lowPolyEntry.getDemoResourceId());
            }
        } else{
            newTableEntries = readRawTable(getGameInstance(), reader, false).toArray(EMPTY_ARRAY);
        }

        // Do this down here so that if an error occurs, the demo data simply is not tracked by FrogLord.
        this.demoTableEntries = newTableEntries;
    }

    @Override
    public void save(DataWriter writer) {
        if (this.executableAddress <= 0)
            return;

        writer.setIndex(this.executableAddress);
        if (this.lowPolygonEntries) {
            for (int i = 0; i < this.demoTableEntries.length; i++)
                ((FroggerPCDemoTableEntry) this.demoTableEntries[i]).saveLowPolyDemoTableEntry(writer);

            writeListTerminator(writer);
        }

        for (int i = 0; i < this.demoTableEntries.length; i++)
            this.demoTableEntries[i].save(writer);

        writeListTerminator(writer);
    }

    private static List<FroggerDemoTableEntry> readRawTable(FroggerGameInstance instance, DataReader reader, boolean pc) {
        List<FroggerDemoTableEntry> rawEntries = new ArrayList<>();

        while (reader.hasMore()) {
            reader.jumpTemp(reader.getIndex());
            int levelId = reader.readInt();
            reader.jumpReturn();
            if (levelId == FroggerDemoTableEntry.TERMINATOR_VALUE) {
                reader.skipBytes(3 * Constants.INTEGER_SIZE);
                break; // Reached terminator.
            }

            FroggerDemoTableEntry newDemoTableEntry = pc ? new FroggerPCDemoTableEntry(instance) : new FroggerDemoTableEntry(instance);
            newDemoTableEntry.load(reader);
            rawEntries.add(newDemoTableEntry);
        }

        return rawEntries;
    }

    private static void writeListTerminator(DataWriter writer) {
        writer.writeInt(FroggerDemoTableEntry.TERMINATOR_VALUE);
        writer.writeInt(FroggerDemoTableEntry.TERMINATOR_VALUE);
        writer.writeInt(FroggerDemoTableEntry.TERMINATOR_VALUE);
    }
}
