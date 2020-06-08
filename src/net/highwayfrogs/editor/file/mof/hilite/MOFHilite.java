package net.highwayfrogs.editor.file.mof.hilite;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.mof.prims.MOFPrimType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MOFController;
import net.highwayfrogs.editor.gui.editor.RenderManager;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * Represents "MR_HILITE".
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
@Setter
public class MOFHilite extends GameObject {
    private HiliteAttachType attachType = HiliteAttachType.NONE;
    private HiliteType type = HiliteType.values()[0];
    private SVector vertex;
    private MOFPolygon polygon;
    private transient MOFPart parent;

    public MOFHilite(MOFPart parent) {
        this.parent = parent;
    }

    public MOFHilite(MOFPart parent, SVector vertex) {
        this(parent);
        this.vertex = vertex;
    }

    @Override
    public void load(DataReader reader) {
        int readType = reader.readUnsignedByteAsShort();

        this.type = readType >= 0 && readType < HiliteType.values().length ? HiliteType.values()[readType] : HiliteType.values()[0];
        this.attachType = HiliteAttachType.values()[reader.readUnsignedByteAsShort()];

        int readIndex = reader.readUnsignedShortAsInt();
        if (this.attachType == HiliteAttachType.VERTEX) {
            this.vertex = getVertices().get(readIndex);
        } else if (this.attachType == HiliteAttachType.PRIM) {
            this.polygon = getPolygons().get(readIndex);
        } else {
            throw new RuntimeException("Cannot handle hilite attach-type: " + this.attachType);
        }


        reader.skipInt(); // Runtime. (Even for interpolation)
        reader.skipInt(); // Runtime.
    }

    /**
     * Sets up the editor for this hilite.
     * @param controller The controller to setup under.
     */
    public void setupEditor(MOFController controller) {
        GUIEditorGrid grid = controller.getUiController().getHiliteEditorGrid();
        RenderManager manager = controller.getRenderManager();
        grid.clearEditor();

        grid.addEnumSelector("Hilite Type", getType(), HiliteType.values(), false, newType -> this.type = newType);

        grid.addButton("Remove Hilite", () -> {
            getParent().getHilites().remove(this);
            grid.clearEditor();
            manager.clearDisplayList(MOFController.HILITE_VERTICE_LIST); // Toss all of the vertice choices.
            controller.updateHiliteBoxes();
        });

        controller.getUiController().getHilitePane().setExpanded(true);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) type.ordinal());
        writer.writeUnsignedByte((short) this.attachType.ordinal());

        int saveId = -1;
        if (this.attachType == HiliteAttachType.PRIM) {
            saveId = getVertices().indexOf(getVertex());
        } else if (this.attachType == HiliteAttachType.VERTEX) {
            int amount = 0;
            for (MOFPrimType primType : MOFPrimType.values()) { // NOTE: This erases the order which seems to not be consistent.
                List<MOFPolygon> polygons = getParent().getMofPolygons().get(primType);
                if (primType == this.polygon.getType()) {
                    int foundIndex = polygons.indexOf(this.polygon);
                    if (foundIndex != -1)
                        saveId = amount + foundIndex;
                    break;
                } else {
                    amount += polygons.size();
                }
            }
        }
        Utils.verify(saveId >= 0, "Invalid save ID, is the hilite %s still registered?", this.attachType.name().toLowerCase());
        writer.writeUnsignedShort(saveId);

        writer.writeNullPointer(); // Runtime.
        writer.writeNullPointer(); // Runtime.
    }

    private List<SVector> getVertices() {
        return getParent().getStaticPartcel().getVertices();
    }

    private List<MOFPolygon> getPolygons() {
        return getParent().getOrderedByLoadPolygons();
    }

    /**
     * Sets the vertex this hilite is attached to.
     * @param vertex The vertex to set.
     */
    public void setVertex(SVector vertex) {
        this.vertex = vertex;
        this.polygon = null;
        this.attachType = HiliteAttachType.VERTEX;
    }

    /**
     * Sets the polygon this hilite is attached to.
     * @param polygon The polygon to set.
     */
    public void setPolygon(MOFPolygon polygon) {
        this.polygon = polygon;
        this.vertex = null;
        this.attachType = HiliteAttachType.VERTEX;
    }

    public enum HiliteAttachType {
        NONE, VERTEX, PRIM // Prim is used in beast wars
    }
}
