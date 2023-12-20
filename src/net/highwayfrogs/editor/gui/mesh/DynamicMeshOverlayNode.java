package net.highwayfrogs.editor.gui.mesh;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode.OverlayTarget;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;

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
        if (data.getOverlayEntry().getFaceCount() == 0)
            return entry;


        TextureAtlas atlas = getMesh().getTextureAtlas();
        Texture texture = atlas.getTextureFromSource(data.getOverlay());

        int texCoord1 = entry.addTexCoordValue(atlas.getUV(texture, 0, 0));
        int texCoord2 = entry.addTexCoordValue(atlas.getUV(texture, texture.getPaddedWidth() - texture.getUpPadding() - 1, 0));
        int texCoord3 = entry.addTexCoordValue(atlas.getUV(texture, 0, texture.getPaddedHeight() - texture.getLeftPadding() - 1));

        for (int i = 0; i < data.getOverlayEntry().getFaceCount(); i++) {
            int startFaceIndex = (data.getOverlayEntry().getFaceStartIndex() + i) * getMesh().getFaceElementSize();
            int meshVertex1 = getMesh().getFaces().get(startFaceIndex);
            int meshVertex2 = getMesh().getFaces().get(startFaceIndex + 2);
            int meshVertex3 = getMesh().getFaces().get(startFaceIndex + 4);

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
        // Do nothing for now I think.
    }

    @Getter
    @AllArgsConstructor
    public static class OverlayTarget {
        private final DynamicMeshDataEntry overlayEntry;
        private final ITextureSource overlay;
    }
}