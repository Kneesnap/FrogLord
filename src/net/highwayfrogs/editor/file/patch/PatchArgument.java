package net.highwayfrogs.editor.file.patch;

import lombok.Getter;

/**
 * Represents an argument for patches.
 * Created by Kneesnap on 1/15/2020.
 */
@Getter
public class PatchArgument {
    private PatchArgumentType type;
    private String name;
    private String description;
    private Object defaultValue;

    // Specific to the type.
    private double minimumValue = Integer.MIN_VALUE;
    private double maximumValue = Integer.MAX_VALUE;

    /**
     * Parses a PatchArgument from a string.
     * @param argStr The argument string to read.
     * @return argument
     */
    public static PatchArgument parsePatchArgument(String argStr) {
        String[] split = argStr.split(",");

        PatchArgument newArg = new PatchArgument();
        newArg.type = PatchArgumentType.BY_NAME.get(split[0]);
        if (newArg.type == null)
            throw new RuntimeException("There is no type argument '" + split[0] + "'.");

        newArg.name = split[1];
        newArg.description = split[2];

        String valStr = split[3];
        if (!newArg.type.getBehavior().isValidString(valStr))
            throw new RuntimeException("'" + valStr + "' is not a valid " + newArg.type + " value.");

        newArg.defaultValue = newArg.type.getBehavior().parseString(valStr);

        if (split.length > 5 && (newArg.type == PatchArgumentType.DECIMAL || newArg.type == PatchArgumentType.INT)) {
            newArg.minimumValue = Double.parseDouble(split[4]);
            newArg.maximumValue = Double.parseDouble(split[5]);
        }

        return newArg;
    }
}
