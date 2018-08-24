package net.highwayfrogs.editor.file.map.form;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Parses the "FORM_DATA" struct.
 * Created by Kneesnap on 8/23/2018.
 */
public class FormData extends GameObject {
    private Form parent;
    private short height; // This is for if heightType is one height for the entire grid.
    private short[] gridFlags; // Believe this is ordered (z * xSize) + x

    private static short FORM_HEIGHT_TYPE = (short) 0;

    public FormData(Form parent) {
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        short formHeightType = reader.readShort(); // There appears to have been several unfinished options for height, and are not used. We do not support them.
        Utils.verify(formHeightType == FORM_HEIGHT_TYPE, "Unsupported Form Height Type: %d.", formHeightType);

        this.height = reader.readShort();

        int squarePointer = reader.readInt(); // Pointer to array of (xCount * zCount) flags. (Type = short)
        int heightsPointer = reader.readInt(); // Pointer to an array of grid heights. This would have been used in the "SQUARE" height mode, however that does not appear to be used in the vanilla game.

        int fullSize = parent.getXGridSquareCount() * parent.getZGridSquareCount();
        if (fullSize % 2 == 0)
            fullSize++;

        reader.jumpTemp(squarePointer); // Really we don't need to jump, as the data is at the current read index, but this is to keep it in spec with the engine.
        this.gridFlags = new short[fullSize];
        for (int i = 0; i < gridFlags.length; i++)
            this.gridFlags[i] = reader.readShort();

        // Maybe odd numbers get an extra byte?
        // Maybe numbers that are the same?

        // 01 01 -> Size: 2 (hmm) (1 * 1) + 1 = 2. ((1 + 1) * 1) = 2.
        // 01 02 -> Size: 2 (huh.)(1 * 2) = 2. (
        // 01 04 -> Size: 4 (1 * 4)
        // 03 03 -> Size: 10 (3 * 3) + 1.
        // 01 03 -> Size: 4

        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(FORM_HEIGHT_TYPE);
        writer.writeShort(this.height);
        writer.writeInt(writer.getIndex() + (2 * Constants.INTEGER_SIZE));
        writer.writeInt(writer.getIndex() - Constants.INTEGER_SIZE - Constants.SHORT_SIZE); // Points to an unused height pointer array, mentioned above. I don't believe this functions in the frogger engine.
        for (short aShort : this.gridFlags)
            writer.writeShort(aShort);
    }
}
