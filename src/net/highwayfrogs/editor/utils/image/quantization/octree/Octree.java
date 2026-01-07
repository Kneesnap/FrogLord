package net.highwayfrogs.editor.utils.image.quantization.octree;

import java.util.ArrayList;
import java.util.List;

// Technically, I've modified this to be 4 dimensional to support the alpha component, so technically it's not an Octree.
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
            this.root = new OTNode(null, 0, this);

        this.root.insert(a, r, g, b, 0, this);
    }

    public OTNode find(int a, int r, int g, int b) {
        return this.root != null ? this.root.find(a, r, g, b, 0) : null;
    }

    public void reduce(int maxCubes) {
        /*System.err.printf("Reducing %d to %d%n",
                this.allLeaves.size(), maxCubes);*/
        while (this.allLeaves.size() > maxCubes) {
            OTNode smallest = this.findMinCube();
            OTNode smallestParent = smallest.parent;
            smallestParent.merge(); // Merge children into the parent so it will become a leaf node.

            // The parent has just become a leaf (was not previously), so it should be added to the list.
            smallestParent.index = this.allLeaves.size();
            this.allLeaves.add(smallestParent);
        }
    }

    public OTNode findMinCube() {
        int minCount = Integer.MAX_VALUE;
        int maxLevel = 0;
        OTNode minCube = null;

        for (OTNode node : this.allLeaves) {
            if (node.count <= minCount && node.level >= maxLevel) {
                minCube = node;
                minCount = node.count;
                maxLevel = node.level;
            }
        }
        return minCube;
    }
}

