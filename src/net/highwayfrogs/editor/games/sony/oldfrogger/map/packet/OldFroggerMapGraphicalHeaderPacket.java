package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * The graphical map header contains pointers to graphical packets.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerMapGraphicalHeaderPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "GRMP";
    private int standardChunkAddress = -1;
    private int lightChunkAddress = -1;
    private int gridChunkAddress = -1;
    private int quadChunkAddress = -1;
    private int vertexChunkAddress = -1;
    private int animChunkAddress = -1;
    private int cameraHeightFieldChunkAddress = -1;

    public OldFroggerMapGraphicalHeaderPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    public void clearReadWriteData() {
        super.clearReadWriteData();
        clear();
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.standardChunkAddress = reader.readInt();
        this.lightChunkAddress = reader.readInt();
        this.gridChunkAddress = reader.readInt();
        this.quadChunkAddress = reader.readInt();
        this.vertexChunkAddress = reader.readInt();
        this.animChunkAddress = reader.readInt();
        this.cameraHeightFieldChunkAddress = reader.readInt();
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(0); // standardChunkAddress
        writer.writeInt(0); // lightChunkAddress
        writer.writeInt(0); // gridChunkAddress
        writer.writeInt(0); // quadChunkAddress
        writer.writeInt(0); // vertexChunkAddress
        writer.writeInt(0); // animChunkAddress
        if (getParentFile().getCameraHeightFieldPacket() != null && getParentFile().getCameraHeightFieldPacket().isActive())
            writer.writeInt(0); // cameraHeightFieldChunkAddress
    }

    @Override
    protected void saveBodySecondPass(DataWriter writer, long fileSizeInBytes) {
        writer.writeInt(getParentFile().getStandardPacket().getLastValidWriteHeaderAddress());
        writer.writeInt(getParentFile().getLightPacket().getLastValidWriteHeaderAddress());
        writer.writeInt(getParentFile().getGridPacket().getLastValidWriteHeaderAddress());
        writer.writeInt(getParentFile().getQuadPacket().getLastValidWriteHeaderAddress());
        writer.writeInt(getParentFile().getVertexPacket().getLastValidWriteHeaderAddress());
        writer.writeInt(getParentFile().getAnimPacket().getLastValidWriteHeaderAddress());
        if (getParentFile().getCameraHeightFieldPacket() != null && getParentFile().getCameraHeightFieldPacket().isActive())
            writer.writeInt(getParentFile().getCameraHeightFieldPacket().getLastValidWriteHeaderAddress());
    }

    @Override
    public void clear() {
        this.standardChunkAddress = -1;
        this.lightChunkAddress = -1;
        this.gridChunkAddress = -1;
        this.quadChunkAddress = -1;
        this.vertexChunkAddress = -1;
        this.animChunkAddress = -1;
        this.cameraHeightFieldChunkAddress = -1;
    }
}