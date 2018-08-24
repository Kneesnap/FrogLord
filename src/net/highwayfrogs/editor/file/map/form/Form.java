package net.highwayfrogs.editor.file.map.form;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the "FORM" struct.
 * Created by Kneesnap on 8/23/2018.
 */
@Getter
@Setter
public class Form extends GameObject {
    private short maxY;
    private short xGridSquareCount;
    private short zGridSquareCount;
    private short xOffset; // Offset to bottom left or grid from entity origin.
    private short zOffset; // Offset to bottom left or grid from entity origin.
    private List<FormData> data = new ArrayList<>();

    @Override
    public void load(DataReader reader) {
        short dataCount = reader.readShort();
        this.maxY = reader.readShort();
        this.xGridSquareCount = reader.readShort();
        this.zGridSquareCount = reader.readShort();
        this.xOffset = reader.readShort();
        this.zOffset = reader.readShort();

        if (dataCount == 0)
            return; // Don't read further. (don't bother writing the offset either if dataCount == 0.

        int dataPointer = reader.readInt();
        reader.jumpTemp(dataPointer);

        for (int i = 0; i < dataCount; i++) {
            FormData data = new FormData(this);
            data.load(reader);
            getData().add(data);
        }

        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getDataCount());
        writer.writeShort(this.maxY);
        writer.writeShort(this.xGridSquareCount);
        writer.writeShort(this.zGridSquareCount);
        writer.writeShort(this.xOffset);
        writer.writeShort(this.zOffset);
        if (getDataCount() > 0) { // If there are no form data objects, don't even write the offset.
            writer.writeInt(writer.getIndex() + Constants.INTEGER_SIZE); //Write the pointer to the data. (In this case it's right after the FORM data.)
            getData().forEach(data -> data.save(writer)); // Save form data.
        }
    }

    /**
     * Get the form data entry count.
     * @return formDataCount
     */
    public int getDataCount() {
        return data.size();
    }
}
