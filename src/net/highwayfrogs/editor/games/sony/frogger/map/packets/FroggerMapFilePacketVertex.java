package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents map vertices.
 * Created by Kneesnap on 5/25/2024.
 */
@Getter
public class FroggerMapFilePacketVertex extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "VRTX";
    private final List<SVector> vertices = new ArrayList<>();

    public FroggerMapFilePacketVertex(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.vertices.clear();
        int vertexCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.
        for (int i = 0; i < vertexCount; i++)
            this.vertices.add(SVector.readWithPadding(reader));
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.vertices.size());
        writer.writeNull(Constants.SHORT_SIZE); // Padding.
        for (int i = 0; i < this.vertices.size(); i++)
            this.vertices.get(i).saveWithPadding(writer);
    }

    @Override
    public void clear() {
        this.vertices.clear();
    }

    @Override
    public void copyAndConvertData(SCFilePacket<? extends SCChunkedFile<FroggerGameInstance>, FroggerGameInstance> newChunk) {
        if (!(newChunk instanceof FroggerMapFilePacketVertex))
            throw new ClassCastException("The provided chunk was of type " + Utils.getSimpleName(newChunk) + " when " + FroggerMapFilePacketVertex.class.getSimpleName() + " was expected.");

        FroggerMapFilePacketVertex newVertexChunk = (FroggerMapFilePacketVertex) newChunk;
        newVertexChunk.getVertices().clear();
        for (int i = 0; i < this.vertices.size(); i++)
            newVertexChunk.getVertices().add(this.vertices.get(i).clone());
    }

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getGraphicalPacket().getVertexPacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Vertex Count", this.vertices.size());
        return propertyList;
    }
}