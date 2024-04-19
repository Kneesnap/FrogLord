package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager;

import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelDesc;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceModel;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.GreatQuestMapMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestMapUIManager.GreatQuestMapListManager;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.entity.GreatQuestMapEditorEntityDisplay;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMaterialMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMesh;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection.MeshViewCollection;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Manages entities for a Great Quest map.
 * TODO: Some entities (like the big pike) aren't getting rendered. I think it's when their texture isn't found..?
 * TODO: Allow teleporting the camera to an entity selected.
 * TODO: We need a generalized property editing system. So for each class can implement an interface, and become editable.
 *  - These will become convertible to text as well.
 *  - Allow creating a class to define editing certain things.
 *  - Allow updating other UI components potentially? Not sure yet.
 *  - This editor should use a standardized vector interface between games.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestEntityManager extends GreatQuestMapListManager<kcCResourceEntityInst, GreatQuestMapEditorEntityDisplay> {
    private final Map<kcCResourceModel, GreatQuestModelMesh> cachedModelMeshes = new HashMap<>();
    private final DisplayList collisionPreviewDisplayList;
    private final DisplayList boundingSphereDisplayList;
    private final List<GreatQuestMapEnvironmentCollection> waterMeshCollections = new ArrayList<>();
    private GreatQuestMapEnvironmentCollection skyBoxCollection;
    private CheckBox showEntityMeshCheckBox;
    private CheckBox showCollisionCheckBox;
    private CheckBox showBoundingSphereCheckBox;
    private static final Pattern DOME_PATTERN = Pattern.compile("(?i)^\\d\\ddome(\\d\\d)?\\.vtx$");
    private static final Pattern WATER_PATTERN = Pattern.compile("(?i)^\\d\\d((lake)|(river)|(waterfall)|(water))(\\d\\d)?\\.((ctm)|(vtx))$");

    public GreatQuestEntityManager(MeshViewController<GreatQuestMapMesh> controller) {
        super(controller);
        this.collisionPreviewDisplayList = getTransparentRenderManager().createDisplayListWithNewGroup();
        this.boundingSphereDisplayList = getTransparentRenderManager().createDisplayListWithNewGroup(); // Draws on top of collision preview.
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        getValueDisplaySetting().setValue(ListDisplayType.ALL);
        this.showEntityMeshCheckBox = getMainGrid().addCheckBox("Show Entity Mesh", true, this::setEntityMeshVisible);
        this.showCollisionCheckBox = getMainGrid().addCheckBox("Show Collision Proxy", false, this::setCollisionProxyVisible);
        this.showBoundingSphereCheckBox = getMainGrid().addCheckBox("Show Bounding Sphere", false, this::setBoundingSphereVisible);
        getMainGrid().addCheckBox("Show Sky Box", true, this::setSkyBoxVisible);
        getMainGrid().addCheckBox("Show Water", true, this::setWaterVisible);

        // Water and sky box should be setup last, because water is the biggest transparent model of all.
        // So, it should come after everything else.
        Platform.runLater(this::setupWaterAndSkyBox);
    }

    private void setupWaterAndSkyBox() {
        // Add skybox.
        for (kcCResource resource : getMap().getChunks()) {
            if (!(resource instanceof kcCResourceModel))
                continue;

            kcCResourceModel resourceModel = (kcCResourceModel) resource;
            if (isFileNameSkyDome(resource.getName())) {
                GreatQuestModelMesh skyBoxMesh = new GreatQuestModelMesh(resourceModel, false);
                this.skyBoxCollection = new GreatQuestMapEnvironmentCollection(this, false);
                this.skyBoxCollection.setMesh(skyBoxMesh.getActualMesh());
            } else if (isFileNameWaterMesh(resource.getName())) {
                GreatQuestModelMesh waterBoxMesh = new GreatQuestModelMesh(resourceModel, false);
                GreatQuestMapEnvironmentCollection waterCollection = new GreatQuestMapEnvironmentCollection(this, true);
                waterCollection.setMesh(waterBoxMesh.getActualMesh());
                this.waterMeshCollections.add(waterCollection);
            }
        }
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
    protected String getListDisplayName(int index, kcCResourceEntityInst entity) {
        return entity != null ? entity.getName() : super.getListDisplayName(index, null);
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
    protected GreatQuestMapEditorEntityDisplay setupDisplay(kcCResourceEntityInst entityInst) {
        kcEntityInst entity = entityInst.getEntity();

        kcCResourceModel model = null;
        kcEntity3DDesc entityDescription = entity != null ? entity.getDescription(getMap()) : null;
        if (entityDescription instanceof kcActorBaseDesc) {
            kcActorBaseDesc actorBaseDesc = (kcActorBaseDesc) entityDescription;
            kcModelDesc modelDesc = actorBaseDesc.getModelDesc(getMap());
            if (modelDesc != null)
                model = modelDesc.getModelResource(getMap());
        }

        GreatQuestModelMesh modelMesh = this.cachedModelMeshes.computeIfAbsent(model, key -> new GreatQuestModelMesh(key, false));
        GreatQuestMapModelMeshCollection entityMeshCollection = new GreatQuestMapModelMeshCollection(this, entityInst);
        entityMeshCollection.setMesh(modelMesh.getActualMesh());
        GreatQuestMapEditorEntityDisplay newDisplay = new GreatQuestMapEditorEntityDisplay(this, entityInst, entityMeshCollection);
        newDisplay.setup();
        return newDisplay;
    }

    @Override
    protected void updateEditor(kcCResourceEntityInst entityInst) {
        if (entityInst != null) {
            getEditorGrid().addLabel("Entity Name", entityInst.getName());
            getEditorGrid().addLabel("Entity Hash", Utils.to0PrefixedHexString(entityInst.getHash()));
            entityInst.getEntity().setupEditor(this, getEditorGrid(), getDelegatesByValue().get(entityInst));
        }
    }

    @Override
    protected void setVisible(kcCResourceEntityInst kcCResourceEntityInst, GreatQuestMapEditorEntityDisplay entityDisplay, boolean visible) {
        if (entityDisplay != null)
            entityDisplay.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(kcCResourceEntityInst oldValue, GreatQuestMapEditorEntityDisplay oldDisplay, kcCResourceEntityInst newValue, GreatQuestMapEditorEntityDisplay newDisplay) {
        if (oldDisplay != null) {
            GreatQuestMapModelMeshCollection collection = oldDisplay.getModelViews();
            for (int i = 0; i < collection.getMeshViews().size(); i++) {
                MeshView oldMeshView = collection.getMeshViews().get(i);
                GreatQuestModelMaterialMesh oldCachedMesh = oldMeshView.getMesh() instanceof GreatQuestModelMaterialMesh ? ((GreatQuestModelMaterialMesh) oldMeshView.getMesh()) : null;
                if (oldCachedMesh != null)
                    oldMeshView.setMaterial(oldCachedMesh.getMaterial());
            }
        }

        if (newDisplay != null) {
            GreatQuestMapModelMeshCollection collection = newDisplay.getModelViews();
            for (int i = 0; i < collection.getMeshViews().size(); i++) {
                MeshView newMeshView = collection.getMeshViews().get(i);
                GreatQuestModelMaterialMesh newCachedMesh = newMeshView.getMesh() instanceof GreatQuestModelMaterialMesh ? ((GreatQuestModelMaterialMesh) newMeshView.getMesh()) : null;
                if (newCachedMesh != null)
                    newMeshView.setMaterial(newCachedMesh.getHighlightedMaterial());
            }
        }
    }

    @Override
    protected kcCResourceEntityInst createNewValue() {
        return null;
    }

    @Override
    protected void onDelegateRemoved(kcCResourceEntityInst kcCResourceEntityInst, GreatQuestMapEditorEntityDisplay display) {
        if (display != null) {
            if (display.getModelViews() != null)
                display.getModelViews().setMesh(null);
            display.removeCollisionPreview();
            display.removeBoundingSphere();
        }
    }

    /**
     * Set if entity meshes are displayed.
     * @param visible the desired visibility state
     */
    public void setEntityMeshVisible(boolean visible) {
        for (GreatQuestMapEditorEntityDisplay display : getDelegatesByValue().values())
            if (display.getModelViews() != null)
                display.getModelViews().setVisible(visible && isValueVisibleByUI(display.getEntityInstance()));
    }

    /**
     * Set if collision proxies are visible.
     * @param visible the desired visibility state
     */
    public void setCollisionProxyVisible(boolean visible) {
        for (GreatQuestMapEditorEntityDisplay display : getDelegatesByValue().values())
            if (display.getCollisionPreview() != null)
                display.getCollisionPreview().setVisible(visible && isValueVisibleByUI(display.getEntityInstance()));
    }

    /**
     * Set if bounding spheres are displayed.
     * @param visible the desired visibility state
     */
    public void setBoundingSphereVisible(boolean visible) {
        for (GreatQuestMapEditorEntityDisplay display : getDelegatesByValue().values())
            if (display.getBoundingSpherePreview() != null)
                display.getBoundingSpherePreview().setVisible(visible && isValueVisibleByUI(display.getEntityInstance()));
    }

    /**
     * Set if the sky box is visible.
     * @param visible the desired visibility state
     */
    private void setSkyBoxVisible(boolean visible) {
        if (this.skyBoxCollection != null)
            this.skyBoxCollection.setVisible(visible);
    }

    /**
     * Set if the water is visible.
     * @param visible the desired visibility state
     */
    private void setWaterVisible(boolean visible) {
        for (int i = 0; i < this.waterMeshCollections.size(); i++)
            this.waterMeshCollections.get(i).setVisible(visible);
    }

    @Getter
    public static class GreatQuestMapModelMeshCollection extends MeshViewCollection<GreatQuestModelMaterialMesh> {
        private final GreatQuestEntityManager manager;
        private final kcCResourceEntityInst entityInst;

        public GreatQuestMapModelMeshCollection(GreatQuestEntityManager manager, kcCResourceEntityInst entityInst) {
            super(manager.getRenderManager().createDisplayList());
            this.manager = manager;
            this.entityInst = entityInst;
        }

        @Override
        protected void onMeshViewSetup(int meshIndex, GreatQuestModelMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewSetup(meshIndex, mesh, meshView);
            if (this.entityInst != null)
                meshView.setOnMouseClicked(event -> this.manager.handleClick(event, this.entityInst));
            this.manager.getController().getMainLight().getScope().add(meshView);
        }

        @Override
        protected void onMeshViewCleanup(int meshIndex, GreatQuestModelMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewCleanup(meshIndex, mesh, meshView);
            meshView.setOnMouseClicked(null);
            meshView.getTransforms().clear();
            this.manager.getController().getMainLight().getScope().remove(meshView);
        }
    }

    @Getter
    public static class GreatQuestMapEnvironmentCollection extends MeshViewCollection<GreatQuestModelMaterialMesh> {
        private final GreatQuestEntityManager manager;
        private final boolean noCulling;

        public GreatQuestMapEnvironmentCollection(GreatQuestEntityManager manager, boolean noCulling) {
            super(manager.getTransparentRenderManager().createDisplayListWithNewGroup());
            this.manager = manager;
            this.noCulling = noCulling;
        }

        @Override
        protected void onMeshViewSetup(int meshIndex, GreatQuestModelMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewSetup(meshIndex, mesh, meshView);
            this.manager.getController().getMainLight().getScope().add(meshView);
            meshView.setMouseTransparent(true); // Allow clicking through water / sky box to select objects.
            if (this.noCulling)
                meshView.setCullFace(CullFace.NONE);
        }

        @Override
        protected void onMeshViewCleanup(int meshIndex, GreatQuestModelMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewCleanup(meshIndex, mesh, meshView);
            meshView.getTransforms().clear();
            this.manager.getController().getMainLight().getScope().remove(meshView);
        }
    }

    /**
     * Test if the provided file name matches the file name pattern for a sky dome.
     * @param fileName the file name to test
     * @return fileNameIsSkyDome
     */
    public static boolean isFileNameSkyDome(String fileName) {
        return DOME_PATTERN.matcher(fileName).matches();
    }

    /**
     * Test if the provided file name matches the file name pattern for a static water mesh.
     * @param fileName the file name to test
     * @return fileNameIsWaterMesh
     */
    public static boolean isFileNameWaterMesh(String fileName) {
        return WATER_PATTERN.matcher(fileName).matches();
    }

    /**
     * Test if the provided file name matches the file name pattern for a static water mesh or a sky dome.
     * @param fileName the file name to test
     * @return fileNameIsEnvironmentMesh
     */
    public static boolean isFileNameEnvironmentalMesh(String fileName) {
        return isFileNameSkyDome(fileName) || isFileNameWaterMesh(fileName);
    }
}