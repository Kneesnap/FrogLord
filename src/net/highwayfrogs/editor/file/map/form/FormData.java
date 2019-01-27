package net.highwayfrogs.editor.file.map.form;

import javafx.scene.control.CheckBox;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.grid.GridSquareFlag;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.AbstractStringConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Parses the "FORM_DATA" struct.
 * Created by Kneesnap on 8/23/2018.
 */
@Getter
@Setter
public class FormData extends GameObject {
    private short height; // This is for if heightType is one height for the entire grid.
    private int[] gridFlags; // Believe this is ordered (z * xSize) + x

    private static final short FORM_HEIGHT_TYPE = (short) 0;

    public FormData(Form parent) {
        this.gridFlags = new int[parent.getXGridSquareCount() * parent.getZGridSquareCount()];
    }

    @Override
    public void load(DataReader reader) {
        short formHeightType = reader.readShort(); // There appears to have been several unfinished options for height, and are not used. We do not support them.
        Utils.verify(formHeightType == FORM_HEIGHT_TYPE, "Unsupported Form Height Type: %d.", formHeightType);

        this.height = reader.readShort();
        int squarePointer = reader.readInt(); // Pointer to array of (xCount * zCount) flags. (Type = short)
        reader.readInt(); // Pointer to an array of grid heights. This would have been used in the "SQUARE" height mode, however that does not appear to be used in the vanilla game.

        reader.jumpTemp(squarePointer); // Really we don't need to jump, as the data is at the current read index, but this is to keep it in spec with the engine.
        for (int i = 0; i < gridFlags.length; i++)
            this.gridFlags[i] = reader.readUnsignedShortAsInt();
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(FORM_HEIGHT_TYPE);

        int heightsPointer = writer.getIndex();
        writer.writeShort(this.height);
        int squareFlagPointer = writer.writeNullPointer();
        writer.writeInt(heightsPointer); // Points to an unused height pointer array, mentioned above. This functionality is not used in Frogger, it may or may not be functional.

        writer.writeAddressTo(squareFlagPointer);
        for (int flag : this.gridFlags)
            writer.writeUnsignedShort(flag);
    }

    /**
     * Setup a form editor.
     * @param editor The editor to setup under.
     */
    public void setupEditor(Form form, GUIEditorGrid editor) {
        editor.addBoldLabel("Form Data:");
        editor.addShortField("Height", getHeight(), this::setHeight, null);
        if (getGridFlags().length == 0)
            return;

        List<Integer> flagIndexList = new ArrayList<>();
        for (int i = 0; i < getGridFlags().length; i++)
            flagIndexList.add(i);

        AtomicBoolean changingState = new AtomicBoolean();
        AtomicInteger selectedIndex = new AtomicInteger();
        Map<GridSquareFlag, CheckBox> flagToggles = new HashMap<>();

        editor.addSelectionBox("Tile (Top Left)", selectedIndex.get(), flagIndexList, newIndex -> {
            int x = (newIndex % form.getZGridSquareCount());
            int z = (newIndex / form.getZGridSquareCount());
            newIndex = (x * form.getZGridSquareCount()) + z;

            selectedIndex.set(newIndex);
            changingState.set(true);
            for (Entry<GridSquareFlag, CheckBox> entry : flagToggles.entrySet()) // Update checkboxes.
                entry.getValue().setSelected((this.gridFlags[newIndex] & entry.getKey().getFlag()) == entry.getKey().getFlag());
            changingState.set(false);
        }).setConverter(new AbstractStringConverter<>(index -> {
            int x = (index % form.getZGridSquareCount());
            int z = (index / form.getZGridSquareCount());
            return "Tile " + index + " (X: " + x + ", Z: " + z + ")";
        }));

        boolean right = false;
        for (GridSquareFlag flag : GridSquareFlag.values()) {
            CheckBox box = new CheckBox(Utils.capitalize(flag.name()));
            box.setSelected((getGridFlags()[selectedIndex.get()] & flag.getFlag()) == flag.getFlag());
            box.selectedProperty().addListener((listener, oldVal, newState) -> {
                if (changingState.get())
                    return;

                if (newState) {
                    this.gridFlags[selectedIndex.get()] |= flag.getFlag();
                } else {
                    this.gridFlags[selectedIndex.get()] ^= flag.getFlag();
                }
            });

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
