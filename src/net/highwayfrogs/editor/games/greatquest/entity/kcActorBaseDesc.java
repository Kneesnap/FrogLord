package net.highwayfrogs.editor.games.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.kcClassID;
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
    private int hier;
    private int numChan;
    private int animSetHash;
    private int proxyDescHash;
    private int animHash;
    private final int[] padActorBase = new int[4];

    @Override
    protected int getTargetClassID() {
        return kcClassID.ACTOR_BASE.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.hThis = reader.readInt();
        this.modelDescHash = reader.readInt();
        this.hier = reader.readInt();
        this.numChan = reader.readInt();
        this.animSetHash = reader.readInt();
        this.proxyDescHash = reader.readInt();
        this.animHash = reader.readInt();
        for (int i = 0; i < this.padActorBase.length; i++)
            this.padActorBase[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.hThis);
        writer.writeInt(this.modelDescHash);
        writer.writeInt(this.hier);
        writer.writeInt(this.numChan);
        writer.writeInt(this.animSetHash);
        writer.writeInt(this.proxyDescHash);
        writer.writeInt(this.animHash);
        for (int i = 0; i < this.padActorBase.length; i++)
            writer.writeInt(this.padActorBase[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Hash: ").append(Utils.to0PrefixedHexString(this.hThis)).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Model", this.modelDescHash);
        builder.append(padding).append("Hier: ").append(this.hier).append(Constants.NEWLINE);
        builder.append(padding).append("NumChan: ").append(this.numChan).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Anim Set", this.animSetHash);
        writeAssetLine(builder, padding, "Collision Proxy", this.proxyDescHash);
        writeAssetLine(builder, padding, "Animation", this.animHash); // TODO: It may be desired to look at the anim set to map this hash if it's unmapped.
    }
}