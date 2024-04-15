package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.GreatQuestMapMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestMapUIManager.GreatQuestMapListManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages entities for a Great Quest map.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestEntityManager extends GreatQuestMapListManager<kcCResourceEntityInst, Box> {
    private final DisplayList entityDisplayList;
    private static final PhongMaterial LIME_MATERIAL = Utils.makeSpecialMaterial(Color.LIMEGREEN);
    private static final PhongMaterial YELLOW_MATERIAL = Utils.makeSpecialMaterial(Color.YELLOW);

    public GreatQuestEntityManager(MeshViewController<GreatQuestMapMesh> controller) {
        super(controller);
        this.entityDisplayList = getRenderManager().createDisplayListWithNewGroup();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        getValueDisplaySetting().setValue(ListDisplayType.ALL);
    }

    @Override
    public String getTitle() {
        return "Entities";
    }

    @Override
    public String getValueName() {
        return "Entity";
    }

    @Override
    public List<kcCResourceEntityInst> getValues() {
        List<kcCResourceEntityInst> entities = new ArrayList<>();
        for (kcCResource resource : getMap().getChunks())
            if (resource instanceof kcCResourceEntityInst)
                entities.add(((kcCResourceEntityInst) resource));

        return entities;
    }

    @Override
    protected Box setupDisplay(kcCResourceEntityInst entityInst) {
        kcEntityInst entity = entityInst.getEntity();
        if (entity instanceof kcEntity3DInst) {
            kcEntity3DInst entity3d = (kcEntity3DInst) entity;
            kcVector4 position = entity3d.getPosition();
            kcVector4 scale = entity3d.getScale();
            Box box = this.entityDisplayList.addBoundingBoxCenteredWithDimensions(position.getX(), position.getY(), position.getZ(), .5 * scale.getX(), .5 * scale.getY(), .5 * scale.getZ(), getSelectedValue() == entityInst ? YELLOW_MATERIAL : LIME_MATERIAL, false);
            box.setOnMouseClicked(event -> handleClick(event, entityInst));
            return box;
        }

        return null;
    }

    @Override
    protected void updateEditor(kcCResourceEntityInst entityInst) {
        if (entityInst != null) {
            getEditorGrid().addLabel("Entity Name", entityInst.getName());
            getEditorGrid().addLabel("Entity Hash", Utils.to0PrefixedHexString(entityInst.getHash()));
            entityInst.getEntity().setupEditor(this, getEditorGrid());
        }
    }

    @Override
    protected void setVisible(kcCResourceEntityInst kcCResourceEntityInst, Box box, boolean visible) {
        if (box != null)
            box.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(kcCResourceEntityInst oldValue, Box oldBox, kcCResourceEntityInst newValue, Box newBox) {
        if (oldBox != null)
            oldBox.setMaterial(LIME_MATERIAL);
        if (newBox != null)
            newBox.setMaterial(YELLOW_MATERIAL);
    }

    @Override
    protected kcCResourceEntityInst createNewValue() {
        return null;
    }

    @Override
    protected void onDelegateRemoved(kcCResourceEntityInst kcCResourceEntityInst, Box box) {
        // Do nothing?
    }
}