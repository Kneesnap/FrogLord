package net.highwayfrogs.editor.games.sony.shared.model.meshview;

import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.shared.model.PTModel;
import net.highwayfrogs.editor.games.sony.shared.model.PTTransformInstanceData;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.PTPolygon;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.PTPrimitiveBlock;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.PTPrimitiveType;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticFile;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPart;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPartCel;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.math.Vector2f;

/**
 * Implements mesh logic for displaying parts of a PTModel.
 * Created by Kneesnap on 5/22/2024.
 */
public class PTModelMeshNode extends DynamicMeshAdapterNode<PTStaticPartCel> {
    private final Vector2f tempVector = new Vector2f();

    public PTModelMeshNode(PTModelMesh mesh) {
        super(mesh);
    }

    @Override
    public PTModelMesh getMesh() {
        return (PTModelMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Add polygons.
        for (PTStaticPart part : getMeshFile().getParts())
            for (PTStaticPartCel partCel : part.getPartCels())
                add(partCel);
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(PTStaticPartCel data) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), data);

        // Add polygons.
        for (int i = 0; i < data.getPrimitiveBlocks().size(); i++) {
            PTPrimitiveBlock block = data.getPrimitiveBlocks().get(i);
            if (block.getPrimitiveType() == PTPrimitiveType.CONTROL || block.getPrimitiveType() == null)
                continue; // Skip non-polygon.

            for (int j = 0; j < block.getPrimitives().size(); j++)
                addPolygon(entry, (PTPolygon) block.getPrimitives().get(j));
        }

        return entry;
    }

    private SVector rotateVector(PTStaticPartCel partCel, SVector input) {
        if (getModel().getTransformData().size() <= partCel.getParentPart().getTransformId() )
            return input; // TODO: This occurs when the .SKEL file isn't resolved for a model, and it uses the part count for the transform count.

        PTTransformInstanceData transform = getModel().getTransformData().get(partCel.getParentPart().getTransformId());
        IVector result = new IVector();
        PSXMatrix.MRApplyMatrix(transform.getMatrix(), input, result);
        return new SVector(result.getFloatX(), result.getFloatY(), result.getFloatZ());
    }

    private void addPolygon(DynamicMeshTypedDataEntry entry, PTPolygon polygon) {
        // Resolve texture.
        ITextureSource textureSource = getMesh().getShadedTextureManager().getShadedTexture(polygon);
        Texture texture = getMesh().getTextureAtlas().getTextureFromSourceOrFallback(textureSource);

        SVector vertex1 = rotateVector(entry.getDataSource(), entry.getDataSource().getVertex(getModel(), polygon.getVertices()[0]));
        SVector vertex2 = rotateVector(entry.getDataSource(), entry.getDataSource().getVertex(getModel(), polygon.getVertices()[1]));
        SVector vertex3 = rotateVector(entry.getDataSource(), entry.getDataSource().getVertex(getModel(), polygon.getVertices()[2]));
        int vtxIndex1 = entry.addVertexValue(vertex1.getFloatX(), vertex1.getFloatY(), vertex1.getFloatZ());
        int vtxIndex2 = entry.addVertexValue(vertex2.getFloatX(), vertex2.getFloatY(), vertex2.getFloatZ());
        int vtxIndex3 = entry.addVertexValue(vertex3.getFloatX(), vertex3.getFloatY(), vertex3.getFloatZ());

        // Add texture UVs.
        if (polygon.getVertexCount() == 4) {
            int uvIndex1 = entry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 0, Vector2f.ZERO)); // uvTopLeft, 0F, 0F
            int uvIndex2 = entry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 1, Vector2f.UNIT_X)); // uvTopRight, 1F, 0F
            int uvIndex3 = entry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 2, Vector2f.UNIT_Y)); // uvBottomLeft, 0F, 1F
            int uvIndex4 = entry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 3, Vector2f.ONE)); // uvBottomRight, 1F, 1F

            // Vertice IDs are the same IDs seen in the map data.
            //int vtxIndex1 = this.vertexEntry.getVertexStartIndex() + polygon.getVertices()[0];
            //int vtxIndex2 = this.vertexEntry.getVertexStartIndex() + polygon.getVertices()[1];
            //int vtxIndex3 = this.vertexEntry.getVertexStartIndex() + polygon.getVertices()[2];
            //int vtxIndex4 = this.vertexEntry.getVertexStartIndex() + polygon.getVertices()[3];

            SVector vertex4 = rotateVector(entry.getDataSource(), entry.getDataSource().getVertex(getModel(), polygon.getVertices()[3]));
            int vtxIndex4 = entry.addVertexValue(vertex4.getFloatX(), vertex4.getFloatY(), vertex4.getFloatZ());

            // JavaFX uses counter-clockwise winding order.
            entry.addFace(vtxIndex3, uvIndex3, vtxIndex2, uvIndex2, vtxIndex1, uvIndex1);
            entry.addFace(vtxIndex3, uvIndex3, vtxIndex4, uvIndex4, vtxIndex2, uvIndex2);
        } else {
            int uvIndex1 = entry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 0, Vector2f.ZERO)); // uvTopLeft, 0F, 0F
            int uvIndex2 = entry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 1, Vector2f.UNIT_X)); // uvTopRight, 1F, 0F
            int uvIndex3 = entry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 2, Vector2f.UNIT_Y)); // uvBottomLeft, 0F, 1F

            // Vertice IDs are the same IDs seen in the map data.
            //int vtxIndex1 = this.vertexEntry.getVertexStartIndex() + polygon.getVertices()[0];
            //int vtxIndex2 = this.vertexEntry.getVertexStartIndex() + polygon.getVertices()[1];
            //int vtxIndex3 = this.vertexEntry.getVertexStartIndex() + polygon.getVertices()[2];

            // JavaFX uses counter-clockwise winding order.
            entry.addFace(vtxIndex3, uvIndex3, vtxIndex2, uvIndex2, vtxIndex1, uvIndex1);
        }
    }

    private Vector2f getTextureCoordinate(PTPolygon polygon, ITextureSource textureSource, Texture texture, int index, Vector2f fallback) {
        Vector2f localUv;
        if (polygon.getPolygonType().isTextured()) {
            localUv = polygon.getTextureUvs()[index].toVector(this.tempVector);
        } else {
            localUv = this.tempVector.setXY(fallback);
        }

        // Get the UVs local to the texture.
        PSXShadeTextureDefinition.tryApplyUntexturedShadingPadding(texture, textureSource, localUv);
        return getMesh().getTextureAtlas().getUV(texture, localUv);
    }
    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        /*SVector vertexPos = entry.getDataSource().getSkinVertices().get(localVertexIndex);
        entry.writeVertexXYZ(localVertexIndex, vertexPos.getFloatX(), vertexPos.getFloatY(), vertexPos.getFloatZ());*/
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        /*PTStaticPartCel partCel = entry.getDataSource();
        PTPolygon polygon = null;

        for (int i = 0; i < partCel.getPrimitiveBlocks().size(); i++) {
            PTPrimitiveBlock primitiveBlock = partCel.getPrimitiveBlocks().get(i);

            for (int j = 0; j < )

        }

        if (polygon == null)
            return;

        ITextureSource textureSource = getMesh().getShadedTextureManager().getShadedTexture(polygon);
        Texture texture = getMesh().getTextureAtlas().getTextureFromSourceOrFallback(textureSource);

        Vector2f uv;
        switch (localTexCoordIndex) {
            case 0:
                uv = getTextureCoordinate(polygon, textureSource, texture, 0, Vector2f.ZERO); // uvTopLeft, 0F, 0F
                break;
            case 1:
                uv = getTextureCoordinate(polygon, textureSource, texture, 1, Vector2f.UNIT_X); // uvTopRight, 1F, 0F
                break;
            case 2:
                uv = getTextureCoordinate(polygon, textureSource, texture, 2, Vector2f.UNIT_Y); // uvBottomLeft, 0F, 1F
                break;
            case 3:
                uv = getTextureCoordinate(polygon, textureSource, texture, 3, Vector2f.ONE); // uvBottomRight, 1F, 1F
                break;
            default:
                throw new IllegalArgumentException("Unsupported local texCoordIndex " + localTexCoordIndex);
        }

        entry.writeTexCoordValue(localTexCoordIndex, uv);*/
    }

    /**
     * Gets the model which mesh data comes from.
     */
    public PTModel getModel() {
        return getMesh().getModel();
    }

    /**
     * Gets the static mesh file which mesh data comes from.
     */
    public PTStaticFile getMeshFile() {
        return getMesh().getModel().getStaticMeshFile();
    }
}