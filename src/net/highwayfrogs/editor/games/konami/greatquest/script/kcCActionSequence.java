package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.TGQChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.toc.KCResourceID;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an action sequence definition.
 * Created by Kneesnap on 3/23/2020.
 */
@Getter
public class kcCActionSequence extends kcCResource {
    private final List<kcAction> actions = new ArrayList<>();

    public kcCActionSequence(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.ACTIONSEQUENCE);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.actions.clear();
        while (reader.hasMore())
            this.actions.add(kcAction.readAction(reader));
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (int i = 0; i < this.actions.size(); i++)
            this.actions.get(i).save(writer);
    }
}