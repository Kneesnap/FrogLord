package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

/**
 * The canvas background image block.
 * Version: 1.4+
 * Created by Kneesnap on 3/6/2020.
 */
@Getter
@Setter
public class MMCanvasBackgroundImage extends MMDataBlockBody {
    private short flags;
    private MMViewDirection viewDirection;
    private float scale; // 1.0 = center to top/left. (OpenGL unit)
    private float[] center = new float[3]; // Center coordinates of image.
    private String fileName; // Image filename. (Relative path)

    public MMCanvasBackgroundImage(MisfitModel3DObject parent) {
        super(OffsetType.CANVAS_BACKGROUND_IMAGES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        this.viewDirection = MMViewDirection.values()[reader.readByte()];
        this.scale = reader.readFloat();
        for (int i = 0; i < this.center.length; i++)
            this.center[i] = reader.readInt();
        this.fileName = reader.readNullTerminatedString();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeByte((byte) this.viewDirection.ordinal());
        writer.writeFloat(this.scale);
        for (float centerVal : this.center)
            writer.writeFloat(centerVal);
        writer.writeTerminatorString(this.fileName);

    }

    public enum MMViewDirection {
        FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM
    }
}
