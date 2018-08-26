package net.highwayfrogs.editor.file.standard.psx.prims.line;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a PSX line.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class PSXLine extends GameObject {
    private short vertices[];

    public PSXLine(int verticeCount) {
        this.vertices = new short[verticeCount];
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < vertices.length; i++)
            this.vertices[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        for (short vertice : vertices)
            writer.writeShort(vertice);
    }
}
