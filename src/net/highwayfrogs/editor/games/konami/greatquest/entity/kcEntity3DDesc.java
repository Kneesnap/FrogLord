package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcSphere;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the 'kcEntity3DDesc' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcEntity3DDesc extends kcBaseDesc {
    private int instanceFlags;
    private final int[] padEntity = new int[3];
    private final kcSphere boundingSphere = new kcSphere();
    private final int[] padEntity3D = new int[4];

    private static final int CLASS_ID = GreatQuestUtils.hash("kcCEntity3D");

    @Override
    protected int getTargetClassID() {
        return CLASS_ID;
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        // Main Data
        this.instanceFlags = reader.readInt();
        for (int i = 0; i < this.padEntity.length; i++)
            this.padEntity[i] = reader.readInt();
        this.boundingSphere.load(reader);
        for (int i = 0; i < this.padEntity3D.length; i++)
            this.padEntity3D[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        // Main Data
        writer.writeInt(this.instanceFlags);
        for (int i = 0; i < this.padEntity.length; i++)
            writer.writeInt(this.padEntity[i]);
        this.boundingSphere.save(writer);
        for (int i = 0; i < this.padEntity3D.length; i++)
            writer.writeInt(this.padEntity3D[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Flags: ").append(Utils.toHexString(this.instanceFlags)).append(Constants.NEWLINE);
        this.boundingSphere.writePrefixedMultiLineInfo(builder, "Bounding Sphere", padding);
    }
}