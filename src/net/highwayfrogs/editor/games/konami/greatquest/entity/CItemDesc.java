package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;

/**
 * Represents the 'CItemDesc' struct.
 * Loaded by 'CItem::Init'
 * This has a hardcoded collision proxy, also created in CItem::Init. This explains why the items like the goblet in The Lost Trail Ruins have such small hitboxes.
 * Additionally, it may also explain why coins have weird hitboxes when viewed in editor.
 * TODO: Communicate this in FrogLord.
 * Created by Kneesnap on 8/21/2023.
 */
public class CItemDesc extends kcActorBaseDesc {
    private int value;
    private int properties;
    private int attributes;
    private static final int PADDING_VALUES = 32;

    public CItemDesc(@NonNull kcCResourceGeneric resource) {
        super(resource);
    }

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
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.value);
        writer.writeInt(this.properties);
        writer.writeInt(this.attributes);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Item Value: ").append(this.value).append(Constants.NEWLINE);
        builder.append(padding).append("Item Properties: ").append(this.properties).append(Constants.NEWLINE);
        builder.append(padding).append("Item Attributes: ").append(this.attributes).append(Constants.NEWLINE);
    }
}