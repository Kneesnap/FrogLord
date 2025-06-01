package net.highwayfrogs.editor.games.sony.shared.mof2.utils;

import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofModelSet;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofXarAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform.MRAnimatedMofTransform;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains static optimization utilities and algorithms for improving MRMof files.
 * Because mof files are a bit complex, it's often desirable to maybe make the MOF file importing/conversion logic from other file formats simpler.
 * This class serves that role by allowing less efficient MOF importing processes, then all the various optimization algorithms available here work on any valid MRModel once it's loaded.
 * For example, if a particular 3D file format doesn't support quads, instead of implementing a tri->quad algorithm for every single 3D file format importer, we'll target the MRModel itself to make it usable with all of them.
 * Created by Kneesnap on 5/20/2025.
 */
public class MRMofOptimizer {
    /**
     * Removes all duplicate transforms from the model.
     * Benefit: Reduces memory usage of the model.
     * @param animatedMof the animatedMof to clear duplicated transforms from
     * @return the number of removed transforms
     */
    public static int removeDuplicateAnimationTransforms(MRAnimatedMof animatedMof) {
        if (animatedMof == null)
            throw new NullPointerException("animatedMof");

        // Determine which transform IDs should be removed.
        List<MRAnimatedMofTransform> transforms = animatedMof.getCommonData().getTransforms();
        IndexBitArray queuedRemovals = new IndexBitArray(transforms.size());

        Map<MRAnimatedMofTransform, Integer> transformIndices = new HashMap<>();
        for (int i = 0; i < transforms.size(); i++) {
            MRAnimatedMofTransform transform = transforms.get(i);
            if (transformIndices.putIfAbsent(transform, i) != null)
                queuedRemovals.setBit(i, true);
        }

        // Mark all transforms as used.
        IndexBitArray usedTransforms = new IndexBitArray(transforms.size());
        for (int i = 0; i < animatedMof.getModelSets().size(); i++) {
            MRAnimatedMofModelSet modelSet = animatedMof.getModelSets().get(i);
            for (int j = 0; j < modelSet.getCelSet().getAnimations().size(); j++) {
                MRAnimatedMofXarAnimation animation = modelSet.getCelSet().getAnimations().get(j);
                condenseDuplicatedTransformUsages(animation, transformIndices); // Do this before start tracking which transforms are used.

                // Mark all actually used transform IDs as used.
                for (int k = 0; k < animation.getTransformIds().size(); k++)
                    usedTransforms.setBit(animation.getTransformIds().get(k), true);
            }
        }

        // Mark unused transforms as pending removal.
        for (int i = 0; i < transforms.size(); i++)
            if (!usedTransforms.getBit(i))
                queuedRemovals.setBit(i, true);

        // Calculate/cache how many removals should occur for a transform ID at a given point.
        int[] transformIdOffsets = queuedRemovals.calculateRemovalAmountLookupBuffer(transforms.size());
        for (int i = 0; i < animatedMof.getModelSets().size(); i++) {
            MRAnimatedMofModelSet modelSet = animatedMof.getModelSets().get(i);
            for (int j = 0; j < modelSet.getCelSet().getAnimations().size(); j++) {
                MRAnimatedMofXarAnimation animation = modelSet.getCelSet().getAnimations().get(j);
                for (int k = 0; k < animation.getTransformIds().size(); k++) {
                    short newTransformId = animation.getTransformIds().get(k);
                    animation.getTransformIds().set(k, (short) (newTransformId - transformIdOffsets[newTransformId]));
                }
            }
        }

        // Remove the queued transforms from the list. Must run last, so old transforms can still evaluate until the moment we're finished.
        queuedRemovals.removeValuesFromList(transforms);
        return queuedRemovals.getBitCount();
    }

    private static void condenseDuplicatedTransformUsages(MRAnimatedMofXarAnimation animation, Map<MRAnimatedMofTransform, Integer> transformIds) {
        if (animation == null)
            throw new NullPointerException("animation");
        if (transformIds == null)
            throw new NullPointerException("transformIds");

        // Mark duplicated transforms for removal.
        List<MRAnimatedMofTransform> transforms = animation.getParentCelSet().getParentModelSet().getParentMof().getCommonData().getTransforms();
        IndexBitArray queuedRemovals = new IndexBitArray(animation.getTransformIds().size());
        for (int i = 0; i < animation.getTransformIds().size(); i++) {
            int transformId = animation.getTransformIds().get(i);
            MRAnimatedMofTransform transform = transforms.get(transformId);

            Integer replacementTransformId = transformIds.get(transform);
            if (replacementTransformId != null && replacementTransformId != transformId)
                animation.getTransformIds().set(i, (short) (int) replacementTransformId);
        }

        // TODO: Condense pairs of transformIds too, to simplify celNumber references?

        // Remove the queued targets locally.
        queuedRemovals.removeValuesFromList(animation.getTransformIds());
    }

    /**
     * Attempts to convert the current animation into an interpolated animation.
     * Benefit: Reduces memory usage of the model at the cost of increased CPU work when rendering in-game.
     * @param mofPart the mofPart to optimize
     * @return true if enabling interpolation actually saved memory. (If it didn't, interpolation will be disabled)
     *  Alternatively, it might also just mean that interpolation was already enabled.
     */
    public static boolean convertToInterpolatedModel(MRMofPart mofPart) {
        if (mofPart == null)
            throw new NullPointerException("mofPart");

        // TODO: Implement.
        throw new UnsupportedOperationException("This feature has not been added yet.");
    }

    /**
     * Condenses any triangles forming the shape of a quad into actual quads.
     * Benefit: Reduces memory usage of the model, and makes detections new versions of old polygons more reliable.
     * @param mofPart the mofPart to optimize
     */
    public static void convertTrianglesFormingQuadsIntoQuads(MRMofPart mofPart) {
        if (mofPart == null)
            throw new NullPointerException("mofPart");

        // TODO: Implement.
        throw new UnsupportedOperationException("This feature has not been added yet.");
    }

    /**
     * Removes any duplicate vertices seen in the mof part.
     * Benefit: Reduces memory usage of the model.
     * @param mofPart the mofPart to optimize
     */
    public static void removeDuplicateVertices(MRMofPart mofPart) {
        if (mofPart == null)
            throw new NullPointerException("mofPart");

        // TODO: Implement. (NOTE: Consider that the vertex must be a perfect match on all partCels as well.)
        throw new UnsupportedOperationException("This feature has not been added yet.");
    }

    /**
     * Calculates the normals of all faces from scratch.
     * Benefit: Makes it very easy to deal with normals if the file doesn't contain any normals.
     * @param mofPart the mofPart to optimize
     */
    public static void recalculateNormals(MRMofPart mofPart) {
        if (mofPart == null)
            throw new NullPointerException("mofPart");

        // TODO: Implement.
        throw new UnsupportedOperationException("This feature has not been added yet.");
    }
}
