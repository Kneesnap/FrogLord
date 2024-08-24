package net.highwayfrogs.editor.games.renderware.chunks;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.chunks.sector.IRwWorldProvider;
import net.highwayfrogs.editor.games.renderware.chunks.sector.RwAtomicSectorChunk;
import net.highwayfrogs.editor.games.renderware.chunks.sector.RwPlaneSectorChunk;
import net.highwayfrogs.editor.games.renderware.chunks.sector.RwSectorBase;
import net.highwayfrogs.editor.games.renderware.mesh.world.RwWorldCombinedMesh;
import net.highwayfrogs.editor.games.renderware.mesh.world.RwWorldMeshController;
import net.highwayfrogs.editor.games.renderware.struct.types.RpWorldChunkInfo;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the RenderWare world chunk.
 * Created by Kneesnap on 8/12/2024.
 */
@Getter
public class RwWorldChunk extends RwStreamChunk implements IRwWorldProvider {
    private final List<RwAtomicSectorChunk> worldSectors = new ArrayList<>();
    private final List<RwPlaneSectorChunk> planeSectors = new ArrayList<>();
    private RpWorldChunkInfo worldInfo; // TODO: Auto-generate this later.

    private RwMaterialListChunk materialList;
    private RwSectorBase rootSector;

    public RwWorldChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.WORLD, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        this.worldInfo = readStruct(reader, RpWorldChunkInfo.class);
        this.materialList = readChunk(reader, RwMaterialListChunk.class);
        if (this.worldInfo.isRootIsWorldSector()) {
            this.rootSector = readChunk(reader, RwAtomicSectorChunk.class);
        } else {
            this.rootSector = readChunk(reader, RwPlaneSectorChunk.class);
        }

        readOptionalExtensionData(reader);
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        this.worldInfo.applyDataFromWorld(this);
        writeStruct(writer, this.worldInfo);
        writeChunk(writer, this.materialList);
        writeChunk(writer, this.rootSector);
        writeOptionalExtensionData(writer);
    }

    @Override
    public RwWorldChunk getWorld() {
        return this;
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return new RwWorldUIController(this);
    }

    @SuppressWarnings("FieldCanBeLocal")
    public static class RwWorldUIController extends GameUIController<GameInstance> {
        private final Button viewButton;
        private final RwWorldChunk world;

        public RwWorldUIController(RwWorldChunk world) {
            super(world.getGameInstance());
            this.world = world;
            this.viewButton = new Button("View World");
            this.viewButton.setOnAction(evt -> MeshViewController.setupMeshViewer(getGameInstance(), new RwWorldMeshController(), new RwWorldCombinedMesh(this.world)));
            loadController(new VBox(3, this.viewButton));
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