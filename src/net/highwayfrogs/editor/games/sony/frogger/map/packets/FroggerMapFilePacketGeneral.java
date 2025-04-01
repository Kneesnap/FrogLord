package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.AbstractStringConverter;

/**
 * Implements the general data file packet.
 * Created by Kneesnap on 5/25/2024.
 */
@Getter
public class FroggerMapFilePacketGeneral extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "GENE";
    @Setter private int startGridCoordX;
    @Setter private int startGridCoordZ;
    @Setter private FroggerMapStartRotation startRotation = FroggerMapStartRotation.NORTH;
    @NonNull @Setter private FroggerMapTheme mapTheme = FroggerMapTheme.SUBURBIA;
    @Setter private int startingTimeLimit = 99;
    private final SVector defaultCameraSourceOffset = new SVector(); // Applied when there is no ZONE for the active area. (If there is a zone, the source/target of the zone will be used.)
    private final SVector defaultCameraTargetOffset = new SVector(); // Further usage described below.
    // FrogInitCustomAmbient adds 128 to these to create a color.
    // I don't see any good reason these are shorts and not bytes, so we'll represent them as bytes.
    private short frogRedLighting;
    private short frogGreenLighting;
    private short frogBlueLighting;

    // Camera offsets are used for several different things:
    // The height in ORG when there are at least 2 players is multiplied by 1.5x in InitialiseCameras()
    // In multiplayer, the following scenarios the camera(s) will reset to use the default offsets.
    //  - When a tie occurs, all cameras
    //  - When a player collects a checkpoint and the game is not complete, their camera
    //  - When a player resets from death, their camera

    private static final int TOTAL_CHECKPOINT_TIMER_ENTRIES = 5;
    private static final int GAME_PERSPECTIVE = 192;

    public FroggerMapFilePacketGeneral(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.startGridCoordX = reader.readUnsignedShortAsInt();
        this.startGridCoordZ = reader.readUnsignedShortAsInt();
        this.startRotation = FroggerMapStartRotation.values()[reader.readShort()];
        if (isAprilFormat()) {
            // The early format is cut off extremely early.
            reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.
            return;
        }

        this.mapTheme = FroggerMapTheme.values()[reader.readShort()];

        // The timer is duplicated once for each frog.
        // It seems at one point it was planned to have the timer change based on how many frogs have been collected.
        // However, the game is not coded to actually use this data, it's just redundant.
        this.startingTimeLimit = reader.readUnsignedShortAsInt();
        reader.skipBytes((TOTAL_CHECKPOINT_TIMER_ENTRIES - 1) * Constants.SHORT_SIZE);

        // This value is unused.
        short unusedPerspective = reader.readShort();
        if (unusedPerspective != GAME_PERSPECTIVE && !getParentFile().isEarlyMapFormat()) // Sanity check.
            getLogger().warning("Map files are expected to have the unused perspective value set to " + GAME_PERSPECTIVE + ", but this one was " + unusedPerspective + ".");

        if (hasCameraData()) {
            this.defaultCameraSourceOffset.loadWithPadding(reader);
            this.defaultCameraTargetOffset.loadWithPadding(reader);
        }

        if (hasFrogColorData()) {
            this.frogRedLighting = reader.readShort();
            this.frogGreenLighting = reader.readShort();
            this.frogBlueLighting = reader.readShort();
            reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.startGridCoordX);
        writer.writeUnsignedShort(this.startGridCoordZ);
        writer.writeShort((short) (this.startRotation != null ? this.startRotation : FroggerMapStartRotation.NORTH).ordinal());
        if (isAprilFormat()) {
            // The early format is cut off extremely early.
            writer.writeNull(Constants.SHORT_SIZE); // Padding.
            return;
        }

        writer.writeShort((short) this.mapTheme.ordinal());

        // The timer is duplicated once for each frog.
        // It seems at one point it was planned to have the timer change based on how many frogs have been collected.
        // However, the game is not coded to actually use this any of the entries beyond the first, so it is redundant.
        for (int i = 0; i < TOTAL_CHECKPOINT_TIMER_ENTRIES; i++)
            writer.writeUnsignedShort(this.startingTimeLimit);

        writer.writeShort((short) GAME_PERSPECTIVE); // Unused perspective variable.
        if (hasCameraData()) {
            this.defaultCameraSourceOffset.saveWithPadding(writer);
            this.defaultCameraTargetOffset.saveWithPadding(writer);
        }

        if (hasFrogColorData()) {
            writer.writeShort(this.frogRedLighting);
            writer.writeShort(this.frogGreenLighting);
            writer.writeShort(this.frogBlueLighting);
            writer.writeNull(Constants.SHORT_SIZE); // Unused padding.
        }
    }

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getHeaderPacket().getGeneralPacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        if (!isAprilFormat()) {
            propertyList.add("Map Theme", this.mapTheme);
            propertyList.add("Starting Time Limit", this.startingTimeLimit);
        }

        propertyList.add("Start Grid Coordinates", "[" + this.startGridCoordX + ", " + this.startGridCoordZ + "]");
        propertyList.add("Start Grid Rotation", (this.startRotation != null ? this.startRotation.name() + " (" + this.startRotation.getArrow() + ")" : "null"));
        if (hasFrogColorData())
            propertyList.add("Frog Ambient Lighting", "<red=" + (this.frogRedLighting + 0x80) + ",green=" + (this.frogGreenLighting + 0x80) + ",blue=" + (this.frogBlueLighting + 0x80) + ">");
        return propertyList;
    }

    /**
     * Test if this is using the April general packet format.
     */
    public boolean isAprilFormat() {
        return getConfig().isSonyPresentation();
    }

    /**
     * Test if this contains camera data.
     */
    public boolean hasCameraData() {
        return !getConfig().isSonyPresentation() && !getMapConfig().isIslandPlaceholder(); // The island placeholders seem to be missing it.
    }

    /**
     * Test if this is using the old map group format.
     */
    public boolean hasFrogColorData() {
        return !getConfig().isAtOrBeforeBuild23() && !getParentFile().isExtremelyEarlyMapFormat();
    }

    /**
     * Creates an editor for the data in this packet.
     * @param controller The controller to create the editor under.
     * @param editor The editor grid to create the editor UI with.
     */
    public void setupEditor(MeshViewController<?> controller, GUIEditorGrid editor) {
        // Add map theme / level timer.
        if (!isAprilFormat()) {
            editor.addLabel("Theme", getParentFile().getMapTheme().name()); // Should look into whether this is ok to edit.
            editor.addUnsignedShortField("Level Timer", this.startingTimeLimit, newStartingTimeLimit -> this.startingTimeLimit = newStartingTimeLimit);
        }

        // Add start tile / rotation data.
        editor.addUnsignedShortField("Start xTile", this.startGridCoordX, newStartGridCoordX -> this.startGridCoordX = newStartGridCoordX);
        editor.addUnsignedShortField("Start zTile", this.startGridCoordZ, newStartGridCoordZ -> this.startGridCoordZ = newStartGridCoordZ);
        editor.addEnumSelector("Start Rotation", this.startRotation, FroggerMapStartRotation.values(), false, newStartRotation -> this.startRotation = newStartRotation)
                .setConverter(new AbstractStringConverter<>(FroggerMapStartRotation::getArrow));

        // Add camera data.
        if (hasCameraData()) {
            FroggerMapFilePacketGrid gridPacket = getParentFile().getGridPacket();
            FroggerGridStack baseStack = gridPacket.getGridStack(this.startGridCoordX, this.startGridCoordZ);
            if (baseStack != null) {
                IVector gridOrigin = new IVector(gridPacket.getWorldXFromGridX(this.startGridCoordX, true), -baseStack.getAverageWorldHeight(), gridPacket.getWorldZFromGridZ(this.startGridCoordZ, true));
                editor.addFloatVector("Camera Source", this.defaultCameraSourceOffset, null, controller, gridOrigin.defaultBits(), gridOrigin, null);
                editor.addFloatVector("Camera Target", this.defaultCameraTargetOffset, null, controller, gridOrigin.defaultBits(), gridOrigin, null);
            }
        }

        // Add frog lighting data.
        if (hasFrogColorData()) {
            // TODO: Go over this data. Which maps is it used in? How can we preview this, adding the frog model to the level? (This would be good just for previewing the start position & rotation anyways) Make an ideal editor.
            editor.addSignedByteField("Ambient Red", (byte) this.frogRedLighting, newFrogRedLighting -> this.frogRedLighting = newFrogRedLighting).setDisable(true);
            editor.addSignedByteField("Ambient Green", (byte) this.frogGreenLighting, newFrogGreenLighting -> this.frogGreenLighting = newFrogGreenLighting).setDisable(true);
            editor.addSignedByteField("Ambient Blue", (byte) this.frogBlueLighting, newFrogBlueLighting -> this.frogBlueLighting = newFrogBlueLighting).setDisable(true);
        }
    }

    /**
     * Represents the different values the frog's start rotation can be.
     * Created by Kneesnap on 1/26/2019.
     */
    @Getter
    @AllArgsConstructor
    public enum FroggerMapStartRotation {
        NORTH("↑"), // 0
        EAST("→"), // 1
        SOUTH("↓"), // 2
        WEST("←"); // 3

        private final String arrow;
    }
}