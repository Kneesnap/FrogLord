package net.highwayfrogs.editor.games.sony.frogger.map.data.form;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a form data entry in a very early format, from roughly around April 1997.
 * Created by Kneesnap on 5/27/2024.
 */
@Setter
@Getter
public class FroggerOldMapFormData extends GameObject {
    private short xMin;
    private short yMin;
    private short zMin;
    private short xMax;
    private short yMax;
    private short zMax;
    private int reaction;
    private final int[] reactionData = new int[3];
    private final List<FroggerOldMapFormPlateau> heights = new ArrayList<>();

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
            FroggerOldMapFormPlateau newPlateau = new FroggerOldMapFormPlateau();
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

    @Getter
    @Setter
    public static class FroggerOldMapFormPlateau extends GameObject {
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
            reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.
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
            writer.writeNull(Constants.SHORT_SIZE); // Padding.
            writer.writeUnsignedShort(this.reaction);
            for (int i = 0; i < this.reactionData.length; i++)
                writer.writeUnsignedShort(this.reactionData[i]);
        }
    }
}