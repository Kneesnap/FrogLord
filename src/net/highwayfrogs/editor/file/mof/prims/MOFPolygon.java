package net.highwayfrogs.editor.file.mof.prims;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureSource;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.standard.psx.PSXGPUPrimitive;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents Playstation polygon data.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public abstract class MOFPolygon extends PSXGPUPrimitive implements TextureSource {
    private int[] vertices; // An integer array so it matches the type PSX
    private short[] en; // I don't know what these are. The good news is that these are only used by the mof-specific polygons, which as it turns out aren't used. So, the retail game shouldn't have any of these to begin with.
    private short[] normals;
    private PSXColorVector color = new PSXColorVector();
    private transient MOFPrimType type;
    private transient MOFPart parentPart;

    public MOFPolygon(MOFPart parent, MOFPrimType type, int verticeCount, int normalCount, int enCount) {
        this.parentPart = parent;
        this.type = type;
        this.vertices = new int[verticeCount];
        this.normals = new short[normalCount];
        this.en = new short[enCount];
    }

    @Override
    public final void load(DataReader reader) {
        for (int i = 0; i < vertices.length; i++)
            this.vertices[i] = reader.readShort();

        for (int i = 0; i < en.length; i++)
            this.en[i] = reader.readShort();

        for (int i = 0; i < normals.length; i++)
            this.normals[i] = reader.readShort();

        if (shouldAddInitialPadding())
            reader.skipShort();

        onLoad(reader);
        this.color.load(reader);
    }

    @Override
    public final void save(DataWriter writer) {
        for (int vertice : vertices)
            writer.writeShort((short) vertice);

        for (short aShort : en)
            writer.writeShort(aShort);

        for (short normal : normals)
            writer.writeShort(normal);

        if (shouldAddInitialPadding())
            writer.writeShort((short) 0);

        onSave(writer);
        this.color.save(writer);
    }

    public boolean shouldAddInitialPadding() {
        return (en.length + normals.length + vertices.length) % 2 > 0;
    }

    /**
     * Called to load middle-data.
     * @param reader The reader to load data from.
     */
    public void onLoad(DataReader reader) {

    }

    /**
     * Called to save middle data.
     * @param writer The writer to save data to.
     */
    public void onSave(DataWriter writer) {

    }

    /**
     * Convert this into a wavefront object face command.
     * @return faceCommand
     */
    public String toObjFaceCommand(boolean showTextures, AtomicInteger textureCounter) {
        StringBuilder builder = new StringBuilder("f");
        for (int i = this.vertices.length - 1; i >= 0; i--) {
            builder.append(" ").append(getVertexStart() + this.vertices[i] + 1);
            if (showTextures)
                builder.append("/").append(textureCounter != null ? textureCounter.incrementAndGet() : 0);
        }

        return builder.toString();
    }

    /**
     * Get the order this should be put in a .obj file.
     * @return orderId
     */
    public int getOrderId() {
        return 0;
    }

    /**
     * Get the base vertex for this polygon.
     * @return vertexStart
     */
    public int getVertexStart() {
        return getParentPart() != null ? getParentPart().getTempVertexStart() : 0;
    }
}
