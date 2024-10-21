package net.highwayfrogs.editor.games.konami.greatquest.proxy;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTriMesh;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;
import java.util.List;

/**
 * This is not an official name, no class/struct seemed to exist here, but it still is likely necessary.
 * This represents a collision proxy that uses a mesh.
 * TODO: Do not allow manual name control of this generic section.
 * TODO: When the name of the kcCResourceTriMesh changes, update our own name here. (Also, this should apply when we switch the selected kcCResourceTriMesh)
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
public class kcProxyTriMeshDesc extends kcProxyDesc {
    private final GreatQuestHash<kcCResourceTriMesh> meshRef; // '.CTM' collision mesh. This is handled by kcCActorBase::CreateCollisionProxy()

    public static final String NAME_SUFFIX = "ProxyDesc"; // This is applied to all kcProxyTriMeshDescs.
    private static final List<String> RECOGNIZED_INVALID_NAMES = Arrays.asList("Fairy Key A", "Fairy Key B", "Fairy Key C", "clover-2");

    public kcProxyTriMeshDesc(@NonNull kcCResourceGeneric resource) {
        super(resource);
        this.meshRef = new GreatQuestHash<>();
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.PROXY_TRI_MESH.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int meshHash = reader.readInt();
        if (GreatQuestUtils.resolveResourceHash(kcCResourceTriMesh.class, this, this.meshRef, meshHash, true)) {
            String modelName = getParentHash().getOriginalString();
            if (!modelName.endsWith(NAME_SUFFIX))
                throw new IllegalStateException("The kcProxyTriMeshDesc name was '" + modelName + "', but it was expected to end with '" + NAME_SUFFIX + "'.");

            modelName = modelName.substring(0, modelName.length() - NAME_SUFFIX.length());

            // The name of the original is based on this.
            int newHash = GreatQuestUtils.hash(modelName);
            if (newHash == this.meshRef.getResource().getHash()) {
                this.meshRef.getResource().getSelfHash().setOriginalString(modelName);
            } else if (!RECOGNIZED_INVALID_NAMES.contains(modelName)) { // This has been observed to happen for a few things in the release game.
                getLogger().warning("Couldn't apply name '" + modelName + "' (" + Utils.to0PrefixedHexString(newHash) + ") to kcCResourceTriMesh " + this.meshRef.getResource().getHashAsHexString() + ".");
            }
        }
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.meshRef.getHashNumber());
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        writeAssetLine(builder, padding, "Collision Mesh", this.meshRef);
    }
}