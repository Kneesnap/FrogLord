package net.highwayfrogs.editor.games.sony.shared.mof2.utils;

import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofModelSet;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofXarAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.flipbook.MRMofFlipbookAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimationPolygonTarget;
import net.highwayfrogs.editor.games.sony.shared.mof2.collision.MRMofCollprim;
import net.highwayfrogs.editor.games.sony.shared.mof2.hilite.MRMofHilite;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPartCel;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains static utilities for working with MRModel objects.
 * Created by Kneesnap on 13/07/2025.
 */
public class MRModelUtils {
    /**
     * Bakes the animated mof to a static mof, replacing the previous model in the archive.
     * @param model the model to bake and replace
     * @return newBakedModel
     */
    public static MRModel bakeAndReplaceAnimatedMof(MRModel model) {
        if (model == null)
            throw new NullPointerException("model");

        // Create model.
        MRModel newModel = new MRModel(model.getGameInstance(), model.getCompleteCounterpart());
        newModel.setFileDefinition(model.getFileDefinition());
        newModel.setIncomplete(model.isIncomplete());
        newModel.setVloFile(model.getVloFile());

        // Create new static mof.
        MRStaticMof newStaticMof = new MRStaticMof(newModel);
        newStaticMof.setUnknownValue(model.getStaticMofs().get(0).getUnknownValue());
        newStaticMof.getParts().add(bakeAnimatedMofToStaticMof(model, newStaticMof));

        newModel.setStaticMof(newStaticMof);
        model.getArchive().replaceFile(model.getFileDisplayName(), model.getIndexEntry(), model, newModel, true);
        return newModel;
    }

    /**
     * Bake an animated model into a static mof.
     * @param model the model to bake
     */
    public static MRMofPart bakeAnimatedMofToStaticMof(MRModel model, MRStaticMof targetMof) {
        if (model == null)
            throw new NullPointerException("model");
        if (targetMof == null)
            throw new NullPointerException("targetMof");
        if (!model.isAnimatedMof())
            throw new IllegalArgumentException("The provided model not an animated model! (Was type: " + model.getModelType() + ")");

        MRAnimatedMof animatedMof = model.getAnimatedMof();
        if (animatedMof.getModelSets().size() != 1) // This is a restriction of the MOF models conceptually, not a limitation of FrogLord.
            throw new IllegalArgumentException("Provided models must contain a SINGLE modelSet, otherwise the animation baking process is not possible!");

        MRAnimatedMofModelSet modelSet = animatedMof.getModelSets().get(0);
        if (modelSet.getModels().size() != 1) // This is a restriction of the MOF models conceptually, not a limitation of FrogLord.
            throw new IllegalArgumentException("The modelSet must contain a single model, otherwise the animation baking process is not possible!");

        MRAnimatedMofModel mofModel = modelSet.getModels().get(0);
        MRStaticMof staticMof = mofModel.getStaticMof();

        MRMofPart newStaticPart = new MRMofPart(targetMof);
        MRMofPartPolygonBuilder polygonBuilder = new MRMofPartPolygonBuilder(newStaticPart);

        // Copy collprims.
        boolean hadStaticBoundingBoxes = false;
        for (int i = 0; i < staticMof.getParts().size(); i++) {
            MRMofPart oldPart = staticMof.getParts().get(i);
            int matrixStartIndex = newStaticPart.getCollPrimMatrices().size();
            for (int j = 0; j < oldPart.getCollPrimMatrices().size(); j++) {
                PSXMatrix oldMatrix = oldPart.getCollPrimMatrices().get(j);
                newStaticPart.getCollPrimMatrices().add(oldMatrix.clone());
            }

            for (int j = 0; j < oldPart.getCollPrims().size(); j++) {
                MRMofCollprim oldCollprim = oldPart.getCollPrims().get(j);
                MRMofCollprim newCollprim = oldCollprim.clone(newStaticPart);
                newCollprim.setMatrixIndex(matrixStartIndex+ oldCollprim.getMatrixIndex());
                newStaticPart.getCollPrims().add(newCollprim);
            }

            if (oldPart.getStaticPartcel().getBoundingBox() != null)
                hadStaticBoundingBoxes = true;
        }

        // Copy core model data.
        MRMofPartCel newPartCel = new MRMofPartCel(newStaticPart);
        newStaticPart.getPartCels().add(newPartCel);
        Map<MRMofPolygon, MRMofTextureAnimationPolygonTarget> oldTargetsByOldPolygon = new HashMap<>();
        Map<MRMofTextureAnimation, MRMofTextureAnimation> newAnimations = new HashMap<>();
        Map<MRMofPolygon, MRMofPolygon> newPolygons = new HashMap<>();
        for (int i = 0; i < staticMof.getParts().size(); i++) {
            MRMofPart oldPart = staticMof.getParts().get(i);
            if (oldPart.getPartCels().size() != 1)
                throw new RuntimeException("Expected all oldParts to have a partCelCount of 1, but this one had " + oldPart.getPartCels().size() + "!");

            int vertexStartIndex = newPartCel.getVertices().size();
            int normalStartIndex = newPartCel.getNormals().size();
            newPartCel.getVertices().addAll(oldPart.getStaticPartcel().getVertices());
            newPartCel.getNormals().addAll(oldPart.getStaticPartcel().getNormals());

            // Track old animation targets.
            for (int j = 0; j < oldPart.getTextureAnimationPolygonTargets().size(); j++) {
                MRMofTextureAnimationPolygonTarget target = oldPart.getTextureAnimationPolygonTargets().get(j);
                oldTargetsByOldPolygon.put(target.getPolygon(), target);
            }

            // Copy animations.
            for (int j = 0; j < oldPart.getTextureAnimations().size(); j++) {
                MRMofTextureAnimation oldAnimation = oldPart.getTextureAnimations().get(j);
                MRMofTextureAnimation newAnimation = new MRMofTextureAnimation(newStaticPart);
                newAnimation.getEntries().addAll(oldAnimation.getEntries());
                newAnimations.put(oldAnimation, newAnimation);
                newStaticPart.addTextureAnimation(newAnimation);
            }

            // Convert polygons.
            List<MRMofPolygon> polygons = oldPart.getOrderedPolygons();
            for (int j = 0; j < polygons.size(); j++) {
                MRMofPolygon oldPolygon = polygons.get(j);
                MRMofPolygon newPolygon = oldPolygon.clone(newStaticPart);

                for (int k = 0; k < newPolygon.getVertices().length; k++)
                    newPolygon.getVertices()[k] += vertexStartIndex;
                for (int k = 0; k < newPolygon.getNormals().length; k++)
                    newPolygon.getNormals()[k] += normalStartIndex;
                for (int k = 0; k < newPolygon.getEnvironmentNormals().length; k++)
                    newPolygon.getEnvironmentNormals()[k] += normalStartIndex;

                MRMofTextureAnimationPolygonTarget target = oldTargetsByOldPolygon.remove(oldPolygon);
                if (target != null) {
                    MRMofTextureAnimation newAnimation = newAnimations.get(target.getAnimation());
                    newStaticPart.setTextureAnimation(newPolygon, newAnimation);
                }

                polygonBuilder.addPolygon(newPolygon);
                newPolygons.put(oldPolygon, newPolygon);
            }
        }
        polygonBuilder.applyPolygonsToPart();

        // Convert XAR animations to flipbook.
        List<MRAnimatedMofXarAnimation> xarAnimations = modelSet.getCelSet().getAnimations();
        for (int i = 0; i < xarAnimations.size(); i++) {
            MRAnimatedMofXarAnimation xarAnimation = xarAnimations.get(i);
            MRMofFlipbookAnimation flipbookAnimation = new MRMofFlipbookAnimation(xarAnimation.getFrameCount(), newStaticPart.getPartCels().size());
            newStaticPart.getFlipbook().getAnimations().add(flipbookAnimation);

            for (int frame = 0; frame < xarAnimation.getFrameCount(); frame++) {
                MRMofPartCel newPartCelAnim = new MRMofPartCel(newStaticPart);
                newStaticPart.getPartCels().add(newPartCelAnim);

                for (int j = 0; j < staticMof.getParts().size(); j++) {
                    MRMofPart oldPart = staticMof.getParts().get(j);
                    MRMofPartCel oldPartCel = oldPart.getStaticPartcel();

                    PSXMatrix transformMatrix = animatedMof.getTransformMatrix(oldPart, xarAnimation, frame);
                    for (int k = 0; k < oldPartCel.getVertices().size(); k++) {
                        SVector vertex = oldPartCel.getVertices().get(k);
                        newPartCelAnim.getVertices().add(new SVector(PSXMatrix.MRApplyMatrix(transformMatrix, vertex, new IVector())));
                    }

                    for (int k = 0; k < oldPartCel.getNormals().size(); k++) {
                        SVector normal = oldPartCel.getNormals().get(k);
                        newPartCelAnim.getNormals().add(normal.clone());
                    }
                }
            }
        }

        // Copy hilites.
        for (int i = 0; i < staticMof.getParts().size(); i++) {
            MRMofPart oldPart = staticMof.getParts().get(i);
            for (int j = 0; j < oldPart.getHilites().size(); j++) {
                MRMofHilite oldHilite = oldPart.getHilites().get(j);
                MRMofHilite newHilite = new MRMofHilite(newStaticPart);
                newHilite.setHiliteType(oldHilite.getHiliteType());
                switch (oldHilite.getAttachType()) {
                    case VERTEX:
                        newHilite.setVertex(oldHilite.getVertex()); // Should still equal the vertex.
                        break;
                    case PRIM:
                        newHilite.setPolygon(newPolygons.get(oldHilite.getPolygon()));
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported HiliteAttachType: " + oldHilite.getHiliteType());
                }

                newStaticPart.getHilites().add(newHilite);
            }
        }

        if (hadStaticBoundingBoxes)
            newPartCel.setBoundingBox(newStaticPart.makeBoundingBox());
        return newStaticPart;
    }
}
