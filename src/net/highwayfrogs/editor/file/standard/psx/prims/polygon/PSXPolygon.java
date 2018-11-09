package net.highwayfrogs.editor.file.standard.psx.prims.polygon;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents Playstation polygon data.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class PSXPolygon extends PSXGPUPrimitive {
    private short vertices[];
    private short padding;
    private transient boolean flippedVertices;

    public static final int REQUIRES_VERTEX_PADDING = 3;
    public static final int REQUIRES_VERTEX_SWAPPING = 4;

    public PSXPolygon(int verticeCount) {
        this.vertices = new short[verticeCount];
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < vertices.length; i++)
            this.vertices[i] = reader.readShort();

        swapIfNeeded();
        if (vertices.length == REQUIRES_VERTEX_PADDING)
            this.padding = reader.readShort(); // Padding? This value seems to sometimes match the last vertices element, and sometimes it doesn't. I don't believe this value is used.
    }

    @Override
    public void save(DataWriter writer) {
        if (isFlippedVertices())
            swapIfNeeded(); // Swap back to default flip state.

        for (short vertice : vertices)
            writer.writeShort(vertice);

        if (vertices.length == REQUIRES_VERTEX_PADDING)
            writer.writeShort(this.padding);
    }

    private void swapIfNeeded() {
        if (vertices.length != REQUIRES_VERTEX_SWAPPING)
            return; // We only need to swap vertexes 2 and 3 if there are 4 vertexes.

        // I forget exactly why we swap this, but it seems to work right when we do.
        short swap = vertices[2];
        vertices[2] = vertices[3];
        vertices[3] = swap;
        this.flippedVertices = !this.flippedVertices;
    }

    /**
     * Convert this into a wavefront object face command.
     * @return faceCommand
     */
    public String toObjFaceCommand(boolean showTextures, AtomicInteger textureCounter) {
        StringBuilder builder = new StringBuilder("f");
        for (short vertice : this.vertices) {
            builder.append(" ").append(vertice + 1);
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
}
