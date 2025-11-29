package net.highwayfrogs.editor.utils.commandparser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.utils.StringUtils;

/**
 * Represents a location where the command was read from.
 * Created by Kneesnap on 10/11/2025.
 */
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class CommandLocation {
    private final String fileName;
    @Setter private int lineNumber = 1;

    /**
     * Gets the display position for where the position is.
     */
    public String getPositionText(boolean lowerCase) {
        StringBuilder builder = new StringBuilder();
        if (!StringUtils.isNullOrWhiteSpace(this.fileName))
            builder.append(lowerCase ? "in '" : "In '").append(this.fileName).append("'");

        if (this.lineNumber > 0) {
            if (builder.length() > 0) {
                builder.append(", on line ");
            } else {
                builder.append(lowerCase ? "at line " : "At line ");
            }

            builder.append(this.lineNumber);
        }

        if (builder.length() == 0)
            builder.append("<UNKNOWN LOCATION>");

        return builder.toString();
    }
}