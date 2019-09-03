package net.highwayfrogs.editor.file.map.poly;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.poly.polygon.*;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture.PolyTextureFlag;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;

/**
 * Holds data about a polygon, and can create / apply polygon data.
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
    private short textureId = 0;
    private short flags;
    private ByteUV[] uvs;
    private MapUIController controller;

    private static final String[] SINGLE_COLOR_NAME = {"Color"};
    private static final String[] TRI_COLOR_NAMES = {"Corner 1", "Corner 2", "Corner 3"};
    private static final String[] QUAD_COLOR_NAMES = {"Top Left", "Bottom Left", "Top Right", "Bottom Right"};
    private static final String[][] COLOR_BANK = {SINGLE_COLOR_NAME, null, TRI_COLOR_NAMES, QUAD_COLOR_NAMES};
    private static final ImageFilterSettings SHOW_SETTINGS = new ImageFilterSettings(ImageState.EXPORT).setTrimEdges(true);

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

    private void updateColors() {
        int newSize = isGouraud() ? (isQuad() ? 4 : 3) : 1;
        if (this.colors.length == newSize)
            return;

        PSXColorVector[] newColors = new PSXColorVector[newSize];
        System.arraycopy(this.colors, 0, newColors, 0, Math.min(this.colors.length, newSize));
        for (int i = 0; i < newColors.length; i++)
            if (newColors[i] == null)
                newColors[i] = new PSXColorVector();

        this.colors = newColors;
        getController().getGeometryManager().setupEditor(); // Update the editor.
    }

    /**
     * Setup an editor for this data.
     */
    public void setupEditor(GUIEditorGrid editor, MapUIController controller) {
        this.controller = controller;

        editor.addCheckBox("Quad", isQuad(), newValue -> {
            int newSize = newValue ? 4 : 3;
            int copySize = Math.min(newSize, this.vertices.length);

            // Copy vertices.
            int[] newVertices = new int[newSize];
            System.arraycopy(this.vertices, 0, newVertices, 0, copySize);
            this.vertices = newVertices;

            // Copy uvs.
            ByteUV[] newUvs = new ByteUV[newSize];
            if (this.uvs != null)
                System.arraycopy(this.uvs, 0, newUvs, 0, copySize);
            for (int i = 0; i < newUvs.length; i++)
                if (newUvs[i] == null)
                    newUvs[i] = new ByteUV();
            this.uvs = newUvs;

            updateColors();
        });

        editor.addCheckBox("Gouraud", isGouraud(), newState -> {
            this.gouraud = newState;
            updateColors();
        });

        editor.addCheckBox("Textured", isTextured(), newState -> {
            this.textured = newState;
            updateColors();
        });

        // Texture Editor.
        if (isTextured()) {
            TextureMap texMap = controller.getMapMesh().getTextureMap();
            VLOArchive suppliedVLO = controller.getController().getFile().getVlo();
            GameImage image = suppliedVLO.getImageByTextureId(texMap.getRemap(getTextureId()));

            // Texture Preview. (Click -> change.)
            ImageView view = editor.addCenteredImage(image.toFXImage(SHOW_SETTINGS), 150);
            view.setOnMouseClicked(evt -> suppliedVLO.promptImageSelection(newImage -> {
                short newValue = newImage.getTextureId();
                if (texMap.getRemapList() != null)
                    newValue = (short) texMap.getRemapList().indexOf(newValue);

                if (newValue == (short) -1) {
                    Utils.makePopUp("This image is not part of the remap! It can't be used!", AlertType.INFORMATION); // Show this as a popup maybe.
                    return;
                }

                this.textureId = newValue;
                view.setImage(newImage.toFXImage(SHOW_SETTINGS));
                controller.getGeometryManager().refreshView();
            }, false));

            // Flags.
            for (PolyTextureFlag flag : PolyTextureFlag.values())
                editor.addCheckBox(Utils.capitalize(flag.name()), testFlag(flag), newState -> setFlag(flag, newState));
        }

        // UVs. (TODO: Better editor? Maybe have sliders + a live preview?)
        if (this.uvs != null) {
            for (int i = 0; i < getUvs().length; i++)
                getUvs()[i].setupEditor("UV #" + i, editor);
        }

        // Color Editor.
        if (this.colors != null) {
            editor.addBoldLabel("Colors:");
            String[] nameArray = COLOR_BANK[this.colors.length - 1];
            for (int i = 0; i < this.colors.length; i++)
                editor.addColorPicker(nameArray[i], this.colors[i].toRGB(), this.colors[i]::fromRGB);
            //TODO: Update map display when color is updated. (Update texture map.)
        }

        controller.getVertexManager().showVertices(getVertices());
        editor.addBoldLabel("Vertice Controls:");
        editor.addButton("Change Vertex", () -> {
            SVector selected = controller.getVertexManager().getSelectedVector();
            if (selected == null) {
                Utils.makePopUp("You must select the vertex you'd like to change first.", AlertType.WARNING);
                return;
            }

            // Allow changing.
            controller.getVertexManager().selectVertex(newVertex -> {
                int oldArrayIndex = Utils.indexOf(getVertices(), controller.getController().getFile().getVertexes().indexOf(selected));
                if (oldArrayIndex == -1)
                    throw new RuntimeException("Failed to find the real index into the vertex array.");

                int newIndex = controller.getController().getFile().getVertexes().indexOf(newVertex);
                if (newIndex == -1)
                    throw new RuntimeException("Failed to find the vertex id for the new vertex.");
                getVertices()[oldArrayIndex] = newIndex;

                //TODO: It would be nice for this to be a live update.
            }, null);

        });
    }

    /**
     * Test if a texture flag is present.
     * @param flag The flag in question.
     * @return flagPresent
     */
    public boolean testFlag(PolyTextureFlag flag) {
        return (this.flags & flag.getFlag()) == flag.getFlag();
    }

    /**
     * Set a texture flag state.
     * @param flag     The flag to set.
     * @param newState The new flag state.
     */
    public void setFlag(PolyTextureFlag flag, boolean newState) {
        boolean currentState = testFlag(flag);
        if (currentState == newState)
            return; // Prevents the ^ operation from breaking the value.

        if (newState) {
            this.flags |= flag.getFlag();
        } else {
            this.flags ^= flag.getFlag();
        }
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
