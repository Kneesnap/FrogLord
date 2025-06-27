package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import javafx.scene.control.Alert.AlertType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapGeneralManager;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the general data file packet.
 * Created by Kneesnap on 5/25/2024.
 */
@Getter
public class FroggerMapFilePacketGeneral extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "GENE";
    @Setter private int startGridCoordX;
    @Setter private int startGridCoordZ;
    @NonNull @Setter private FroggerMapStartRotation startRotation = FroggerMapStartRotation.NORTH;
    @NonNull @Setter private FroggerMapTheme mapTheme = FroggerMapTheme.SUBURBIA;
    @Setter private int startingTimeLimit = 99;
    private final SVector defaultCameraSourceOffset = new SVector(); // Applied when there is no ZONE for the active area. (If there is a zone, the source/target of the zone will be used.)
    private final SVector defaultCameraTargetOffset = new SVector(); // Further usage described below.
    // FrogInitCustomAmbient adds 128 to these to create a color.
    // I don't see any good reason these are shorts and not bytes, so we'll represent them as bytes.
    private byte frogRedLighting = -0x80; // (0x80 when treated as unsigned)
    private byte frogGreenLighting = -0x80; // (0x80 when treated as unsigned)
    private byte frogBlueLighting = -0x80; // (0x80 when treated as unsigned)

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
            getLogger().warning("Map files are expected to have the unused perspective value set to %d, but this one was %d.", GAME_PERSPECTIVE, unusedPerspective);

        if (hasCameraData()) {
            this.defaultCameraSourceOffset.loadWithPadding(reader);
            this.defaultCameraTargetOffset.loadWithPadding(reader);
        }

        if (hasFrogColorData()) {
            short blue = reader.readShort(); // Despite the game labelling this red, it seems to actually be blue.
            short green = reader.readShort();
            short red = reader.readShort(); // Despite the game labelling this blue, it seems to actually be red.
            reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding

            // Load the color values the same way the game will.
            // Important to use addition instead of bitwise OR.
            loadFrogLightingFromBgr(((blue + 0x80) << 16) + ((green + 0x80) << 8) + (red + 0x80));
        }
    }

    private void loadFrogLightingFromBgr(int bgrValue) {
        this.frogBlueLighting = (byte) ((bgrValue >>> 16) & 0xFF);
        this.frogGreenLighting = (byte) ((bgrValue >>> 8) & 0xFF);
        this.frogRedLighting = (byte) (bgrValue & 0xFF);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        if (!doesStartTileLookValid())
            FXUtils.makePopUp("The player start position for " + getParentFile().getFileDisplayName() + " is invalid. This may cause problems in-game.", AlertType.ERROR);

        writer.writeUnsignedShort(this.startGridCoordX);
        writer.writeUnsignedShort(this.startGridCoordZ);
        writer.writeShort((short) this.startRotation.ordinal());
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
            short naiveBlue = (short) (((this.frogBlueLighting & 0xFF) - 0x80) & 0xFF);
            short naiveGreen = (short) (((this.frogGreenLighting & 0xFF) - 0x80) & 0xFF);
            short realRed = (short) (((this.frogRedLighting & 0xFF) - 0x80) & 0xFF);
            int greenOverflow = (realRed >= 0x80) ? 1 : 0;
            int blueOverflow = (naiveGreen >= 0x80 || ((naiveGreen - greenOverflow) & 0xFF) >= 0x80) ? 1 : 0;
            writer.writeShort((short) ((naiveBlue - blueOverflow) & 0xFF)); // Despite the game labelling this red, it seems to actually be blue.
            writer.writeShort((short) ((naiveGreen - greenOverflow) & 0xFF));
            writer.writeShort(realRed); // Despite the game labelling this blue, it seems to actually be red.
            writer.writeNull(Constants.SHORT_SIZE); // Unused padding.
        }
    }

    @Override
    public void clear() {
        this.startGridCoordX = 0;
        this.startGridCoordZ = 0;
        this.startRotation = FroggerMapStartRotation.NORTH;
        this.mapTheme = FroggerMapTheme.SUBURBIA;
        this.defaultCameraSourceOffset.setValues((short) 0, (short) 0, (short) 0);
        this.defaultCameraTargetOffset.setValues((short) 0, (short) 0, (short) 0);
        this.frogRedLighting = -0x80; // (0x80 when treated as unsigned)
        this.frogGreenLighting = -0x80; // (0x80 when treated as unsigned)
        this.frogBlueLighting = -0x80; // (0x80 when treated as unsigned)
    }

    @Override
    public void copyAndConvertData(SCFilePacket<? extends SCChunkedFile<FroggerGameInstance>, FroggerGameInstance> newChunk) {
        if (!(newChunk instanceof FroggerMapFilePacketGeneral))
            throw new ClassCastException("The provided chunk was of type " + Utils.getSimpleName(newChunk) + " when " + FroggerMapFilePacketGeneral.class.getSimpleName() + " was expected.");

        FroggerMapFilePacketGeneral newGeneralChunk = (FroggerMapFilePacketGeneral) newChunk;
        newGeneralChunk.setStartGridCoordX(this.startGridCoordX);
        newGeneralChunk.setStartGridCoordZ(this.startGridCoordZ);
        newGeneralChunk.setStartRotation(this.startRotation);
        newGeneralChunk.setMapTheme(this.mapTheme);
        newGeneralChunk.setStartingTimeLimit(this.startingTimeLimit);
        newGeneralChunk.getDefaultCameraSourceOffset().setValues(this.defaultCameraSourceOffset);
        newGeneralChunk.getDefaultCameraTargetOffset().setValues(this.defaultCameraTargetOffset);
        newGeneralChunk.frogRedLighting = this.frogRedLighting;
        newGeneralChunk.frogGreenLighting = this.frogGreenLighting;
        newGeneralChunk.frogBlueLighting = this.frogBlueLighting;
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
        propertyList.add("Start Grid Rotation", this.startRotation.name() + " (" + this.startRotation.getArrow() + ")");
        if (hasFrogColorData())
            propertyList.add("Frog Ambient Lighting", "<red=" + (this.frogRedLighting & 0xFF) + ",green=" + (this.frogGreenLighting & 0xFF) + ",blue=" + (this.frogBlueLighting & 0xFF) + ">");
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
     * Test if the frog color data is in a state where it impacts rendering.
     */
    public boolean isFrogColorDataActive() {
        return hasFrogColorData() && (this.frogRedLighting != -0x80 || this.frogGreenLighting != -0x80 || this.frogBlueLighting != -0x80);
    }

    /**
     * Creates an editor for the data in this packet.
     * @param manager The manager to create the editor under.
     * @param editor The editor grid to create the editor UI with.
     */
    public void setupEditor(FroggerUIMapGeneralManager manager, GUIEditorGrid editor) {
        MeshViewController<?> controller = manager.getController();
        // Add map theme / level timer.
        if (!isAprilFormat()) {
            editor.addLabel("Theme", getParentFile().getMapTheme().name()); // Should look into whether this is ok to edit.
            editor.addUnsignedShortField("Level Timer", this.startingTimeLimit, newStartingTimeLimit -> this.startingTimeLimit = newStartingTimeLimit);
        }

        // Add start tile / rotation data.
        editor.addUnsignedShortField("Start xTile", this.startGridCoordX,
                newX -> !doesStartTileLookValid() || testStartTileLooksValid(newX, this.startGridCoordZ), newX -> {
            this.startGridCoordX = newX;
            manager.updatePlayerCharacter();
        });
        editor.addUnsignedShortField("Start zTile", this.startGridCoordZ,
                newZ -> !doesStartTileLookValid() || testStartTileLooksValid(this.startGridCoordX, newZ), newZ -> {
            this.startGridCoordZ = newZ;
            manager.updatePlayerCharacter();
        });
        editor.addEnumSelector("Start Rotation", this.startRotation, FroggerMapStartRotation.values(), false,
                        newStartRotation -> {
            this.startRotation = newStartRotation;
            manager.updatePlayerCharacter();
        }).setConverter(new AbstractStringConverter<>(FroggerMapStartRotation::getArrow));

        // Add frog lighting data.
        if (hasFrogColorData()) {
            editor.addColorPicker("Ambient Frog Color", getAmbientFrogColorAsRgb(), newRgb -> {
                loadFrogLightingFromBgr(ColorUtils.swapRedBlue(newRgb));
                manager.getFrogLight().setColor(ColorUtils.fromRGB(newRgb, 1F));
                manager.updatePlayerCharacterLighting();
            });
        }

        // Add camera data.
        if (hasCameraData()) {
            FroggerMapFilePacketGrid gridPacket = getParentFile().getGridPacket();
            FroggerGridStack baseStack = gridPacket.getGridStack(this.startGridCoordX, this.startGridCoordZ);
            if (baseStack != null) {
                int squareY = DataUtils.floatToFixedPointInt4Bit(baseStack.getHighestGridSquareYAsFloat());
                IVector gridOrigin = new IVector(gridPacket.getWorldXFromGridX(this.startGridCoordX, true), squareY, gridPacket.getWorldZFromGridZ(this.startGridCoordZ, true));
                editor.addFloatVector("Camera Source", this.defaultCameraSourceOffset, null, controller, gridOrigin.defaultBits(), gridOrigin, null, null);
                editor.addFloatVector("Camera Target", this.defaultCameraTargetOffset, null, controller, gridOrigin.defaultBits(), gridOrigin, null, null);
            }
        }
    }

    /**
     * Gets the ambient frog color as an RGB value.
     * @return rgbValue
     */
    public int getAmbientFrogColorAsRgb() {
        return ColorUtils.toRGB(this.frogRedLighting, this.frogGreenLighting, this.frogBlueLighting);
    }
    
    private boolean doesStartTileLookValid() {
        return testStartTileLooksValid(this.startGridCoordX, this.startGridCoordZ);
    }

    private boolean testStartTileLooksValid(int newStartX, int newStartZ) {
        FroggerMapFilePacketGrid gridPacket = getParentFile().getGridPacket();
        if (newStartX < 0 || newStartZ < 0 || newStartX >= gridPacket.getGridXCount() || newStartZ >= gridPacket.getGridZCount())
            return false;

        FroggerGridStack gridStack = gridPacket.getGridStack(newStartX, newStartZ);
        return gridStack.getGridSquares().size() > 0;
    }

    /**
     * Represents the different values the frog's start rotation can be.
     * Created by Kneesnap on 1/26/2019.
     */
    @Getter
    @AllArgsConstructor
    public enum FroggerMapStartRotation {
        NORTH("↑", 0F), // 0
        EAST("→", 90F), // 1
        SOUTH("↓", 180F), // 2
        WEST("←", 270F); // 3

        private final String arrow;
        private final float rotationInDegrees;
    }

    /**
     * Tests the color loading & saving logic works for all possible colors.
     */
    @SuppressWarnings("unused")
    private static void testColorLoadingAndSaving() {
        for (short blue = 0; blue < 256; blue++) {
            for (short green = 0; green < 256; green++) {
                for (short red = 0; red < 256; red++) {
                    // Load the color values the same way the game will.
                    // Important to use addition instead of bitwise OR.
                    int bgrValue = (((blue + 0x80) << 16) + ((green + 0x80) << 8) + (red + 0x80));
                    byte blueLighting = (byte) ((bgrValue >>> 16) & 0xFF);
                    byte greenLighting = (byte) ((bgrValue >>> 8) & 0xFF);
                    byte redLighting = (byte) (bgrValue & 0xFF);

                    short naiveBlue = (short) (((blueLighting & 0xFF) - 0x80) & 0xFF);
                    short naiveGreen = (short) (((greenLighting & 0xFF) - 0x80) & 0xFF);
                    short realRed = (short) (((redLighting & 0xFF) - 0x80) & 0xFF);
                    int greenOverflow = (realRed >= 0x80) ? 1 : 0;
                    int blueOverflow = (naiveGreen >= 0x80 || ((naiveGreen - greenOverflow) & 0xFF) >= 0x80) ? 1 : 0;
                    short realGreen = (short) ((naiveGreen - greenOverflow) & 0xFF);
                    short realBlue = (short) ((naiveBlue - blueOverflow) & 0xFF);
                    int finalBgrValue = (((realBlue + 0x80) << 16) + ((realGreen + 0x80) << 8) + (realRed + 0x80));
                    if ((bgrValue & 0xFFFFFF) != (finalBgrValue & 0xFFFFFF))
                        Utils.getInstanceLogger().warning("Color mismatch! [%02X,%02X,%02X/%06X] -> [%02X,%02X,%02X] -> [%02X,%02X,%02X/%06X] (Naive Green: %02X, Green Overflow: %d, Naive Blue: %02X, Blue Overflow: %d)", blue, green, red, bgrValue, blueLighting & 0xFF, greenLighting & 0xFF, redLighting & 0xFF, realBlue, realGreen, realRed, finalBgrValue, naiveGreen, greenOverflow, naiveBlue, blueOverflow);
                }
            }
        }
    }
}