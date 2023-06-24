package net.highwayfrogs.editor.games.tgq.model;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a single vertex. Data is optional, and is assumed to be present based on the vertex components stored separately from the vertex.
 * Created by Kneesnap on 6/22/2023.
 */
@Getter
public class kcVertex {
    private float x;
    private float y;
    private float z;
    private float normalX;
    private float normalY;
    private float normalZ;
    private long diffuse;
    private long specular;
    private float u0;
    private float v0;
    private float u1;
    private float v1;
    private float[] weight;
    private float w;
    private float psize;

    /**
     * Loads vertex data from the reader for the given components.
     * @param reader     The reader to load vertex data from.
     * @param components The components describing the vertex data to load.
     */
    public void load(DataReader reader, kcVertexFormatComponent[] components) {
        if (components == null)
            return;

        for (int i = 0; i < components.length; i++)
            this.load(reader, components[i]);
    }

    /**
     * Loads vertex data from the reader for the given components.
     * @param reader    The reader to load vertex data from.
     * @param component The component describing the vertex data to load.
     */
    public void load(DataReader reader, kcVertexFormatComponent component) {
        // NOTE: If we have to implement compression, use this same function, but change readFloat() to a function which takes arguments and "decompresses" it, if that's enabled or just reads the float directly.
        if (component.getStride() > reader.getRemaining())
            return;

        float red;
        float green;
        float blue;

        switch (component) {
            case POSITION_XYZF: // 12
                this.x = reader.readFloat();
                this.y = reader.readFloat();
                this.z = reader.readFloat();
                this.w = 1F;
                break;
            case POSITION_XYZWF: // 16
                this.x = reader.readFloat();
                this.y = reader.readFloat();
                this.z = reader.readFloat();
                this.w = reader.readFloat();
                break;
            case NORMAL_XYZF: // 12
                this.normalX = reader.readFloat();
                this.normalY = reader.readFloat();
                this.normalZ = reader.readFloat();
                break;
            case NORMAL_XYZWF: // 16
                this.normalX = reader.readFloat();
                this.normalY = reader.readFloat();
                this.normalZ = reader.readFloat();
                reader.readFloat();
                // Yes, despite this one sounding like it should include W, it doesn't actually appear to assign W.
                // It only skips over it. I don't think I'm wrong because I read both the ghidra decompiled output and the raw assembly.
                // There is no 'normalW' value, so perhaps this makes sense.
                break;
            case DIFFUSE_RGBF: // 12
                red = reader.readFloat();
                green = reader.readFloat();
                blue = reader.readFloat();
                this.diffuse = (((int) (red * 255F)) << 16) | (((int) (green * 255F)) << 8) | (int) (blue * 255F);
                break;
            case DIFFUSE_RGBAF: // 16
                red = reader.readFloat();
                green = reader.readFloat();
                blue = reader.readFloat();
                float alpha = reader.readFloat();
                this.diffuse = (((long) (alpha * 255F)) << 24) | (((long) (red * 255F)) << 16) | (((long) (green * 255F)) << 8) | (long) (blue * 255F);
                break;
            case DIFFUSE_RGBAI: // 4
                this.diffuse = reader.readUnsignedIntAsLong();
                break;
            case DIFFUSE_RGBA255F: // 16
                red = reader.readFloat();
                green = reader.readFloat();
                blue = reader.readFloat();
                alpha = reader.readFloat();
                this.diffuse = ((((long) alpha) & 0xFF) << 24) | ((((long) red) & 0xFF) << 16) | ((((long) green) & 0xFF) << 8) | (((long) blue) & 0xFF);
                break;
            case SPECULAR_RGBF: // 12
            case SPECULAR_RGBAF: // 16
            case SPECULAR_RGBAI: // 4
            case SPECULAR_RGBA255F: // 16
                // The actual code in the PS2 PAL version skips this.
                // It prints an error message, but continues reading, assuming the stride is calculable, so it can skip.
                // It is unknown if the PC version uses this yet.
                reader.skipBytes(component.getStride());
                throw new RuntimeException("Cannot read unsupported vertex format " + component + ".");
            case WEIGHT1F: // 4
                if (this.weight == null || this.weight.length != 1)
                    this.weight = new float[1];
                this.weight[0] = reader.readFloat();
                break;
            case WEIGHT2F: // 8
                if (this.weight == null || this.weight.length != 2)
                    this.weight = new float[2];
                this.weight[0] = reader.readFloat();
                this.weight[1] = reader.readFloat();
                break;
            case TEX1F: // 8
                this.u0 = reader.readFloat();
                this.v0 = reader.readFloat();
                break;
            case TEX2F: // 16
                this.u0 = reader.readFloat();
                this.v0 = reader.readFloat();
                this.u1 = reader.readFloat();
                this.v1 = reader.readFloat();
                break;
            case TEX1_STQP: // 16
                this.u0 = reader.readFloat();
                this.v0 = reader.readFloat();
                reader.skipBytes(8); // Not sure why we skip it, but that's what the PS2 PAL version does.
                break;
            case WEIGHT3F: // 12
                if (this.weight == null || this.weight.length != 3)
                    this.weight = new float[3];

                this.weight[0] = reader.readFloat();
                this.weight[1] = reader.readFloat();
                this.weight[2] = reader.readFloat();
                break;
            case WEIGHT4F: // 16
                if (this.weight == null || this.weight.length != 4)
                    this.weight = new float[4];

                this.weight[0] = reader.readFloat();
                this.weight[1] = reader.readFloat();
                this.weight[2] = reader.readFloat();
                this.weight[3] = reader.readFloat();
                break;
            case MATRIX_INDICES: // 16
                // Unused / unimplemented. This behavior matches PS2 PAL.
                reader.skipBytes(16);
                break;
            case PSIZE: // 4
                this.psize = reader.readFloat();
                break;
            default:
                throw new RuntimeException("Cannot read vertex data due to unsupported kcVertexFormatComponent " + component);
        }
    }

    /**
     * Saves vertex data to the writer for the given components.
     * @param writer     The writer to write vertex data from.
     * @param components The components describing the vertex data to write.
     */
    public void save(DataWriter writer, kcVertexFormatComponent[] components) {
        if (components == null)
            return;

        for (int i = 0; i < components.length; i++)
            this.save(writer, components[i]);
    }

    /**
     * Saves vertex data to the writer for the given component.
     * @param writer    The writer to write vertex data from.
     * @param component The component describing the vertex data to write.
     */
    public void save(DataWriter writer, kcVertexFormatComponent component) {
        switch (component) {
            case POSITION_XYZF: // 12
                writer.writeFloat(this.x);
                writer.writeFloat(this.y);
                writer.writeFloat(this.z);
                break;
            case POSITION_XYZWF: // 16
                writer.writeFloat(this.x);
                writer.writeFloat(this.y);
                writer.writeFloat(this.z);
                writer.writeFloat(this.w);
                break;
            case NORMAL_XYZF: // 12
                writer.writeFloat(this.normalX);
                writer.writeFloat(this.normalY);
                writer.writeFloat(this.normalZ);
                break;
            case NORMAL_XYZWF: // 16
                writer.writeFloat(this.normalX);
                writer.writeFloat(this.normalY);
                writer.writeFloat(this.normalZ);
                writer.writeFloat(1F); // Unused value for 'W'.
                break;
            case DIFFUSE_RGBF: // 12
                writer.writeFloat(getDiffuseRed());
                writer.writeFloat(getDiffuseGreen());
                writer.writeFloat(getDiffuseBlue());
                break;
            case DIFFUSE_RGBAF: // 16
                writer.writeFloat(getDiffuseRed());
                writer.writeFloat(getDiffuseGreen());
                writer.writeFloat(getDiffuseBlue());
                writer.writeFloat(getDiffuseAlpha());
                break;
            case DIFFUSE_RGBAI: // 4
                writer.writeUnsignedInt(this.diffuse);
                break;
            case DIFFUSE_RGBA255F: // 16
                writer.writeFloat(getDiffuseRed255F());
                writer.writeFloat(getDiffuseGreen255F());
                writer.writeFloat(getDiffuseBlue255F());
                writer.writeFloat(getDiffuseAlpha255F());
                break;
            case SPECULAR_RGBF: // 12
            case SPECULAR_RGBAF: // 16
            case SPECULAR_RGBAI: // 4
            case SPECULAR_RGBA255F: // 16
                // The actual code in the PS2 PAL version skips this.
                // It prints an error message, but continues reading, assuming the stride is calculable, so it can skip.
                // It is unknown if the PC version uses this yet.
                writer.writeNull(component.getStride());
                throw new RuntimeException("Cannot write unsupported vertex format " + component + ".");
            case WEIGHT1F: // 4
                if (this.weight == null || this.weight.length != 1)
                    this.weight = new float[1];
                writer.writeFloat(this.weight[0]);
                break;
            case WEIGHT2F: // 8
                if (this.weight == null || this.weight.length != 2)
                    this.weight = new float[2];
                writer.writeFloat(this.weight[0]);
                writer.writeFloat(this.weight[1]);
                break;
            case TEX1F: // 8
                writer.writeFloat(this.u0);
                writer.writeFloat(this.v0);
                break;
            case TEX2F: // 16
                writer.writeFloat(this.u0);
                writer.writeFloat(this.v0);
                writer.writeFloat(this.u1);
                writer.writeFloat(this.v1);
                break;
            case TEX1_STQP: // 16
                writer.writeFloat(this.u0);
                writer.writeFloat(this.v0);
                writer.writeFloat(1F); // Unused
                writer.writeFloat(1F); // Unused
                break;
            case WEIGHT3F: // 12
                if (this.weight == null || this.weight.length != 3)
                    this.weight = new float[3];

                writer.writeFloat(this.weight[0]);
                writer.writeFloat(this.weight[1]);
                writer.writeFloat(this.weight[2]);
                break;
            case WEIGHT4F: // 16
                if (this.weight == null || this.weight.length != 4)
                    this.weight = new float[4];

                writer.writeFloat(this.weight[0]);
                writer.writeFloat(this.weight[1]);
                writer.writeFloat(this.weight[2]);
                writer.writeFloat(this.weight[3]);
                break;
            case MATRIX_INDICES: // 16
                // Unused / unimplemented. This behavior matches PS2 PAL.
                writer.writeNull(16);
                break;
            case PSIZE: // 4
                writer.writeFloat(this.psize);
                break;
            default:
                throw new RuntimeException("Cannot read vertex data due to unsupported kcVertexFormatComponent " + component);
        }
    }

    /**
     * Gets the red component of the diffuse color as a floating point number from 0 to 1F.
     * @return colorComponent
     */
    public float getDiffuseRed() {
        return ((this.diffuse >> 16) & 0xFF) / 255F;
    }

    /**
     * Gets the red component of the diffuse color as a floating point number from 0 to 255F.
     * @return colorComponent
     */
    public float getDiffuseRed255F() {
        return ((this.diffuse >> 16) & 0xFF);
    }

    /**
     * Gets the green component of the diffuse color as a floating point number from 0 to 1F.
     * @return colorComponent
     */
    public float getDiffuseGreen() {
        return ((this.diffuse >> 8) & 0xFF) / 255F;
    }

    /**
     * Gets the green component of the diffuse color as a floating point number from 0 to 255F.
     * @return colorComponent
     */
    public float getDiffuseGreen255F() {
        return ((this.diffuse >> 8) & 0xFF);
    }

    /**
     * Gets the blue component of the diffuse color as a floating point number from 0 to 1F.
     * @return colorComponent
     */
    public float getDiffuseBlue() {
        return (this.diffuse & 0xFF) / 255F;
    }

    /**
     * Gets the blue component of the diffuse color as a floating point number from 0 to 255F.
     * @return colorComponent
     */
    public float getDiffuseBlue255F() {
        return (this.diffuse & 0xFF);
    }

    /**
     * Gets the alpha component of the diffuse color as a floating point number from 0 to 1F.
     * @return colorComponent
     */
    public float getDiffuseAlpha() {
        return ((this.diffuse >> 24) & 0xFF) / 255F;
    }

    /**
     * Gets the alpha component of the diffuse color as a floating point number from 0 to 255F.
     * @return colorComponent
     */
    public float getDiffuseAlpha255F() {
        return ((this.diffuse >> 24) & 0xFF);
    }
}