package net.highwayfrogs.editor.games.sony.beastwars.ui;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.beastwars.map.data.BeastWarsMapLine;
import net.highwayfrogs.editor.games.sony.beastwars.map.mesh.BeastWarsMapMesh;
import net.highwayfrogs.editor.games.sony.beastwars.ui.BeastWarsMapUIManager.BeastWarsMapListManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * Displays lines.
 * Created by Kneesnap on 9/26/2023.
 */
public class BeastWarsLineManager extends BeastWarsMapListManager<BeastWarsMapLine, DisplayList> {
    private static final PhongMaterial MATERIAL_WHITE = Utils.makeUnlitSharpMaterial(Color.WHITE);
    private static final PhongMaterial MATERIAL_YELLOW = Utils.makeUnlitSharpMaterial(Color.YELLOW);

    public BeastWarsLineManager(MeshViewController<BeastWarsMapMesh> controller) {
        super(controller);
    }

    @Override
    public String getTitle() {
        return "Map Lines";
    }

    @Override
    public String getValueName() {
        return "Line";
    }

    @Override
    public List<BeastWarsMapLine> getValues() {
        return getMap().getLines();
    }

    @Override
    protected DisplayList setupDisplay(BeastWarsMapLine line) {
        boolean isSelected = (line == getValueSelectionBox().getValue());

        DisplayList newLineList = getRenderManager().createDisplayList();
        for (int i = 1; i < line.getPositions().size(); i++) {
            SVector lastPos = line.getPositions().get(i - 1);
            SVector currPos = line.getPositions().get(i);

            PhongMaterial material = isSelected ? MATERIAL_YELLOW : MATERIAL_WHITE;
            Cylinder cylinder = newLineList.addLine(lastPos.getFloatX(), lastPos.getFloatY(), lastPos.getFloatZ(), currPos.getFloatX(), currPos.getFloatY(), currPos.getFloatZ(), 5, material);
            cylinder.setOnMouseClicked(event -> getValueSelectionBox().getSelectionModel().select(line));
        }

        return newLineList;
    }

    @Override
    protected void updateEditor(BeastWarsMapLine line) {
        // Unknown Values
        getEditorGrid().addSignedIntegerField("Unknown #1", line.getUnknown1(), line::setUnknown1);
        getEditorGrid().addSignedIntegerField("Unknown #2", line.getUnknown2(), line::setUnknown2);
        getEditorGrid().addSignedIntegerField("Unknown #3", line.getUnknown3(), line::setUnknown3);
        getEditorGrid().addSignedIntegerField("Unknown #4", line.getUnknown4(), line::setUnknown4);


        // TODO: Show positions
        // TODO: Add + Remove positions.
    }

    @Override
    protected void onDelegateRemoved(BeastWarsMapLine removedLine, DisplayList displayList) {
        getRenderManager().removeDisplayList(displayList);
    }

    @Override
    protected void setVisible(BeastWarsMapLine beastWarsMapLine, DisplayList displayList, boolean visible) {
        displayList.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(BeastWarsMapLine oldLine, DisplayList oldDisplayList, BeastWarsMapLine newLine, DisplayList newDisplayList) {
        if (oldDisplayList != null)
            for (Node node : oldDisplayList.getNodes())
                if (node instanceof Shape3D) // Apply de-selected material.
                    ((Shape3D) node).setMaterial(MATERIAL_WHITE);

        if (newDisplayList != null)
            for (Node node : newDisplayList.getNodes())
                if (node instanceof Shape3D) // Apply selection material.
                    ((Shape3D) node).setMaterial(MATERIAL_YELLOW);
    }

    @Override
    protected BeastWarsMapLine createNewValue() {
        return new BeastWarsMapLine(getMap());
    }
}