package net.highwayfrogs.editor.games.konami.greatquest.map.octree;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.map.octree.kcOctTree.kcOctTreeStatus;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Implementation of _kcOctTreeTraversalInfo.
 * @param <TContext> the context type
 * Created by Kneesnap on 11/17/2024.
 */
@Getter
@Setter
public class kcOctTreeTraversalInfo<TContext> {
    private final kcOctTree tree;
    private final TContext context;
    private final Vector3f nodeOrigin;
    private float nodeDimension;
    private final BiFunction<kcOctTreeTraversalInfo<TContext>, Integer, kcOctTreeStatus> testCallback;
    private final BiConsumer<TContext, kcOctLeaf> actionCallback;
    private final IntList nodeIds = new IntList(16);

    public kcOctTreeTraversalInfo(kcOctTree tree, BiFunction<kcOctTreeTraversalInfo<TContext>, Integer, kcOctTreeStatus> testCallback, BiConsumer<TContext, kcOctLeaf> actionCallback, TContext context) {
        if (tree == null)
            throw new NullPointerException("tree");
        if (testCallback == null)
            throw new NullPointerException("testCallback");
        if (actionCallback == null)
            throw new NullPointerException("actionCallback");

        // kcOctTreeTraverse
        this.tree = tree;
        this.context = context;
        this.nodeOrigin = tree.getOffset().clone();
        this.testCallback = testCallback;
        this.actionCallback = actionCallback;
    }

    /**
     * Adds a leaf to the results.
     * @param leafId the leaf id to add
     */
    public void addLeaf(int leafId) {
        if ((leafId & kcOctTree.FLAG_IS_LEAF) != kcOctTree.FLAG_IS_LEAF)
            throw new IllegalArgumentException("The ID " + NumberUtils.toHexString(leafId) + " was not a leaf id!");

        if (this.nodeIds.isArrayFull())
            runActionCallbacks();
        this.nodeIds.add(leafId);
    }

    /**
     * Runs action callbacks for each of the queued node ids.
     */
    public void runActionCallbacks() {
        for (int i = 0; i < this.nodeIds.size(); i++) {
            int leafId = this.nodeIds.get(i);
            kcOctLeaf leaf = this.tree.getLeaves().get(leafId);
            this.actionCallback.accept(this.context, leaf);
        }
        this.nodeIds.clear();
    }

    /**
     * Runs the test callback for the given branch id.
     * @param branchId the branch id to run the callback for
     * @return status
     */
    public kcOctTreeStatus runTestCallback(int branchId) {
        kcOctTreeStatus status = this.testCallback.apply(this, branchId);
        if (status == null)
            throw new IllegalArgumentException("Received a null kcOctTreeStatus from the test callback!");

        return status;
    }
}
