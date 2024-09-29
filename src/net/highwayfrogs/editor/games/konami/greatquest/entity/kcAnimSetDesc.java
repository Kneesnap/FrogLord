package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceAnimSet;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceTrack;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains animation set data.
 * Created by Kneesnap on 4/16/2024.
 */
@Getter
public class kcAnimSetDesc extends kcBaseDesc {
    private final GreatQuestHash<kcCResourceAnimSet> parentHash;
    private final List<GreatQuestHash<kcCResourceTrack>> animationRefs = new ArrayList<>();

    public kcAnimSetDesc(@NonNull kcCResourceAnimSet resource) {
        super(resource);
        this.parentHash = new GreatQuestHash<>(resource);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int selfHash = reader.readInt();
        if (selfHash != this.parentHash.getHashNumber())
            throw new RuntimeException("The kcAnimSetDesc reported the parent chunk as " + Utils.to0PrefixedHexString(selfHash) + ", but it was expected to be " + this.parentHash.getHashNumberAsString() + ".");

        this.animationRefs.clear();
        int animationCount = reader.readInt() + 1;
        for (int i = 0; i < animationCount; i++) {
            int hash = reader.readInt();
            GreatQuestHash<kcCResourceTrack> animation = new GreatQuestHash<>(hash);
            GreatQuestUtils.resolveResourceHash(kcCResourceTrack.class, this, animation, hash, false); // It is common for these to not resolve, so don't warn about it.
            this.animationRefs.add(animation);
        }
    }

    @Override
    protected void saveData(DataWriter writer) {
        writer.writeInt(this.parentHash.getHashNumber());
        writer.writeInt(this.animationRefs.size() - 1);
        for (int i = 0; i < this.animationRefs.size(); i++)
            writer.writeInt(this.animationRefs.get(i).getHashNumber());
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        for (int i = 0; i < this.animationRefs.size(); i++) {
            builder.append(padding).append("-");
            writeAssetLine(builder, padding, "kcAnimSetDesc Entry", this.animationRefs.get(i));
        }

        builder.append(Constants.NEWLINE);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.ANIM_SET.getClassId();
    }
}