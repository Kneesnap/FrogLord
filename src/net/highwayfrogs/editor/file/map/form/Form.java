package net.highwayfrogs.editor.file.map.form;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.grid.GridSquareFlag;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.map.manager.FormManager;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Reads the "FORM" struct.
 * Appears to be entity collision info, like being able to walk on logs or birds.
 * Created by Kneesnap on 8/23/2018.
 */
@Getter
@Setter
public class Form extends GameObject {
    private short xGridSquareCount; // Number of x grid squares in this form.
    private short zGridSquareCount; // Number of z grid squares in this form.
    private short xOffset; // Offset to bottom left or grid from entity origin.
    private short zOffset; // Offset to bottom left or grid from entity origin.
    private FormData data; // Null is allowed.

    public static final int GRID_PIXELS = 20;

    @Override
    public void load(DataReader reader) {
        int dataCount = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Max Y, Runtime variable.
        this.xGridSquareCount = reader.readShort();
        this.zGridSquareCount = reader.readShort();
        this.xOffset = reader.readShort();
        this.zOffset = reader.readShort();

        if (dataCount == 0) {
            this.xGridSquareCount = -1;
            this.zGridSquareCount = -1;
            this.xOffset = -1;
            this.zOffset = -1;
            return; // There is no form data.
        }

        Utils.verify(dataCount == 1, "Invalid Form Data Count: " + dataCount); // The game only supports 1 form data even if it has a count for more.

        reader.jumpTemp(reader.readInt()); // Form Data Pointer.
        this.data = new FormData(this);
        data.load(reader);
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(hasData() ? 1 : 0);
        writer.writeShort((short) 0);
        writer.writeShort(this.xGridSquareCount);
        writer.writeShort(this.zGridSquareCount);
        writer.writeShort(this.xOffset);
        writer.writeShort(this.zOffset);

        if (hasData()) { // Save the form data if there's any.
            int dataPointer = writer.writeNullPointer();
            writer.writeAddressTo(dataPointer); // We don't write this if we don't have data. I forget why
            getData().save(writer);
        }
    }

    /**
     * Test if this form has form data.
     * @return hasFormData
     */
    public boolean hasData() {
        return getData() != null;
    }

    /**
     * Setup a form editor.
     * @param editor The editor to setup under.
     */
    public void setupEditor(FormManager manager, GUIEditorGrid editor) {
        editor.addBoldLabel("Form:");

        // Grid Counts:
        editor.addShortField("X Grid Count", getXGridSquareCount(), newX -> {
            setXGridSquareCount(newX);
            manager.setupEditor();
        }, null);

        editor.addShortField("Z Grid Count", getZGridSquareCount(), newZ -> {
            setZGridSquareCount(newZ);
            manager.setupEditor();
        }, null);

        // Allow changing offsets.
        editor.addShortField("xOffset", getXOffset(), this::setXOffset, null);
        editor.addShortField("zOffset", getZOffset(), this::setZOffset, null);

        // Add Form Data.
        if (hasData()) {
            getData().setupEditor(this, editor);
            editor.addButton("Remove Form Data", () -> {
                setData(null);
                manager.setupEditor();
            });
        } else {
            editor.addButton("Add Form Data", () -> {
                if (getXGridSquareCount() <= 0 || getZGridSquareCount() <= 0) {
                    System.out.println("Grid Counts must be positive non-zero numbers!"); // Might want to make this a popup in the future.
                    return;
                }

                setData(new FormData(this));
                manager.setupEditor();
            });
        }
    }

    /**
     * Changes the X size of this form.
     * @param newXCount New size.
     */
    public void setXGridSquareCount(short newXCount) {
        if (hasData()) {
            int[] oldFlags = getData().getGridFlags();
            int[] newFlags = new int[newXCount * getZGridSquareCount()];

            for (int i = 0; i < oldFlags.length; i++) {
                int x = getXFromIndex(i);
                if (x >= newXCount)
                    continue;

                newFlags[getIndex(x, getZFromIndex(i), newXCount)] = oldFlags[i];
            }

            getData().setGridFlags(newFlags);
        }

        this.xGridSquareCount = newXCount;
    }

    /**
     * Changes the Z size of this form.
     * @param newZCount New size.
     */
    public void setZGridSquareCount(short newZCount) {
        if (hasData()) {
            int[] oldFlags = getData().getGridFlags();
            int[] newFlags = new int[getXGridSquareCount() * newZCount];

            for (int i = 0; i < oldFlags.length; i++) {
                int z = getZFromIndex(i);
                if (z >= newZCount)
                    continue;

                newFlags[getIndex(getXFromIndex(i), z, getXGridSquareCount())] = oldFlags[i];
            }

            getData().setGridFlags(newFlags);
        }

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
        if (!hasData())
            throw new RuntimeException("Tried to create FormData image without FormData.");

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
                    int flags = getData().getGridFlags()[index];
                    for (int i = 0; i < GridSquareFlag.values().length; i++) {
                        GridSquareFlag flag = GridSquareFlag.values()[i];
                        if (width < 0 || height < 0)
                            break; // Can't display any more.
                        if ((flags & flag.getFlag()) != flag.getFlag())
                            continue; // Flag didn't match.

                        graphics.setColor(flag.getUiColor());
                        graphics.drawRect(startX++, startY++, width, height);
                        width -= 2;
                        height -= 2;
                    }
                }
            }
        }

        graphics.dispose();
        return Utils.toFXImage(newImage, false);
    }

    /**
     * Calculates the index of a grid flag.
     * @return index
     */
    public static int getIndex(int x, int z, int xCount) {
        return (z * xCount) + x;
    }
}
