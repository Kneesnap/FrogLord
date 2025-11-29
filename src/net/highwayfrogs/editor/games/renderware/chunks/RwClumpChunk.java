package net.highwayfrogs.editor.games.renderware.chunks;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.RwVersion;
import net.highwayfrogs.editor.games.renderware.mesh.clump.material.RwClumpCombinedMesh;
import net.highwayfrogs.editor.games.renderware.mesh.clump.material.RwClumpCombinedMeshController;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.games.renderware.struct.types.RwStructInt32;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a clump chunk.
 * Created by Kneesnap on 8/25/2024.
 */
@Getter
public class RwClumpChunk extends RwStreamChunk {
    private final RwFrameListChunk frameList;
    private final RwGeometryListChunk geometryList;
    private final List<RwAtomicChunk> atomics = new ArrayList<>();
    private final List<RwClumpLight> lights = new ArrayList<>();
    private final List<RwClumpCamera> cameras = new ArrayList<>();

    public RwClumpChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.CLUMP, version, parentChunk);
        this.frameList = new RwFrameListChunk(streamFile, version, this);
        this.geometryList = new RwGeometryListChunk(streamFile, version, this);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        RpClumpChunkInfo clumpInfo = readStruct(reader, RpClumpChunkInfo.class, false);
        readChunk(reader, this.frameList);
        readChunk(reader, this.geometryList);

        // Read atomics.
        this.atomics.clear();
        this.lights.clear();
        this.cameras.clear();
        for (int i = 0; i < clumpInfo.getAtomicCount(); i++)
            this.atomics.add(readChunk(reader, RwAtomicChunk.class));

        // Doesn't seem to exist in Frogger Beyond, but does exist in Rescue.
        if (RwVersion.isAtLeast(getVersion(), RwVersion.VERSION_3403)) {
            // Read lights.
            for (int i = 0; i < clumpInfo.getLightCount(); i++) {
                int frameIndex = readStruct(reader, RwStructInt32.class, false).getValue();
                this.lights.add(readChunk(reader, new RwClumpLight(getStreamFile(), version, this, frameIndex)));
            }

            // Read cameras.
            for (int i = 0; i < clumpInfo.getCameraCount(); i++) {
                int frameIndex = readStruct(reader, RwStructInt32.class, false).getValue();
                this.cameras.add(readChunk(reader, new RwClumpCamera(getStreamFile(), version, this, frameIndex)));
            }
        }

        // Read extension data.
        readOptionalExtensionData(reader);
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeStruct(writer, new RpClumpChunkInfo(this), false);
        writeChunk(writer, this.frameList);
        writeChunk(writer, this.geometryList);

        // Write atomics.
        for (int i = 0; i < this.atomics.size(); i++)
            writeChunk(writer, this.atomics.get(i));

        // Doesn't seem to exist in Frogger Beyond, but does exist in Rescue.
        if (RwVersion.isAtLeast(getVersion(), RwVersion.VERSION_3403)) {
            // Write lights.
            for (int i = 0; i < this.lights.size(); i++) {
                RwClumpLight light = this.lights.get(i);
                writeStruct(writer, new RwStructInt32(getGameInstance(), light.getFrameIndex()), false);
                writeChunk(writer, light);
            }

            // Write cameras.
            for (int i = 0; i < this.cameras.size(); i++) {
                RwClumpCamera camera = this.cameras.get(i);
                writeStruct(writer, new RwStructInt32(getGameInstance(), camera.getFrameIndex()), false);
                writeChunk(writer, camera);
            }
        }

        writeOptionalExtensionData(writer);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("Atomics", this.atomics.size());
        propertyList.add("Lights", this.lights.size());
        propertyList.add("Cameras", this.cameras.size());
        propertyList.add("Frames", this.frameList.getFrames().size());
        propertyList.add("Geometries", this.geometryList.getGeometries().size());
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return new RwClumpUIController(this);
    }

    @Override
    public void handleDoubleClick() {
        openMeshViewer();
    }

    /**
     * Opens the mesh viewer for the clump.
     */
    public void openMeshViewer() {
        MeshViewController.setupMeshViewer(getGameInstance(), new RwClumpCombinedMeshController(getGameInstance()), new RwClumpCombinedMesh(this));
    }

    /**
     * Represents RpClumpChunkInfo as defined in baclump.h.
     */
    @Getter
    public static class RpClumpChunkInfo extends RwStruct {
        private int atomicCount;
        private int lightCount;
        private int cameraCount;

        public RpClumpChunkInfo(GameInstance instance) {
            super(instance, RwStructType.CLUMP_CHUNK_INFO);
        }

        public RpClumpChunkInfo(RwClumpChunk clumpChunk) {
            this(clumpChunk.getGameInstance());
            this.atomicCount = clumpChunk.getAtomics().size();
            this.lightCount = clumpChunk.getLights().size();
            this.cameraCount = clumpChunk.getCameras().size();
        }

        @Override
        public void load(DataReader reader, int version, int byteLength) {
            this.atomicCount = reader.readInt();
            this.lightCount = reader.readInt();
            this.cameraCount = reader.readInt();
        }

        @Override
        public void save(DataWriter writer, int version) {
            writer.writeInt(this.atomicCount);
            writer.writeInt(this.lightCount);
            writer.writeInt(this.cameraCount);
        }

        @Override
        public void addToPropertyList(PropertyListNode propertyList) {
            super.addToPropertyList(propertyList);
            propertyList.add("Atomic Count", this.atomicCount);
            propertyList.add("Light Count", this.lightCount);
            propertyList.add("Camera Count", this.cameraCount);
        }

        @Override
        public String toString() {
            return "RpClumpChunkInfo{atomics=" + this.atomicCount + ",lights=" + this.lightCount
                    + ",cameras=" + this.cameraCount + "}";
        }
    }

    @Getter
    public static class RwClumpLight extends RwLightChunk {
        private final int frameIndex;

        public RwClumpLight(RwStreamFile streamFile, int version, RwStreamChunk parentChunk, int frameIndex) {
            super(streamFile, version, parentChunk);
            this.frameIndex = frameIndex;
        }
    }

    @Getter
    public static class RwClumpCamera extends RwCameraChunk {
        private final int frameIndex;

        public RwClumpCamera(RwStreamFile streamFile, int version, RwStreamChunk parentChunk, int frameIndex) {
            super(streamFile, version, parentChunk);
            this.frameIndex = frameIndex;
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    public static class RwClumpUIController extends GameUIController<GameInstance> {
        private final Button viewButton;
        private final RwClumpChunk clump;

        public RwClumpUIController(RwClumpChunk clump) {
            super(clump.getGameInstance());
            this.clump = clump;
            this.viewButton = new Button("View Model");
            this.viewButton.setOnAction(evt -> this.clump.openMeshViewer());
            loadController(new VBox(3, this.viewButton));
            // TODO: Can we include a 3D preview within this area. Would be nice if there was an extra-simple 3D viewer.
        }

        @Override
        public VBox getRootNode() {
            return (VBox) super.getRootNode();
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            getRootNode().setAlignment(Pos.CENTER);
        }
    }
}