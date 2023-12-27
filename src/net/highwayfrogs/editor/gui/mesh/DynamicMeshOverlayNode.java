package net.highwayfrogs.editor.gui.mesh;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode.OverlayTarget;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;

/**
 * Allows highlighting a node by overlaying all if its faces with an ITextureSource.
 * TODO: Another idea is updating the existing face's texture coordinates, and storing the old texture coordinate values. Hmm, now that's an idea.
 * Created by Kneesnap on 9/25/2023.
 */
public class DynamicMeshOverlayNode extends DynamicMeshAdapterNode<OverlayTarget> {
    public DynamicMeshOverlayNode(DynamicMesh mesh) {
        super(mesh);
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(OverlayTarget data) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), data);
        if (data.getOverlayEntry().getFaceCount() == 0)
            return entry;

        TextureAtlas atlas = getMesh().getTextureAtlas();
        Texture texture = atlas.getTextureFromSourceOrFallback(data.getOverlay());

        int texCoord1 = entry.addTexCoordValue(atlas.getUV(texture, 0, 0));
        int texCoord2 = entry.addTexCoordValue(atlas.getUV(texture, texture.getPaddedWidth() - texture.getLeftPadding() - 1, 0));
        int texCoord3 = entry.addTexCoordValue(atlas.getUV(texture, 0, texture.getPaddedHeight() - texture.getRightPadding() - 1));
        int valuesPerVertex = getMesh().getVertexFormat().getVertexIndexSize();

        // For each face/polygon in the target entry:
        for (int i = 0; i < data.getOverlayEntry().getFaceCount(); i++) {
            // Calculate the absolute face index of the target polygon.
            int startFaceIndex = (data.getOverlayEntry().getFaceStartIndex() + i) * getMesh().getFaceElementSize();

            // Each vertex is spaced in between the tex coord index, because the faces array includes a value for both texcoord and normal.
            int meshVertex1 = getMesh().getEditableFaces().get(startFaceIndex);
            int meshVertex2 = getMesh().getEditableFaces().get(startFaceIndex + valuesPerVertex);
            int meshVertex3 = getMesh().getEditableFaces().get(startFaceIndex + (2 * valuesPerVertex));

            // Add a new face which uses those vertices. (Test that Z-Fighting isn't an issue, we might need to change this up slightly if it is.)
            entry.addFace(meshVertex1, texCoord1, meshVertex2, texCoord2, meshVertex3, texCoord3);
        }

        return entry;
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        // TODO: Update vertex from reading face information.
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        // Do nothing, since the texture coordinates we use for the fallback texture do not change.
    }

    @Getter
    @AllArgsConstructor
    public static class OverlayTarget {
        private final DynamicMeshDataEntry overlayEntry;
        private final ITextureSource overlay;
    }
}