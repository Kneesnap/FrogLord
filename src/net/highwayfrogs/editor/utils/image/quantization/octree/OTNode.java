package net.highwayfrogs.editor.utils.image.quantization.octree;

class OTNode {
    public final OTNode parent;
    public final int level;
    public final Octree oTree;
    public OTNode[] children;
    public int red;
    public int green;
    public int blue;
    public int alpha;
    public int count;
    public int index = -1; // Index into the allLeaves list.

    private static final int CHILD_COUNT = 16;

    public OTNode(OTNode parent, int level, Octree outer) {
        this.red = 0;
        this.green = 0;
        this.blue = 0;
        this.alpha = 0;
        this.parent = parent;
        this.level = level;
        this.oTree = outer;
    }

    public void insert(int a, int r, int g, int b, int level, Octree outer) {
        if (level < oTree.maxLevel) {
            int index = computeIndex(a, r, g, b, level);
            if (this.children == null)
                this.children = new OTNode[CHILD_COUNT];
            if (this.children[index] == null)
                this.children[index] = new OTNode(this, level + 1, outer);

            this.children[index].insert(a, r, g, b, level + 1, outer);
        } else {
            if (this.count == 0) {
                this.index = this.oTree.allLeaves.size();
                this.oTree.allLeaves.add(this);
            }
            this.red += r;
            this.green += g;
            this.blue += b;
            this.alpha += a;
            this.count += 1;
        }
    }

    public int computeIndex(int a, int r, int g, int b, int level) {
        int nShift = 8 - level; // 8 bits per byte.
        int aBits = (a >> (nShift - 3)) & 0x08;
        int rBits = (r >> (nShift - 2)) & 0x04;
        int gBits = (g >> (nShift - 1)) & 0x02;
        int bBits = (b >> nShift) & 0x01;
        return aBits | rBits | gBits | bBits;
    }

    public OTNode find(int a, int r, int g, int b, int level) {
        if (level < this.oTree.maxLevel) {
            int index = computeIndex(a, r, g, b, level);
            if (this.children != null && this.children[index] != null) {
                return this.children[index].find(a, r, g, b, level + 1);
            } else if (this.count > 0) {
                return this;
            } else {
                /*System.err.printf(
                        "No leaf node to represent RGB(%d, %d, %d)%n",
                        r, g, b);*/
                return null;
            }
        } else {
            return this;
        }
    }

    public int getColor() {
        return makeARGB(this.alpha / this.count, this.red / this.count,
                this.green / this.count, this.blue / this.count);
    }

    public void merge() {
        if (this.children == null)
            return;

        for (OTNode child : this.children) {
            if (child != null) {
                if (child.count > 0) {
                    OTNode removedChild = this.oTree.allLeaves.remove(child.index); // Remove the child.
                    if (removedChild != child)
                        throw new RuntimeException("Removed the wrong child node! (Expected node to be at index " + child.index + ", but got a different node that thought it was at index " + removedChild.index + " instead!)");

                    // Update indices for all remaining leaves.
                    for (int i = child.index; i < this.oTree.allLeaves.size(); i++)
                        this.oTree.allLeaves.get(i).index--;

                    child.index = -1;
                } else {
                    throw new RuntimeException("Recursively merging non-leaf");
                    //child.merge(); // Previously, System.exit(0) was here instead of an exception.
                }

                this.count += child.count;
                this.red += child.red;
                this.green += child.green;
                this.blue += child.blue;
                this.alpha += child.alpha;
            }
        }

        for (int i = 0; i < CHILD_COUNT; i++)
            this.children[i] = null;
    }

    static int makeARGB(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }
}