package net.highwayfrogs.editor.file.standard.psx.prims.polygon;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents Playstation polygon data.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class PSXPolygon extends PSXGPUPrimitive {
    private short vertices[];

    public PSXPolygon(int verticeCount) {
        this.vertices = new short[verticeCount];
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < vertices.length; i++)
            this.vertices[i] = reader.readShort();

        swapIfNeeded();
        if (vertices.length % 2 == 0)
            reader.readShort(); // Padding.
    }

    @Override
    public void save(DataWriter writer) {
        for (short vertice : vertices)
            writer.writeShort(vertice);

        swapIfNeeded();
        if (vertices.length % 2 != 0)
            writer.writeNull(Constants.SHORT_SIZE);
    }

    private void swapIfNeeded() {
        if (vertices.length != 4)
            return; // We only need to swap vertexes 2 and 3 if there are 4 vertexes.

        // I forget exactly why we swap this, but it seems to work right when we do.
        short swap = vertices[2];
        vertices[2] = vertices[3];
        vertices[3] = swap;
    }
}
