package net.highwayfrogs.editor.games.sony.shared.mof2.hilite;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.games.sony.shared.ui.file.MOFController;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.RenderManager;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

/**
 * Represents "MR_HILITE", which I believe is short for "Highlight", and is some kind of detail/attachment to a MOF / 3D model.
 * Created by Kneesnap on 1/8/2019.
 */
public class MRMofHilite extends SCSharedGameData {
    @Getter private final transient MRMofPart parentPart;
    @Getter @Setter private short hiliteType;
    @NonNull @Getter @Setter private HiliteAttachType attachType = HiliteAttachType.NONE;
    @Getter private SVector vertex;
    @Getter private MRMofPolygon polygon;
    private int tempAttachIndex = -1;

    public MRMofHilite(@NonNull MRMofPart parentPart) {
        super(parentPart.getGameInstance());
        this.parentPart = parentPart;
    }

    public MRMofHilite(@NonNull MRMofPart parent, SVector vertex) {
        this(parent);
        this.vertex = vertex;
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.parentPart.getLogger(), "hilite=" + Utils.getLoadingIndex(this.parentPart.getHilites(), this), AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    @Override
    public void load(DataReader reader) {
        this.hiliteType = reader.readUnsignedByteAsShort();
        this.attachType = HiliteAttachType.values()[reader.readUnsignedByteAsShort()];
        this.tempAttachIndex = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // Runtime. (Even for interpolation)
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // Runtime.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte(this.hiliteType);
        writer.writeUnsignedByte((short) this.attachType.ordinal());

        int saveId;
        if (this.attachType == HiliteAttachType.VERTEX) {
            if (this.vertex == null)
                throw new RuntimeException("Cannot save MRMofHilite with a NULL vertex!");

            saveId = this.parentPart.getStaticPartcel().getVertices().indexOf(this.vertex);
        } else if (this.attachType == HiliteAttachType.PRIM) {
            if (this.polygon == null)
                throw new RuntimeException("Cannot save MRMofHilite with a NULL polygon!");

            saveId = this.parentPart.getPolygonIndex(this.polygon); // OK to use even if polygons have not been written yet.
        } else {
            throw new RuntimeException("Cannot handle saving hilite attach-type: " + this.attachType);
        }

        // Validate ID.
        if (saveId < 0)
            throw new RuntimeException("Invalid save ID, is the value for the hilite " + this.attachType + " still registered?");

        // Write data.
        writer.writeUnsignedShort(saveId);
        writer.writeNullPointer(); // Runtime.
        writer.writeNullPointer(); // Runtime.
    }

    /**
     * Get the Hilite type as seen in Frogger.
     * @return froggerHiliteType
     */
    public FroggerHiliteType getFroggerHiliteType() {
        if (!getGameInstance().isFrogger())
            return null;

        if (this.hiliteType < 0 || this.hiliteType >= FroggerHiliteType.values().length)
            throw new RuntimeException("Unrecognized Frogger Hilite Attach Type: " + this.hiliteType);

        return FroggerHiliteType.values()[this.hiliteType];
    }

    /**
     * Sets up the editor for this hilite.
     * @param controller The controller to setup under.
     */
    public void setupEditor(MOFController controller) {
        GUIEditorGrid grid = controller.getUiController().getHiliteEditorGrid();
        RenderManager manager = controller.getRenderManager();
        grid.clearEditor();

        FroggerHiliteType froggerHiliteType = getFroggerHiliteType();

        if (froggerHiliteType != null) {
            grid.addEnumSelector("Hilite Type", froggerHiliteType, FroggerHiliteType.values(), false, newType -> this.hiliteType = (short) newType.ordinal());
        } else {
            grid.addUnsignedByteField("Hilite Type", this.hiliteType, this::setHiliteType);
        }

        grid.addButton("Remove Hilite", () -> {
            this.parentPart.getHilites().remove(this);
            grid.clearEditor();
            manager.clearDisplayList(MOFController.HILITE_VERTICE_LIST); // Toss the vertice choices.
            controller.updateHiliteBoxes();
        });

        controller.getUiController().getHilitePane().setExpanded(true);
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
    public void setPolygon(MRMofPolygon polygon) {
        this.polygon = polygon;
        this.vertex = null;
        this.attachType = HiliteAttachType.VERTEX;
    }

    /**
     * Resolve the attachment value by its ID.
     * Should happen after all MOF data such as polygons are loaded so it can actually resolve.
     */
    public void resolveAttachment() {
        if (this.tempAttachIndex < 0)
            throw new RuntimeException("The tempAttachIndex is not set, suggesting the hilite does not need its attachment resolved.");

        int attachIndex = this.tempAttachIndex;
        this.tempAttachIndex = -1;
        if (this.attachType == HiliteAttachType.VERTEX) {
            this.vertex = this.parentPart.getStaticPartcel().getVertices().get(attachIndex);
        } else if (this.attachType == HiliteAttachType.PRIM) {
            this.polygon = this.parentPart.getPolygon(attachIndex);
            if (this.polygon == null)
                throw new RuntimeException("The polygon at index " + attachIndex + " could not be resolved.");
        } else {
            throw new RuntimeException("Cannot handle resolving hilite attach-type: " + this.attachType);
        }
    }

    public enum HiliteAttachType {
        NONE, VERTEX, PRIM // Prim is used in beast wars
    }
}