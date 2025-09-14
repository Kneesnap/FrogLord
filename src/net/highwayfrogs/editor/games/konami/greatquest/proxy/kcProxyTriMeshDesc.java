package net.highwayfrogs.editor.games.konami.greatquest.proxy;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTriMesh;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.lambda.Consumer5;

import java.util.Arrays;
import java.util.List;

/**
 * This is not an official name, no class/struct seemed to exist here, but it still is likely necessary.
 * This represents a collision proxy that uses a mesh.
 * Created by Kneesnap on 8/22/2023.
 */
public class kcProxyTriMeshDesc extends kcProxyDesc {
    @Getter private final GreatQuestHash<kcCResourceTriMesh> meshRef; // '.CTM' collision mesh. This is handled by kcCActorBase::CreateCollisionProxy()
    private final Consumer5<GreatQuestHash<kcCResourceTriMesh>, String, String, Integer, Integer> nameChangeListener = this::onMeshNameChange;

    public static final String NAME_SUFFIX = "ProxyDesc"; // This is applied to all kcProxyTriMeshDescs.
    private static final List<String> RECOGNIZED_INVALID_NAMES = Arrays.asList("Fairy Key A", "Fairy Key B", "Fairy Key C", "clover-2");

    public kcProxyTriMeshDesc(@NonNull kcCResourceGeneric resource) {
        super(resource, kcProxyDescType.TRIMESH);
        this.meshRef = new GreatQuestHash<>();
        this.meshRef.getResourceChangeListeners().add(this::onMeshChange);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.PROXY_TRI_MESH.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int meshHash = reader.readInt();

        // If we resolve the tri mesh successfully, our goal is to generate the collision mesh name.
        if (GreatQuestUtils.resolveLevelResourceHash(kcCResourceTriMesh.class, this, this.meshRef, meshHash, true)) {
            String modelName = getParentHash().getOriginalString();
            if (!modelName.endsWith(NAME_SUFFIX))
                throw new IllegalStateException("The kcProxyTriMeshDesc name was '" + modelName + "', but it was expected to end with '" + NAME_SUFFIX + "'.");

            modelName = modelName.substring(0, modelName.length() - NAME_SUFFIX.length());

            // The name of the original is based on this.
            int newHash = GreatQuestUtils.hash(modelName);
            if (newHash == this.meshRef.getResource().getHash()) {
                this.meshRef.getResource().getSelfHash().setOriginalString(modelName);
            } else if (!RECOGNIZED_INVALID_NAMES.contains(modelName)) { // This has been observed to happen for a few things in the release game.
                getLogger().warning("Couldn't apply name '%s' (%08X) to kcCResourceTriMesh %s.", modelName, newHash, this.meshRef.getResource().getHashAsHexString());
            }
        }
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.meshRef.getHashNumber());
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.PROXY_TRI_MESH_DESCRIPTION;
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        writeAssetLine(builder, padding, "Collision Mesh", this.meshRef);
    }

    private static final String CONFIG_KEY_COLLISION = "collisionMesh";

    @Override
    public void fromConfig(Config input) {
        super.fromConfig(input);
        int meshHash = GreatQuestUtils.getAsHash(input.getKeyValueNodeOrError(CONFIG_KEY_COLLISION), -1, this.meshRef);
        GreatQuestUtils.resolveLevelResourceHash(kcCResourceTriMesh.class, getParentFile(), this, this.meshRef, meshHash, true);
    }

    @Override
    public void toConfig(Config output) {
        super.toConfig(output);
        output.getOrCreateKeyValueNode(CONFIG_KEY_COLLISION).setAsString(this.meshRef.getAsString());
    }

    @Override
    public boolean isStatic() {
        // This feature is technically implemented (kcCProxyTriMesh::Intersect will skip the sphere check),
        // but it is unclear when this would ever be desired.
        // The game never uses it either, so this keeps it disabled.
        return false;
    }

    // When the reference to kcCResourceTriMesh changes, update the name listeners.
    private void onMeshChange(GreatQuestHash<kcCResourceTriMesh> hashObj, kcCResourceTriMesh oldMesh, kcCResourceTriMesh newMesh, int newHash, String newName) {
        if (oldMesh != null)
            oldMesh.getSelfHash().getStringChangeListeners().remove(this.nameChangeListener);
        if (newMesh != null) {
            // Add a name change listener.
            newMesh.getSelfHash().getStringChangeListeners().add(this.nameChangeListener);

            // Change the name of this section to match the newly applied mesh.
            if (getResource().isHashBasedOnName() && newMesh.isHashBasedOnName() && newName != null)
                getResource().setName(newName + NAME_SUFFIX);
        }
    }

    // When the name of the linked kcCResourceTriMesh changes, update the name of this too.
    private void onMeshNameChange(GreatQuestHash<kcCResourceTriMesh> hashObj, String oldName, String newName, int oldHash, int newHash) {
        if (getResource().isHashBasedOnName() && hashObj.getResource().isHashBasedOnName() && newName != null) {
            getResource().setName(newName + NAME_SUFFIX);
        } else if (getResource().getHash() == oldHash) {
            getResource().getSelfHash().setHash(newHash, newName, false);
        }
    }
}