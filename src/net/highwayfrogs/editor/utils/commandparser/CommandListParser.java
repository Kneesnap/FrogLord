package net.highwayfrogs.editor.utils.commandparser;

import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.commandparser.CommandListException.CommandListExecutionError;
import net.highwayfrogs.editor.utils.commandparser.CommandListException.CommandListSyntaxError;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a generic command-list parser used to read a sequential list of commands.
 * Anticipated Uses:
 *  - .ffs (Frogger map exporting/importing)
 *  - .gqmap (Frogger Great Quest map exporting/importing)
 *  - .obj importing (In the future)
 * Created by Kneesnap on 10/11/2025.
 */
public class CommandListParser<TContext extends CommandListExecutionContext> {
    private final Map<String, TextCommand<TContext>> commandsByName = new HashMap<>();

    /**
     * Registers a command to the parser.
     * @param command the command to register
     */
    public void registerCommand(TextCommand<TContext> command) {
        if (command == null)
            throw new NullPointerException("command");

        TextCommand<TContext> oldCommand = this.commandsByName.putIfAbsent(command.getName().toLowerCase(), command);
        if (oldCommand != null)
            throw new IllegalArgumentException("The command '" + command.getName() + "' has already been registered. (Same Command: " + (oldCommand == command) + ")");
    }

    /**
     * Gets a command by its name (case-insensitive)
     * @param commandName the command to lookup
     * @return command, or null if none can be found
     */
    public TextCommand<TContext> getCommandByName(String commandName) {
        return commandName != null ? this.commandsByName.get(commandName.toLowerCase()) : null;
    }

    /**
     * Read the contents of a file and execute it as commands.
     * @param context the context to execute the commands under
     * @param inputFile the file to read commands from
     * @throws CommandListException Thrown to indicate an error while attempting to execute the commands.
     * @throws IOException Thrown to indicate an error reading the provided input file
     */
    public void executeCommands(TContext context, File inputFile) throws CommandListException, IOException {
        if (context == null)
            throw new NullPointerException("context");
        if (inputFile == null)
            throw new NullPointerException("inputFile");

        List<String> lines = Files.readAllLines(inputFile.toPath());
        executeCommands(context, lines, inputFile.getName(), 1);
    }

    /**
     * Executes a list of commands.
     * @param context the context to execute the commands under
     * @param list the list of commands to execute
     * @param fileName the name of the file (or other source of commands). Null allowed.
     * @throws CommandListException Thrown to indicate an error while attempting to execute the commands.
     */
    public void executeCommands(TContext context, List<String> list, String fileName) throws CommandListException {
        executeCommands(context, list, fileName, 1);
    }

    /**
     * Executes a list of commands.
     * @param context the context to execute the commands under
     * @param list the list of commands to execute
     * @param fileName the name of the file (or other source of commands). Null allowed.
     * @param baseLineNumber the first line number
     * @throws CommandListException Thrown to indicate an error while attempting to execute the commands.
     */
    public void executeCommands(TContext context, List<String> list, String fileName, int baseLineNumber) throws CommandListException {
        if (context == null)
            throw new NullPointerException("context");
        if (list == null)
            throw new NullPointerException("list");

        CommandLocation location = new CommandLocation(fileName, baseLineNumber);
        for (int i = 0; i < list.size(); i++) {
            location.setLineNumber(baseLineNumber + i);
            executeCommand(context, list.get(i), location);
        }
    }

    /**
     * Executes a list of commands from the text body of the config node.
     * @param context the context to execute the commands under
     * @param config the config node to execute commands from
     * @throws CommandListException Thrown to indicate an error while attempting to execute the commands.
     */
    public void executeCommands(TContext context, Config config) throws CommandListException {
        if (context == null)
            throw new NullPointerException("context");
        if (config == null)
            throw new NullPointerException("config");

        CommandLocation location = new CommandLocation(config.getRootNode().getSectionName(), config.getOriginalLineNumber());
        List<ConfigValueNode> orderedText = config.getTextNodes();
        for (int i = 0; i < orderedText.size(); i++) {
            ConfigValueNode node = orderedText.get(i);
            String text = node.getAsString();
            if (StringUtils.isNullOrWhiteSpace(text))
                continue;

            if (node.getOriginalLineNumber() > 0)
                location.setLineNumber(node.getOriginalLineNumber());
            executeCommand(context, text, location);
        }
    }

    /**
     * Executes a single command.
     * @param context the context to execute the command under
     * @param inputTextLine the full unparsed command
     * @param location the location where the command was obtained from
     * @return true iff the command was executed, false if there was no command, it was skipped, etc
     * @throws CommandListException Thrown to indicate an error while attempting to execute the command
     */
    public boolean executeCommand(TContext context, String inputTextLine, CommandLocation location) throws CommandListException {
        if (context == null)
            throw new NullPointerException("context");
        if (inputTextLine == null)
            return false;

        String strippedCommand = stripComment(inputTextLine, location);
        if (strippedCommand.isEmpty())
            return false;

        OptionalArguments arguments;
        try {
            arguments = OptionalArguments.parse(strippedCommand);
        } catch (Throwable th) {
            throw new CommandListSyntaxError(location, th, "Could not interpret '%s' as a command, is it formatted correctly?", strippedCommand);
        }

        if (!arguments.hasNext()) // Only time this will occur is if only named arguments (--Flag) are provided.
            throw new CommandListSyntaxError(location, "The line '%s' did not include the name of a command to execute.", strippedCommand);

        // Resolve command.
        String commandName = arguments.useNext().getAsString();
        TextCommand<TContext> command = getCommandByName(commandName);
        if (command == null) {
            context.getLogger().warning("Skipping unrecognized command '%s'%s.",
                    commandName,
                    (location != null ? " found " + location.getPositionText(true) : ""));
            return false;
        }

        // This may throw an error, and that is okay.
        command.validateBeforeExecution(context, arguments, location);

        try {
            command.execute(context, arguments);
            arguments.warnAboutUnusedArguments(context.getLogger());
        } catch (Throwable th) {
            throw new CommandListExecutionError(location, th, "Failed to execute '%s'.", strippedCommand);
        }

        return true;
    }

    private static String stripComment(String input, CommandLocation location) throws CommandListSyntaxError {
        boolean stringOpen = false;
        boolean escapeActive = false;
        int commentIndex = -1;
        for (int i = 0; i < input.length(); i++) {
            char tempChar = input.charAt(i);
            if (escapeActive) {
                escapeActive = false;
            } else if (tempChar == '\\') {
                escapeActive = true;
            } else if (tempChar == '"') {
                stringOpen = !stringOpen;
            } else if (tempChar == '#' && !stringOpen) {
                commentIndex = i;
                break;
            }
        }

        if (escapeActive)
            throw new CommandListSyntaxError(location, "Cannot end a line with an unfinished escape-sequence (backslash).");
        if (stringOpen)
            throw new CommandListSyntaxError(location, "Cannot end a line with an unfinished string. (Missing the '\"' character)");

        String output = input;
        if (commentIndex >= 0)
            output = output.substring(0, commentIndex);

        return output.trim();
    }
}
