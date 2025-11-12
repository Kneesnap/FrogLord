package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.games.renderware.IRwStreamChunkWithEmbeddedStruct;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.struct.types.RwV3d;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the rwStreamMatrix as defined in babinmtx.h
 * Created by Kneesnap on 8/25/2024.
 */
@Getter
public class RwStreamMatrixChunk extends RwStreamChunk implements IRwStreamChunkWithEmbeddedStruct {
    private final RwV3d right;
    private final RwV3d up;
    private final RwV3d at;
    private final RwV3d pos;
    private int type;

    public RwStreamMatrixChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.MATRIX, version, parentChunk);
        this.right = new RwV3d(streamFile.getGameInstance());
        this.up = new RwV3d(streamFile.getGameInstance());
        this.at = new RwV3d(streamFile.getGameInstance());
        this.pos = new RwV3d(streamFile.getGameInstance());
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        readEmbeddedStruct(reader);
    }

    @Override
    public void loadEmbeddedStructData(DataReader reader, int version, int dataLength) {
        this.right.load(reader, version, dataLength);
        this.up.load(reader, version, dataLength);
        this.at.load(reader, version, dataLength);
        this.pos.load(reader, version, dataLength);
        this.type = reader.readInt();
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeEmbeddedStruct(writer);
    }

    @Override
    public void saveEmbeddedStructData(DataWriter writer, int version) {
        this.right.save(writer, version);
        this.up.save(writer, version);
        this.at.save(writer, version);
        this.pos.save(writer, version);
        writer.writeInt(this.type);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("Type", this.type);
        propertyList.addProperties("Position", this.pos);
        propertyList.addProperties("At", this.at);
        propertyList.addProperties("Right", this.right);
        propertyList.addProperties("Up", this.up);
    }
}