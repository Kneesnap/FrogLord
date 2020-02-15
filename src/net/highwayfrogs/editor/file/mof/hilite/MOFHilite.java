package net.highwayfrogs.editor.file.mof.hilite;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFPart;
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
public class MOFHilite extends GameObject {
    @Setter private HiliteType type = HiliteType.values()[0];
    private SVector vertex;
    private transient MOFPart parent;

    public static final int FLAG_VERTEX = Constants.BIT_FLAG_0;

    public MOFHilite(MOFPart parent) {
        this.parent = parent;
    }

    public MOFHilite(MOFPart parent, SVector vertex) {
        this(parent);
        this.vertex = vertex;
    }

    @Override
    public void load(DataReader reader) {
        this.type = HiliteType.values()[reader.readUnsignedByteAsShort()];
        short flags = reader.readUnsignedByteAsShort();
        this.vertex = getVertices().get(reader.readUnsignedShortAsInt());
        reader.skipInt(); // Runtime.
        reader.skipInt(); // Runtime.

        Utils.verify(flags == FLAG_VERTEX, "MOFHilite was not a vertex hilite! [" + flags + "]"); // This is the only setting that works in Frogger. The presumed prim setting which is bit flag 1 does initialize, but does nothing, at least in Frogger.
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
        writer.writeUnsignedByte((short) FLAG_VERTEX);

        int saveId = getVertices().indexOf(getVertex());
        Utils.verify(saveId >= 0, "Invalid save ID, is the hilite vertex still registered?");
        writer.writeUnsignedShort(saveId);

        writer.writeNullPointer(); // Runtime.
        writer.writeNullPointer(); // Runtime.
    }

    private List<SVector> getVertices() {
        return getParent().getStaticPartcel().getVertices();
    }
}
