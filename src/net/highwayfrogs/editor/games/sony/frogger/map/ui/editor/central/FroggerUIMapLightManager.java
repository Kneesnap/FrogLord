package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central;

import javafx.scene.*;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerMapLight;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquare;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerCentralUIManager.FroggerCentralMapListManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapLightManager.FroggerMapLightPreview;
import net.highwayfrogs.editor.games.sony.shared.misc.MRLightType;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.fxobject.Cone3D;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages map lighting for Frogger.
 * TODO: For changing the direction of a light, a rotation gizmo is ideal.
 * Created by Kneesnap on 6/1/2024.
 */
@Getter
public class FroggerUIMapLightManager extends FroggerCentralMapListManager<FroggerMapLight, FroggerMapLightPreview> {
    private final AmbientLight specialFrogletLight = new AmbientLight(ColorUtils.fromRGB(0xA0A0A0));
    private final List<AmbientLight> regularAmbientLights = new ArrayList<>();
    private final DisplayList lightList;
    private DisplayList coneList;
    private CheckBox applyLightingToEntitiesCheckBox;
    private CheckBox showPreviewsCheckBox;

    public FroggerUIMapLightManager(MeshViewController<FroggerMapMesh> controller) {
        super(controller);
        this.lightList = getRenderManager().createDisplayList();
    }

    @Override
    public void onSetup() {
        this.lightList.add(this.specialFrogletLight);
        super.onSetup();
        updateEntityLighting();
    }

    @Override
    public void setupNodesWhichRenderLast() {
        super.setupNodesWhichRenderLast();
        this.coneList = getTransparentRenderManager().createDisplayListWithNewGroup();
        update3DLightPreviews();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        this.applyLightingToEntitiesCheckBox = getMainGrid().addCheckBox("Apply Lighting to Entities", true, newValue -> updateEntityLighting());
        this.showPreviewsCheckBox = getMainGrid().addCheckBox("Show Spotlights", false, newValue -> update3DLightPreviews());
        getValueDisplaySetting().setValue(ListDisplayType.ALL); // All lights should show by default.
    }

    @Override
    protected Image getListDisplayImage(int index, FroggerMapLight light) {
        FroggerMapLightPreview preview = getDelegatesByValue().get(light);
        return preview != null ? preview.getColorPreviewImage() : null;
    }


    @Override
    public String getTitle() {
        return "Lighting";
    }

    @Override
    public String getValueName() {
        return "Light";
    }

    @Override
    public List<FroggerMapLight> getValues() {
        return getMap().getLightPacket().getLights();
    }

    @Override
    protected boolean tryAddValue(FroggerMapLight light) {
        return light != null && getMap().getLightPacket().addLight(light);
    }

    @Override
    protected boolean tryRemoveValue(FroggerMapLight light) {
        return light != null && getMap().getLightPacket().removeLight(light);
    }

    @Override
    protected FroggerMapLightPreview setupDisplay(FroggerMapLight light) {
        FroggerMapLightPreview preview = new FroggerMapLightPreview(this, light);
        preview.updateLight();
        return preview;
    }

    @Override
    protected void updateEditor(FroggerMapLight light) {
        light.makeEditor(getEditorGrid(), this, getDelegatesByValue().get(light));
    }

    @Override
    protected void setVisible(FroggerMapLight light, FroggerMapLightPreview lightPreview, boolean visible) {
        if (lightPreview != null)
            lightPreview.setVisible(visible);
    }

    @Override
    public boolean isValueVisibleByUI(FroggerMapLight light) {
        return super.isValueVisibleByUI(light) && isLightingAppliedToEntities();
    }

    @Override
    protected void onSelectedValueChange(FroggerMapLight oldLight, FroggerMapLightPreview oldLightPreview, FroggerMapLight newLight, FroggerMapLightPreview newLightPreview) {
        // Nothing for now.
    }

    /**
     * Update the 3D representations of lights in 3D space.
     */
    public void update3DLightPreviews() {
        getDelegatesByValue().values().forEach(FroggerMapLightPreview::updateShapes);
    }

    @Override
    protected FroggerMapLight createNewValue() {
        // Ensure the type of light created is capable of being added to the map.
        MRLightType lightType = MRLightType.AMBIENT;
        if (getMap().getLightPacket().hasAmbientLight()) {
            lightType = MRLightType.PARALLEL;
            if (getMap().getLightPacket().hasMaxNumberOfParallelLights())
                lightType = MRLightType.POINT;
        }

        return new FroggerMapLight(getMap(), lightType);
    }

    @Override
    protected void onDelegateRemoved(FroggerMapLight light, FroggerMapLightPreview lightPreview) {
        lightPreview.onRemove();
    }

    private Group getEntityGroup() {
        return getController().getEntityManager().getLitEntityRenderList().getRoot();
    }

    /**
     * Updates whether lighting is applied to entities.
     */
    public void updateEntityLighting() {
        if (isLightingAppliedToEntities()) {
            getController().getMainLight().getScope().remove(getEntityGroup());
        } else {
            addToScope(getController().getMainLight(), getEntityGroup());
        }

        getController().getGeneralManager().updatePlayerCharacterLighting();
        updateValueVisibility();
    }

    /**
     * Updates the light provided (Or creates a new one) to match the data provided by the FroggerMapLight.
     * @param light The map light to apply to the 3D light preview.
     * @param fxLight The 3D light preview to update.
     * @return updatedLightObject
     */
    public LightBase updateLight(FroggerMapLight light, LightBase fxLight) {
        switch (light.getLightType()) {
            case AMBIENT:
                AmbientLight ambLight = fxLight instanceof AmbientLight ? (AmbientLight) fxLight : new AmbientLight();
                ambLight.setColor(ColorUtils.fromBGR(light.getColor()));
                fxLight = ambLight;
                break;

            case PARALLEL:
                // IMPORTANT! JavaFX does NOT support parallel (directional) lights [AndyEder]
                PointLight parallelLight = fxLight instanceof PointLight ? (PointLight) fxLight : new PointLight();
                parallelLight.setColor(ColorUtils.fromBGR(light.getColor()));
                // Use direction as a vector to set a position to simulate a parallel light as best as we can
                parallelLight.setTranslateX(-light.getDirection().getFloatX(12) * 1024);
                parallelLight.setTranslateY(-light.getDirection().getFloatY(12) * 1024);
                parallelLight.setTranslateZ(-light.getDirection().getFloatZ(12) * 1024);
                fxLight = parallelLight;
                break;

            case POINT: // Point lights are not used.
                PointLight pointLight = fxLight instanceof PointLight ? (PointLight) fxLight : new PointLight();
                pointLight.setColor(ColorUtils.fromBGR(light.getColor()));
                // Assuming direction is position? Are POINT lights ever used? [AndyEder]
                pointLight.setTranslateX(light.getDirection().getFloatX());
                pointLight.setTranslateY(light.getDirection().getFloatY());
                pointLight.setTranslateZ(light.getDirection().getFloatZ());
                // TODO: How do we set the intensity / falloff?
                // TODO: Newer versions of JAvaFX (16+) have attenuation constants which we can use to control the distance, https://openjfx.io/javadoc/17/javafx.graphics/javafx/scene/PointLight.html
                fxLight = pointLight;
                break;
        }

        return fxLight;
    }

    /**
     * Applies ambient lighting to the given node.
     * @param node The node to apply to ambient lighting to
     * @param useSpecialLighting If true, the special froglet lighting will be applied
     */
    public void setAmbientLightingMode(Node node, boolean useSpecialLighting) {
        FroggerUIMapGeneralManager generalManager = getController().getGeneralManager();
        AmbientLight specialLight = (generalManager.getPlayerCharacterView() == node) ? generalManager.getFrogLight() : this.specialFrogletLight;

        // Checkpoints have special lighting applied by ent_gen.c.
        if (isLightingAppliedToEntities() && useSpecialLighting) { // 'CHECKPOINT' is valid starting from PSX Alpha to retail.
            // Remove regular ambient lights from the frog.
            for (int i = 0; i < this.regularAmbientLights.size(); i++)
                removeFromScope(this.regularAmbientLights.get(i), node);

            // Apply special froglet ambient light.
            addToScope(specialLight, node);
        } else {
            // Remove special froglet ambient light.
            removeFromScope(specialLight, node);

            // Add regular ambient lights to the node.
            for (int i = 0; i < this.regularAmbientLights.size(); i++)
                addToScope(this.regularAmbientLights.get(i), node);
        }
    }

    private static void addToScope(LightBase light, Node node) {
        if (!light.getScope().contains(node)) {
            light.getScope().add(node);
            light.setLightOn(true); // If we always keep it visible, it'll be active when the scope is empty, which to JavaFX means "apply everywhere", but to us means "apply nowhere"
        }
    }

    private static void removeFromScope(LightBase light, Node node) {
        if (light.getScope().remove(node) && light.getScope().isEmpty())
            light.setLightOn(false); // If we always keep it visible, it'll be active when the scope is empty, which to JavaFX means "apply everywhere", but to us means "apply nowhere"
    }

    /**
     * Remove the given node from all ambient lights which might explicitly include the given node in their scope.
     * @param node the node to remove.
     */
    public void clearAmbientLighting(Node node) {
        // Remove regular ambient lights from the frog.
        for (int i = 0; i < this.regularAmbientLights.size(); i++)
            removeFromScope(this.regularAmbientLights.get(i), node);

        // Remove special froglet ambient light.
        removeFromScope(this.specialFrogletLight, node);
    }

    /**
     * Tests if lighting is applied to entities.
     */
    public boolean isLightingAppliedToEntities() {
        return this.applyLightingToEntitiesCheckBox == null || this.applyLightingToEntitiesCheckBox.isSelected();
    }

    public static class FroggerMapLightPreview {
        @Getter private final FroggerUIMapLightManager lightingManager;
        @Getter private final FroggerMapLight mapLight;
        @Getter private LightBase fxLight;
        @Getter private boolean visible;
        @Getter private Cylinder directionalLine;
        private Image colorPreviewImage;
        private int lastColorPreviewColorValue;
        private MeshView meshView;

        public FroggerMapLightPreview(FroggerUIMapLightManager lightingManager, FroggerMapLight mapLight) {
            this.mapLight = mapLight;
            this.lightingManager = lightingManager;
        }

        /**
         * Gets the color image preview.
         */
        public Image getColorPreviewImage() {
            if (this.lastColorPreviewColorValue == this.mapLight.getColor() && this.colorPreviewImage != null)
                return this.colorPreviewImage;

            this.lastColorPreviewColorValue = this.mapLight.getColor();
            return this.colorPreviewImage = ColorUtils.makeColorImageNoCache(ColorUtils.fromBGR(this.mapLight.getColor()), 8, 8);
        }

        /**
         * Sets whether the fx light is visible
         * @param visible the visibility state to apply
         */
        public void setVisible(boolean visible) {
            this.visible = visible;

            if (this.meshView != null)
                this.meshView.setVisible(visible);
            if (this.directionalLine != null)
                this.directionalLine.setVisible(visible);

            if (this.fxLight == null)
                return;

            if (this.fxLight instanceof AmbientLight) {
                // Ambient lights are tracked separately because of checkpoint lighting.
                boolean didChangeOccur = false;
                AmbientLight ambientLight = (AmbientLight) this.fxLight;
                if (visible) {
                    if (!this.lightingManager.regularAmbientLights.contains(ambientLight))
                        didChangeOccur = this.lightingManager.regularAmbientLights.add(ambientLight);
                } else {
                    didChangeOccur = this.lightingManager.regularAmbientLights.remove(ambientLight);
                }

                if (didChangeOccur) {
                    this.lightingManager.getController().getEntityManager().updateAllEntityMeshes(); // This is necessary for things to be lit properly.
                    this.lightingManager.getController().getGeneralManager().updatePlayerCharacterLighting();
                }
            } else {
                // Regular lights are chucked into a group for lit entities.
                if (visible && this.lightingManager.isLightingAppliedToEntities()) {
                    addToScope(this.fxLight, this.lightingManager.getEntityGroup());
                } else {
                    removeFromScope(this.fxLight, this.lightingManager.getEntityGroup());
                }
            }

            if (visible) {
                this.lightingManager.getLightList().addIfMissing(this.fxLight);
            } else {
                this.lightingManager.getLightList().remove(this.fxLight);
            }
        }

        /**
         * Updates the light preview in 3D space.
         */
        public void updateLight() {
            LightBase newLight = this.lightingManager.updateLight(this.mapLight, this.fxLight);
            if (this.fxLight != newLight) {
                boolean wasVisible = this.visible;
                setVisible(false); // Make current light invisible.

                // Setup new light.
                this.fxLight = newLight;
                setVisible(wasVisible);
            }

            updateShapes();
        }

        /**
         * Update the 3D preview shapes.
         */
        public void updateShapes() {
            if (this.lightingManager.showPreviewsCheckBox == null || !this.lightingManager.showPreviewsCheckBox.isSelected()) {
                removeShapes();
                return;
            }

            // Remove the line before adding the updated one to ensure there aren't multiple lines for the same light.
            if (this.directionalLine != null) {
                this.lightingManager.getLightList().remove(this.directionalLine);
                this.directionalLine = null;
            }

            if (this.mapLight.getLightType() == MRLightType.PARALLEL) {
                float startX, startY, startZ;
                startY = 0;
                startX = DataUtils.fixedPointShortToFloat4Bit(this.mapLight.getMapFile().getGridPacket().getBaseGridX());
                startZ = DataUtils.fixedPointShortToFloat4Bit(this.mapLight.getMapFile().getGridPacket().getBaseGridZ());

                SVector direction = this.mapLight.getDirection();
                this.directionalLine = this.lightingManager.getLightList().addLine(startX, startY, startZ,
                        startX + (10 * FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT * direction.getFloatX(12)),
                        startY + (10 * FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT * direction.getFloatY(12)),
                        startZ + (10 * FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT * direction.getFloatZ(12)),
                        3, Scene3DUtils.makeUnlitSharpMaterial(Color.RED));
                this.directionalLine.setOnMouseClicked(event -> this.lightingManager.getValueSelectionBox().getSelectionModel().select(this.mapLight));
            }

            if (this.lightingManager.getConeList() != null) {
                if (this.mapLight.getLightType() == MRLightType.SPOT) {
                    if (this.meshView == null) {
                        this.meshView = new MeshView();
                        this.meshView.setCullFace(CullFace.BACK);
                        this.lightingManager.getConeList().add(this.meshView);
                        this.meshView.setOnMouseClicked(event -> this.lightingManager.getValueSelectionBox().getSelectionModel().select(this.mapLight));
                    }

                    TriangleMesh mesh = (TriangleMesh) this.meshView.getMesh();
                    if (mesh == null)
                        this.meshView.setMesh(mesh = new TriangleMesh());

                    float height = calculateLightConeHeightViaRaycasting();
                    float radius = (float) Math.tan(Math.PI * (this.mapLight.getAttribute0() / 4096F)) * height;
                    Cone3D.createCone(mesh, 30, radius, height);
                    Scene3DUtils.setNodePosition(this.meshView, this.mapLight.getPosition().getFloatX(), this.mapLight.getPosition().getFloatY(), this.mapLight.getPosition().getFloatZ());
                    Scene3DUtils.setNodeRotation(this.meshView, (float) Math.asin(this.mapLight.getDirection().getFloatZ(12)), 0, (float) -Math.asin(this.mapLight.getDirection().getFloatX(12)));
                    this.meshView.setMaterial(Scene3DUtils.makeUnlitSharpMaterial(ColorUtils.fromRGB(ColorUtils.swapRedBlue(this.mapLight.getColor()), .333F)));
                } else if (this.meshView != null) {
                    this.meshView.setMesh(null);
                }
            }
        }

        private float calculateLightConeHeightViaRaycasting() {
            float startX = this.mapLight.getPosition().getFloatX();
            float startY = this.mapLight.getPosition().getFloatY();
            float startZ = this.mapLight.getPosition().getFloatZ();
            float dirX = this.mapLight.getDirection().getFloatX(12);
            float dirY = this.mapLight.getDirection().getFloatY(12);
            float dirZ = this.mapLight.getDirection().getFloatZ(12);

            int gridSquaresMissed = 0;
            float currX = startX, currY = startY, currZ = startZ;
            Vector3f tempVector = new Vector3f();
            FroggerMapFilePacketGrid gridPacket = this.mapLight.getMapFile().getGridPacket();
            int attempts = 0;
            while (gridSquaresMissed < 20 && ++attempts < 1000) { // TODO: Is this right?
                if (gridSquaresMissed > 0) {
                    currX += FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT * dirX;
                    currY += FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT * dirY;
                    currZ += FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT * dirZ;
                } else {
                    currX += dirX;
                    currY += dirY;
                    currZ += dirZ;
                }

                int gridX = gridPacket.getGridXFromWorldX(currX);
                int gridZ = gridPacket.getGridZFromWorldZ(currZ);
                if (gridX < 0 || gridZ < 0 || gridX >= gridPacket.getGridXCount() || gridZ >= gridPacket.getGridZCount()) {
                    gridSquaresMissed++;
                    continue;
                }

                FroggerGridStack gridStack = gridPacket.getGridStack(gridX, gridZ);
                gridSquaresMissed = 0;
                for (int i = 0; i < gridStack.getGridSquares().size(); i++) {
                    FroggerGridSquare gridSquare = gridStack.getGridSquares().get(i);
                    float polygonY = gridSquare.getPolygon().getCenterOfPolygon(tempVector).getY();
                    if (Math.abs(polygonY - currY) < dirY)
                        return Math.abs(polygonY - startY) + (FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT * .5F);
                }
            }

            // Couldn't find, so here's a default.
            return 15 * FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT;
        }

        private float calculateLightConeHeightViaRaycastingWorkingBackup() { // TODO: TOSS
            float startX = this.mapLight.getPosition().getFloatX();
            float startY = this.mapLight.getPosition().getFloatY();
            float startZ = this.mapLight.getPosition().getFloatZ();
            float dirX = this.mapLight.getDirection().getFloatX(12);
            float dirY = this.mapLight.getDirection().getFloatY(12);
            float dirZ = this.mapLight.getDirection().getFloatZ(12);

            int gridSquaresMissed = 0;
            float currX = startX, currY = startY, currZ = startZ;
            Vector3f tempVector = new Vector3f();
            FroggerMapFilePacketGrid gridPacket = this.mapLight.getMapFile().getGridPacket();
            int attempts = 0;
            while (gridSquaresMissed < 20 && ++attempts < 1000) { // TODO: Is this right?
                if (gridSquaresMissed > 0) {
                    currX += FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT * dirX;
                    currY += FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT * dirY;
                    currZ += FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT * dirZ;
                } else {
                    currX += dirX;
                    currY += dirY;
                    currZ += dirZ;
                }

                int gridX = gridPacket.getGridXFromWorldX(currX);
                int gridZ = gridPacket.getGridZFromWorldZ(currZ);
                if (gridX < 0 || gridZ < 0 || gridX >= gridPacket.getGridXCount() || gridZ >= gridPacket.getGridZCount()) {
                    gridSquaresMissed++;
                    continue;
                }

                FroggerGridStack gridStack = gridPacket.getGridStack(gridX, gridZ);
                gridSquaresMissed = 0;
                for (int i = 0; i < gridStack.getGridSquares().size(); i++) {
                    FroggerGridSquare gridSquare = gridStack.getGridSquares().get(i);
                    float polygonY = gridSquare.getPolygon().getCenterOfPolygon(tempVector).getY();
                    if (Math.abs(polygonY - currY) < dirY)
                        return Math.abs(polygonY - startY) + (FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT * .5F);
                }
            }

            // Couldn't find, so here's a default.
            return 15 * FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH_FLOAT;
        }

        /**
         * Called when the preview is removed.
         */
        private void onRemove() {
            setVisible(false);
            this.fxLight = null;
            removeShapes();
        }

        /**
         * Removes the 3D preview.
         */
        private void removeShapes() {
            if (this.directionalLine != null) {
                this.lightingManager.getLightList().remove(this.directionalLine);
                this.directionalLine = null;
            }
            if (this.meshView != null) {
                this.lightingManager.getConeList().remove(this.meshView);
                this.meshView = null;
            }
        }
    }
}