package net.highwayfrogs.editor.games.konami.greatquest.model;

/**
 * A registry of different kcGameSystem primitive drawing types.
 * Created by Kneesnap on 6/22/2023.
 */
public enum kcPrimitiveType {
    UNSUPPORTED,
    POINT_LIST,
    LINE_LIST,
    LINE_STRIP,
    TRIANGLE_LIST,
    TRIANGLE_STRIP,
    TRIANGLE_FAN;

    /**
     * Calculate the number of primitives of the primitive type formed from a provided number of vertices.
     * @param numberOfVertices The number of vertices.
     * @return primitive count
     */
    public int calculatePrimCount(int numberOfVertices) {
        switch (this) {
            case POINT_LIST:
                return numberOfVertices;
            case LINE_LIST:
                return numberOfVertices / 2;
            case LINE_STRIP:
                return numberOfVertices - 1;
            case TRIANGLE_LIST:
                return numberOfVertices / 3;
            case TRIANGLE_STRIP:
            case TRIANGLE_FAN:
                return numberOfVertices - 2;
            default:
                throw new RuntimeException("Cannot calculate the prim count for kcPrimitiveType: " + this);
        }
    }

    /**
     * Calculates the number of vertices required to form a number of prims of the primitive type.
     * @param primCount The number of prims to require.
     * @return Number of vertices required
     */
    public int calculateVertexCount(int primCount) {
        switch (this) {
            case POINT_LIST:
                return primCount;
            case LINE_LIST:
                return primCount * 2;
            case LINE_STRIP:
                return primCount + 1;
            case TRIANGLE_LIST:
                return primCount * 3;
            case TRIANGLE_STRIP:
            case TRIANGLE_FAN:
                return primCount + 2;
            default:
                throw new RuntimeException("Cannot calculate the vertex count for kcPrimitiveType: " + this);
        }
    }
}