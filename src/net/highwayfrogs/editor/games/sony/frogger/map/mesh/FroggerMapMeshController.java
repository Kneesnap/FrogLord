package net.highwayfrogs.editor.games.sony.frogger.map.mesh;

import javafx.scene.SubScene;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapCameraZone;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGeneral;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.FroggerUIGeometryManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.FroggerUIMapAnimationManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.*;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.DataUtils;

/**
 * Controls a Frogger map mesh.
 * Created by Kneesnap on 5/28/2024.
 */
@Getter
public class FroggerMapMeshController extends MeshViewController<FroggerMapMesh> {
    private FroggerUIMapGeneralManager generalManager;
    private FroggerUIGeometryManager bakedGeometryManager;
    private FroggerUIMapAnimationManager animationManager;
    private FroggerUIMapEntityManager entityManager;
    private FroggerUIMapFormManager formManager;
    private FroggerUIMapPathManager pathManager;
    private FroggerUIMapLightManager lightManager;

    public FroggerMapMeshController(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public FroggerGameInstance getGameInstance() {
        return (FroggerGameInstance) super.getGameInstance();
    }

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        if (getMapFile().getMapTheme() == FroggerMapTheme.SKY) // Extend view area to cover SKY LAND.
            getFirstPersonCamera().getCamera().setFarClip(getFirstPersonCamera().getCamera().getFarClip() * 1.5);
        getMainLight().getScope().add(getMeshView());
        getMainLight().getScope().addAll(getAxisDisplayList().getNodes());
        this.generalManager.getSidePanel().requestFocus();
    }

    @Override
    protected void setupManagers() {
        addManager(this.generalManager = new FroggerUIMapGeneralManager(this));
        addManager(this.bakedGeometryManager = new FroggerUIGeometryManager(this));
        addManager(this.entityManager = new FroggerUIMapEntityManager(this));
        addManager(this.formManager = new FroggerUIMapFormManager(this));
        addManager(this.pathManager = new FroggerUIMapPathManager(this));
        addManager(this.lightManager = new FroggerUIMapLightManager(this));
        addManager(this.animationManager = new FroggerUIMapAnimationManager(this));
    }

    @Override
    public String getMeshDisplayName() {
        return getMapFile().getFileDisplayName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        FroggerMapFile map = getMapFile();
        FroggerMapFilePacketGeneral generalPacket = map.getGeneralPacket();
        FroggerMapFilePacketGrid gridPacket = map.getGridPacket();

        // The zone is used.
        FroggerMapCameraZone zone = map.getZonePacket().getCameraZone(generalPacket.getStartGridCoordX(), generalPacket.getStartGridCoordZ());
        SVector sourceOffset = zone != null ? zone.getNorthSourceOffset() : generalPacket.getDefaultCameraSourceOffset();
        SVector targetOffset = zone != null ? zone.getNorthTargetOffset() : generalPacket.getDefaultCameraTargetOffset();

        FroggerGridStack startStack = gridPacket.getGridStack(generalPacket.getStartGridCoordX(), generalPacket.getStartGridCoordZ());
        float gridX = DataUtils.fixedPointIntToFloat4Bit(gridPacket.getWorldXFromGridX(generalPacket.getStartGridCoordX(), true));
        float baseY = startStack != null ? startStack.getHighestGridSquareYAsFloat() : 0;
        float gridZ = DataUtils.fixedPointIntToFloat4Bit(gridPacket.getWorldZFromGridZ(generalPacket.getStartGridCoordZ(), true));

        // Make sure the start position is off the ground.
        float yOffset = sourceOffset.getFloatY();
        if (Math.abs(yOffset) <= .0001)
            yOffset = -100f;

        getFirstPersonCamera().setPos(gridX + sourceOffset.getFloatX(), baseY + yOffset, gridZ + sourceOffset.getFloatZ());
        getFirstPersonCamera().setCameraLookAt(gridX + targetOffset.getFloatX(), baseY + targetOffset.getFloatY(), gridZ + targetOffset.getFloatZ()); // Set the camera to look at the start position, too. The -1 is necessary to fix some near-zero math. It fixes it for QB.MAP for example.
    }

    /**
     * Gets the map file.
     */
    public FroggerMapFile getMapFile() {
        return getMesh().getMap();
    }
}