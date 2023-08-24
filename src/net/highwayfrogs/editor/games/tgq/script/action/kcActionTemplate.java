package net.highwayfrogs.editor.games.tgq.script.action;

import net.highwayfrogs.editor.games.tgq.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.tgq.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.tgq.script.kcArgument;
import net.highwayfrogs.editor.games.tgq.script.kcParam;

import java.util.ArrayList;
import java.util.List;

/**
 * A basic template kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
public abstract class kcActionTemplate extends kcAction {
    private final List<kcParam> arguments = new ArrayList<>();

    public kcActionTemplate(kcActionID action) {
        super(action);
    }

    @Override
    public void load(kcParamReader reader) {
        this.arguments.clear();
        kcArgument[] arguments = getArgumentTemplate(reader.getArguments());
        for (int i = 0; i < arguments.length; i++)
            this.arguments.add(reader.next());
    }

    @Override
    public void save(kcParamWriter writer) {
        for (int i = 0; i < this.arguments.size(); i++)
            writer.write(this.arguments.get(i));
    }
}
