package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapPathPacket.OldFroggerMapPath;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.path.OldFroggerSpline;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerMapUIManager.OldFroggerMapListManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.List;

/**
 * Manages spline paths.
 * Created by Kneesnap on 12/12/2023.
 */
public class OldFroggerPathManager extends OldFroggerMapListManager<OldFroggerMapPath, DisplayList> {
    private static final PhongMaterial SPLINE_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.LIGHTPINK);

    public OldFroggerPathManager(MeshViewController<OldFroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public String getTitle() {
        return "Spline Paths";
    }

    @Override
    public String getValueName() {
        return "Path";
    }

    @Override
    public List<OldFroggerMapPath> getValues() {
        return getMap().getPathPacket().getPaths();
    }

    @Override
    protected DisplayList setupDisplay(OldFroggerMapPath path) {
        DisplayList displayList = getRenderManager().createDisplayList();

        for (int i = 0; i < path.getSplines().size(); i++) {
            OldFroggerSpline spline = path.getSplines().get(i);

            int splineLength = spline.calculateLength();
            final int stepSize = Math.min(32, splineLength);
            if (stepSize == 0) { // If the path is 100% empty, just show the start.
                Vector pos = spline.calculateSplinePoint(0);
                addPathLineSegment(displayList, pos.getFloatX(), pos.getFloatY(), pos.getFloatZ(), pos.getFloatX(), pos.getFloatY(), pos.getFloatZ(), .2, SPLINE_MATERIAL, true, path, spline, 0);
                continue;
            }

            final int numSteps = 1 + (splineLength / stepSize);

            for (int step = 0; step < numSteps; ++step) {
                Vector v0 = spline.calculateSplinePoint(step * stepSize);
                Vector v1 = spline.calculateSplinePoint(Math.min((step + 1) * stepSize, splineLength));
                if (!v0.equals(v1))
                    addPathLineSegment(displayList, v0.getFloatX(), v0.getFloatY(), v0.getFloatZ(), v1.getFloatX(), v1.getFloatY(), v1.getFloatZ(), 0.20,
                            SPLINE_MATERIAL, step == 0, path, spline, (step * stepSize) + (stepSize / 2));
            }
        }

        return displayList;
    }

    private void handleClick(OldFroggerMapPath path, OldFroggerSpline spline, int segDistance) {
        getValueSelectionBox().getSelectionModel().select(path);
    }

    /**
     * Adds a cylindrical representation of a 3D line.
     * @param x0        The x-coordinate defining the start of the line segment.
     * @param y0        The y-coordinate defining the start of the line segment.
     * @param z0        The z-coordinate defining the start of the line segment.
     * @param x1        The x-coordinate defining the end of the line segment.
     * @param y1        The y-coordinate defining the end of the line segment.
     * @param z1        The z-coordinate defining the end of the line segment.
     * @param radius    The radius of the cylinder (effectively the 'width' of the line).
     * @param material  The material used to render the line segment.
     * @param showStart Whether or not to display a sphere at the start of the line segment.
     * @return The newly created/added cylinder (cylinder primitive only!)
     */
    public Cylinder addPathLineSegment(DisplayList displayList, double x0, double y0, double z0, double x1, double y1, double z1, double radius, PhongMaterial material, boolean showStart, OldFroggerMapPath path, OldFroggerSpline spline, int segDistance) {
        EventHandler<MouseEvent> mouseEventEventHandler = evt -> handleClick(path, spline, segDistance);
        final Point3D yAxis = new Point3D(0.0, 1.0, 0.0);
        final Point3D p0 = new Point3D(x0, y0, z0);
        final Point3D p1 = new Point3D(x1, y1, z1);
        final Point3D diff = p1.subtract(p0);
        final double length = diff.magnitude();

        final Point3D mid = p1.midpoint(p0);
        final Translate moveToMidpoint = new Translate(mid.getX(), mid.getY(), mid.getZ());

        final Point3D axisOfRotation = diff.crossProduct(yAxis);
        final double angle = Math.acos(diff.normalize().dotProduct(yAxis));
        final Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);

        Cylinder line = new Cylinder(radius, length, 3);
        line.setMaterial(material);
        line.setDrawMode(DrawMode.FILL);
        line.setCullFace(CullFace.BACK);
        line.setMouseTransparent(false);
        line.setOnMouseClicked(mouseEventEventHandler);
        line.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
        displayList.add(line);

        if (showStart) {
            Sphere sphStart = displayList.addSphere(x0, y0, z0, radius * 5.0, material, false);
            sphStart.setOnMouseClicked(mouseEventEventHandler);
        }

        return line;
    }

    @Override
    protected void updateEditor(OldFroggerMapPath path) {
        path.setupEditor(this, getEditorGrid());
    }

    @Override
    protected void setVisible(OldFroggerMapPath oldFroggerMapPath, DisplayList displayList, boolean visible) {
        displayList.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(OldFroggerMapPath oldValue, DisplayList oldDelegate, OldFroggerMapPath newValue, DisplayList newDelegate) {
        // Do nothing
    }

    @Override
    protected OldFroggerMapPath createNewValue() {
        return new OldFroggerMapPath(getMap().getGameInstance(), getMap().getPathPacket().getPaths().size());
    }

    @Override
    protected void onDelegateRemoved(OldFroggerMapPath oldPath, DisplayList displayList) {
        getRenderManager().removeDisplayList(displayList);
    }
}