package net.highwayfrogs.editor.file.patch;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.patch.commands.PatchCommand;
import net.highwayfrogs.editor.file.patch.commands.PatchCommandManager;
import net.highwayfrogs.editor.file.patch.reference.PatchTextReference;
import net.highwayfrogs.editor.file.patch.reference.PatchValueReference;
import net.highwayfrogs.editor.file.patch.reference.PatchWrapperValue;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The runtime used to run a patch.
 * Created by Kneesnap on 1/15/2020.
 */
@Getter
public class PatchRuntime {
    private FroggerEXEInfo exeInfo = GUIMain.EXE_CONFIG;
    private DataWriter exeWriter;
    private GamePatch patch;
    @Setter private int executionLevel = 0;
    private Map<String, PatchValue> variables = new HashMap<>();
    private boolean hadError = false;

    public PatchRuntime(GamePatch patch) {
        this.patch = patch;
    }

    /**
     * Gets a variable value by its name.
     * @param varName The variable name to get.
     * @return varValue
     */
    public PatchValue getVariable(String varName) {
        return getVariable(varName, false);
    }

    /**
     * Gets a variable value by its name.
     * @param varName The variable name to get.
     * @return varValue
     */
    public PatchValue getVariable(String varName, boolean allowNull) {
        if (varName.equalsIgnoreCase("$VERSION"))
            return new PatchValue(PatchArgumentType.STRING, exeInfo.getInternalName());

        if (!allowNull && !this.variables.containsKey(varName))
            throw new RuntimeException("Cannot get value of unknown variable '" + varName + "'.");
        return this.variables.get(varName);
    }

    /**
     * Runs the patch.
     */
    public void run() {
        String lastLine = null;
        try {
            for (String line : patch.getCode()) {
                lastLine = line;
                runLine(line);
            }
        } catch (Exception ex) {
            hadError = true;
            printRuntimeInformation();
            System.out.println("Encountered an error while running '" + getPatch().getName() + "'.");
            System.out.println("Line: '" + lastLine + "'.");
            Utils.makeErrorPopUp("There was an error while applying the patch.", ex, true);
        }
        onFinish();
    }

    /**
     * Attempts to setup the patch.
     */
    public boolean runSetup() {
        this.hadError = false;
        this.variables.clear();
        this.executionLevel = 0;
        setupVariables();

        String lastLine = null;
        try {
            for (String line : patch.getArgsCode()) {
                lastLine = line;
                runLine(line);
            }

            return true;
        } catch (Exception ex) {
            hadError = true;
            printRuntimeInformation();
            System.out.println("Encountered an error while running setup for '" + getPatch().getName() + "'.");
            System.out.println("Line: '" + lastLine + "'.");
            Utils.makeErrorPopUp("There was an error while setting up the patch.", ex, true);
            return false;
        }
    }

    /**
     * Print information about this runtime.
     */
    public void printRuntimeInformation() {
        System.out.println("Name: " + getPatch().getName());
        System.out.println("Variables:");
        for (String varName : this.variables.keySet())
            System.out.println(" - " + varName + " = " + this.variables.get(varName));
    }

    private void runLine(String line) {
        int spaceCount = 0;
        while (line.length() > spaceCount && line.charAt(spaceCount) == ' ')
            spaceCount++;

        if (spaceCount == line.length() || spaceCount > this.executionLevel)
            return; // Skip this line, it's at a higher execution level, or the line has no code.

        this.executionLevel = spaceCount; // Set the execution level to match the space count, it can only decrease.

        // Read command name.
        int read = spaceCount;
        StringBuilder labelBuilder = new StringBuilder();
        while (line.length() > read && Character.isLetterOrDigit(line.charAt(read))) {
            labelBuilder.append(line.charAt(read));
            read++;
        }
        while (line.length() > read && line.charAt(read) == ' ')
            read++;

        // Read arguments.
        boolean isString = false;
        StringBuilder lastArg = new StringBuilder();
        List<PatchValueReference> args = new ArrayList<>();
        while (line.length() > read) { // Read until there's nothing left to read.
            char temp = line.charAt(read);
            if (lastArg.length() == 0 && temp == '"') {
                lastArg.append(temp);
                isString = true;
            } else if (isString && temp == '"') {
                lastArg.append(temp);
                isString = false;
                args.add(new PatchWrapperValue(PatchValue.parseStringAsPatchValue(lastArg.toString())));
                lastArg = new StringBuilder();
            } else if (!isString && temp == ' ') {
                if (lastArg.length() > 0) { // Only reset it if there's something to reset.
                    PatchValue value = PatchValue.parseStringAsPatchValue(lastArg.toString());
                    args.add(value != null ? new PatchWrapperValue(value) : new PatchTextReference(lastArg.toString()));
                    lastArg = new StringBuilder();
                }
            } else {
                lastArg.append(temp);
            }

            read++;
        }

        if (lastArg.length() > 0) {
            PatchValue value = PatchValue.parseStringAsPatchValue(lastArg.toString());
            args.add(value != null ? new PatchWrapperValue(value) : new PatchTextReference(lastArg.toString()));
        }

        // Execute Command.
        String cmdName = labelBuilder.toString();
        PatchCommand command = PatchCommandManager.getCommand(cmdName);
        if (command == null)
            throw new RuntimeException("Could run execute unknown patch command '" + cmdName + "'.");

        command.execute(this, args);
    }

    private void setupVariables() {
        getPatch().getDefaultVariables().forEach(this.variables::put); // Copy the default variables too.

        for (PatchArgument argument : patch.getArguments())
            this.variables.put(argument.getName(), new PatchValue(argument.getType(), argument.getDefaultValue()));

        // Use version-specific variables.
        Map<String, PatchValue> versionValues = getPatch().getVersionSpecificVariables().get(getExeInfo().getInternalName());
        if (versionValues != null)
            versionValues.forEach(this.variables::put);
    }

    private void onFinish() {
        if (this.exeWriter != null) {
            this.exeWriter.closeReceiver();
            this.exeWriter = null;
        }
    }

    /**
     * Gets the writer which is writing to the exe.
     * @return exeWriter
     */
    public DataWriter getExeWriter() {
        if (this.exeWriter == null)
            this.exeWriter = getExeInfo().getWriter();
        return this.exeWriter;
    }
}
