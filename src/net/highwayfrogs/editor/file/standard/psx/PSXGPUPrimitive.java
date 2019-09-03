package net.highwayfrogs.editor.file.standard.psx;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;

/**
 * Represents a struct in LIBGPU.H (PsyQ) which allows drawing to the screen in some way.
 * Created by Kneesnap on 8/25/2018.
 */
public abstract class PSXGPUPrimitive extends GameObject {

    /**
     * Get the vertices owned by this primitive.
     * @return vertices
     */
    public abstract int[] getVertices();

    /**
     * Get the number of vertices stored by this primitive.
     * @return verticeCount
     */

    public int getVerticeCount() {
        return getVertices().length;
    }

    /**
     * Get the TextureTreeNode for this polygon.
     * @param map The map to get this from.
     * @return node
     */
    public abstract TextureTreeNode getNode(TextureMap map);

    /**
     * Test if this face has 4 vertices.
     * @return isQuadFace
     */
    public boolean isQuadFace() {
        return getVerticeCount() == 4;
    }

    /**
     * Test if this face has 3 vertices.
     * @return triFace
     */
    public boolean isTriFace() {
        return getVerticeCount() == 3;
    }
}
