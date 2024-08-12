package net.highwayfrogs.editor.games.sony.shared.landscaper;

import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameObject.SCSharedGameObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the base of a landscape, functionality shared between the landscape itself and the nodes.
 * Created by Kneesnap on 7/16/2024.
 */
public class LandscapeBase<TVertex extends LandscapeVertex, TPolygon extends LandscapePolygon> extends SCSharedGameObject {
    protected final List<TVertex> vertices = new ArrayList<>();
    private final List<TVertex> immutableVertices = Collections.unmodifiableList(this.vertices);
    protected final List<TPolygon> polygons = new ArrayList<>();
    private final List<TPolygon> immutablePolygons = Collections.unmodifiableList(this.polygons);

    public LandscapeBase(SCGameInstance instance) {
        super(instance);
    }

    /**
     * Gets the vertices registered to the landscape.
     */
    public List<TVertex> getVertices() {
        return this.immutableVertices;
    }

    /**
     * Gets the polygons registered to the landscape.
     */
    public List<TPolygon> getPolygons() {
        return this.immutablePolygons;
    }
}
