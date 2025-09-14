package net.highwayfrogs.editor.games.konami.greatquest.model;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceModel;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcIGenericResourceData;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the kcModelDesc struct.
 * Loaded by kcCModel::Init
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
public class kcModelDesc extends kcBaseDesc implements IInfoWriter, kcIGenericResourceData {
    private final GreatQuestHash<kcCResourceGeneric> parentHash; // The hash of this object's parent. Unused.
    private final GreatQuestHash<kcCResourceModel> modelRef; // Resolved by kcCModel::Init()

    private static final int EXPECTED_MATERIAL_HASH = -1; // Never used.

    public kcModelDesc(@NonNull kcCResourceGeneric resource) {
        super(resource);
        this.parentHash = new GreatQuestHash<>(resource);
        this.modelRef = new GreatQuestHash<>();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int hThis = reader.readInt();
        int modelHash = reader.readInt();
        int materialHash = reader.readInt();

        // Resolve the model.
        GreatQuestUtils.resolveLevelResourceHash(kcCResourceModel.class, this, this.modelRef, modelHash, false); // There are quite a few models which have been removed but still have their model descriptions.

        // Warn if things look wrong.
        if (materialHash != EXPECTED_MATERIAL_HASH)
            getLogger().warning("Expected material hash to be %d, but was actually %08X.", EXPECTED_MATERIAL_HASH, materialHash);
        if (hThis != this.parentHash.getHashNumber())
            throw new RuntimeException("The kcModelDesc reported the parent chunk as " + NumberUtils.to0PrefixedHexString(hThis) + ", but it was expected to be " + this.parentHash.getHashNumberAsString() + ".");
    }

    @Override
    public void saveData(DataWriter writer) {
        writer.writeInt(this.parentHash.getHashNumber());
        writer.writeInt(this.modelRef.getHashNumber());
        writer.writeInt(EXPECTED_MATERIAL_HASH);
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
    public kcCResourceGeneric getResource() {
        return (kcCResourceGeneric) super.getResource();
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.MODEL_DESCRIPTION;
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        writeAssetLine(builder, ", ", "Model", this.modelRef, kcCResourceModel::getFileName);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        writeAssetLine(builder, padding, "Model", this.modelRef);
    }

    /**
     * Gets the referenced model.
     */
    public kcCResourceModel getModel() {
        return this.modelRef.getResource();
    }
}