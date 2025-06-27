package net.highwayfrogs.editor.games.sony.frogger.map.data.form;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquareFlag;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Reads the "FORM" struct.
 * Represents a collision grid on an entity, to control how the player interacts with the given entity.
 * TODO: We need to create a 3D preview for this. Perhaps allow saving it to a separate file too, so it can be shared between levels.
 *  - I'm thinking we make it pop up a new window which has a 3D preview of the model with the form data shown in 3D space. Use rotation mode instead of FPS camera (orr will that break with rotations?)
 * Created by Kneesnap on 8/23/2018.
 */
public class FroggerFormGrid extends SCGameData<FroggerGameInstance> {
    @Getter private final FroggerMapFile mapFile; // If the form grid is linked to a particular map file, it is placed here.
    @Getter private short xGridSquareCount; // Number of x grid squares in this form. This value is sometimes garbage, even in the retail game (SUB3.MAP, Both PC & PSX). TODO: On change, update the dependant data entries.
    @Getter private short zGridSquareCount; // Number of z grid squares in this form. This value is sometimes garbage, even in the retail game (SUB3.MAP, Both PC & PSX).
    @Getter private short xOffset; // Offset to bottom left or grid from entity origin.
    @Getter private short zOffset; // Offset to bottom left or grid from entity origin.
    private final List<FroggerFormGridData> formDataEntries = new ArrayList<>();
    private final List<FroggerFormGridData> immutableFormDataEntries = Collections.unmodifiableList(this.formDataEntries);
    @Getter private transient IFroggerFormEntry formEntry; // The form entry which is used by this map form. TODO: Perhaps enforce this when choosing what forms to apply to an entity, and whatnot.

    public static final int GRID_PIXELS = 20;

    public FroggerFormGrid(FroggerGameInstance instance) {
        super(instance);
        this.mapFile = null;
    }

    public FroggerFormGrid(@NonNull FroggerMapFile mapFile) {
        super(mapFile.getGameInstance());
        this.mapFile = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        int formDataEntryCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Max Y, Runtime variable.
        this.xGridSquareCount = reader.readShort();
        this.zGridSquareCount = reader.readShort();
        this.xOffset = reader.readShort();
        this.zOffset = reader.readShort();

        // Warn if we've got an unexpected number of form data entries.
        // The game only supports 1 form data in the retail build even if the data structure can hold more. It appears more than 1 was supported in build 20 though.
        // I don't know if them being supported means the game used them though.
        if (formDataEntryCount != 0 && formDataEntryCount != 1)
            getLogger().warning("Form has %d FroggerFormGridData entries. Later builds of Frogger will ignore them.", formDataEntryCount);

        // Skip pointer table.
        int formDataPointerList = reader.getIndex();
        reader.skipBytes(formDataEntryCount * Constants.POINTER_SIZE);

        // Read form data entries.
        this.formDataEntries.clear();
        for (int i = 0; i < formDataEntryCount; i++) {
            // Read the pointer table to verify the start of the next form data entry.
            reader.jumpTemp(formDataPointerList);
            int nextFormDataEntryStartAddress = reader.readInt();
            formDataPointerList = reader.getIndex();
            reader.jumpReturn();

            requireReaderIndex(reader, nextFormDataEntryStartAddress, "Expected FroggerFormGridData list entry " + i);
            FroggerFormGridData newFormDataEntry = new FroggerFormGridData(this);
            this.formDataEntries.add(newFormDataEntry);
            newFormDataEntry.load(reader);
        }

        if (this.formDataEntries.size() > 0 && (this.xGridSquareCount <= 0 || this.zGridSquareCount <= 0))
            getLogger().warning("Contained a form data entry while having dimensions of [%d, %d]", this.xGridSquareCount, this.zGridSquareCount);
    }

    @Override
    public void save(DataWriter writer) {
        if (!isEmpty() && (this.xGridSquareCount <= 0 || this.zGridSquareCount <= 0 || this.xGridSquareCount > 255 || this.zGridSquareCount > 255))
            Utils.handleProblem(ProblemResponse.CREATE_POPUP, getLogger(), Level.WARNING, "Form grid for %s contains a form data entry while having dimensions of [%d, %d].\nThis may cause issues with the game!", this.formEntry != null ? this.formEntry.getFormTypeName() : "UNKNOWN_FORM_TYPE", this.xGridSquareCount, this.zGridSquareCount);

        writer.writeUnsignedShort(this.formDataEntries.size());
        writer.writeNull(Constants.SHORT_SIZE); // Runtime value.
        writer.writeShort(this.xGridSquareCount);
        writer.writeShort(this.zGridSquareCount);
        writer.writeShort(this.xOffset);
        writer.writeShort(this.zOffset);

        // Write placeholder table.
        int formDataPointerList = writer.getIndex();
        for (int i = 0; i < this.formDataEntries.size(); i++)
            writer.writeNullPointer();

        // Write form data entries.
        for (int i = 0; i < this.formDataEntries.size(); i++) {
            // Write the pointer table to the start of the next form data entry.
            int nextFormDataEntryStartAddress = writer.getIndex();
            writer.jumpTemp(formDataPointerList);
            writer.writeInt(nextFormDataEntryStartAddress);
            formDataPointerList = writer.getIndex();
            writer.jumpReturn();

            // Write form data entry.
            this.formDataEntries.get(i).save(writer);
        }
    }

    /**
     * Tests if the form grid data matches that of another form grid.
     * @param other the other form grid to test
     * @return true if the form grid data matches
     */
    public boolean doesFormGridDataMatch(FroggerFormGrid other) {
        if (other == null)
            return false;

        if (isEmpty() != other.isEmpty()) {
            return false;
        } else if (isEmpty() && other.isEmpty()) {
            return true;
        }

        return this.xGridSquareCount == other.xGridSquareCount && this.zGridSquareCount == other.zGridSquareCount
                && this.xOffset == other.xOffset && this.zOffset == other.zOffset
                && this.formDataEntries.equals(other.formDataEntries);
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), FroggerFormGrid::getLoggerInfo, this);
    }

    /**
     * Gets the logger information.
     */
    public String getLoggerInfo() {
        return (this.mapFile != null ? this.mapFile.getFileDisplayName() + "|" : "")
                + "FormGrid{" + (this.formEntry != null ? this.formEntry.getFormTypeName() : "Unknown Entity")
                + (this.mapFile != null ? ",formIndex=" + this.mapFile.getFormPacket().getForms().indexOf(this) : "") + "}";
    }

    /**
     * Gets the available form data entries.
     */
    public List<FroggerFormGridData> getFormDataEntries() {
        return this.immutableFormDataEntries;
    }

    /**
     * Checks if this form is shared across all usages of the form, instead of being locally defined in a specific map.
     */
    public boolean isGlobal() {
        return this.mapFile == null && this.formEntry != null && this == this.formEntry.getFormGrid();
    }

    /**
     * The form is considered empty if it has no data entries.
     * When a form is empty, its data may be garbage/meaningless.
     */
    public boolean isEmpty() {
        return this.formDataEntries.isEmpty();
    }

    /**
     * Sets which form entry the grid is appropriate
     * @param newEntry the form entry associated with this form grid.
     * @return The form grid which should replace this object instance, if there is one.
     */
    public FroggerFormGrid initFormEntry(IFroggerFormEntry newEntry) {
        if (newEntry == this.formEntry)
            return this;

        if (this.formEntry != null)
            throw new IllegalStateException("Changing the formEntry of a formGrid is not permitted once it has been applied.");

        this.formEntry = newEntry;

        FroggerFormGrid globalFormGrid = newEntry.getFormGrid();
        if (globalFormGrid == null) {
            // Create a copy of this form, and apply it to the form book.
            FroggerFormGrid newFormGrid = DataUtils.cloneSerializableObject(this, new FroggerFormGrid(getGameInstance()));
            newEntry.setFormGrid(newFormGrid);
            newFormGrid.initFormEntry(newEntry);
            return newFormGrid;
        } else if (this.doesFormGridDataMatch(globalFormGrid)) {
            return globalFormGrid;
        } else {
            // This isn't the same as the one seen in the form grid.
            if (!getMapFile().isExtremelyEarlyMapFormat())
                getLogger().warning("The form grid does not match the one found in the form book. This should work fine, but is not expected. (Unless this is a modified version of the game.)");
            return this;
        }
    }

    /**
     * Gets the x offset as a floating point number.
     */
    public float getXOffsetAsFloat() {
        return DataUtils.fixedPointShortToFloat4Bit(this.xOffset);
    }

    /**
     * Gets the z offset as a floating point number.
     */
    public float getZOffsetAsFloat() {
        return DataUtils.fixedPointShortToFloat4Bit(this.zOffset);
    }

    /**
     * Setup a form editor.
     * @param manager the form UI manager
     * @param editor The editor to setup under.
     */
    public void setupEditor(MeshUIManager<? extends FroggerMapMesh> manager, GUIEditorGrid editor) {
        editor.addBoldNormalLabel("Form Type:", isGlobal() ? "Shared globally" : "Local to this map");
        editor.addBoldNormalLabel("Used By:", this.formEntry != null ? this.formEntry.getFormTypeName() : "None");

        if (this.formDataEntries.isEmpty()) // The data shown here is ignored by the game when there is no form data. So the garbage values we see are fine.
            editor.addBoldLabel("Data ignored, as there is no form data.");

        // Grid Counts:
        editor.addSignedShortField("Grid Width (X)", this.xGridSquareCount, newX -> newX > 0 || this.formDataEntries.isEmpty(), newX -> {
            setXGridSquareCount(newX);
            if (manager != null)
                manager.updateEditor();
        });

        editor.addSignedShortField("Grid Length (Z)", this.zGridSquareCount, newZ -> newZ > 0 || this.formDataEntries.isEmpty(), newZ -> {
            setZGridSquareCount(newZ);
            if (manager != null)
                manager.updateEditor();
        });

        // Allow changing offsets.
        editor.addFixedShort("Origin Offset X", this.xOffset, newXOffset -> this.xOffset = newXOffset);
        editor.addFixedShort("Origin Offset Z", this.zOffset, newZOffset -> this.zOffset = newZOffset);

        // Add Form Data.
        if (this.formDataEntries.isEmpty()) {
            editor.addButton("Add Form Data", () -> {
                if (this.xGridSquareCount <= 0 || this.zGridSquareCount <= 0) {
                    FXUtils.makePopUp("Grid Counts must be positive non-zero numbers!", AlertType.WARNING);
                    return;
                }

                this.formDataEntries.add(new FroggerFormGridData(this));
                if (manager != null)
                    manager.updateEditor();
            });
        } else {
            for (int i = 0; i < this.formDataEntries.size(); i++) {
                FroggerFormGridData formDataEntry = this.formDataEntries.get(i);
                formDataEntry.setupEditor(editor);
                editor.addButton("Remove Form Data #" + (i + 1), () -> {
                    this.formDataEntries.remove(formDataEntry);
                    if (manager != null)
                        manager.updateEditor();
                });
            }
        }
    }

    /**
     * Changes the X size of this form.
     * @param newXCount New size.
     */
    public void setXGridSquareCount(short newXCount) {
        for (int i = 0; i < this.formDataEntries.size(); i++)
            this.formDataEntries.get(i).resizeGrid(newXCount, this.zGridSquareCount);

        this.xGridSquareCount = newXCount;
    }

    /**
     * Changes the Z size of this form.
     * @param newZCount New size.
     */
    public void setZGridSquareCount(short newZCount) {
        for (int i = 0; i < this.formDataEntries.size(); i++)
            this.formDataEntries.get(i).resizeGrid(this.xGridSquareCount, newZCount);

        this.zGridSquareCount = newZCount;
    }

    /**
     * Get the X coordinate from the flag index.
     * @param index The index to use.
     * @return x
     */
    public int getXFromIndex(int index) {
        return (index % getXGridSquareCount());
    }

    /**
     * Get the Z coordinate from the flag index.
     * @param index The index to use.
     * @return z
     */
    public int getZFromIndex(int index) {
        return (index / getXGridSquareCount());
    }

    /**
     * Creates a copy of the form grid for the given map file.
     * @param newMapFile the new map file to copy the data to
     * @return clonedFormGrid
     */
    public FroggerFormGrid clone(FroggerMapFile newMapFile) {
        return DataUtils.cloneSerializableObject(this, new FroggerFormGrid(newMapFile));
    }

    /**
     * Make a display for this form.
     * @param selectedIndex The selected index.
     * @return image
     */
    public Image makeDataImage(int selectedIndex) {
        if (this.formDataEntries.isEmpty())
            throw new RuntimeException("Tried to create FroggerFormGridData image without FroggerFormGridData.");

        FroggerFormGridData formDataEntry = this.formDataEntries.get(0);
        BufferedImage newImage = new BufferedImage(getXGridSquareCount() * GRID_PIXELS, getZGridSquareCount() * GRID_PIXELS, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = newImage.createGraphics();

        // Fill background.
        graphics.setColor(Color.GRAY);
        graphics.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());

        for (int x = 0; x < getXGridSquareCount(); x++) {
            for (int z = 0; z < getZGridSquareCount(); z++) {
                int index = getFormGridIndex(x, z, getXGridSquareCount());

                int startX = (x * GRID_PIXELS);
                int startY = ((getZGridSquareCount() - 1 - z) * GRID_PIXELS);
                int width = GRID_PIXELS;
                int height = GRID_PIXELS;

                // Draw black outline.
                graphics.setColor(Color.BLACK);
                graphics.drawRect(startX++, startY++, width, height);
                width -= 2;
                height -= 2;

                // Draw remaining parts.
                if (index == selectedIndex) { // Filling in the square not only makes it clear what you have selected, but means we don't need to update the image every time we change a flag.
                    graphics.setColor(Color.GREEN);
                    graphics.fillRect(startX, startY, width, height);
                } else {
                    // Draw flag values. (Makes it easier to identify which squares have similar flags.)
                    short flags = formDataEntry.getGridSquareFlags(index);
                    FroggerMapFormSquareReaction reaction = FroggerMapFormSquareReaction.getReactionFromFlags(flags);
                    if (reaction != null) {
                        graphics.setColor(reaction.getPreviewColor());
                        graphics.drawRect(startX, startY, width, height);
                    } else {
                        for (int i = 0; i < FroggerGridSquareFlag.values().length; i++) {
                            FroggerGridSquareFlag flag = FroggerGridSquareFlag.values()[i];
                            if (!flag.isFormData() || flag.getUiColor() == null)
                                continue;

                            if (width < 0 || height < 0)
                                break; // Can't display anymore.
                            if ((flags & flag.getBitFlagMask()) != flag.getBitFlagMask())
                                continue; // Flag didn't match.

                            graphics.setColor(flag.getUiColor());
                            graphics.drawRect(startX++, startY++, width, height);
                            width -= 2;
                            height -= 2;
                        }
                    }
                }
            }
        }

        graphics.dispose();
        return FXUtils.toFXImage(newImage, false);
    }

    /**
     * Calculates the index of a grid flag.
     * @return index
     */
    public static int getFormGridIndex(int x, int z, int xCount) {
        return (z * xCount) + x;
    }
}
