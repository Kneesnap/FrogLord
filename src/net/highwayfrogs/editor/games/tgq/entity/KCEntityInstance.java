package net.highwayfrogs.editor.games.tgq.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.toc.TGQChunk3DModel;

/**
 * Represents an entity instance.
 * Created by Kneesnap on 1/4/2021.
 */
public abstract class KCEntityInstance extends GameObject {
    @Getter @Setter private int size = 0xF0; // Useless on PC.
    @Getter @Setter private TGQChunk3DModel model;
    private int pDesc; // This seems to be a pointer left-over from when the file was built. It gets overwritten on load in the game.
    @Getter @Setter private int priority;
    @Getter @Setter private int group;
    @Getter @Setter private int scriptIndex;
    @Getter @Setter
    private int hTarget; // TODO: This looks like a hash? Could it be a link to another section, maybe the general section?

    @Override
    public void load(DataReader reader) {
        this.size = reader.readInt(); // Useless on PC.

        int modelHash = reader.readInt(); // TODO: Get model.

        this.pDesc = reader.readInt();
        this.priority = reader.readInt();
        this.group = reader.readInt();
        this.scriptIndex = reader.readInt();
        this.hTarget = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.size);
        // TODO: Model hash.
        writer.writeInt(this.pDesc);
        writer.writeInt(this.priority);
        writer.writeInt(this.group);
        writer.writeInt(this.scriptIndex);
        writer.writeInt(this.hTarget);
    }
}
