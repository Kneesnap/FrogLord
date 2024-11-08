package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcIGenericResourceData;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcSphere;
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Represents the 'kcEntity3DDesc' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public abstract class kcEntity3DDesc extends kcBaseDesc implements kcIGenericResourceData {
    private int instanceFlags; // This doesn't appear to be used, it seems to be the default flags value for which kcEntity3DInst will pull its default value from.
    private final kcSphere boundingSphere = new kcSphere(0, 0, 0, 1F); // Positioned relative to entity position.
    private static final int PADDING_VALUES = 3;
    private static final int PADDING_VALUES_3D = 4;
    private static final int CLASS_ID = GreatQuestUtils.hash("kcCEntity3D");

    protected kcEntity3DDesc(@NonNull kcCResourceGeneric resource) {
        super(resource);
        this.boundingSphere.setRadius(1F); // Default radius is 1.
    }

    @Override
    protected int getTargetClassID() {
        return CLASS_ID;
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        // Main Data
        this.instanceFlags = reader.readInt();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
        this.boundingSphere.load(reader);
        reader.skipBytesRequireEmpty(PADDING_VALUES_3D * Constants.INTEGER_SIZE);
    }

    @Override
    public void saveData(DataWriter writer) {
        // Main Data
        writer.writeInt(this.instanceFlags);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
        this.boundingSphere.save(writer);
        writer.writeNull(PADDING_VALUES_3D * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Flags: ").append(NumberUtils.toHexString(this.instanceFlags)).append(Constants.NEWLINE);
        this.boundingSphere.writePrefixedMultiLineInfo(builder, "Bounding Sphere", padding);
    }

    @Override
    public kcCResourceGeneric getResource() {
        return (kcCResourceGeneric) super.getResource();
    }

    @Override
    public abstract kcCResourceGenericType getResourceType();

    @Override
    public void handleDoubleClick() {
        // Do nothing by default.
    }
}