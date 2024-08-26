package net.highwayfrogs.editor.games.renderware.chunks;

import javafx.scene.control.ContextMenu;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.IRwStreamChunkWithEmbeddedStruct;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.games.renderware.struct.types.RwV3d;
import net.highwayfrogs.editor.games.renderware.ui.IRwStreamChunkUIEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an rwFrameList.
 * Created by Kneesnap on 8/25/2024.
 */
@Getter
public class RwFrameListChunk extends RwStreamChunk implements IRwStreamChunkWithEmbeddedStruct {
    private final List<RwStreamFrame> frames = new ArrayList<>();

    public RwFrameListChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.FRAME_LIST, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        readEmbeddedStruct(reader);

        // Read frame extension data.
        for (int i = 0; i < this.frames.size(); i++)
            readChunk(reader, this.frames.get(i).getExtension(), false);
    }

    @Override
    public void loadEmbeddedStructData(DataReader reader, int version, int dataLength) {
        // Represents 'rwStreamFrameList' as defined in babinfrm.c.
        int frameCount = reader.readInt();

        // Read frames.
        this.frames.clear();
        for (int i = 0; i < frameCount; i++) {
            RwStreamFrame newFrame = new RwStreamFrame(this);
            newFrame.load(reader, version, dataLength);
            this.frames.add(newFrame);
            this.childUISections.add(newFrame);
        }
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeEmbeddedStruct(writer);

        // Write frame extension data.
        for (int i = 0; i < this.frames.size(); i++)
            writeChunk(writer, this.frames.get(i).getExtension(), false);
    }

    @Override
    public void saveEmbeddedStructData(DataWriter writer, int version) {
        writer.writeInt(this.frames.size());

        // Write frame data.
        for (int i = 0; i < this.frames.size(); i++) {
            RwStreamFrame frame = this.frames.get(i);
            frame.save(writer, version);
            this.childUISections.add(frame);
        }
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Frames", this.frames.size());
        return propertyList;
    }

    @Override
    protected String getLoggerInfo() {
        return super.getLoggerInfo() + ",frameCount=" + this.frames.size();
    }

    /**
     * Represents the RwFrame/'rwStreamFrame' struct as defined in babinfrm.c
     */
    @Getter
    public static class RwStreamFrame extends RwStruct implements IRwStreamChunkUIEntry {
        private final RwFrameListChunk frameList;
        private final RwV3d matrixRight;
        private final RwV3d matrixUp;
        private final RwV3d matrixAt;
        private final RwV3d matrixPos;
        private int parentIndex;
        private int data;
        private final RwExtensionChunk extension;

        public RwStreamFrame(RwFrameListChunk chunk) {
            super(chunk.getGameInstance(), RwStructType.STREAM_FRAME);
            this.frameList = chunk;
            this.matrixRight = new RwV3d(getGameInstance());
            this.matrixUp = new RwV3d(getGameInstance());
            this.matrixAt = new RwV3d(getGameInstance());
            this.matrixPos = new RwV3d(getGameInstance());
            this.extension = new RwExtensionChunk(chunk.getStreamFile(), chunk.getVersion(), chunk);
        }

        @Override
        public void load(DataReader reader, int version, int byteLength) {
            this.matrixRight.load(reader, version, byteLength);
            this.matrixUp.load(reader, version, byteLength);
            this.matrixAt.load(reader, version, byteLength);
            this.matrixPos.load(reader, version, byteLength);
            this.parentIndex = reader.readInt();
            this.data = reader.readInt();
        }

        @Override
        public void save(DataWriter writer, int version) {
            this.matrixRight.save(writer, version);
            this.matrixUp.save(writer, version);
            this.matrixAt.save(writer, version);
            this.matrixPos.save(writer, version);
            writer.writeInt(this.parentIndex);
            writer.writeInt(this.data);
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            propertyList.add("Matrix Position", this.matrixPos);
            propertyList.add("Matrix At", this.matrixAt);
            propertyList.add("Matrix Right", this.matrixRight);
            propertyList.add("Matrix Up", this.matrixUp);
            propertyList.add("Parent Index", this.parentIndex);
            propertyList.add("Data", this.data);
            return propertyList;
        }

        @Override
        public String toString() {
            return "RwStreamFrame{parentIndex=" + this.parentIndex + ",data=" + this.data
                    + "mtxPos=" + this.matrixPos + ",mtxAt=" + this.matrixAt
                    + ",mtxRight=" + this.matrixRight + ",mtxUp=" + this.matrixUp + "}";
        }

        @Override
        public List<? extends IRwStreamChunkUIEntry> getChildUISections() {
            return Collections.singletonList(this.extension);
        }

        @Override
        public RwStreamFile getStreamFile() {
            return this.frameList.getStreamFile();
        }

        @Override
        public void setupRightClickMenuItems(ContextMenu contextMenu) {
            // Don't add anything.
        }
    }
}