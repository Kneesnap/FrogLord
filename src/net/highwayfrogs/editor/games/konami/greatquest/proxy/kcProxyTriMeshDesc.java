package net.highwayfrogs.editor.games.konami.greatquest.proxy;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceTriMesh;

/**
 * This is not an official name, no class/struct seemed to exist here, but it still is likely necessary.
 * This represents a collision proxy that uses a mesh.
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public class kcProxyTriMeshDesc extends kcProxyDesc {
    private int meshHash; // Hash to identify the '.CTM' collision mesh.

    public kcProxyTriMeshDesc(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.PROXY_TRI_MESH.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.meshHash = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.meshHash);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        writeAssetLine(builder, padding, "Collision Mesh", this.meshHash);
    }

    /**
     * Resolve the referenced tri mesh.
     * @param chunkedFile the chunked file to start looking in
     * @return triMesh, or null if not found
     */
    public kcCResourceTriMesh getTriMesh(GreatQuestChunkedFile chunkedFile) {
        return GreatQuestUtils.findResourceByHash(chunkedFile, getGameInstance(), this.meshHash);
    }
}