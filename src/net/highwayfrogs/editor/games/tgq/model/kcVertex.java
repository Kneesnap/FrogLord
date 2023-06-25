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
    public void load(DataReader reader, kcVertexFormatComponent[] components, long fvf) {
        if (components == null)
            return;

        for (int i = 0; i < components.length; i++)
            this.load(reader, components[i], fvf);
    }

    /**
     * Loads vertex data from the reader for the given components.
     * @param reader    The reader to load vertex data from.
     * @param component The component describing the vertex data to load.
     */
    public void load(DataReader reader, kcVertexFormatComponent component, long fvf) {
        if ((fvf & kcModel.FVF_FLAG_PS2_COMPRESSED) == kcModel.FVF_FLAG_PS2_COMPRESSED) {
            loadCompressedPS2(reader, component);
        } else {
            loadNormal(reader, component);
        }
    }

    /**
     * Loads vertex data from the reader for the given components.
     * @param reader    The reader to load vertex data from.
     * @param component The component describing the vertex data to load.
     */
    public void loadNormal(DataReader reader, kcVertexFormatComponent component) {
        // NOTE: If we have to implement compression, use this same function, but change readFloat() to a function which takes arguments and "decompresses" it, if that's enabled or just reads the float directly.

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

    private static final int PS2_FIXED_PT_MAIN_UNIT = 4096;
    private static final int PS2_FIXED_PT_OTHER_UNIT = 16;
    private static final float PS2_MAIN_MULTIPLIER = 1F / PS2_FIXED_PT_MAIN_UNIT;
    private static final float PS2_OTHER_MULTIPLIER = 1F / PS2_FIXED_PT_OTHER_UNIT;

    private static float readPs2Float(DataReader reader) {
        return readPs2Float(reader, PS2_MAIN_MULTIPLIER);
    }

    private static float readPs2Float(DataReader reader, float multiplier) {
        return reader.readShort() * multiplier;
    }

    /**
     * Loads vertex data from the reader for the given components.
     * @param reader    The reader to load vertex data from.
     * @param component The component describing the vertex data to load.
     */
    public void loadCompressedPS2(DataReader reader, kcVertexFormatComponent component) {
        short red;
        short green;
        short blue;
        short alpha;

        switch (component) {
            case POSITION_XYZF: // 6
                this.x = readPs2Float(reader, PS2_OTHER_MULTIPLIER);
                this.y = readPs2Float(reader, PS2_OTHER_MULTIPLIER);
                this.z = readPs2Float(reader, PS2_OTHER_MULTIPLIER);
                this.w = 1F;
                break;
            case POSITION_XYZWF: // 8
                this.x = readPs2Float(reader, PS2_OTHER_MULTIPLIER);
                this.y = readPs2Float(reader, PS2_OTHER_MULTIPLIER);
                this.z = readPs2Float(reader, PS2_OTHER_MULTIPLIER);
                this.w = readPs2Float(reader, PS2_OTHER_MULTIPLIER);
                break;
            case NORMAL_XYZF: // 6
                this.normalX = readPs2Float(reader);
                this.normalY = readPs2Float(reader);
                this.normalZ = readPs2Float(reader);
                break;
            case NORMAL_XYZWF: // 8
                this.normalX = readPs2Float(reader);
                this.normalY = readPs2Float(reader);
                this.normalZ = readPs2Float(reader);
                readPs2Float(reader); // Unused, there is no "normalW" value.
                break;
            case DIFFUSE_RGBAF: // 8
                // Honestly... I don't think this works. The code I see in the game doesn't look like it works. Perhaps it's just bad ghidra decompiler output.
                red = (short) (reader.readShort() & 0xFF);
                green = (short) (reader.readShort() & 0xFF);
                blue = (short) (reader.readShort() & 0xFF);
                alpha = (short) (reader.readShort() & 0xFF);
                this.diffuse = ((alpha * 255L) << 24) | ((red * 255L) << 16) | ((green * 255L) << 8) | (blue * 255L);
                break;
            case DIFFUSE_RGBA255F: // 8
                red = (short) (reader.readShort() & 0xFF);
                green = (short) (reader.readShort() & 0xFF);
                blue = (short) (reader.readShort() & 0xFF);
                alpha = (short) (reader.readShort() & 0xFF);
                this.diffuse = (((long) alpha) << 24) | (((long) red) << 16) | (((long) green) << 8) | ((long) blue);
                break;
            case DIFFUSE_RGBF: // 6
            case DIFFUSE_RGBAI: // 4
            case SPECULAR_RGBF: // 6
            case SPECULAR_RGBAF: // 8
            case SPECULAR_RGBAI: // 4
            case SPECULAR_RGBA255F: // 8
                // The actual code in the PS2 PAL version skips this.
                // It prints an error message, but continues reading, assuming the stride is calculable, so it can skip.
                // It is unknown if the PC version uses this yet.
                reader.skipBytes(component.getPs2CompressedStride());
                throw new RuntimeException("Cannot read unsupported vertex format " + component + ".");
            case WEIGHT1F: // 2
                if (this.weight == null || this.weight.length != 1)
                    this.weight = new float[1];
                this.weight[0] = readPs2Float(reader);
                break;
            case WEIGHT2F: // 4
                if (this.weight == null || this.weight.length != 2)
                    this.weight = new float[2];
                this.weight[0] = readPs2Float(reader);
                this.weight[1] = readPs2Float(reader);
                break;
            case TEX1F: // 4
                this.u0 = readPs2Float(reader);
                this.v0 = readPs2Float(reader);
                break;
            case TEX2F: // 8
                this.u0 = readPs2Float(reader);
                this.v0 = readPs2Float(reader);
                this.u1 = readPs2Float(reader);
                this.v1 = readPs2Float(reader);
                break;
            case TEX1_STQP: // 8
                this.u0 = readPs2Float(reader);
                this.v0 = readPs2Float(reader);
                reader.skipBytes(4); // Not sure why we skip it, but that's what the PS2 PAL version does.
                break;
            case WEIGHT3F: // 6
                if (this.weight == null || this.weight.length != 3)
                    this.weight = new float[3];

                this.weight[0] = readPs2Float(reader);
                this.weight[1] = readPs2Float(reader);
                this.weight[2] = readPs2Float(reader);
                break;
            case WEIGHT4F: // 8
                if (this.weight == null || this.weight.length != 4)
                    this.weight = new float[4];

                this.weight[0] = readPs2Float(reader);
                this.weight[1] = readPs2Float(reader);
                this.weight[2] = readPs2Float(reader);
                this.weight[3] = readPs2Float(reader);
                break;
            case MATRIX_INDICES: // 8
                // Unused / unimplemented. This behavior matches PS2 PAL.
                reader.skipBytes(8);
                break;
            case PSIZE: // 2
                this.psize = readPs2Float(reader, PS2_OTHER_MULTIPLIER);
                break;
            default:
                throw new RuntimeException("Cannot read vertex data due to unsupported kcVertexFormatComponent " + component);
        }
    }

    private static void writePs2Float(DataWriter writer, float value) {
        writePs2Float(writer, value, PS2_FIXED_PT_MAIN_UNIT);
    }

    private static void writePs2Float(DataWriter writer, float value, int unit) {
        int temp = (int) Math.round((double) value * unit);
        if (temp > Short.MAX_VALUE || temp < Short.MIN_VALUE)
            throw new RuntimeException("Cannot save the value '" + value + "' while PS2 compression is enabled for the model, because this coordinate is too extreme to represent. in a 16 bit number. (" + temp + ")");

        writer.writeShort((short) temp);
    }

    /**
     * Saves vertex data to the writer for the given components.
     * @param writer     The writer to write vertex data from.
     * @param components The components describing the vertex data to write.
     */
    public void save(DataWriter writer, kcVertexFormatComponent[] components, long fvf) {
        if (components == null)
            return;

        for (int i = 0; i < components.length; i++)
            this.save(writer, components[i], fvf);
    }

    /**
     * Saves vertex data to the writer for the given component.
     * @param writer    The writer to write vertex data from.
     * @param component The component describing the vertex data to write.
     */
    public void save(DataWriter writer, kcVertexFormatComponent component, long fvf) {
        if ((fvf & kcModel.FVF_FLAG_PS2_COMPRESSED) == kcModel.FVF_FLAG_PS2_COMPRESSED) {
            savePs2Compressed(writer, component);
        } else {
            saveNormal(writer, component);
        }
    }

    /**
     * Saves vertex data to the writer for the given component.
     * @param writer    The writer to write vertex data from.
     * @param component The component describing the vertex data to write.
     */
    public void saveNormal(DataWriter writer, kcVertexFormatComponent component) {
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
     * Saves vertex data to the writer for the given component.
     * @param writer    The writer to write vertex data from.
     * @param component The component describing the vertex data to write.
     */
    public void savePs2Compressed(DataWriter writer, kcVertexFormatComponent component) {
        switch (component) {
            case POSITION_XYZF: // 6
                writePs2Float(writer, this.x, PS2_FIXED_PT_OTHER_UNIT);
                writePs2Float(writer, this.y, PS2_FIXED_PT_OTHER_UNIT);
                writePs2Float(writer, this.z, PS2_FIXED_PT_OTHER_UNIT);
                break;
            case POSITION_XYZWF: // 8
                writePs2Float(writer, this.x, PS2_FIXED_PT_OTHER_UNIT);
                writePs2Float(writer, this.y, PS2_FIXED_PT_OTHER_UNIT);
                writePs2Float(writer, this.z, PS2_FIXED_PT_OTHER_UNIT);
                writePs2Float(writer, this.w, PS2_FIXED_PT_OTHER_UNIT);
                break;
            case NORMAL_XYZF: // 6
                writePs2Float(writer, this.normalX);
                writePs2Float(writer, this.normalY);
                writePs2Float(writer, this.normalZ);
                break;
            case NORMAL_XYZWF: // 8
                writePs2Float(writer, this.normalX);
                writePs2Float(writer, this.normalY);
                writePs2Float(writer, this.normalZ);
                writePs2Float(writer, 1F); // Unused value for 'W'.
                break;
            case DIFFUSE_RGBAF: // 8
                writer.writeFloat((short) getDiffuseRed());
                writer.writeFloat((short) getDiffuseGreen());
                writer.writeFloat((short) getDiffuseBlue());
                writer.writeFloat((short) getDiffuseAlpha());
                break;
            case DIFFUSE_RGBA255F: // 8
                writer.writeShort((short) getDiffuseRed255F());
                writer.writeShort((short) getDiffuseGreen255F());
                writer.writeShort((short) getDiffuseBlue255F());
                writer.writeShort((short) getDiffuseAlpha255F());
                break;
            case DIFFUSE_RGBF: // 6
            case DIFFUSE_RGBAI: // 4
            case SPECULAR_RGBF: // 6
            case SPECULAR_RGBAF: // 8
            case SPECULAR_RGBAI: // 4
            case SPECULAR_RGBA255F: // 8
                // The actual code in the PS2 PAL version skips this.
                // It prints an error message, but continues reading, assuming the stride is calculable, so it can skip.
                // It is unknown if the PC version uses this yet.
                writer.writeNull(component.getPs2CompressedStride());
                throw new RuntimeException("Cannot write unsupported vertex format " + component + ".");
            case WEIGHT1F: // 2
                if (this.weight == null || this.weight.length != 1)
                    this.weight = new float[1];
                writePs2Float(writer, this.weight[0]);
                break;
            case WEIGHT2F: // 4
                if (this.weight == null || this.weight.length != 2)
                    this.weight = new float[2];
                writePs2Float(writer, this.weight[0]);
                writePs2Float(writer, this.weight[1]);
                break;
            case TEX1F: // 4
                writePs2Float(writer, this.u0);
                writePs2Float(writer, this.v0);
                break;
            case TEX2F: // 8
                writePs2Float(writer, this.u0);
                writePs2Float(writer, this.v0);
                writePs2Float(writer, this.u1);
                writePs2Float(writer, this.v1);
                break;
            case TEX1_STQP: // 8
                writePs2Float(writer, this.u0);
                writePs2Float(writer, this.v0);
                writePs2Float(writer, 1F); // Unused
                writePs2Float(writer, 1F); // Unused
                break;
            case WEIGHT3F: // 6
                if (this.weight == null || this.weight.length != 3)
                    this.weight = new float[3];

                writePs2Float(writer, this.weight[0]);
                writePs2Float(writer, this.weight[1]);
                writePs2Float(writer, this.weight[2]);
                break;
            case WEIGHT4F: // 8
                if (this.weight == null || this.weight.length != 4)
                    this.weight = new float[4];

                writePs2Float(writer, this.weight[0]);
                writePs2Float(writer, this.weight[1]);
                writePs2Float(writer, this.weight[2]);
                writePs2Float(writer, this.weight[3]);
                break;
            case MATRIX_INDICES: // 8
                // Unused / unimplemented. This behavior matches PS2 PAL.
                writer.writeNull(8);
                break;
            case PSIZE: // 2
                writePs2Float(writer, this.psize, PS2_FIXED_PT_OTHER_UNIT);
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