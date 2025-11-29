package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcBox4;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a triangle mesh, likely used for collision.
 * In the retail game, some of these are named, others are unnamed, despite having valid hashes.
 * The names can be found by taking the corresponding kcTriProxyDesc sections and chopping off "ProxyDesc".
 * Created by Kneesnap on 4/17/2024.
 */
@Getter
public class kcCResourceTriMesh extends kcCResource {
    private final kcCTriMesh triMesh;

    public static final String EXTENSION = "ctm";
    public static final String EXTENSION_SUFFIX = "." + EXTENSION;

    public kcCResourceTriMesh(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.TRIMESH);
        this.triMesh = new kcCTriMesh(getGameInstance());
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.triMesh.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        this.triMesh.save(writer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public GreatQuestHash<kcCResourceTriMesh> getSelfHash() {
        return (GreatQuestHash<kcCResourceTriMesh>) super.getSelfHash();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        this.triMesh.addToPropertyList(propertyList);
    }

    @Getter
    public static class kcCTriMesh extends GameData<GreatQuestInstance> implements IMultiLineInfoWriter, IPropertyListCreator {
        private final kcBox4 boundingBox = new kcBox4();
        private final List<kcVector4> vertices = new ArrayList<>();
        private final List<kcCFace> faces = new ArrayList<>();

        public kcCTriMesh(GreatQuestInstance gameInstance) {
            super(gameInstance);
        }

        @Override
        public void load(DataReader reader) {
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
            writer.writeIntAtPos(dataSizeAddress, writer.getIndex() - dataStartAddress);
        }

        @Override
        public void writeMultiLineInfo(StringBuilder builder, String padding) {
            String newPadding = padding + " ";
            this.boundingBox.writePrefixedMultiLineInfo(builder, "Bounding Box", padding, newPadding);
            builder.append(padding).append("Vertices (").append(this.vertices.size()).append("):").append(Constants.NEWLINE);
            for (int i = 0; i < this.vertices.size(); i++)
                this.vertices.get(i).writePrefixedInfoLine(builder, "Vertex", newPadding);

            builder.append(padding).append("Faces (").append(this.faces.size()).append("):").append(Constants.NEWLINE);
            for (int i = 0; i < this.faces.size(); i++)
                this.faces.get(i).writePrefixedInfoLine(builder, "", newPadding);
        }

        @Override
        public void addToPropertyList(PropertyListNode propertyList) {
            propertyList.add("Vertices", this.vertices.size());
            propertyList.add("Faces", this.faces.size());
        }
    }

    @Getter
    public static class kcCFace extends GameData<GreatQuestInstance> implements IInfoWriter {
        private final kcCTriMesh parentMesh;
        private final int[] vertices = new int[3];
        @Setter private int flags;
        private final kcVector4 normal;

        public static final int FLAG_DEBUG_USE_OFFSET_WHEN_DRAWING_WIREFRAME = Constants.BIT_FLAG_24; // 0x01000000 (????, Not Seen, kcCTriMesh::RenderWireframe)
        public static final int FLAG_SKIP_CAMERA_RAYCAST = Constants.BIT_FLAG_25; // 0x02000000 (Yellow, Rolling Rapids Creek)
        public static final int FLAG_CLIMBABLE = Constants.BIT_FLAG_31; //  0x80000000 (Light Brown, Rolling Rapids Creek) CONFIRMED, CFrogCtl::TestClimb.

        // The following flags are found in the PC data and PS2 Prototype. (Most likely also PS2 NTSC + PS2 PAL)
        // However, I checked pretty much the full binary and, I was unable to find any code which used these flags.
        // I also attempted to use a debugger to find any calls that Ghidra might have missed. No other code seemed to access the flags.
        // I suspect these flags meant something to the original editor. But, these faces don't have consistent enough usage patterns to definitively guess what they could have meant.
        private static final int FLAG_UNKNOWN_18 = Constants.BIT_FLAG_18; // 0x00040000 (Fairy Town Spring, Tree Door)
        private static final int FLAG_UNKNOWN_19 = Constants.BIT_FLAG_19; // 0x00080000 (Fairy Town Spring)
        private static final int FLAG_UNKNOWN_20 = Constants.BIT_FLAG_20; // 0x00100000 (River Town)
        private static final int FLAG_UNKNOWN_21 = Constants.BIT_FLAG_21; // 0x00200000 (River Town)
        private static final int FLAG_UNKNOWN_22 = Constants.BIT_FLAG_22; // 0x00400000 (Fairy Town, Buildings)
        private static final int FLAG_UNKNOWN_23 = Constants.BIT_FLAG_23; // 0x00800000 (River Town)
        private static final int FLAG_UNKNOWN_26 = Constants.BIT_FLAG_26; // 0x04000000 (Fairy Town Spring)
        private static final int FLAG_UNKNOWN_27 = Constants.BIT_FLAG_27; // 0x08000000 (Fairy Town Spring Water Bed)
        private static final int FLAG_UNKNOWN_28 = Constants.BIT_FLAG_28; // 0x10000000 (Fairy Town Spring Invisible Wall)
        private static final int FLAG_UNKNOWN_29 = Constants.BIT_FLAG_29; // 0x20000000 (Fairy Town Spring Buildings & Goblin Fort)
        private static final int FLAG_UNKNOWN_30 = Constants.BIT_FLAG_30; // 0x40000000 (Fairy Town Spring River Bed)


        public kcCFace(kcCTriMesh parentMesh) {
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

        @Override
        public void writeInfo(StringBuilder builder) {
            builder.append("kcCFace{flags=").append(NumberUtils.toHexString(this.flags))
                    .append(",vertices=").append(Arrays.toString(this.vertices))
                    .append(",normal=");

            this.normal.writeInfo(builder);
            builder.append("}");
        }
    }
}