package net.highwayfrogs.editor.file.map.form;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;

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

    @Override
    public void load(DataReader reader) {
        int dataCount = reader.readUnsignedShortAsInt();
        reader.readShort(); // Max Y, Runtime variable.
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
    public void setupEditor(MapUIController controller, GUIEditorGrid editor) {
        editor.addShortField("X Grid Square Count", getXGridSquareCount(), this::setXGridSquareCount, null);
        editor.addShortField("Z Grid Square Count", getZGridSquareCount(), this::setZGridSquareCount, null);
        editor.addShortField("xOffset", getXOffset(), this::setXOffset, null);
        editor.addShortField("zOffset", getZOffset(), this::setZOffset, null);

        if (hasData()) {
            editor.addBoldLabel("Form Data:");
            getData().setupEditor(controller, editor);
            editor.addButton("Remove Data", () -> {
                setData(null);
                controller.setupFormEditor();
            });
        } else {
            editor.addButton("Add Data", () -> {
                setData(new FormData(this));
                controller.setupFormEditor();
            });
        }
    }
}
