package net.highwayfrogs.editor.games.konami.greatquest.entity;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Contains animation set data.
 * Created by Kneesnap on 4/16/2024.
 */
public class kcAnimSetDesc extends kcBaseDesc {
    private int selfHash;
    private int[] hashes = EMPTY_HASHES;

    private static final int[] EMPTY_HASHES = new int[1];

    public kcAnimSetDesc(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.selfHash = reader.readInt();
        int handleCount = reader.readInt();
        this.hashes = new int[handleCount + 1];
        for (int i = 0; i < this.hashes.length; i++)
            this.hashes[i] = reader.readInt();
    }

    @Override
    protected void saveData(DataWriter writer) {
        writer.writeInt(this.selfHash);
        writer.writeInt(this.hashes.length - 1);
        for (int i = 0; i < this.hashes.length; i++)
            writer.writeInt(this.hashes[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Self Hash[").append(Utils.to0PrefixedHexString(this.selfHash)).append("]:").append(Constants.NEWLINE);

        for (int i = 0; i < this.hashes.length; i++) {
            builder.append(padding).append(" - ");
            writeAssetLine(builder, padding, "kcAnimSetDesc Entry", this.hashes[i]);
        }

        builder.append(Constants.NEWLINE);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.ANIM_SET.getClassId();
    }
}