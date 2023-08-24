package net.highwayfrogs.editor.games.tgq.script.cause;

import net.highwayfrogs.editor.games.tgq.script.kcScriptDisplaySettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a basic "dumb" script cause that can be used as a replacement for another script.
 * Created by Kneesnap on 8/16/2023.
 */
public class kcScriptCauseDummy extends kcScriptCause {
    private int subCauseType;
    private List<Integer> unhandledValues;

    public kcScriptCauseDummy(kcScriptCauseType type) {
        super(type, 0);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.subCauseType = subCauseType;
        this.unhandledValues = extraValues != null ? new ArrayList<>(extraValues) : null;
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.subCauseType);
        if (this.unhandledValues != null)
            output.addAll(this.unhandledValues);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("Unknown [").append(getType().name()).append('/').append(this.subCauseType)
                .append("]");

        if (this.unhandledValues != null && this.unhandledValues.size() > 0) {
            builder.append(", Unhandled Values: [");
            for (int i = 0; i < this.unhandledValues.size(); i++) {
                if (i > 0)
                    builder.append(", ");
                builder.append(this.unhandledValues.get(i));
            }

            builder.append(']');
        }

    }

    @Override
    public boolean validateArgumentCount(int argumentCount) {
        return true;
    }
}
