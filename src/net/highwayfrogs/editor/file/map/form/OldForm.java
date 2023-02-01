package net.highwayfrogs.editor.file.map.form;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the form data seen roughly around April 1997.
 * Created by Kneesnap on 2/1/2023.
 */
@Getter
public class OldForm extends GameObject {
    private final MAPFile mapFile;
    private int entityTypeId;
    private int mofId;
    private final List<OldFormData> formData = new ArrayList<>();

    public OldForm(MAPFile file) {
        this.mapFile = file;
    }

    @Override
    public void load(DataReader reader) {
        this.entityTypeId = reader.readUnsignedShortAsInt();
        this.mofId = reader.readUnsignedShortAsInt();

        int formCount = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Padding.

        // Read form data.
        for (int i = 0; i < formCount; i++) {
            reader.jumpTemp(reader.readInt());
            OldFormData newData = new OldFormData();
            newData.load(reader);
            reader.jumpReturn();
            this.formData.add(newData);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.entityTypeId);
        writer.writeUnsignedShort(this.mofId);
        writer.writeUnsignedShort(this.formData.size());
        writer.writeShort((short) 0); // Padding.

        throw new UnsupportedOperationException("Not implemented.");
    }

    @Getter
    @Setter
    public static class OldFormData extends GameObject {
        private short xMin;
        private short yMin;
        private short zMin;
        private short xMax;
        private short yMax;
        private short zMax;
        private int reaction;
        private final int[] reactionData = new int[3];
        private final List<OldFormPlateau> heights = new ArrayList<>();

        @Override
        public void load(DataReader reader) {
            this.xMin = reader.readShort();
            this.yMin = reader.readShort();
            this.zMin = reader.readShort();
            this.xMax = reader.readShort();
            this.yMax = reader.readShort();
            this.zMax = reader.readShort();
            this.reaction = reader.readUnsignedShortAsInt();
            for (int i = 0; i < this.reactionData.length; i++)
                this.reactionData[i] = reader.readUnsignedShortAsInt();

            int numberOfHeights = reader.readInt();
            for (int i = 0; i < numberOfHeights; i++) {
                OldFormPlateau newPlateau = new OldFormPlateau();
                newPlateau.load(reader);
                this.heights.add(newPlateau);
            }
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.xMin);
            writer.writeShort(this.yMin);
            writer.writeShort(this.zMin);
            writer.writeShort(this.xMax);
            writer.writeShort(this.yMax);
            writer.writeShort(this.zMax);
            writer.writeUnsignedShort(this.reaction);
            for (int i = 0; i < this.reactionData.length; i++)
                writer.writeUnsignedShort(this.reactionData[i]);

            writer.writeInt(this.heights.size());
            for (int i = 0; i < this.heights.size(); i++)
                this.heights.get(i).save(writer);
        }
    }

    @Getter
    @Setter
    public static class OldFormPlateau extends GameObject {
        private short xMin;
        private short zMin;
        private short xMax;
        private short zMax;
        private short y;
        private int reaction;
        private final int[] reactionData = new int[3];

        @Override
        public void load(DataReader reader) {
            this.xMin = reader.readShort();
            this.zMin = reader.readShort();
            this.xMax = reader.readShort();
            this.zMax = reader.readShort();
            this.y = reader.readShort();
            reader.skipShort(); // Padding.
            this.reaction = reader.readUnsignedShortAsInt();
            for (int i = 0; i < this.reactionData.length; i++)
                this.reactionData[i] = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.xMin);
            writer.writeShort(this.zMin);
            writer.writeShort(this.xMax);
            writer.writeShort(this.zMax);
            writer.writeShort(this.y);
            writer.writeShort((short) 0); // Padding.
            writer.writeUnsignedShort(this.reaction);
            for (int i = 0; i < this.reactionData.length; i++)
                writer.writeUnsignedShort(this.reactionData[i]);
        }
    }
}
