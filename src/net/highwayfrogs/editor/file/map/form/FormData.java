package net.highwayfrogs.editor.file.map.form;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

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
        reader.skipPointer(); // Pointer to an array of grid heights. This would have been used in the "SQUARE" height mode, however that does not appear to be used in the vanilla game.

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

        writer.align(Constants.INTEGER_SIZE); // Padding.
    }
}
