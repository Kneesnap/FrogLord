package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerMapUIManager.OldFroggerMapListManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.List;
import java.util.UUID;

/**
 * Manages vertex displays for the map.
 * Created by Kneesnap on 12/12/2023.
 */
public class OldFroggerVertexManager extends OldFroggerMapListManager<SVector, Sphere> {
    private DisplayList verticeDisplayList;

    private static final PhongMaterial MATERIAL_YELLOW = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);
    private static final PhongMaterial MATERIAL_GREEN = Scene3DUtils.makeUnlitSharpMaterial(Color.LIME);
    private static final UUID VERTEX_POSITION_IDENTIFIER = UUID.randomUUID();

    public OldFroggerVertexManager(MeshViewController<OldFroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        this.verticeDisplayList = getRenderManager().createDisplayList();
        super.onSetup();
    }

    @Override
    public String getTitle() {
        return "Vertices";
    }

    @Override
    public String getValueName() {
        return "Vertex";
    }

    @Override
    public List<SVector> getValues() {
        return getMap().getVertexPacket().getVertices();
    }

    @Override
    protected Sphere setupDisplay(SVector vector) {
        Sphere sphere = this.verticeDisplayList.addSphere(vector.getFloatX(), vector.getFloatY(), vector.getFloatZ(), 1, MATERIAL_YELLOW, false);
        if (vector == getSelectedValue())
            sphere.setMaterial(MATERIAL_GREEN);

        return sphere;
    }

    @Override
    protected void updateEditor(SVector vertex) {
        getEditorGrid().addPositionEditor(getController(), VERTEX_POSITION_IDENTIFIER, "Position", vertex,
                ((meshView, oldX, oldY, oldZ, newX, newY, newZ, flags) -> getMesh().getMainNode().updateVertex(getSelectedValueIndex())));
    }

    @Override
    protected void setValuesVisible(boolean valuesVisible) {
        super.setValuesVisible(valuesVisible);
        this.verticeDisplayList.setVisible(valuesVisible);
    }

    @Override
    protected void setVisible(SVector sVector, Sphere sphere, boolean visible) {
        sphere.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(SVector oldValue, Sphere oldSphere, SVector newValue, Sphere newSphere) {
        if (oldSphere != null)
            oldSphere.setMaterial(MATERIAL_YELLOW);

        if (newSphere != null)
            newSphere.setMaterial(MATERIAL_GREEN);
    }

    @Override
    protected SVector createNewValue() {
        return new SVector();
    }

    @Override
    protected void onDelegateRemoved(SVector removedValue, Sphere sphere) {
        if (removedValue != null)
            this.verticeDisplayList.remove(sphere);
    }
}