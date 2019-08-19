package net.highwayfrogs.editor.file.map.poly;

import lombok.Getter;
import lombok.Setter;

/**
 * Holds data about a polygon, and can create / apply polygon data.
 * Created by Kneesnap on 8/19/2019.
 */
@Getter
@Setter
public class MAPPolygonData {
    private boolean shaded; // Gouraud or not. G vs F.
    private boolean textured; // Textured or not. T vs ''

    //TODO: UV Editor.
    //TODO: Vertice Selection.
    //TODO: buildPreviewImage.
    //TODO: Create editor.
    //TODO: Load data from a polygon.
    //TODO: Apply data to a polygon.
    //TODO: Create a new polygon.

}
