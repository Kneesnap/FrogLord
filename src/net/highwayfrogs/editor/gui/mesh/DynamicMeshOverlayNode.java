package net.highwayfrogs.editor.gui.mesh;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode.OverlayTarget;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.fx.wrapper.FXFloatArrayBatcher;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows highlighting a node by overlaying all if its faces with an ITextureSource.
 * Created by Kneesnap on 9/25/2023.
 */
public class DynamicMeshOverlayNode extends DynamicMeshAdapterNode<OverlayTarget> {
    private final Map<DynamicMeshDataEntry, OverlayTarget> overlayTargetsByEntry = new HashMap<>();

    public DynamicMeshOverlayNode(DynamicMesh mesh) {
        super(mesh);
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(OverlayTarget data) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), data);
        this.overlayTargetsByEntry.put(data.getOverlayEntry(), data);
        if (data.getOverlayEntry().getWrittenFaceCount() == 0)
            return entry; // Abort if the overlay target has no face data written.

        TextureAtlas atlas = getMesh().getTextureAtlas();
        Texture texture = atlas.getTextureFromSourceOrFallback(data.getOverlay());

        int texCoord1 = entry.addTexCoordValue(getUv(texture, 0));
        int texCoord2 = entry.addTexCoordValue(getUv(texture, 1));
        int texCoord3 = entry.addTexCoordValue(getUv(texture, 2));
        int valuesPerVertex = getMesh().getVertexFormat().getVertexIndexSize();

        // For each face/polygon in the target entry:
        for (int i = 0; i < data.getOverlayEntry().getWrittenFaceCount(); i++) {
            // Calculate the absolute face index of the target polygon.
            int startFaceIndex = (data.getOverlayEntry().getPendingFaceStartIndex() + i) * getMesh().getFaceElementSize();

            // Each vertex is spaced in between the tex coord index, because the faces array includes a value for both texCoord and normal.
            int meshVertex1 = getMesh().getEditableFaces().get(startFaceIndex);
            int meshVertex2 = getMesh().getEditableFaces().get(startFaceIndex + valuesPerVertex);
            int meshVertex3 = getMesh().getEditableFaces().get(startFaceIndex + (2 * valuesPerVertex));

            // Add a new face which uses those vertices. (Test that Z-Fighting isn't an issue, we might need to change this up slightly if it is.)
            entry.addFace(meshVertex1, texCoord1, meshVertex2, texCoord2, meshVertex3, texCoord3);
        }

        return entry;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onEntryRemoved(DynamicMeshDataEntry entry) {
        super.onEntryRemoved(entry);
        if (entry instanceof DynamicMeshAdapterNode.DynamicMeshTypedDataEntry) {
            DynamicMeshTypedDataEntry typedDataEntry = (DynamicMeshTypedDataEntry) entry;
            OverlayTarget target = typedDataEntry.getDataSource();
            this.overlayTargetsByEntry.remove(target.getOverlayEntry(), target);
        }
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
                return atlas.getUV(texture, texture.getLeftPadding(), texture.getUpPadding());
            case 1:
                return atlas.getUV(texture, texture.getPaddedWidth() - texture.getRightPadding() - 1, texture.getUpPadding());
            case 2:
                return atlas.getUV(texture, texture.getLeftPadding(), texture.getPaddedHeight() - texture.getDownPadding() - 1);
            default:
                throw new UnsupportedOperationException("Cannot update localTexCoordIndex " + localTexCoordIndex + " as a DynamicMeshOverlayNode. It should not exist.");
        }
    }

    /**
     * Gets the OverlayTarget corresponding to the original entry, if there is one.
     * @param originalEntry the original entry to lookup the OverlayTarget from
     * @return overlayTarget if exists
     */
    public OverlayTarget getOverlayTarget(DynamicMeshDataEntry originalEntry) {
        return this.overlayTargetsByEntry.get(originalEntry);
    }

    /**
     * Sets the overlay texture for a given entry to be the texture source.
     * If the overlay target does not exist, and the texture source is not null, it will be created.
     * If the overlay target does exist, and the texture source is null, it will be destroyed.
     * @param originalEntry the entry to overlay upon
     * @param textureSource the texture source to apply
     */
    public void setOverlayTexture(DynamicMeshDataEntry originalEntry, ITextureSource textureSource) {
        if (originalEntry == null)
            throw new NullPointerException("originalEntry");

        OverlayTarget overlayTarget = getOverlayTarget(originalEntry);
        if (textureSource != null) {
            if (overlayTarget != null) {
                overlayTarget.setOverlay(textureSource);
                updateTexCoords(overlayTarget);
            } else {
                add(new OverlayTarget(originalEntry, textureSource));
            }
        } else if (overlayTarget != null) { // Destroy the overlay.
            remove(overlayTarget);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class OverlayTarget {
        private final DynamicMeshDataEntry overlayEntry;
        @Setter(AccessLevel.PRIVATE) private ITextureSource overlay;
    }
}