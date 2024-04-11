package net.highwayfrogs.editor.games.konami.greatquest.entity;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;

/**
 * Represents the 'CItemDesc' struct.
 * Created by Kneesnap on 8/21/2023.
 */
public class CItemDesc extends kcActorBaseDesc {
    private int value;
    private int properties;
    private int attributes;
    private final int[] padItem = new int[32];

    @Override
    protected int getTargetClassID() {
        return kcClassID.ITEM.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.value = reader.readInt();
        this.properties = reader.readInt();
        this.attributes = reader.readInt();
        for (int i = 0; i < this.padItem.length; i++)
            this.padItem[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.value);
        writer.writeInt(this.properties);
        writer.writeInt(this.attributes);
        for (int i = 0; i < this.padItem.length; i++)
            writer.writeInt(this.padItem[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Item Value: ").append(this.value).append(Constants.NEWLINE);
        builder.append(padding).append("Item Properties: ").append(this.properties).append(Constants.NEWLINE);
        builder.append(padding).append("Item Attributes: ").append(this.attributes).append(Constants.NEWLINE);
    }
}