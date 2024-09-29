package net.highwayfrogs.editor.games.konami.greatquest.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcAnimSetDesc;

/**
 * Represents either kcCAnimSet or kcAnimSetDesc. One of those.
 * Created by Kneesnap on 4/16/2024.
 */
@Getter
public class kcCResourceAnimSet extends kcCResource {
    private final kcAnimSetDesc animSetDesc;

    public kcCResourceAnimSet(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.ANIMSET);
        this.animSetDesc = new kcAnimSetDesc(this);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.animSetDesc.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        this.animSetDesc.save(writer);
    }
}