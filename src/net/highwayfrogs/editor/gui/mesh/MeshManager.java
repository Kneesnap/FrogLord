package net.highwayfrogs.editor.gui.mesh;

import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.FrogMesh;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages mesh data.
 * TODO: Appears to be used by old FrogLord systems. Let's toss this later.
 * Created by Kneesnap on 1/21/2019.
 */
@Getter
public class MeshManager {
    private final FrogMesh<?> mesh;
    private final List<MeshData> meshData = new ArrayList<>();

    public MeshManager(FrogMesh<?> mesh) {
        this.mesh = mesh;
    }

    /**
     * Remove mesh data.
     * @param data The data to remove.
     */
    public void removeMesh(MeshData data) {
        int removeFaceIndex = getFaceIndex(data);
        int removeTexIndex = getTexIndex(data);
        Utils.verify(meshData.remove(data), "Failed to remove MeshData.");

        ObservableIntegerArray faces = getMesh().getFaces();
        int newFaceSize = faces.size() - data.getFaceCount();

        int textureIdDecrease = (data.getTexCoordCount() / getMesh().getTexCoordElementSize());
        for (int i = removeFaceIndex; i < newFaceSize; i++) {
            int value = faces.get(i + data.getFaceCount());
            if ((i - removeFaceIndex) % 2 > 0) // If it's a texture entry, since we just removed some texture entries, we need to decrease the texture ids in the faces.
                value -= textureIdDecrease;

            faces.set(i, value);
        }
        faces.resize(newFaceSize);

        ObservableFloatArray coords = getMesh().getTexCoords();
        int newTexSize = coords.size() - data.getTexCoordCount();
        for (int i = removeTexIndex; i < newTexSize; i++)
            coords.set(i, coords.get(i + data.getTexCoordCount()));
        coords.resize(newTexSize);
    }

    /**
     * Record recent changes as mesh changes.
     */
    public MeshData addMesh() {
        MeshData lastData = getMeshData().isEmpty() ? null : getMeshData().get(getMeshData().size() - 1);

        int texIndex = lastData != null ? (getTexIndex(lastData) + lastData.getTexCoordCount()) : getMesh().getTextureCount();
        int faceIndex = lastData != null ? (getFaceIndex(lastData) + lastData.getFaceCount()) : getMesh().getFaceCount();
        MeshData newData = new MeshData(getMesh().getFaces().size() - faceIndex, getMesh().getTexCoords().size() - texIndex);
        getMeshData().add(newData);
        return newData;
    }

    private int getFaceIndex(MeshData data) {
        int faceIndex = getMesh().getFaceCount();

        for (MeshData tempData : getMeshData()) {
            if (tempData == data)
                return faceIndex;
            faceIndex += tempData.getFaceCount();
        }

        throw new RuntimeException("MeshData is not registered in manager!");
    }

    private int getTexIndex(MeshData data) {
        int texIndex = getMesh().getTextureCount();

        for (MeshData tempData : getMeshData()) {
            if (tempData == data)
                return texIndex;
            texIndex += tempData.getTexCoordCount();
        }

        throw new RuntimeException("MeshData is not registered in manager!");
    }
}