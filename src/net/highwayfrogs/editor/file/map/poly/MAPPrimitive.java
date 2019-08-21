package net.highwayfrogs.editor.file.map.poly;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.map.manager.GeometryManager;

import java.util.Arrays;

/**
 * Represents a MAP primitive.
 * Created by Kneesnap on 1/26/2019.
 */
@Getter
public abstract class MAPPrimitive extends PSXGPUPrimitive {
    @Setter private int[] vertices;
    private MAPPrimitiveType type;
    @Setter private boolean allowDisplay; // Whether or not this prim can be included in a MAP_GROUP.

    public MAPPrimitive(MAPPrimitiveType type, int verticeCount) {
        this.type = type;
        this.vertices = new int[verticeCount];
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < vertices.length; i++)
            this.vertices[i] = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        for (int vertice : getVertices())
            writer.writeUnsignedShort(vertice);
    }

    /**
     * Setup an editor for this prim.
     * @param manager The geometry manager.
     * @param editor  The editor to create.
     */
    public void setupEditor(GeometryManager manager, GUIEditorGrid editor) {
        editor.addLabel("Type", getType().name());
        editor.addLabel("Vertices", Arrays.toString(getVertices()));
    }
}
