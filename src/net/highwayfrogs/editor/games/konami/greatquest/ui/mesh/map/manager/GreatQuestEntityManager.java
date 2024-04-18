package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager;

import javafx.application.Platform;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelDesc;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceModel;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.GreatQuestMapMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager.GreatQuestMapModelMeshCollection;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestMapUIManager.GreatQuestMapListManager;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMaterialMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMesh;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection.MeshViewCollection;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Manages entities for a Great Quest map.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestEntityManager extends GreatQuestMapListManager<kcCResourceEntityInst, GreatQuestMapModelMeshCollection> {
    private final Map<kcCResourceModel, GreatQuestModelMesh> cachedModelMeshes = new HashMap<>();
    private final List<GreatQuestMapEnvironmentCollection> waterMeshCollections = new ArrayList<>();
    private GreatQuestMapEnvironmentCollection skyBoxCollection;
    private static final Pattern DOME_PATTERN = Pattern.compile("(?i)^\\d\\ddome(\\d\\d)?\\.vtx$");
    private static final Pattern WATER_PATTERN = Pattern.compile("(?i)^\\d\\d((lake)|(river)|(waterfall)|(water))(\\d\\d)?\\.vtx$");

    public GreatQuestEntityManager(MeshViewController<GreatQuestMapMesh> controller) {
        super(controller);
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        getValueDisplaySetting().setValue(ListDisplayType.ALL);

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
                GreatQuestModelMesh skyBoxMesh = new GreatQuestModelMesh(resourceModel, false);
                GreatQuestMapEnvironmentCollection waterCollection = new GreatQuestMapEnvironmentCollection(this, true);
                waterCollection.setMesh(skyBoxMesh.getActualMesh());
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
    public List<kcCResourceEntityInst> getValues() {
        List<kcCResourceEntityInst> entities = new ArrayList<>();
        for (kcCResource resource : getMap().getChunks())
            if (resource instanceof kcCResourceEntityInst)
                entities.add(((kcCResourceEntityInst) resource));

        return entities;
    }

    @Override
    protected GreatQuestMapModelMeshCollection setupDisplay(kcCResourceEntityInst entityInst) {
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
        return entityMeshCollection;
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
    protected void setVisible(kcCResourceEntityInst kcCResourceEntityInst, GreatQuestMapModelMeshCollection meshViews, boolean visible) {
        if (meshViews != null)
            meshViews.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(kcCResourceEntityInst oldValue, GreatQuestMapModelMeshCollection oldMeshViews, kcCResourceEntityInst newValue, GreatQuestMapModelMeshCollection newMeshViews) {
        if (oldMeshViews != null) {
            for (int i = 0; i < oldMeshViews.getMeshViews().size(); i++) {
                MeshView oldMeshView = oldMeshViews.getMeshViews().get(i);
                GreatQuestModelMaterialMesh oldCachedMesh = oldMeshView.getMesh() instanceof GreatQuestModelMaterialMesh ? ((GreatQuestModelMaterialMesh) oldMeshView.getMesh()) : null;
                if (oldCachedMesh != null)
                    oldMeshView.setMaterial(oldCachedMesh.getMaterial());
            }
        }


        if (newMeshViews != null) {
            for (int i = 0; i < newMeshViews.getMeshViews().size(); i++) {
                MeshView newMeshView = newMeshViews.getMeshViews().get(i);
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
    protected void onDelegateRemoved(kcCResourceEntityInst kcCResourceEntityInst, GreatQuestMapModelMeshCollection meshViews) {
        // Do nothing?
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
            meshView.setOnMouseClicked(event -> this.manager.handleClick(event, this.entityInst));
            this.manager.getController().getMainLight().getScope().add(meshView);

            kcEntityInst entityInst = this.entityInst.getEntity();
            if (entityInst instanceof kcEntity3DInst) {
                kcEntity3DInst entity3D = (kcEntity3DInst) entityInst;
                Scene3DUtils.setNodePosition(meshView, entity3D.getPosition().getX(), entity3D.getPosition().getY(), entity3D.getPosition().getZ());
                Scene3DUtils.setNodeScale(meshView, entity3D.getScale().getX(), entity3D.getScale().getY(), entity3D.getScale().getZ());
                // Scene3DUtils.setNodeRotation(meshView, entity3D.getRotation().getX(), entity3D.getRotation().getZ(), entity3D.getRotation().getY());
                // TODO: Some objects if PI / 2 is subtracted from their X start looking correct. However, this breaks others. Need to figure out what's going on here.
                // TODO: CCoinDesc, CGemDesc, CPropDesc
                //  - ? CPropDesc (AGGGHHH. Bruiser's Chair + Lilypads DO need rotation, but treasure chest & Oyster do not) FrogPad description & regular flags perfectly match Oyster soo.
                //  - CharacterParams
                kcEntity3DDesc entityDesc = entity3D.getDescription(this.manager.getMap());
                boolean hasAnimationSet = (entityDesc instanceof kcActorBaseDesc) && ((kcActorBaseDesc) entityDesc).getAnimationSet(this.manager.getMap()) != null;
                meshView.getTransforms().add(new Rotate(Math.toDegrees(entity3D.getRotation().getZ()), Rotate.Z_AXIS));
                meshView.getTransforms().add(new Rotate(Math.toDegrees(entity3D.getRotation().getY()), Rotate.Y_AXIS));
                meshView.getTransforms().add(new Rotate(Math.toDegrees(entity3D.getRotation().getX() - (hasAnimationSet ? Math.PI / 2 : 0)), Rotate.X_AXIS));
            }
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
            super(manager.getRenderManager().createDisplayList());
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