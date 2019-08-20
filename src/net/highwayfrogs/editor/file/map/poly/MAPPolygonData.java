package net.highwayfrogs.editor.file.map.poly;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.poly.polygon.*;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

import java.util.Arrays;

/**
 * Holds data about a polygon, and can create / apply polygon data.
 * TODO: If a type change happens, be careful to make sure data structures get updated.
 * Created by Kneesnap on 8/19/2019.
 */
@Getter
@Setter
public class MAPPolygonData {
    private boolean gouraud; // Gouraud or not. G vs F.
    private boolean textured; // Textured or not. T vs ''
    private boolean allowDisplay = true; // Whether or not to include in a map group.
    private int[] vertices;
    private PSXColorVector[] colors;
    private short textureId;
    private short flags;
    private ByteUV[] uvs;

    private static final String[] SINGLE_COLOR_NAME = {"Color"};
    private static final String[] TRI_COLOR_NAMES = {"1?", "2?", "3?"}; //TODO
    private static final String[] QUAD_COLOR_NAMES = {"Top Left", "Bottom Left", "Top Right", "Bottom Right"};
    private static final String[][] COLOR_BANK = {SINGLE_COLOR_NAME, null, TRI_COLOR_NAMES, QUAD_COLOR_NAMES};

    /**
     * Gets the amount of vertices this polygon uses.
     * @return verticeCount
     */
    public int getVerticeCount() {
        return this.vertices.length;
    }

    /**
     * Test if this is a quad, as opposed to a tri.
     */
    public boolean isQuad() {
        return this.vertices.length == 4;
    }

    /**
     * Test if this is a tri, as opposed to a quad.
     */
    public boolean isTri() {
        return this.vertices.length == 3;
    }

    /**
     * Loads the polygon data from a polygon.
     * @param poly The polygon to load data from.
     */
    public void loadFrom(MAPPolygon poly) {
        // Apply default data.
        this.gouraud = false;
        this.textured = false;
        this.textureId = (short) 0;
        this.flags = (short) 0;
        this.uvs = null;
        this.colors = null;

        // Load type-specific data.
        if (poly instanceof MAPPolyFlat) { // F3 and F4.
            this.colors = new PSXColorVector[]{((MAPPolyFlat) poly).getColor()};
        } else if (poly instanceof MAPPolyTexture) { // FT3, FT4, GT3, GT4.
            this.textured = true;
            this.gouraud = (poly instanceof MAPPolyGT3 || poly instanceof MAPPolyGT4);

            MAPPolyTexture polyTex = (MAPPolyTexture) poly;
            this.flags = polyTex.getFlags();
            this.uvs = cloneUVs(polyTex.getUvs());
            this.textureId = polyTex.getTextureId();
            this.colors = cloneColors(polyTex.getVectors());
        } else if (poly instanceof MAPPolyGouraud) { // G3, G4.
            MAPPolyGouraud gouraudPoly = (MAPPolyGouraud) poly;
            this.gouraud = true;
            this.colors = cloneColors(gouraudPoly.getColors());
        }

        // Load general data.
        this.allowDisplay = poly.isAllowDisplay();
        this.vertices = Arrays.copyOf(poly.getVertices(), poly.getVerticeCount()); // Load vertices.
    }

    /**
     * Determines the type of polygon this is.
     * @return polygonType
     */
    public MAPPolygonType getPolygonType() {
        if (!isTextured() && !isGouraud()) {
            return isQuad() ? MAPPolygonType.F4 : MAPPolygonType.F3;
        } else if (!isTextured() && isGouraud()) {
            return isQuad() ? MAPPolygonType.G4 : MAPPolygonType.G3;
        } else if (isTextured() && !isGouraud()) {
            return isQuad() ? MAPPolygonType.FT4 : MAPPolygonType.FT3;
        } else {
            return isQuad() ? MAPPolygonType.GT4 : MAPPolygonType.GT3;
        }
    }

    /**
     * Generates a new polygon with the stored data.
     */
    public MAPPolygon makeNewPolygon() {
        MAPPolygon newPoly = getPolygonType().getMaker().get();
        applyToPolygon(newPoly);
        return newPoly;
    }

    /**
     * Applies the data stored here to the given polygon.
     * @param poly The polygon to apply to.
     */
    public void applyToPolygon(MAPPolygon poly) {
        MAPPolygonType polygonType = getPolygonType();
        if (polygonType != poly.getType())
            throw new RuntimeException("MAPPolygonData determined its type as " + polygonType + ", but tried to apply itself to a " + poly.getType() + "!");

        // Apply type-specific data.
        if (poly instanceof MAPPolyFlat) { // F3 and F4.
            ((MAPPolyFlat) poly).setColor(this.colors[0]);
        } else if (poly instanceof MAPPolyTexture) { // FT3, FT4, GT3, GT4.
            MAPPolyTexture polyTex = (MAPPolyTexture) poly;
            polyTex.setFlags(this.flags);
            polyTex.setTextureId(this.textureId);
            polyTex.setVectors(cloneColors(this.colors));
            polyTex.setUvs(cloneUVs(this.uvs));
        } else if (poly instanceof MAPPolyGouraud) { // G3, G4.
            MAPPolyGouraud gouraudPoly = (MAPPolyGouraud) poly;
            gouraudPoly.setColors(cloneColors(this.colors));
        }

        // Apply general data.
        poly.setVertices(Arrays.copyOf(this.vertices, getVerticeCount()));
        poly.setFlippedVertices(true);
        poly.setAllowDisplay(this.allowDisplay);
    }

    /**
     * Setup an editor for this data.
     */
    public void setupEditor(GUIEditorGrid editor) {
        //TODO: Toggles. [careful here to make sure it stays compatible!]

        if (isTextured()) {
            //TODO: textureId, flags, uvs, image preview.
        }

        // Color Editor.
        if (this.colors != null) {
            editor.addBoldLabel("Colors:");
            String[] nameArray = COLOR_BANK[this.colors.length - 1];
            for (int i = 0; i < this.colors.length; i++)
                editor.addColorPicker(nameArray[i], this.colors[i].toRGB(), this.colors[i]::fromRGB);
            //TODO: Update preview when color is updated.
        }

        // TODO: Vertice tools. [Show vertices, change them. Add new ones]
        editor.addLabel("Vertices", Arrays.toString(this.vertices));
    }

    private static ByteUV[] cloneUVs(ByteUV[] toClone) {
        ByteUV[] newArray = new ByteUV[toClone.length];
        for (int i = 0; i < newArray.length; i++)
            newArray[i] = toClone[i].clone();
        return newArray;
    }

    private static PSXColorVector[] cloneColors(PSXColorVector[] toClone) {
        PSXColorVector[] newArray = new PSXColorVector[toClone.length];
        for (int i = 0; i < newArray.length; i++)
            newArray[i] = toClone[i].clone();
        return newArray;
    }
}
