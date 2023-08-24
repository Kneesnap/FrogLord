package net.highwayfrogs.editor.games.tgq;

import net.highwayfrogs.editor.Constants;

/**
 * Represents an object which can write information about itself to a string builder.
 * This should write to a single line.
 * Created by Kneesnap on 8/23/2023.
 */
public interface IInfoWriter {
    /**
     * Writes prefixed information to the builder.
     * @param builder The builder to write to.
     * @param prefix  The prefix to apply.
     * @param padding The padding to write with.
     */
    default void writePrefixedInfoLine(StringBuilder builder, String prefix, String padding) {
        builder.append(padding).append(prefix).append(": ");
        writeInfo(builder); // No padding provided since this is single line.
        builder.append(Constants.NEWLINE);
    }

    /**
     * Writes info to the builder.
     * @param builder The builder to write to.
     */
    void writeInfo(StringBuilder builder);

    interface IMultiLineInfoWriter {
        /**
         * Writes prefixed information to the builder.
         * The written data can be multiple lines.
         * @param builder    The builder to write to.
         * @param prefix     The prefix to apply.
         * @param oldPadding The padding to write the prefix with.
         */
        default void writePrefixedMultiLineInfo(StringBuilder builder, String prefix, String oldPadding) {
            builder.append(oldPadding).append(' ').append(prefix).append(":").append(Constants.NEWLINE);
            writeMultiLineInfo(builder, oldPadding + " ");
        }

        /**
         * Writes prefixed information to the builder.
         * The written data can be multiple lines.
         * @param builder    The builder to write to.
         * @param prefix     The prefix to apply.
         * @param oldPadding The padding to write the prefix with.
         * @param newPadding The padding to write upcoming data with.
         */
        default void writePrefixedMultiLineInfo(StringBuilder builder, String prefix, String oldPadding, String newPadding) {
            builder.append(oldPadding).append(' ').append(prefix).append(":").append(Constants.NEWLINE);
            writeMultiLineInfo(builder, newPadding);
        }

        /**
         * Writes info to the builder without any padding.
         * @param builder The builder to write to.
         */
        default void writeMultiLineInfo(StringBuilder builder) {
            writeMultiLineInfo(builder, "");
        }

        /**
         * Writes info to the builder.
         * @param builder The builder to write to.
         * @param padding The padding to add at the start.
         */
        void writeMultiLineInfo(StringBuilder builder, String padding);
    }
}
