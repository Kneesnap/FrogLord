package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements a basic "dumb" script cause that can be used as a replacement for another script.
 * Created by Kneesnap on 8/16/2023.
 */
@Getter
public class kcScriptCauseDummy extends kcScriptCause {
    private int subCauseType;
    private List<Integer> unhandledValues;

    public kcScriptCauseDummy(kcScript script, kcScriptCauseType type) {
        super(script, type, 0, 0);
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
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        this.subCauseType = arguments.useNext().getAsInteger();
        if (arguments.hasNext()) {
            if (this.unhandledValues == null)
                this.unhandledValues = new ArrayList<>();

            this.unhandledValues.clear();
            while (arguments.hasNext())
                this.unhandledValues.add(arguments.useNext().getAsInteger());
        }
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsString(NumberUtils.to0PrefixedHexString(this.subCauseType), false);
        if (this.unhandledValues != null)
            for (int i = 0; i < this.unhandledValues.size(); i++)
                arguments.createNext().setAsString(NumberUtils.to0PrefixedHexString(this.unhandledValues.get(i)), false);
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ (this.subCauseType << 24) ^ this.unhandledValues.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && ((kcScriptCauseDummy) obj).getSubCauseType() == this.subCauseType
                && Objects.equals(((kcScriptCauseDummy) obj).getUnhandledValues(), this.unhandledValues);
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

    @Override
    public boolean validateGqsArgumentCount(int argumentCount) {
        return true;
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        printWarning(logger, "is a dummy cause, which isn't supposed to happen!");
    }
}