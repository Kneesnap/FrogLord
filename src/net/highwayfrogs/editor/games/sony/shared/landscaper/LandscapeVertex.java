package net.highwayfrogs.editor.games.sony.shared.landscaper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.system.math.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a vertex used as part of a landscape.
 * Created by Kneesnap on 7/16/2024.
 */
public abstract class LandscapeVertex extends SCSharedGameData implements ILandscapeComponent {
    @Getter private final LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon> owner; // The node which owns the vertex.
    @Getter @Setter(AccessLevel.PACKAGE) private int vertexId = -1;
    private final List<LandscapePolygon> connectedPolygons = new ArrayList<>();
    private final List<LandscapePolygon> immutableConnectedPolygons = Collections.unmodifiableList(this.connectedPolygons);

    public LandscapeVertex(LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon> owner) {
        super(owner.getGameInstance());
        this.owner = owner;
    }

    @Override
    public Landscape getLandscape() {
        return this.owner != null ? this.owner.getLandscape() : null;
    }

    @Override
    public boolean isRegistered() {
        return getLandscape() != null && this.vertexId >= 0;
    }

    /**
     * Gets the polygons using this vertex.
     */
    public List<LandscapePolygon> getConnectedPolygons() {
        return this.immutableConnectedPolygons;
    }

    /**
     * Get the internal connected polygon list for editing purposes.
     * This list should be handled with extra care when a polygon has the same vertex more than once.
     * When that occurs, the polygon should only be in this list once, but it also should not be removed prematurely.
     * Eg: If we remove a vertex, but the vertex is still used at another index, it should not be removed from the list.
     */
    List<LandscapePolygon> getInternalConnectedPolygonList() {
        return this.connectedPolygons;
    }

    /**
     * Gets the local vertex position without object allocation, when feasible.
     * Do not modify the returned object as it may not be safe to modify.
     * @return localPosition
     */
    public Vector3f getCachedLocalPosition() {
        return getNewLocalPosition();
    }

    /**
     * Gets the local vertex position.
     * The object returned is guaranteed to be safe to modify, as it is a new instance.
     * @return localPosition
     */
    public Vector3f getNewLocalPosition() {
        return getLocalPosition(new Vector3f());
    }

    /**
     * Store the local vertex position into the given output vector.
     * @param output the vector to store the position within. If null, a new vector will be allocated.
     * @return localPosition
     */
    public abstract Vector3f getLocalPosition(Vector3f output);

    /**
     * Gets the vertex origin position without object allocation, when feasible.
     * Do not modify the returned object as it may not be safe to modify.
     * @return vertexOriginPosition
     */
    public Vector3f getCachedOriginPosition() {
        return getNewOriginPosition();
    }

    /**
     * Gets the vertex origin position.
     * The object returned is guaranteed to be safe to modify, as it is a new instance.
     * @return vertexOriginPosition
     */
    public Vector3f getNewOriginPosition() {
        return getOriginPosition(new Vector3f());
    }

    /**
     * Store the vertex origin position of the vertex into the given output vector.
     * @param output the vector to store the position within. If null, a new vector will be allocated.
     * @return vertexOriginPosition
     */
    public abstract Vector3f getOriginPosition(Vector3f output);

    /**
     * Gets the vertex world position without object allocation, when feasible.
     * Do not modify the returned object as it may not be safe to modify.
     * @return worldPosition
     */
    public Vector3f getCachedWorldPosition() {
        return getNewWorldPosition();
    }

    /**
     * Gets the vertex world position.
     * The object returned is guaranteed to be safe to modify, as it is a new instance.
     * @return worldPosition
     */
    public Vector3f getNewWorldPosition() {
        return getWorldPosition(new Vector3f());
    }

    /**
     * Gets the world position of the vertex.
     * @param output the vector to store the position within. If null, a new vector will be allocated.
     * @return worldPosition
     */
    public Vector3f getWorldPosition(Vector3f output) {
        if (output == null)
            output = new Vector3f();

        return output.setXYZ(getCachedWorldPosition()).add(getCachedLocalPosition());
    }

    /**
     * Called when the landscape vertex is removed from a node.
     * The node may or may not be registered at the time of removal.
     */
    protected abstract void onRemoveFromNode();
}
