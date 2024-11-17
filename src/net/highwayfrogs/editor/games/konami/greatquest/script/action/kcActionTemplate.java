package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcActionExecutor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;
import net.highwayfrogs.editor.utils.objects.StringNode;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A basic template kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public abstract class kcActionTemplate extends kcAction {
    private final List<kcParam> arguments = new ArrayList<>();

    public kcActionTemplate(kcActionExecutor executor, kcActionID action) {
        super(executor, action);
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

    @Override
    public void printWarnings(Logger logger) {
        super.printWarnings(logger);

        // Print argument warnings.
        kcParam[] params = this.arguments.toArray(new kcParam[0]);
        kcArgument[] argumentTemplates = getArgumentTemplate(params);
        for (int i = 0; i < this.arguments.size(); i++) {
            kcParam argument = this.arguments.get(i);
            kcArgument argumentTemplate = argumentTemplates[i];
            String warning = argument.getInvalidValueWarning(getExecutor(), argumentTemplate.getType());
            if (warning != null)
                printWarning(logger, warning + " (kcParamType: " + argumentTemplate.getType() + ")");
        }
    }

    /**
     * Gets or creates the param at the given index.
     * @param paramIndex the index to obtain the param from
     * @return paramAtIndex
     */
    protected kcParam getOrCreateParam(int paramIndex) {
        if (paramIndex < 0 || paramIndex >= kcAction.MAX_ARGUMENT_COUNT)
            throw new IndexOutOfBoundsException("Invalid param index: " + paramIndex);

        while (paramIndex >= this.arguments.size())
            this.arguments.add(new kcParam());

        return this.arguments.get(paramIndex);
    }

    /**
     * Gets or creates the param at the given index.
     * @param paramIndex the index to obtain the param from
     * @return paramAtIndex
     */
    protected kcParam getParamOrError(int paramIndex) {
        if (paramIndex < 0 || paramIndex >= this.arguments.size())
            throw new IndexOutOfBoundsException("There was no param available at the index: " + paramIndex);

        return this.arguments.get(paramIndex);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        kcParam[] params = this.arguments.toArray(new kcParam[0]);
        kcArgument[] argumentTemplates = getArgumentTemplate(params);

        // NOTE: We should NOT cache getGqsArgumentCount() or argumentTemplates
        int gqsArgumentCount = getGqsArgumentCount(argumentTemplates);
        for (int i = 0; i < gqsArgumentCount; i++) {
            boolean addedArguments = false;
            while (gqsArgumentCount > this.arguments.size()) {
                this.arguments.add(new kcParam()); // Add arguments as needed.
                addedArguments = true;
            }

            if (addedArguments)
                params = this.arguments.toArray(new kcParam[0]);

            // Load the argument.
            StringNode node = arguments.useNext();
            kcArgument argumentTemplate = argumentTemplates[i]; // NOTE: WE DO NOT CACHE THIS ARRAY, since the template can change as we load values.
            this.arguments.get(i).fromConfigNode(getExecutor(), getGameInstance(), node, argumentTemplate.getType());

            // React to any changes caused by loading the argument.
            argumentTemplates = getArgumentTemplate(params); //  his is because it can change based on the arguments as we load them.
            gqsArgumentCount = getGqsArgumentCount(argumentTemplates);
        }
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        kcParam[] params = this.arguments.toArray(new kcParam[0]);
        kcArgument[] argumentTemplates = getArgumentTemplate(params);
        int argumentCount = getGqsArgumentCount(argumentTemplates);
        for (int i = 0; i < argumentCount; i++) {
            StringNode node = arguments.createNext();
            kcArgument argumentTemplate = argumentTemplates[i];
            this.arguments.get(i).toConfigNode(getExecutor(), settings, node, argumentTemplate.getType());
        }
    }
}