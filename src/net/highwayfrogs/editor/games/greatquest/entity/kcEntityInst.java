package net.highwayfrogs.editor.games.greatquest.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.greatquest.TGQChunkedFile;
import net.highwayfrogs.editor.games.greatquest.toc.kcCResourceEntityInst;

/**
 * Represents the 'kcEntityInst' struct.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
@Setter
@NoArgsConstructor
public class kcEntityInst extends GameObject implements IMultiLineInfoWriter {
    private transient kcCResourceEntityInst resource;
    private int descriptionHash;
    private int priority;
    private int group;
    private int scriptIndex;
    private int targetEntityHash;

    public static final int SIZE_IN_BYTES = 28;

    public kcEntityInst(kcCResourceEntityInst resource) {
        this.resource = resource;
    }

    @Override
    public void load(DataReader reader) {
        reader.skipInt(); // Size in bytes.
        this.descriptionHash = reader.readInt();
        reader.skipInt(); // Runtime pointer to description.
        this.priority = reader.readInt();
        this.group = reader.readInt();
        this.scriptIndex = reader.readInt();
        this.targetEntityHash = reader.readInt();
    }

    @Override
    public final void save(DataWriter writer) {
        int sizePtr = writer.writeNullPointer();
        saveData(writer);
        writer.writeAddressAt(sizePtr, writer.getIndex() - sizePtr);
    }

    /**
     * Saves data to the writer.
     * @param writer The writer to save data to.
     */
    public void saveData(DataWriter writer) {
        writer.writeInt(this.descriptionHash);
        writer.writeInt(0); // Runtime pointer to description.
        writer.writeInt(this.priority);
        writer.writeInt(this.group);
        writer.writeInt(this.scriptIndex);
        writer.writeInt(this.targetEntityHash);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        TGQChunkedFile chunkedFile = this.resource != null ? this.resource.getParentFile() : null;

        TGQChunkedFile.writeAssetLine(chunkedFile, builder, padding, "Description", this.descriptionHash);
        builder.append(padding).append("Priority: ").append(this.priority).append(Constants.NEWLINE);
        builder.append(padding).append("Group: ").append(this.group).append(Constants.NEWLINE);
        builder.append(padding).append("Script Index: ").append(this.scriptIndex).append(Constants.NEWLINE);
        TGQChunkedFile.writeAssetLine(chunkedFile, builder, padding, "Target Entity", this.targetEntityHash);
    }
}