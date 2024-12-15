package net.highwayfrogs.editor.games.konami.greatquest.model;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a model node in the kcGameSystem.
 * This seems to be a group of primitives which render together. When iterating the model for rendering, the game will iterate this instead of going through the other list.
 * TODO: Should we start rendering models by this? Probably.
 * Created by Kneesnap on 6/22/2023.
 */
@Getter
public class kcModelNode extends GameData<GreatQuestInstance> {
    private int nodeId; // uint, bone tag
    private final List<kcModelPrim> primitives = new ArrayList<>();

    public kcModelNode(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.nodeId = reader.readInt();
        long primitiveCount = reader.readUnsignedIntAsLong();

        // Populate primitives list for reading.
        this.primitives.clear();
        for (long i = 0; i < primitiveCount; i++)
            this.primitives.add(null);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.nodeId);
        writer.writeUnsignedInt(this.primitives.size());
    }

    /**
     * Populates the primitive list.
     * @param primitives the primitives to build the local list from
     * @param startIndex the index to start grabbing primitives from
     * @return the first index after the last primitive part of the node
     */
    public int loadPrimsFromList(List<kcModelPrim> primitives, int startIndex) {
        if (startIndex + this.primitives.size() > primitives.size())
            throw new RuntimeException("kcModelNode[nodeId=" + this.nodeId + "] said it had " + this.primitives.size() + ", but this bled outside the range of valid primitives!");

        // Copy into list.
        for (int i = 0; i < this.primitives.size(); i++) {
            kcModelPrim targetPrim = primitives.get(startIndex + i);
            this.primitives.set(i, targetPrim);
            targetPrim.setParentNode(this, true);
        }

        return startIndex + this.primitives.size();
    }

    @Override
    public String toString() {
        return "kcModelPrim{boneId=" + this.nodeId + ",primitives=" + this.primitives.size() + "}";
    }
}