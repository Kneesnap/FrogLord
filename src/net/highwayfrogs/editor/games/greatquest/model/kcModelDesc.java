package net.highwayfrogs.editor.games.greatquest.model;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.IInfoWriter;
import net.highwayfrogs.editor.games.greatquest.entity.kcBaseDesc;
import net.highwayfrogs.editor.games.greatquest.kcClassID;
import net.highwayfrogs.editor.games.greatquest.toc.kcCResourceModel;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the kcModelDesc struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcModelDesc extends kcBaseDesc implements IInfoWriter {
    private int hashOfChunkContainingThis; // The hash of the chunk containing this object.
    private int modelChunkHash; // The hash of the chunk which is a reference to a model file.
    private int materialHash = -1; // Seems to be -1.

    @Override
    public int getTargetClassID() {
        return kcClassID.MODEL.getClassId();
    }

    @Override
    public boolean allowAlternativeClassID() {
        return true;
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.hashOfChunkContainingThis = reader.readInt();
        this.modelChunkHash = reader.readInt();
        this.materialHash = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        writer.writeInt(this.hashOfChunkContainingThis);
        writer.writeInt(this.modelChunkHash);
        writer.writeInt(this.materialHash);
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        builder.append("Hash: ").append(Utils.to0PrefixedHexString(this.hashOfChunkContainingThis));
        writeAssetInfo(builder, ", ", "Model", this.modelChunkHash, kcCResourceModel::getFileName);
        writeAssetInfo(builder, ", ", "Material", this.materialHash, kcCResourceModel::getName);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Hash: ").append(Utils.to0PrefixedHexString(this.hashOfChunkContainingThis)).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Model", this.modelChunkHash);
        writeAssetLine(builder, padding, "Material", this.modelChunkHash);
    }
}