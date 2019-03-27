package net.highwayfrogs.editor.file.map.path.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathSegment;
import net.highwayfrogs.editor.file.map.path.PathType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents PATH_LINE.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
public class LineSegment extends PathSegment {
    private SVector start = new SVector();
    private SVector end = new SVector();

    public LineSegment() {
        super(PathType.LINE);
    }

    @Override
    protected void loadData(DataReader reader) {
        this.start.loadWithPadding(reader);
        this.end.loadWithPadding(reader);
    }

    @Override
    protected void saveData(DataWriter writer) {
        this.start.saveWithPadding(writer);
        this.end.saveWithPadding(writer);
    }

    @Override
    protected PSXMatrix calculatePosition(PathInfo info) {
        int deltaX = end.getX() - start.getX();
        int deltaY = end.getY() - start.getY();
        int deltaZ = end.getZ() - start.getZ();

        PSXMatrix matrix = new PSXMatrix();
        matrix.getTransform()[0] = start.getX() + ((deltaX * info.getSegmentDistance()) / getLength());
        matrix.getTransform()[1] = start.getY() + ((deltaY * info.getSegmentDistance()) / getLength());
        matrix.getTransform()[2] = start.getZ() + ((deltaZ * info.getSegmentDistance()) / getLength());
        return matrix;
    }

    @Override
    public void setupEditor(Path path, MapUIController controller, GUIEditorGrid editor) {
        super.setupEditor(path, controller, editor);

        editor.addFloatSVector("Start:", getStart(), () -> this.refreshAssociatedComponents(controller));
        editor.addFloatSVector("End:", getEnd(), () -> this.refreshAssociatedComponents(controller));
    }

    /**
     * Refresh associated components.
     */
    private void refreshAssociatedComponents(MapUIController controller) {
        // Recalculate line segment length
        this.recalculateLength();

        // Update / refresh associated components
        controller.setupPathEditor();
        controller.getController().rebuildPathDisplay();
        controller.getController().resetEntities();
    }

    /**
     * Recalculates the length of the line segment.
     */
    public void recalculateLength() {
        float deltaX = end.getFloatX() - start.getFloatX();
        float deltaY = end.getFloatY() - start.getFloatY();
        float deltaZ = end.getFloatZ() - start.getFloatZ();

        float length = (float)Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));

        setLength(Utils.floatToFixedPointInt(length, 4));
    }
}
