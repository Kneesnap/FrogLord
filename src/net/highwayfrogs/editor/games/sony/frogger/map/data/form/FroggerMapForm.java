package net.highwayfrogs.editor.games.sony.frogger.map.data.form;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquareFlag;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketForm;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapFormManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
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
import java.util.List;
import java.util.logging.Level;

/**
 * Reads the "FORM" struct.
 * Appears to be entity collision info, like being able to walk on logs or birds.
 * TODO: We need to create a 3D preview for this. Perhaps allow saving it to a separate file too, so it can be shared between levels.
 *  - I'm thinking we make it pop up a new window which has a 3D preview of the model with the form data shown in 3D space. Use rotation mode instead of FPS camera (orr will that break with rotations?)
 * Created by Kneesnap on 8/23/2018.
 */
@Getter
public class FroggerMapForm extends SCGameData<FroggerGameInstance> {
    private final FroggerMapFile mapFile;
    private short xGridSquareCount; // Number of x grid squares in this form. This value is sometimes garbage, even in the retail game (SUB3.MAP, Both PC & PSX).
    private short zGridSquareCount; // Number of z grid squares in this form. This value is sometimes garbage, even in the retail game (SUB3.MAP, Both PC & PSX).
    private short xOffset; // Offset to bottom left or grid from entity origin.
    private short zOffset; // Offset to bottom left or grid from entity origin.
    private final List<FroggerMapFormData> formDataEntries = new ArrayList<>();

    public static final int GRID_PIXELS = 20;

    public FroggerMapForm(FroggerMapFile mapFile) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
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
            getLogger().warning("Form has " + formDataEntryCount + " FroggerMapFormData entries. Later builds of Frogger will ignore them.");

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

            requireReaderIndex(reader, nextFormDataEntryStartAddress, "Expected FroggerMapFormData list entry " + i);
            FroggerMapFormData newFormDataEntry = new FroggerMapFormData(this);
            this.formDataEntries.add(newFormDataEntry);
            newFormDataEntry.load(reader);
        }

        if (this.formDataEntries.size() > 0 && (this.xGridSquareCount <= 0 || this.zGridSquareCount <= 0))
            getLogger().warning("Contained a form data entry while having dimensions of [%d, %d]", this.xGridSquareCount, this.zGridSquareCount);
    }

    /**
     * Gets the index of this form.
     */
    public int getFormIndex() {
        FroggerMapFilePacketForm formPacket = this.mapFile.getFormPacket();
        return formPacket.getLoadingIndex(formPacket.getForms(), this);
    }

    /**
     * Gets the logger information.
     */
    public String getLoggerInfo() {
        return this.mapFile != null ? this.mapFile.getFileDisplayName() + "|MapForm{" + getFormIndex() + "}" : Utils.getSimpleName(this);
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), FroggerMapForm::getLoggerInfo, this);
    }

    @Override
    public void save(DataWriter writer) {
        if (this.formDataEntries.size() > 0 && (this.xGridSquareCount <= 0 || this.zGridSquareCount <= 0))
            Utils.handleProblem(ProblemResponse.CREATE_POPUP, getLogger(), Level.WARNING, "Map form %d in %s contains a form data entry while having dimensions of [%d, %d].\nThis may cause issues with the game!", getFormIndex(), this.mapFile.getFileDisplayName(), this.xGridSquareCount, this.zGridSquareCount);

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
    public void setupEditor(FroggerUIMapFormManager manager, GUIEditorGrid editor) {
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

                this.formDataEntries.add(new FroggerMapFormData(this));
                if (manager != null)
                    manager.updateEditor();
            });
        } else {
            for (int i = 0; i < this.formDataEntries.size(); i++) {
                FroggerMapFormData formDataEntry = this.formDataEntries.get(i);
                formDataEntry.setupEditor(this, editor);
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
     * Make a display for this form.
     * @param selectedIndex The selected index.
     * @return image
     */
    public Image makeDataImage(int selectedIndex) {
        if (this.formDataEntries.isEmpty())
            throw new RuntimeException("Tried to create FroggerMapFormData image without FroggerMapFormData.");

        FroggerMapFormData formDataEntry = this.formDataEntries.get(0);
        BufferedImage newImage = new BufferedImage(getXGridSquareCount() * GRID_PIXELS, getZGridSquareCount() * GRID_PIXELS, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = newImage.createGraphics();

        // Fill background.
        graphics.setColor(Color.GRAY);
        graphics.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());

        for (int x = 0; x < getXGridSquareCount(); x++) {
            for (int z = 0; z < getZGridSquareCount(); z++) {
                int index = getIndex(x, z, getXGridSquareCount());

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
    public static int getIndex(int x, int z, int xCount) {
        return (z * xCount) + x;
    }
}