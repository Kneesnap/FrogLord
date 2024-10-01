package net.highwayfrogs.editor.games.konami.greatquest.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceModel;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the kcModelDesc struct.
 * Loaded by kcCModel::Init
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcModelDesc extends kcBaseDesc implements IInfoWriter {
    private final GreatQuestHash<kcCResourceGeneric> parentHash; // The hash of this object's parent.
    private final GreatQuestHash<kcCResourceModel> model;

    private static final int EXPECTED_MATERIAL_HASH = -1; // Never used.

    public kcModelDesc(@NonNull kcCResourceGeneric resource) {
        super(resource);
        this.parentHash = new GreatQuestHash<>(resource);
        this.model = new GreatQuestHash<>();
    }

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
        int hThis = reader.readInt();
        int modelHash = reader.readInt();
        int materialHash = reader.readInt();

        // Resolve the model.
        GreatQuestUtils.resolveResourceHash(kcCResourceModel.class, this, this.model, modelHash, false); // There are quite a few models which have been removed but still have their model descriptions.

        // Warn if things look wrong.
        if (materialHash != EXPECTED_MATERIAL_HASH)
            getLogger().warning("Expected material hash to be " + EXPECTED_MATERIAL_HASH + ", but was actually " + Utils.to0PrefixedHexString(materialHash));
        if (hThis != this.parentHash.getHashNumber())
            throw new RuntimeException("The kcModelDesc reported the parent chunk as " + Utils.to0PrefixedHexString(hThis) + ", but it was expected to be " + this.parentHash.getHashNumberAsString() + ".");
    }

    @Override
    public void saveData(DataWriter writer) {
        writer.writeInt(this.parentHash.getHashNumber());
        writer.writeInt(this.model.getHashNumber());
        writer.writeInt(EXPECTED_MATERIAL_HASH);
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        writeAssetInfo(builder, ", ", "Model", this.model.getHashNumber(), kcCResourceModel::getFileName);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        writeAssetLine(builder, padding, "Model", this.model);
    }
}