package net.highwayfrogs.editor.games.konami.greatquest.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcBox4;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a triangle mesh, likely used for collision.
 * Created by Kneesnap on 4/17/2024.
 */
@Getter
public class kcCResourceTriMesh extends kcCResource {
    private final kcBox4 boundingBox;
    private final List<kcVector4> vertices = new ArrayList<>();
    private final List<kcCFace> faces = new ArrayList<>();

    public kcCResourceTriMesh(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.TRIMESH);
        this.boundingBox = new kcBox4();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        reader.skipInt(); // Skip the size.
        this.boundingBox.load(reader);
        int vertexCount = reader.readInt();
        reader.skipPointer(); // Runtime pointer.
        int faceCount = reader.readInt();
        reader.skipPointer(); // Runtime pointer.

        // Read faces.
        this.faces.clear();
        for (int i = 0; i < faceCount; i++) {
            kcCFace newFace = new kcCFace(this);
            newFace.load(reader);
            this.faces.add(newFace);
        }

        // Read vertices.
        this.vertices.clear();
        for (int i = 0; i < vertexCount; i++) {
            kcVector4 newVertex = new kcVector4();
            newVertex.load(reader);
            this.vertices.add(newVertex);
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        int dataStartAddress = writer.getIndex();
        int dataSizeAddress = writer.writeNullPointer();
        this.boundingBox.save(writer);
        writer.writeInt(this.vertices.size());
        writer.writeNullPointer(); // Runtime pointer.
        writer.writeInt(this.faces.size());
        writer.writeNullPointer(); // Runtime pointer.

        // Write faces.
        for (int i = 0; i < this.faces.size(); i++)
            this.faces.get(i).save(writer);

        // Write vertices.
        for (int i = 0; i < this.vertices.size(); i++)
            this.vertices.get(i).save(writer);

        // Ensure we get the size right.
        writer.writeAddressAt(dataSizeAddress, writer.getIndex() - dataStartAddress);
    }

    @Getter
    public static class kcCFace extends GameData<GreatQuestInstance> {
        private final kcCResourceTriMesh parentMesh;
        private final int[] vertices = new int[3];
        private int flags;
        private final kcVector4 normal;

        public kcCFace(kcCResourceTriMesh parentMesh) {
            super(parentMesh.getGameInstance());
            this.parentMesh = parentMesh;
            this.normal = new kcVector4();
        }

        @Override
        public void load(DataReader reader) {
            for (int i = 0; i < this.vertices.length; i++)
                this.vertices[i] = reader.readInt();
            this.flags = reader.readInt();
            this.normal.load(reader);
        }

        @Override
        public void save(DataWriter writer) {
            for (int i = 0; i < this.vertices.length; i++)
                writer.writeInt(this.vertices[i]);
            writer.writeInt(this.flags);
            this.normal.save(writer);
        }
    }
}