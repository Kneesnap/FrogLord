package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceAnimSet;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceNamedHash;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceSkeleton;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the 'kcActorBaseDesc' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcActorBaseDesc extends kcEntity3DDesc {
    private int hThis;
    private int modelDescHash;
    private int hierarchyHash;
    private int numChan;
    private int animSetHash;
    private int proxyDescHash;
    private int animationHash;
    private final int[] padActorBase = new int[4];

    public kcActorBaseDesc(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.ACTOR_BASE.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.hThis = reader.readInt();
        this.modelDescHash = reader.readInt();
        this.hierarchyHash = reader.readInt();
        this.numChan = reader.readInt();
        this.animSetHash = reader.readInt();
        this.proxyDescHash = reader.readInt();
        this.animationHash = reader.readInt();
        for (int i = 0; i < this.padActorBase.length; i++)
            this.padActorBase[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.hThis);
        writer.writeInt(this.modelDescHash);
        writer.writeInt(this.hierarchyHash);
        writer.writeInt(this.numChan);
        writer.writeInt(this.animSetHash);
        writer.writeInt(this.proxyDescHash);
        writer.writeInt(this.animationHash);
        for (int i = 0; i < this.padActorBase.length; i++)
            writer.writeInt(this.padActorBase[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Hash: ").append(Utils.to0PrefixedHexString(this.hThis)).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Model", this.modelDescHash);
        writeAssetLine(builder, padding, "Animation Hierarchy", this.hierarchyHash);
        builder.append(padding).append("NumChan: ").append(this.numChan).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Anim Set", this.animSetHash);
        writeAssetLine(builder, padding, "Collision Proxy", this.proxyDescHash);
        writeAssetLine(builder, padding, "Animation", this.animationHash); // TODO: It may be desired to look at the anim set to map this hash if it's unmapped.
    }

    /**
     * Search for the model description referenced in this description.
     * @param parentFile the file to start the search from
     * @return modelDesc
     */
    public kcModelDesc getModelDesc(GreatQuestChunkedFile parentFile) {
        return GreatQuestUtils.findGenericResourceByHash(parentFile, getGameInstance(), this.modelDescHash, kcCResourceGeneric::getAsModelDescription);
    }

    /**
     * Search for the animation hierarchy referenced in this description.
     * @param parentFile the file to start the search from
     * @return modelDesc
     */
    public kcCResourceSkeleton getAnimationHierarchy(GreatQuestChunkedFile parentFile) {
        return GreatQuestUtils.findResourceByHash(parentFile, getGameInstance(), this.hierarchyHash);
    }

    /**
     * Search for the animation set referenced in this description.
     * @param parentFile the file to start the search from
     * @return animSet
     */
    public kcCResourceAnimSet getAnimationSet(GreatQuestChunkedFile parentFile) {
        return GreatQuestUtils.findResourceByHash(parentFile, getGameInstance(), this.animSetHash);
    }

    /**
     * Search for the active animation referenced in this description.
     * @param parentFile the file to start the search from
     * @return animation
     */
    public kcCResourceNamedHash getAnimation(GreatQuestChunkedFile parentFile) {
        return GreatQuestUtils.findResourceByHash(parentFile, getGameInstance(), this.animationHash);
    }

    /**
     * Search for the collision proxy description referenced in this description.
     * @param parentFile the file to start the search from
     * @return animation
     */
    public kcProxyDesc getCollisionProxyDescription(GreatQuestChunkedFile parentFile) {
        return GreatQuestUtils.findGenericResourceByHash(parentFile, getGameInstance(), this.proxyDescHash, kcCResourceGeneric::getAsProxyDescription);
    }
}