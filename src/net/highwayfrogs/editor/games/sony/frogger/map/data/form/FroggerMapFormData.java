package net.highwayfrogs.editor.games.sony.frogger.map.data.form;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquareFlag;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Parses the "FORM_DATA" struct.
 * Created by Kneesnap on 8/23/2018.
 */
@Getter
public class FroggerMapFormData extends SCGameData<FroggerGameInstance> {
    private final FroggerMapForm form;
    @Setter private short height; // This is for if heightType is one height for the entire grid.
    private short[][] gridFlags; // Believe this is ordered (z * xSize) + x

    private static final short FORM_HEIGHT_TYPE = (short) 0;

    public FroggerMapFormData(FroggerMapForm form) {
        super(form.getGameInstance());
        this.gridFlags = new short[form.getZGridSquareCount()][form.getXGridSquareCount()];
        this.form = form;
    }

    @Override
    public void load(DataReader reader) {
        short formHeightType = reader.readShort(); // There appears to have been several unfinished options for height, and are not used. We do not support them.
        Utils.verify(formHeightType == FORM_HEIGHT_TYPE, "Unsupported Form Height Type: %d.", formHeightType);

        int expectedGridSquareHeightsPointer = reader.getIndex();
        this.height = reader.readShort();
        int gridSquareFlagsPointer = reader.readInt(); // Pointer to array of (xCount * zCount) flags. (Type = short)
        int gridSquareHeightsPointer = reader.readInt(); // Pointer to an array of grid heights. This would have been used in the "SQUARE" height mode, however that does not appear to be used in the vanilla game.

        // Validate expectations for grid square heights. (This is more of a sanity check)
        if (expectedGridSquareHeightsPointer != gridSquareHeightsPointer)
            getLogger().warning("Expected the GridSquare height list to point to the single height at " + NumberUtils.toHexString(expectedGridSquareHeightsPointer) + ", but it actually pointed to " + NumberUtils.toHexString(gridSquareHeightsPointer));

        // Read grid square flag list, and warn if we don't recognize them.
        requireReaderIndex(reader, gridSquareFlagsPointer, "Expected GridSquareFlag list");
        for (int z = 0; z < this.gridFlags.length; z++) {
            for (int x = 0; x < this.gridFlags[z].length; x++) {
                short gridSquareFlags = this.gridFlags[z][x] = reader.readShort();
                FroggerMapFormSquareReaction reaction = FroggerMapFormSquareReaction.getReactionFromFlags(gridSquareFlags);
                if (reaction == null)
                    getLogger().warning("Grid Square[z=" + z + ",x=" + x + "] contains an unrecognized grid square flag reaction! (Value: " + NumberUtils.toHexString(DataUtils.shortToUnsignedInt(gridSquareFlags)) + ")");
            }
        }

        // An extra short here is often not empty. It seems to usually (but not always) increment in intervals of 0x20 when two forms are similar.
        // Additionally, similar levels (ORG1/ORG2/ORG3/ORG4/ORG5) do not seem to have consistent values even though they are pretty much the same level.
        // Also, the form data seems to be 0x20 bytes long.
        // My current guess is that this is the least significant u16 part of a pointer which existed in the program exporting the map file.
        // This pointer was probably to some form data (current? next? unsure), but it's hard to tell. And it could even be malloc'd memory which didn't get cleared meaning this might not even be consistent.
        // It also appears that it's possible for a value to be shared between different levels, but it's rare. It suggests maps were exported in batch too.
        // Since the game doesn't use this, it is safe to ignore this value.
        reader.align(Constants.INTEGER_SIZE); // Padding.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(FORM_HEIGHT_TYPE);
        int gridSquareHeightsPointer = writer.getIndex();
        writer.writeShort(this.height);
        int gridSquareFlagsPointer = writer.writeNullPointer(); // Pointer to array of (xCount * zCount) flags. (Type = short)
        writer.writeInt(gridSquareHeightsPointer); // Pointer to an array of grid heights. This would have been used in the "SQUARE" height mode, however that does not appear to be used in the vanilla game.

        // Write grid square flag list.
        writer.writeAddressTo(gridSquareFlagsPointer);
        for (int z = 0; z < this.gridFlags.length; z++)
            for (int x = 0; x < this.gridFlags[z].length; x++)
                writer.writeShort(this.gridFlags[z][x]);
        writer.align(Constants.INTEGER_SIZE); // Padding.
    }

    /**
     * Gets the index of this form.
     */
    public int getFormDataIndex() {
        return this.form.getFormDataEntries().indexOf(this);
    }

    /**
     * Gets the logger information.
     */
    public String getLoggerInfo() {
        return this.form != null ? this.form.getLoggerInfo() + "|MapFormData{" + getFormDataIndex() + "}" : Utils.getSimpleName(this);
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), FroggerMapFormData::getLoggerInfo, this);
    }

    /**
     * Gets the flags of a grid square at the given index position.
     * @param gridIndex the index into the grid squares which the coordinates can be calculated from
     */
    public short getGridSquareFlags(int gridIndex) {
        return getGridSquareFlags(gridIndex % this.form.getXGridSquareCount(), gridIndex / this.form.getXGridSquareCount());
    }

    /**
     * Gets the flags of a grid square at the given xy position.
     * @param gridX the grid x coordinate of the grid square
     * @param gridZ the grid z coordinate of the grid square
     */
    public short getGridSquareFlags(int gridX, int gridZ) {
        if (gridX < 0 || gridX >= this.form.getXGridSquareCount())
            throw new ArrayIndexOutOfBoundsException("Invalid Grid X coordinate: " + gridX);
        if (gridZ < 0 || gridZ >= this.form.getZGridSquareCount())
            throw new ArrayIndexOutOfBoundsException("Invalid Grid Z coordinate: " + gridZ);

        return this.gridFlags[gridZ][gridX];
    }

    /**
     * Gets the flag state of a grid square at the given index position.
     * @param gridIndex the index into the grid squares which the coordinates can be calculated from
     * @param flag the flag to set the state with
     */
    public boolean getGridSquareFlagState(int gridIndex, FroggerGridSquareFlag flag) {
        return getGridSquareFlagState(gridIndex % this.form.getXGridSquareCount(), gridIndex / this.form.getXGridSquareCount(), flag);
    }

    /**
     * Gets the flag state of a grid square at the given xy position.
     * @param gridX the grid x coordinate of the grid square
     * @param gridZ the grid z coordinate of the grid square
     * @param flag the flag to set the state with
     */
    public boolean getGridSquareFlagState(int gridX, int gridZ, FroggerGridSquareFlag flag) {
        if (gridX < 0 || gridX >= this.form.getXGridSquareCount())
            throw new ArrayIndexOutOfBoundsException("Invalid Grid X coordinate: " + gridX);
        if (gridZ < 0 || gridZ >= this.form.getZGridSquareCount())
            throw new ArrayIndexOutOfBoundsException("Invalid Grid Z coordinate: " + gridZ);
        if (flag == null)
            throw new NullPointerException("flag");
        if (!flag.isFormData())
            throw new IllegalArgumentException("FroggerGridSquareFlag " + flag.name() + " is not applicable to FroggerMapFormData.");

        return (this.gridFlags[gridZ][gridX] & flag.getBitFlagMask()) == flag.getBitFlagMask();
    }

    /**
     * Sets the flag state of a grid square at the given index position.
     * @param gridIndex the index into the grid squares which the coordinates can be calculated from
     * @param flags the flags to apply
     */
    public void setGridSquareFlags(int gridIndex, short flags) {
        setGridSquareFlags(gridIndex % this.form.getXGridSquareCount(), gridIndex / this.form.getXGridSquareCount(), flags);
    }

    /**
     * Sets the flag state of a grid square at the given xy position.
     * @param gridX the grid x coordinate of the grid square
     * @param gridZ the grid z coordinate of the grid square
     * @param flags the flags to apply
     */
    public void setGridSquareFlags(int gridX, int gridZ, short flags) {
        if (gridX < 0 || gridX >= this.form.getXGridSquareCount())
            throw new ArrayIndexOutOfBoundsException("Invalid Grid X coordinate: " + gridX);
        if (gridZ < 0 || gridZ >= this.form.getZGridSquareCount())
            throw new ArrayIndexOutOfBoundsException("Invalid Grid Z coordinate: " + gridZ);

        this.gridFlags[gridZ][gridX] = flags;
    }

    /**
     * Sets the flag state of a grid square at the given index position.
     * @param gridIndex the index into the grid squares which the coordinates can be calculated from
     * @param flag the flag to set the state with
     * @param newState the state of the flag to apply
     */
    public void setGridSquareFlagState(int gridIndex, FroggerGridSquareFlag flag, boolean newState) {
        setGridSquareFlagState(gridIndex % this.form.getXGridSquareCount(), gridIndex / this.form.getXGridSquareCount(), flag, newState);
    }

    /**
     * Sets the flag state of a grid square at the given xy position.
     * @param gridX the grid x coordinate of the grid square
     * @param gridZ the grid z coordinate of the grid square
     * @param flag the flag to set the state with
     * @param newState the state of the flag to apply
     */
    public void setGridSquareFlagState(int gridX, int gridZ, FroggerGridSquareFlag flag, boolean newState) {
        if (gridX < 0 || gridX >= this.form.getXGridSquareCount())
            throw new ArrayIndexOutOfBoundsException("Invalid Grid X coordinate: " + gridX);
        if (gridZ < 0 || gridZ >= this.form.getZGridSquareCount())
            throw new ArrayIndexOutOfBoundsException("Invalid Grid Z coordinate: " + gridZ);
        if (flag == null)
            throw new NullPointerException("flag");
        if (!flag.isFormData())
            throw new IllegalArgumentException("FroggerGridSquareFlag " + flag.name() + " is not applicable to FroggerMapFormData.");

        if (newState) {
            this.gridFlags[gridZ][gridX] |= (short) flag.getBitFlagMask();
        } else {
            this.gridFlags[gridZ][gridX] &= (short) ~flag.getBitFlagMask();
        }
    }

    /**
     * Resize the grid to the new size, keeping old flags.
     * This should only be called by FroggerMapForm, which is able to do it before it has its internal data update.
     * @param newX the new x grid size
     * @param newZ the new z grid size
     */
    void resizeGrid(int newX, int newZ) {
        int oldX = this.form.getXGridSquareCount();
        int oldZ = this.form.getZGridSquareCount();
        if (oldX == newX && oldZ == newZ)
            return;

        // Create new grid flags and copy flags.
        short[][] newGridFlags = new short[newZ][newX];
        int maxX = Math.min(oldX, newX);
        int maxZ = Math.min(oldZ, newZ);
        for (int z = 0; z < maxZ; z++)
            System.arraycopy(this.gridFlags[z], 0, newGridFlags[z], 0, maxX);

        this.gridFlags = newGridFlags;
    }

    /**
     * Setup a form editor.
     * @param editor The editor to setup under.
     */
    public void setupEditor(FroggerMapForm form, GUIEditorGrid editor) {
        AtomicInteger selectedIndex = new AtomicInteger();

        editor.addBoldLabel("Form Data:");
        ImageView view = getGridFlags().length > 0 ? editor.addCenteredImage(form.makeDataImage(selectedIndex.get())) : null;

        editor.addSignedShortField("Height", getHeight(), this::setHeight);
        if (getGridFlags().length == 0)
            return;

        List<Integer> flagIndexList = Utils.getIntegerList(this.form.getXGridSquareCount() * this.form.getZGridSquareCount());
        Map<FroggerGridSquareFlag, CheckBox> flagToggles = new HashMap<>();
        ComboBox<FroggerMapFormSquareReaction>[] selectionBox = new ComboBox[1];

        assert view != null; // Fixes IDE error checking.

        ComboBox<Integer> tileId = editor.addSelectionBox("Tile (Bottom Left)", selectedIndex.get(), flagIndexList, newIndex -> {
            selectedIndex.set(newIndex);
            for (Entry<FroggerGridSquareFlag, CheckBox> entry : flagToggles.entrySet()) // Update checkboxes.
                entry.getValue().setSelected(getGridSquareFlagState(newIndex, entry.getKey()));

            if (selectionBox[0] != null) {
                FroggerMapFormSquareReaction reaction = FroggerMapFormSquareReaction.getReactionFromFlags(getGridSquareFlags(newIndex));
                if (reaction != null)
                    selectionBox[0].setValue(reaction);
            }
            view.setImage(form.makeDataImage(newIndex));
        });
        tileId.setConverter(new AbstractStringConverter<>(index -> "Tile " + index + " (X: " + form.getXFromIndex(index) + ", Z: " + form.getZFromIndex(index) + ")"));

        // Handles clicking in the form view.
        view.setOnMouseClicked(evt -> {
            int formGridX = (int) (evt.getX() / FroggerMapForm.GRID_PIXELS);
            int formGridZ = form.getZGridSquareCount() - 1 - (int) (evt.getY() / FroggerMapForm.GRID_PIXELS);
            int newIndex = FroggerMapForm.getIndex(formGridX, formGridZ, form.getXGridSquareCount());
            tileId.getSelectionModel().select(newIndex);
            tileId.setValue(newIndex);
        });

        FroggerMapFormSquareReaction reaction = FroggerMapFormSquareReaction.getReactionFromFlags(getGridSquareFlags(selectedIndex.get()));
        if (reaction != null) {
            ComboBox<FroggerMapFormSquareReaction> comboBox = editor.addEnumSelector("Player Reaction", reaction, FroggerMapFormSquareReaction.values(), false, newReaction -> setGridSquareFlags(selectedIndex.get(), newReaction.getGridSquareFlagBitMask()));
            comboBox.setConverter(new AbstractStringConverter<>(FroggerMapFormSquareReaction::getDisplayName));
            selectionBox[0] = comboBox;
        } else {
            // Add fallback checkboxes if we don't recognize the reaction.
            boolean right = false;
            for (FroggerGridSquareFlag flag : FroggerGridSquareFlag.values()) {
                if (!flag.isFormData())
                    continue;

                CheckBox box = new CheckBox(StringUtils.capitalize(flag.name()));
                box.setSelected(getGridSquareFlagState(selectedIndex.get(), flag));
                box.selectedProperty().addListener((listener, oldVal, newState) ->
                        setGridSquareFlagState(selectedIndex.get(), flag, newState));

                if (right) {
                    editor.setupSecondNode(box, false);
                    editor.addRow(20);
                } else {
                    editor.setupNode(box);
                }

                flagToggles.put(flag, box);
                right = !right;
            }
        }
    }
}