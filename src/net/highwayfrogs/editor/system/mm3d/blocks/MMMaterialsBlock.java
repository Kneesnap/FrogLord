package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

/**
 * Defines a material.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
@Setter
public class MMMaterialsBlock extends MMDataBlockBody {
    private short flags;
    private int texture; // This can expand past normal texture count, as long as we increase texture count saved.
    private String name;
    private float[] ambient = new float[4];
    private float[] diffuse = new float[4]; // Diffuse color.
    private float[] specular = new float[4];
    private float[] emissive = new float[4];
    private float shininess;

    public static final short FLAG_NO_TEXTURE = 0b1111;
    public static final short FLAG_CLAMP_S = Constants.BIT_FLAG_4; // Clamp S texture coordinates. (Do not repeat)
    public static final short FLAG_CLAMP_T = Constants.BIT_FLAG_5; // Clamp T texture coordinates. (Do not repeat)

    public MMMaterialsBlock(MisfitModel3DObject parent) {
        super(OffsetType.MATERIALS, parent);

        // Setup default values.
        this.ambient[0] = .2F;
        this.ambient[1] = .2F;
        this.ambient[2] = .2F;
        this.ambient[3] = 1F;

        this.diffuse[0] = 1F;
        this.diffuse[1] = 1F;
        this.diffuse[2] = 1F;
        this.diffuse[3] = 1F;

        this.specular[this.specular.length - 1] = 1F;
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        this.texture = reader.readInt();
        this.name = reader.readNullTerminatedString();
        readFloatArray(reader, this.ambient);
        readFloatArray(reader, this.diffuse);
        readFloatArray(reader, this.specular);
        readFloatArray(reader, this.emissive);
        this.shininess = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeInt(this.texture);
        writer.writeTerminatorString(this.name);
        writeFloatArray(writer, this.ambient);
        writeFloatArray(writer, this.diffuse);
        writeFloatArray(writer, this.specular);
        writeFloatArray(writer, this.emissive);
        writer.writeFloat(this.shininess);
    }

    /**
     * Test if this material has a texture.
     * @return hasTexture
     */
    public boolean hasTexture() {
        return (this.flags & FLAG_NO_TEXTURE) == 0;
    }
}
