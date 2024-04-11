package net.highwayfrogs.editor.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Locale;

/**
 * Contains static utilities relating to time.
 * Created by Kneesnap on 4/10/2024.
 */
public class TimeUtils {
    private static final String TEMPLATE = "[yyyy['-'MM['-'dd]]][yyyy['/'MM['/'dd]]][MM'-'dd['-'yyyy]][MM'/'dd['/'yyyy]][' 'hh[':'mm[':'ss]]' 'a][' 'HH[':'mm[':'ss]]][' 'vvvv][' 'VV]";
    private static final DateTimeFormatter DATE_PARSE_FORMATTER = DateTimeFormatter.ofPattern(TEMPLATE, Locale.ENGLISH);

    /**
     * Parses an ambiguous time / date expression, as long as it follows one the following format:
     * "[Date] [24 Hour Time] [Time Zone]" Example: "2024-04-11 8:37:50 PM America/Los_Angeles"
     * "<Unix Epoch Timestamp In Seconds>" Example: "1712806672" -> April 11th 2024 3:37:50 AM UTC
     *
     * Valid Date Formats:
     * yyyy, yyyy-MM, yyyy/MM, yyyy-MM-dd, yyyy/MM/dd, MM-dd, MM/dd, MM-dd-yyyy, MM/dd/yyyy
     * If omitted, no date will be assumed.
     *
     * Valid Time of Day Formats:
     * hh a, hh:mm a, hh:mm:ss a, HH, HH:mm, HH:mm:ss
     * If omitted, no time of day will be assumed
     *
     * Valid Time Zones:
     * Examples: "America/Los_Angeles", "Pacific Time", "PT", "UTC"
     * Any valid ZoneID or generic "time-zone" name.
     * Reference: <a href="https://docs.oracle.com/javase/9/docs/api/java/time/format/DateTimeFormatter.html#parseBest-java.lang.CharSequence-java.time.temporal.TemporalQuery...-"/>
     *
     * If the date format is not parsed successfully, an IllegalArgumentException will be thrown.
     * @param input the input string to parse
     * @return parsedTime
     */
    public static TemporalAccessor parseAmbiguousTimestamp(String input) {
        if (input == null || input.isEmpty())
            throw new NullPointerException("input");

        // Parse epoch timestamp.
        if (input.length() > 7 && Utils.isInteger(input))
            return Instant.ofEpochSecond(Long.parseLong(input));

        try {
            return DATE_PARSE_FORMATTER.parseBest(input, ZonedDateTime::from, YearMonth::from, Year::from);
        } catch (DateTimeParseException exception) {
            throw new RuntimeException("Failed to parse timestamp from '" + input + "'", exception);
        }
    }

    /**
     * Writes a timestamp to a StringBuilder.
     * The output format prioritizes conciseness and showing the most significant bits first, due to anticipated use in sorted list displays.
     * @param accessor the timestamp to write
     * @param militaryTime if true, a number in [0,23] will be used hour of day, otherwise [1,12] [AM|PM] will be used.
     * @param showTimeZone if true AND the time zone is successfully obtained from the accessor, include the time zone in the output
     */
    public static String writeTimestampConciselyToString(TemporalAccessor accessor, boolean militaryTime, boolean showTimeZone) {
        StringBuilder builder = new StringBuilder();
        writeTimestampConcisely(builder, accessor, militaryTime, showTimeZone);
        return builder.toString();
    }

    /**
     * Writes a timestamp to a StringBuilder.
     * Is can be used instead of a DateFormat, because we want to be able to output question-mark characters if portions of the date are not set.
     * The output format prioritizes conciseness and showing the most significant bits first, due to anticipated use in sorted list displays.
     * @param builder the string builder to write to
     * @param accessor the timestamp to write
     * @param militaryTime if true, a number in [0,23] will be used hour of day, otherwise [1,12] [AM|PM] will be used.
     * @param showTimeZone if true AND the time zone is successfully obtained from the accessor, include the time zone in the output
     */
    public static void writeTimestampConcisely(StringBuilder builder, TemporalAccessor accessor, boolean militaryTime, boolean showTimeZone) {
        if (builder == null)
            throw new NullPointerException("builder");

        writeCalendarDateConcisely(builder, accessor);
        if (accessor != null && accessor.isSupported(ChronoField.HOUR_OF_DAY)) {
            builder.append(' ');
            writeTimeOfDayConcisely(builder, accessor, militaryTime, showTimeZone);
        }
    }

    /**
     * Writes a timestamp to a StringBuilder.
     * The output format prioritizes conciseness and showing the most significant bits first, due to anticipated use in sorted list displays.
     * @param builder the string builder to write to
     * @param accessor the source of date information
     */
    public static void writeCalendarDateConcisely(StringBuilder builder, TemporalAccessor accessor) {
        if (builder == null)
            throw new NullPointerException("builder");

        // Write year.
        if (accessor != null && accessor.isSupported(ChronoField.YEAR)) {
            builder.append(String.format("%04d", accessor.get(ChronoField.YEAR)));
        } else {
            builder.append("????");
        }

        // Write month.
        builder.append('-');
        if (accessor != null && accessor.isSupported(ChronoField.MONTH_OF_YEAR)) {
            builder.append(String.format("%02d", accessor.get(ChronoField.MONTH_OF_YEAR)));
        } else {
            builder.append("??");
        }

        // Write day.
        builder.append('-');
        if (accessor != null && accessor.isSupported(ChronoField.DAY_OF_MONTH)) {
            builder.append(String.format("%02d", accessor.get(ChronoField.DAY_OF_MONTH)));
        } else {
            builder.append("??");
        }
    }

    /**
     * Writes a time of day to a StringBuilder in the form: "HOUR[:MINUTE[:SECONDS]][ AM| PM][ TimeZone]"
     * The output format prioritizes conciseness and showing the most significant bits first, due to anticipated use in sorted list displays.
     * @param builder the string builder to write to
     * @param accessor the timestamp to write
     * @param preferMilitaryTime if true, a number in [0,23] will be used hour of day, otherwise [1,12] [AM|PM] will be used.
     * @param showTimeZone if true AND the time zone is successfully obtained from the accessor, include the time zone in the output
     */
    public static void writeTimeOfDayConcisely(StringBuilder builder, TemporalAccessor accessor, boolean preferMilitaryTime, boolean showTimeZone) {
        if (builder == null)
            throw new NullPointerException("builder");
        if (accessor == null)
            throw new NullPointerException("accessor");

        boolean hasMinutes = accessor.isSupported(ChronoField.MINUTE_OF_HOUR);
        boolean useMilitaryTime = preferMilitaryTime && hasMinutes && accessor.isSupported(ChronoField.HOUR_OF_DAY);
        if (useMilitaryTime) {
            builder.append(String.format("%02d", accessor.get(ChronoField.HOUR_OF_DAY)));
        } else if (accessor.isSupported(ChronoField.CLOCK_HOUR_OF_AMPM)) {
            builder.append(String.format("%02d", accessor.get(ChronoField.CLOCK_HOUR_OF_AMPM)));
        } else {
            // Couldn't find hour so abort.
            builder.append("{Fallback Time Display: ")
                    .append(accessor)
                    .append('}');
            return;
        }

        // Write minutes & seconds.
        if (hasMinutes) {
            builder.append(':').append(String.format("%02d", accessor.get(ChronoField.MINUTE_OF_HOUR)));
            if (accessor.isSupported(ChronoField.SECOND_OF_MINUTE))
                builder.append(':').append(String.format("%02d", accessor.get(ChronoField.SECOND_OF_MINUTE)));
        }

        // Write AM/PM suffix.
        if (!useMilitaryTime && accessor.isSupported(ChronoField.AMPM_OF_DAY))
            builder.append((accessor.get(ChronoField.AMPM_OF_DAY) == 0) ? " AM" : " PM");

        // Show time zone (if enabled).
        if (showTimeZone) {
            ZoneOffset offset = accessor.query(TemporalQueries.offset());

            if (offset != null) {
                String partialDisplay = offset.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault());
                String fullDisplay = offset.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault());

                boolean usePartialDisplay = (partialDisplay != null && !partialDisplay.isEmpty());
                boolean useFullDisplay = (fullDisplay != null && !fullDisplay.isEmpty());
                if (usePartialDisplay || useFullDisplay) {
                    builder.append(" (");
                    if (usePartialDisplay) {
                        builder.append(partialDisplay);
                        if (useFullDisplay)
                            builder.append(", ");
                    }

                    if (useFullDisplay)
                        builder.append(fullDisplay);

                    builder.append(')');
                }
            }
        }
    }
}