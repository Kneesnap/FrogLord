package net.highwayfrogs.editor.games.tgq.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.math.kcVector4;
import net.highwayfrogs.editor.games.tgq.toc.kcCResourceEntityInst;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the 'kcEntity3DInst' struct.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
@Setter
@NoArgsConstructor
public class kcEntity3DInst extends kcEntityInst {
    private int flags;
    private kcAxisType billboardAxis;
    private final kcVector4 position = new kcVector4();
    private final kcVector4 rotation = new kcVector4();
    private final kcVector4 scale = new kcVector4();
    private final int[] padding = new int[39];

    public static final int SIZE_IN_BYTES = 240;

    public kcEntity3DInst(kcCResourceEntityInst entity) {
        super(entity);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.flags = reader.readInt();
        this.billboardAxis = kcAxisType.getType(reader.readInt(), false);
        this.position.load(reader);
        this.rotation.load(reader);
        this.scale.load(reader);
        for (int i = 0; i < this.padding.length; i++)
            this.padding[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.flags);
        writer.writeInt(this.billboardAxis.ordinal());
        this.position.save(writer);
        this.rotation.save(writer);
        this.scale.save(writer);
        for (int i = 0; i < this.padding.length; i++)
            writer.writeInt(this.padding[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Flags: ").append(Utils.toHexString(this.flags)).append(Constants.NEWLINE);
        builder.append(padding).append("Billboard Axis: ").append(this.billboardAxis).append(Constants.NEWLINE);
        this.position.writePrefixedInfoLine(builder, "Position", padding);
        this.rotation.writePrefixedInfoLine(builder, "Rotation", padding);
        this.scale.writePrefixedInfoLine(builder, "Scale", padding);
    }
}
