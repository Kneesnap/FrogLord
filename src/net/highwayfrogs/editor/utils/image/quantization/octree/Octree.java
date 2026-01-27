package net.highwayfrogs.editor.utils.image.quantization.octree;

import java.util.ArrayList;
import java.util.List;

// I've modified this to be 4 dimensional to support the alpha component, so it's not technically an Octree.
// I don't care enough to rename all this stuff though, since this quantization algorithm seems identifiable by the term "Octree".
class Octree {
    OTNode root;
    int maxLevel;
    List<OTNode> allLeaves;

    public Octree() {
        this.root = null;
        this.maxLevel = 5;
        this.allLeaves = new ArrayList<>();
    }

    public void insert(int a, int r, int g, int b) {
        if (this.root == null)
            this.root = new OTNode(this, null, 0);

        this.root.insert(a, r, g, b);
    }

    public OTNode find(int a, int r, int g, int b) {
        return this.root != null ? this.root.find(a, r, g, b) : null;
    }

    public void reduce(int maxCubes, boolean mergeAlpha) {
        /*System.err.printf("Reducing %d to %d%n",
                this.allLeaves.size(), maxCubes);*/
        while (this.allLeaves.size() > maxCubes) {
            OTNode smallest = this.findMinCube(mergeAlpha);
            if (smallest == null) {
                if (mergeAlpha)
                    throw new IllegalArgumentException("Cannot reduce down to " + maxCubes + " colors because ");

                throw new IllegalArgumentException("Cannot reduce down to " + maxCubes + " colors.");
            }

            smallest.parent.merge(); // Merge children into the parent, turning it into a leaf node.
        }
    }

    public OTNode findMinCube(boolean mergeAlpha) {
        int minCount = Integer.MAX_VALUE;
        int maxLevel = 0;
        OTNode minCube = null;

        // NOTE: I'm not sure if this is a good way to find the nodes, but it seems like it works well enough.
        // A better option might be to store in the parent a sum of all its child.count values. Then, pick the node with the smallest value, with the tiebreaker preferring larger levels.
        // While implementing this is a pretty straightforward task, and it would improve performance, proving that the color selection is better would take quite a bit of effort.
        for (OTNode node : this.allLeaves) {
            if (node.count <= minCount && node.level >= maxLevel && (mergeAlpha || canMergeWithoutDestroyingAlpha(node.parent))) {
                minCube = node;
                minCount = node.count;
                maxLevel = node.level;
            }
        }
        return minCube;
    }

    private static boolean canMergeWithoutDestroyingAlpha(OTNode parent) {
        if (parent.children == null)
            return true;

        for (int i = 0; i < OTNode.ALPHA_INDEX_BIT; i++) {
            OTNode lowAlphaNode = parent.children[i];
            OTNode highAlphaNode = parent.children[i | OTNode.ALPHA_INDEX_BIT];
            if (lowAlphaNode != null && highAlphaNode != null)
                return false; // There's different alpha between the nodes.
        }

        return true;
    }
}

