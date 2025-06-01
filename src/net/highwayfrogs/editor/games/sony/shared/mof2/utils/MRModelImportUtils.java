package net.highwayfrogs.editor.games.sony.shared.mof2.utils;

import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimationPolygonTarget;
import net.highwayfrogs.editor.games.sony.shared.mof2.collision.MRMofCollprim;
import net.highwayfrogs.editor.games.sony.shared.mof2.hilite.MRMofHilite;
import net.highwayfrogs.editor.games.sony.shared.mof2.hilite.MRMofHilite.HiliteAttachType;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.util.*;

/**
 * Contains utilities which may be useful when converting to the mof format.
 * Created by Kneesnap on 5/20/2025.
 */
public class MRModelImportUtils {
    /**
     * Copies data from an old mof part to a new mof part.
     * This is useful because most 3D modelling tools won't be able to track or edit much of the non-mesh data stored within a MOF.
     * So, instead of deleting it when replacing an old part with a new one, why not copy it directly?
     * This function should only be called after all partCels, vertices, normals, and polygons have been fully setup.
     * What all does this function copy?
     *  - Collprims
     *  - Collprim matrices
     *  - Hilites
     *  - Texture animations
     * @param oldPart the old part which the data should be copied from
     * @param newPart the new part which the data should be copied to
     */
    public static void copyNonMeshDataFromOldPartToNewPart(ILogger logger, MRMofPart oldPart, MRMofPart newPart) {
        if (oldPart == null)
            throw new NullPointerException("newPart");
        if (newPart == null)
            throw new NullPointerException("oldPart");
        if (oldPart == newPart)
            throw new IllegalArgumentException("The oldPart and the newPart were the same.");

        // Copy collprim matrices.
        if (oldPart.getCollPrimMatrices().size() > 0)
            newPart.getCollPrimMatrices().addAll(oldPart.getCollPrimMatrices());

        // Copy collprims.
        if (oldPart.getCollPrims() != null)
            for (MRMofCollprim oldCollprim : oldPart.getCollPrims())
                newPart.getCollPrims().add(oldCollprim.clone(newPart));

        // Copy hilites.
        Map<SVector, List<MRMofPolygon>> polygonsPerVertex = getPolygonsByVertex(newPart);
        for (MRMofHilite oldHilite : oldPart.getHilites()) {
            MRMofHilite newHilite = new MRMofHilite(newPart);
            newHilite.setHiliteType(oldHilite.getHiliteType());

            String failureReason;
            switch (oldHilite.getAttachType()) {
                case PRIM:
                    failureReason = "The polygon used by the old hilite wasn't identified in the new mof part.";
                    newHilite.setPolygon(getNewPolygonFromOldPolygon(newPart, oldHilite.getPolygon(), polygonsPerVertex));
                    break;
                case VERTEX:
                    failureReason = "The vertex used by the old hilite wasn't found in the new mof part.";
                    int vertexId = getIndexOfNewVertexInPartCelOfOldVertex(newPart, oldHilite.getVertex(), .25);
                    if (vertexId >= 0)
                        newHilite.setVertex(newPart.getStaticPartcel().getVertices().get(vertexId));
                    break;
                case NONE:
                    failureReason = null; // Just ignore the failure.
                    break;
                default:
                    failureReason = "The old hilite's attachment type was not supported (" + oldHilite.getAttachType() + ")";
                    break;
            }

            if (newHilite.getAttachType() != HiliteAttachType.NONE) {
                newPart.getHilites().add(newHilite);
            } else if (logger != null && failureReason != null) {
                logger.warning("Failed to copy a hilite to the new mof part!%nReason: %s", failureReason);
            }
        }

        // Copy texture animations to the new model.
        Map<MRMofTextureAnimation, MRMofTextureAnimation> oldToNewAnimations = new HashMap<>();
        for (MRMofTextureAnimation oldAnimation : oldPart.getTextureAnimations()) {
            MRMofTextureAnimation newAnimation = oldAnimation.clone(newPart);
            if (newPart.addTextureAnimation(newAnimation)) {
                oldToNewAnimations.put(oldAnimation, newAnimation);
            } else if (logger != null) {
                logger.warning("Failed to copy a texture animation to the new mof part!");
            }
        }

        // Copy texture animation targets, when they are possible to resolve.
        Map<GameImage, MRMofTextureAnimation> animationsByImage = new HashMap<>();
        List<MRMofTextureAnimationPolygonTarget> missedAnimationTargets = new ArrayList<>();
        for (MRMofTextureAnimationPolygonTarget animationTarget : oldPart.getTextureAnimationPolygonTargets()) {
            MRMofTextureAnimation newAnimation = oldToNewAnimations.get(animationTarget.getAnimation());
            if (newAnimation == null)
                continue;

            MRMofPolygon oldPolygon = animationTarget.getPolygon();
            GameImage defaultTexture = oldPolygon != null ? oldPolygon.getDefaultTexture() : null;
            MRMofTextureAnimation temp;
            if (defaultTexture != null && ((temp = animationsByImage.putIfAbsent(defaultTexture, animationTarget.getAnimation())) != null && temp != animationTarget.getAnimation()))
                missedAnimationTargets.add(animationTarget);
        }

        // Apply animations by texture ID.
        List<MRMofPolygon> polygons = newPart.getOrderedPolygons();
        for (MRMofPolygon polygon : polygons) {
            MRMofTextureAnimation textureAnimation = animationsByImage.get(polygon.getDefaultTexture());
            if (textureAnimation != null)
                newPart.setTextureAnimation(polygon, oldToNewAnimations.get(textureAnimation));
        }

        // Apply remaining animations.
        for (MRMofTextureAnimationPolygonTarget animationTarget : missedAnimationTargets) {
            MRMofTextureAnimation newAnimation = oldToNewAnimations.get(animationTarget.getAnimation());
            if (newAnimation == null)
                continue;

            MRMofPolygon oldPolygon = animationTarget.getPolygon();
            MRMofPolygon newPolygon = getNewPolygonFromOldPolygon(newPart, oldPolygon, polygonsPerVertex);
            if (newPolygon != null)
                newPart.setTextureAnimation(newPolygon, newAnimation);
        }

        // Don't transition flipbook, because that is the responsibility of the individual file format to allow for animations like that.
    }

    private static Map<SVector, List<MRMofPolygon>> getPolygonsByVertex(MRMofPart mofPart) {
        Map<SVector, List<MRMofPolygon>> polygonsPerVertex = new HashMap<>();
        List<MRMofPolygon> polygons = mofPart.getOrderedPolygons();
        List<SVector> vertices = mofPart.getStaticPartcel().getVertices();
        for (int i = 0; i < polygons.size(); i++) {
            MRMofPolygon polygon = polygons.get(i);
            for (int j = 0; j < polygon.getVertexCount(); j++) {
                int vertexId = polygon.getVertices()[j];
                if (vertexId < 0 || vertexId >= vertices.size())
                    continue;

                SVector vertex = vertices.get(vertexId);
                List<MRMofPolygon> vertexPolygons = polygonsPerVertex.computeIfAbsent(vertex, key -> new ArrayList<>());
                if (!vertexPolygons.contains(polygon))
                    vertexPolygons.add(polygon);
            }
        }

        return polygonsPerVertex;
    }

    private static int getIndexOfNewVertexInPartCelOfOldVertex(MRMofPart newPart, SVector oldVertexPos, double distanceThreshold) {
        double bestDistance = Double.MAX_VALUE;
        List<SVector> vertices = newPart.getStaticPartcel().getVertices();

        int bestVertexId = -1;
        for (int i = 0; i < vertices.size(); i++) {
            double testDistance = vertices.get(i).distanceSquared(oldVertexPos);
            if (testDistance < bestDistance) {
                bestDistance = testDistance;
                bestVertexId = i;
            }
        }

        if (bestVertexId >= 0 && (distanceThreshold * distanceThreshold) >= bestDistance)
            return bestVertexId;

        return -1;
    }

    private static MRMofPolygon getNewPolygonFromOldPolygon(MRMofPart newPart, MRMofPolygon oldPolygon, Map<SVector, List<MRMofPolygon>> polygonsPerVertex) {
        List<SVector> oldVertices = oldPolygon.getMofPart().getStaticPartcel().getVertices();
        List<SVector> newVertices = newPart.getStaticPartcel().getVertices();

        // Find the list of polygons to search.
        HashSet<MRMofPolygon> polygonSet = new HashSet<>();
        for (int i = 0; i < oldPolygon.getVertexCount(); i++) {
            List<MRMofPolygon> polygons = polygonsPerVertex.get(oldVertices.get(oldPolygon.getVertices()[i]));
            if (polygons != null)
                polygonSet.addAll(polygons);
        }
        for (int i = 0; i < oldPolygon.getVertexCount(); i++) {
            int oldVertexId = oldPolygon.getVertices()[i];
            int newVertexId = getIndexOfNewVertexInPartCelOfOldVertex(newPart, oldVertices.get(oldVertexId), .5);
            if (newVertexId < 0 || newVertexId >= newVertices.size())
                continue;

            List<MRMofPolygon> polygons = polygonsPerVertex.get(newVertices.get(newVertexId));
            if (polygons != null)
                polygonSet.addAll(polygons);
        }

        if (polygonSet.isEmpty())
            return null; // Couldn't resolve it.

        for (MRMofPolygon newPolygon : polygonSet) {
            boolean allVerticesMatch = true;
            if (oldPolygon.getPolygonType().isTextured() != newPolygon.getPolygonType().isTextured())
                continue;
            if (oldPolygon.getPolygonType().isTextured() && oldPolygon.getTextureId() != newPolygon.getTextureId())
                continue; // Wrong texture, abort!

            // Test if the polygon vertices appear to be roughly in the same area.
            for (int j = 0; j < Math.min(oldPolygon.getVertexCount(), newPolygon.getVertexCount()) && allVerticesMatch; j++) {
                SVector newVertex = newVertices.get(newPolygon.getVertices()[j]);
                boolean matchedVertex = false;
                for (int k = 0; k < oldPolygon.getVertexCount() && !matchedVertex; k++) {
                    SVector oldVertex = oldVertices.get(oldPolygon.getVertices()[k]);
                    if (newVertex.distanceSquared(oldVertex) <= 1)
                        matchedVertex = true;
                }

                if (!matchedVertex)
                    allVerticesMatch = false;
            }

            if (allVerticesMatch)
                return newPolygon;
        }

        return null;
    }
}