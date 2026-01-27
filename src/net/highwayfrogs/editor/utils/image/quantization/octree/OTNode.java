package net.highwayfrogs.editor.utils.image.quantization.octree;

class OTNode {
    public final Octree tree;
    public final OTNode parent;
    public final int level;
    public OTNode[] children;
    public int red;
    public int green;
    public int blue;
    public int alpha;
    public int count;

    private static final int CHILD_COUNT = 16;
    static final int ALPHA_INDEX_BIT = 0x08;

    public OTNode(Octree tree, OTNode parent, int level) {
        this.red = 0;
        this.green = 0;
        this.blue = 0;
        this.alpha = 0;
        this.parent = parent;
        this.level = level;
        this.tree = tree;
    }

    public void insert(int a, int r, int g, int b) {
        if (this.level < this.tree.maxLevel) {
            int index = computeIndex(a, r, g, b);
            if (this.children == null)
                this.children = new OTNode[CHILD_COUNT];
            if (this.children[index] == null)
                this.children[index] = new OTNode(this.tree, this, this.level + 1);

            this.children[index].insert(a, r, g, b);
        } else {
            addColor(r, g, b, a, 1);
        }
    }

    public int computeIndex(int a, int r, int g, int b) {
        int nShift = 8 - this.level - 1; // 8 bits per byte, but we only want to shift a max of 7, min of 1.
        int aBits = (a >> (nShift - 3)) & 0x08; // The same bit is tested across each component, despite the difference in shift amount. (We subtract so that building the index is easier, not to test a different bit)
        int rBits = (r >> (nShift - 2)) & 0x04;
        int gBits = (g >> (nShift - 1)) & 0x02;
        int bBits = (b >> nShift) & 0x01;
        return aBits | rBits | gBits | bBits;
    }

    public OTNode find(int a, int r, int g, int b) {
        if (this.level >= this.tree.maxLevel || this.count > 0)
            return this; // This is a leaf.

        int index = computeIndex(a, r, g, b);
        if (this.children != null && this.children[index] != null) {
            return this.children[index].find(a, r, g, b);
        } else {
                /*System.err.printf(
                        "No leaf node to represent RGB(%d, %d, %d)%n",
                        r, g, b);*/
            return null;
        }
    }

    public int getColor() {
        return makeARGB(this.alpha / this.count, this.red / this.count,
                this.green / this.count, this.blue / this.count);
    }

    public void merge() {
        if (this.children == null)
            return;

        for (int i = 0; i < CHILD_COUNT; i++) {
            OTNode child = this.children[i];
            if (child == null)
                continue;

            if (child.count <= 0) {
                //child.merge(); // Previously, System.exit(0) was here instead of an exception.
                throw new RuntimeException("Recursively merging non-leaf.");
            }

            // Remove the child.
            if (!this.tree.allLeaves.remove(child))
                throw new RuntimeException("Failed to remove the child node!");

            // Add the colors from the child node to this node.
            addColor(child.red, child.green, child.blue, child.alpha, child.count);
            this.children[i] = null;
        }
    }

    private void addColor(int r, int g, int b, int a, int colorCount) {
        if (this.count == 0)
            this.tree.allLeaves.add(this);

        this.red += r;
        this.green += g;
        this.blue += b;
        this.alpha += a;
        this.count += colorCount;
    }

    static int makeARGB(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }
}