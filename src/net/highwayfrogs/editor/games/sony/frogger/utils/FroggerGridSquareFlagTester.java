package net.highwayfrogs.editor.games.sony.frogger.utils;

import net.highwayfrogs.editor.games.sony.SCGameObject;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerMapForm;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerMapFormData;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquare;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquareFlag;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * This utility is used to print grid square flags seen to be used together.
 * Primarily used for debugging grid square flags.
 * Created by Kneesnap on 6/14/2024.
 */
public class FroggerGridSquareFlagTester extends SCGameObject<FroggerGameInstance> {
    private final boolean[][] flagTracker = new boolean[FroggerGridSquareFlag.values().length][FroggerGridSquareFlag.values().length];

    public FroggerGridSquareFlagTester(FroggerGameInstance instance) {
        super(instance);
    }

    /**
     * Add a group of flags to the tracker.
     * @param flags the flags to add
     */
    public void add(int flags) {
        for (int i = 0; i < FroggerGridSquareFlag.values().length; i++) {
            FroggerGridSquareFlag mainTestFlag = FroggerGridSquareFlag.values()[i];
            if ((flags & mainTestFlag.getBitFlagMask()) == 0)
                continue; // Flag not set.

            for (int j = 0; j < FroggerGridSquareFlag.values().length; j++) {
                FroggerGridSquareFlag tempTestFlag = FroggerGridSquareFlag.values()[j];
                if ((flags & tempTestFlag.getBitFlagMask()) == tempTestFlag.getBitFlagMask())
                    this.flagTracker[i][j] = true;
            }
        }
    }

    /**
     * Print a list of the flags seen / not seen together.
     */
    public void printResults() {
        // Determine which flags are used / unused.
        String[] flagsSeenWithFlag = new String[FroggerGridSquareFlag.values().length];
        String[] flagsNotSeenWithFlag = new String[FroggerGridSquareFlag.values().length];

        StringBuilder seenBuilder = new StringBuilder();
        StringBuilder notSeenBuilder = new StringBuilder();
        for (int i = 0; i < FroggerGridSquareFlag.values().length; i++) {
            boolean[] flagSeen = this.flagTracker[i];

            seenBuilder.setLength(0);
            notSeenBuilder.setLength(0);
            for (int j = 0; j < FroggerGridSquareFlag.values().length; j++) {
                StringBuilder currentBuilder = flagSeen[j] ? seenBuilder : notSeenBuilder;

                if (currentBuilder.length() > 0)
                    currentBuilder.append(", ");

                FroggerGridSquareFlag tempTestFlag = FroggerGridSquareFlag.values()[j];
                currentBuilder.append(tempTestFlag.name());
            }

            if (seenBuilder.length() > 0)
                flagsSeenWithFlag[i] = seenBuilder.toString();
            if (notSeenBuilder.length() > 0)
                flagsNotSeenWithFlag[i] = notSeenBuilder.toString();
        }

        // Print output.
        getLogger().info("Flags NOT Seen Together:");
        for (int i = 0; i < FroggerGridSquareFlag.values().length; i++)
            if (flagsNotSeenWithFlag[i] != null)
                getLogger().info(" - " + FroggerGridSquareFlag.values()[i].name() + ": [" + (flagsNotSeenWithFlag[i] != null ? flagsNotSeenWithFlag[i] : "") + "]");
        getLogger().info("");

        getLogger().info("Flags Seen Together:");
        for (int i = 0; i < FroggerGridSquareFlag.values().length; i++)
            if (flagsSeenWithFlag[i] != null)
                getLogger().info(" - " + FroggerGridSquareFlag.values()[i].name() + ": [" + (flagsSeenWithFlag[i] != null ? flagsSeenWithFlag[i] : "") + "]");
        getLogger().info("");
    }

    /**
     * Print information about the grid square flags loaded in the game instance.
     * @param gameInstance the frogger game instance to scan
     */
    public static void printFlagInformation(FroggerGameInstance gameInstance) {
        List<FroggerMapFile> mapFiles = gameInstance.getMainArchive().getAllFiles(FroggerMapFile.class);

        // First do grid squares.
        FroggerGridSquareFlagTester gridFlagTester = new FroggerGridSquareFlagTester(gameInstance);
        for (FroggerMapFile mapFile : mapFiles) {
            FroggerMapFilePacketGrid gridPacket = mapFile.getGridPacket();
            for (int z = 0; z < gridPacket.getGridZCount(); z++) {
                for (int x = 0; x < gridPacket.getGridXCount(); x++) {
                    FroggerGridStack gridStack = gridPacket.getGridStack(x, z);
                    if (gridStack != null)
                        for (FroggerGridSquare square : gridStack.getGridSquares())
                            gridFlagTester.add(square.getFlags());
                }
            }
        }
        gridFlagTester.getLogger().info("Grid Square Flags:");
        gridFlagTester.printResults();

        // Then do form squares.
        FroggerGridSquareFlagTester formFlagTester = new FroggerGridSquareFlagTester(gameInstance);
        for (FroggerMapFile mapFile : mapFiles) {
            for (FroggerMapForm form : mapFile.getFormPacket().getForms()) {
                for (FroggerMapFormData formData : form.getFormDataEntries()) {
                    short[][] gridFlags = formData.getGridFlags();
                    for (int z = 0; z < gridFlags.length; z++)
                        for (int x = 0; x < gridFlags[z].length; x++)
                            formFlagTester.add(Utils.shortToUnsignedInt(gridFlags[z][x]));
                }
            }
        }
        formFlagTester.getLogger().info("Form Square Flags:");
        formFlagTester.printResults();
    }
}