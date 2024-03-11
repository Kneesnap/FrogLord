package net.highwayfrogs.editor.gui.mesh;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode.OverlayTarget;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.fx.wrapper.FXFloatArrayBatcher;

/**
 * Allows highlighting a node by overlaying all if its faces with an ITextureSource.
 * Created by Kneesnap on 9/25/2023.
 */
public class DynamicMeshOverlayNode extends DynamicMeshAdapterNode<OverlayTarget> {
    public DynamicMeshOverlayNode(DynamicMesh mesh) {
        super(mesh);
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(OverlayTarget data) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), data);
        if (data.getOverlayEntry().getWrittenFaceCount() == 0)
            return entry;

        TextureAtlas atlas = getMesh().getTextureAtlas();
        Texture texture = atlas.getTextureFromSourceOrFallback(data.getOverlay());

        int texCoord1 = entry.addTexCoordValue(getUv(texture, 0));
        int texCoord2 = entry.addTexCoordValue(getUv(texture, 1));
        int texCoord3 = entry.addTexCoordValue(getUv(texture, 2));
        int valuesPerVertex = getMesh().getVertexFormat().getVertexIndexSize();

        // For each face/polygon in the target entry:
        for (int i = 0; i < data.getOverlayEntry().getWrittenFaceCount(); i++) {
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
    public void updateVertex(DynamicMeshTypedDataEntry overlayEntry, int localVertexIndex) {
        DynamicMeshDataEntry targetEntry = overlayEntry.getDataSource().getOverlayEntry();
        if (targetEntry == null)
            return;

        // Read vertex data from target entry.
        FXFloatArrayBatcher sourceArray = targetEntry.getMesh().getEditableVertices();
        int sourceVertexIndex = targetEntry.getVertexMeshArrayIndex(localVertexIndex);
        float x = sourceArray.get(sourceVertexIndex++);
        float y = sourceArray.get(sourceVertexIndex++);
        float z = sourceArray.get(sourceVertexIndex);

        // Write vertex position for overlay entry.
        overlayEntry.writeVertexXYZ(localVertexIndex, x, y, z);
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        TextureAtlas atlas = getMesh().getTextureAtlas();
        Texture texture = atlas.getTextureFromSourceOrFallback(entry.getDataSource().getOverlay());
        entry.writeTexCoordValue(localTexCoordIndex, getUv(texture, localTexCoordIndex));
    }

    private Vector2f getUv(Texture texture, int localTexCoordIndex) {
        TextureAtlas atlas = getMesh().getTextureAtlas();

        switch (localTexCoordIndex) {
            case 0:
                return atlas.getUV(texture, 0, 0);
            case 1:
                return atlas.getUV(texture, texture.getPaddedWidth() - texture.getLeftPadding() - 1, 0);
            case 2:
                return atlas.getUV(texture, 0, texture.getPaddedHeight() - texture.getRightPadding() - 1);
            default:
                throw new UnsupportedOperationException("Cannot update localTexCoordIndex " + localTexCoordIndex + " as a DynamicMeshOverlayNode. It should not exist.");
        }
    }

    @Getter
    @AllArgsConstructor
    public static class OverlayTarget {
        private final DynamicMeshDataEntry overlayEntry;
        private final ITextureSource overlay;
    }
}