package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;

import java.util.*;

/**
 * Maps the mod manifest's [Gameplay] keys onto the bundled Frogger executable patches and back.
 * Created by jimbolina on 11/6/2026.
 */
public class FroggerModGameplay {
    public static final String KEY_FRAME_RATE = "frameRate";
    public static final String KEY_JUNGLE_SWAP = "jungleSwap";
    public static final String KEY_INITIAL_LEVELS = "initialLevels";
    public static final String KEY_VISIBLE_LEVELS = "visibleLevels";

    public static final String PATCH_FPS = "fps";
    public static final String PATCH_JUNGLE_SWAP = "jungle-swap";
    public static final String PATCH_INITIAL_LEVELS = "initial-levels";

    public static final String ARGUMENT_FPS = "fps";
    public static final String VARIABLE_APPLIED = "applied";
    public static final String ARGUMENT_LEVEL_PREFIX = "level";
    public static final String ARGUMENT_FLAGS_PREFIX = "flags";

    public static final int LEVEL_CALL_COUNT = 15;
    public static final int FLAG_COMPLETED = 1;
    public static final int FLAG_SELECTABLE = 2;
    public static final int FLAG_ZONE_ACCESSIBLE = 4;
    public static final int FLAGS_INITIAL = FLAG_SELECTABLE | FLAG_ZONE_ACCESSIBLE;
    public static final int FLAGS_VISIBLE = FLAG_ZONE_ACCESSIBLE;
    public static final int MINIMUM_FRAME_RATE = 1;
    public static final int MAXIMUM_FRAME_RATE = 1000;

    private FroggerModGameplay() {
    }

    /**
     * Gets the number the executable uses for a level code, which is the level's FroggerMapLevelID ordinal.
     * @param levelCode the FroggerMapLevelID internal name, such as "CAV1"
     * @return the level number, or -1 if the code names no level
     */
    public static int getLevelNumber(String levelCode) {
        FroggerMapLevelID levelId = getLevelId(levelCode);
        return levelId != null ? levelId.ordinal() : -1;
    }

    /**
     * Gets the level code for a number read from the executable.
     * @param levelNumber the level number
     * @return the level code, or null if the number names no level
     */
    public static String getLevelCode(int levelNumber) {
        FroggerMapLevelID[] values = FroggerMapLevelID.values();
        return levelNumber >= 0 && levelNumber < values.length ? values[levelNumber].getInternalName() : null;
    }

    /**
     * Gets the initial-levels argument holding the level of one call.
     * @param call the call number, from 1 to LEVEL_CALL_COUNT
     * @return the argument name
     */
    public static String getLevelArgumentName(int call) {
        return ARGUMENT_LEVEL_PREFIX + call;
    }

    /**
     * Gets the initial-levels argument holding the flags of one call.
     * @param call the call number, from 1 to LEVEL_CALL_COUNT
     * @return the argument name
     */
    public static String getFlagsArgumentName(int call) {
        return ARGUMENT_FLAGS_PREFIX + call;
    }

    /**
     * Checks gameplay settings before they are turned into patch arguments. A null value means the key is absent.
     * @param initialLevels levels that start selectable and accessible
     * @param visibleLevels levels that start accessible only
     * @param jungleSwap whether JUN2 replaces JUN1
     * @param frameRate the game's frame rate
     * @return the problems found, empty when the settings can be applied
     */
    public static List<String> validate(List<String> initialLevels, List<String> visibleLevels, Boolean jungleSwap, Integer frameRate) {
        List<String> problems = new ArrayList<>();
        if (frameRate != null && (frameRate < MINIMUM_FRAME_RATE || frameRate > MAXIMUM_FRAME_RATE))
            problems.add(KEY_FRAME_RATE + " " + frameRate + " is outside " + MINIMUM_FRAME_RATE + ".." + MAXIMUM_FRAME_RATE + ".");

        if (initialLevels == null) {
            if (visibleLevels != null)
                problems.add(KEY_VISIBLE_LEVELS + " needs " + KEY_INITIAL_LEVELS + ", because the patch rewrites every initial unlock.");
            return problems;
        }

        if (initialLevels.isEmpty())
            problems.add(KEY_INITIAL_LEVELS + " must name at least one level.");

        Set<String> seen = new HashSet<>();
        checkLevels(KEY_INITIAL_LEVELS, initialLevels, seen, problems);
        if (visibleLevels != null)
            checkLevels(KEY_VISIBLE_LEVELS, visibleLevels, seen, problems);

        int total = initialLevels.size() + (visibleLevels != null ? visibleLevels.size() : 0);
        if (total > LEVEL_CALL_COUNT)
            problems.add(KEY_INITIAL_LEVELS + " and " + KEY_VISIBLE_LEVELS + " name " + total + " levels, but the executable has " + LEVEL_CALL_COUNT + " unlock calls.");
        return problems;
    }

    /**
     * Turns gameplay settings into the bundled patches to apply and their argument values.
     * A null value means the key is absent, so its patch is not applied; jungleSwap applies its patch only when false.
     * @param initialLevels levels that start selectable and accessible
     * @param visibleLevels levels that start accessible only; null or empty means none
     * @param jungleSwap whether JUN2 replaces JUN1
     * @param frameRate the game's frame rate
     * @return argument values by name, keyed by patch id, in application order
     * @throws IllegalArgumentException if {@link #validate} reports a problem
     */
    public static Map<String, Map<String, String>> toPatchArguments(List<String> initialLevels, List<String> visibleLevels, Boolean jungleSwap, Integer frameRate) {
        List<String> problems = validate(initialLevels, visibleLevels, jungleSwap, frameRate);
        if (!problems.isEmpty())
            throw new IllegalArgumentException(String.join(" ", problems));

        Map<String, Map<String, String>> patches = new LinkedHashMap<>();
        if (frameRate != null) {
            Map<String, String> arguments = new LinkedHashMap<>();
            arguments.put(ARGUMENT_FPS, String.valueOf(frameRate));
            patches.put(PATCH_FPS, arguments);
        }

        if (Boolean.FALSE.equals(jungleSwap))
            patches.put(PATCH_JUNGLE_SWAP, new LinkedHashMap<String, String>());

        if (initialLevels != null) {
            List<int[]> calls = new ArrayList<>();
            for (String levelCode : initialLevels)
                calls.add(new int[] {getLevelNumber(levelCode), FLAGS_INITIAL});
            if (visibleLevels != null)
                for (String levelCode : visibleLevels)
                    calls.add(new int[] {getLevelNumber(levelCode), FLAGS_VISIBLE});

            int[] padding = calls.get(0);
            while (calls.size() < LEVEL_CALL_COUNT)
                calls.add(padding);

            Map<String, String> arguments = new LinkedHashMap<>();
            for (int i = 0; i < LEVEL_CALL_COUNT; i++) {
                arguments.put(getLevelArgumentName(i + 1), String.valueOf(calls.get(i)[0]));
                arguments.put(getFlagsArgumentName(i + 1), String.valueOf(calls.get(i)[1]));
            }
            patches.put(PATCH_INITIAL_LEVELS, arguments);
        }

        return patches;
    }

    /**
     * Recovers gameplay settings from the values a bundled patch's [ArgsCode] read back from an executable.
     * @param patchId the bundled patch id
     * @param argsReadBack the patch's variables after setup; for jungle-swap this holds the "applied" variable
     * @return the recovered settings and any problems
     */
    public static FroggerModGameplaySettings fromPatchArguments(String patchId, Map<String, String> argsReadBack) {
        return fromPatchArguments(patchId, argsReadBack, new FroggerModGameplaySettings());
    }

    /**
     * Recovers gameplay settings from the values a bundled patch's [ArgsCode] read back from an executable.
     * @param patchId the bundled patch id
     * @param argsReadBack the patch's variables after setup; for jungle-swap this holds the "applied" variable
     * @param settings the settings to fill, so several patches can be read into one result
     * @return the settings given
     */
    public static FroggerModGameplaySettings fromPatchArguments(String patchId, Map<String, String> argsReadBack, FroggerModGameplaySettings settings) {
        if (PATCH_FPS.equals(patchId)) {
            Integer frameRate = parseInteger(argsReadBack, ARGUMENT_FPS, patchId, settings.problems);
            if (frameRate != null)
                settings.frameRate = frameRate;
        } else if (PATCH_JUNGLE_SWAP.equals(patchId)) {
            String applied = argsReadBack.get(VARIABLE_APPLIED);
            if ("true".equalsIgnoreCase(applied)) {
                settings.jungleSwap = Boolean.FALSE;
            } else if ("false".equalsIgnoreCase(applied)) {
                settings.jungleSwap = Boolean.TRUE;
            } else {
                settings.problems.add(patchId + ": the variable '" + VARIABLE_APPLIED + "' was not read back (" + applied + ").");
            }
        } else if (PATCH_INITIAL_LEVELS.equals(patchId)) {
            readLevelCalls(argsReadBack, settings);
        } else {
            settings.problems.add("'" + patchId + "' is not a gameplay patch.");
        }

        return settings;
    }

    private static void readLevelCalls(Map<String, String> argsReadBack, FroggerModGameplaySettings settings) {
        Map<String, Integer> flagsByLevel = new LinkedHashMap<>();
        for (int call = 1; call <= LEVEL_CALL_COUNT; call++) {
            Integer levelNumber = parseInteger(argsReadBack, getLevelArgumentName(call), PATCH_INITIAL_LEVELS, settings.problems);
            Integer flags = parseInteger(argsReadBack, getFlagsArgumentName(call), PATCH_INITIAL_LEVELS, settings.problems);
            if (levelNumber == null || flags == null)
                continue;

            String levelCode = getLevelCode(levelNumber);
            if (levelCode == null) {
                settings.problems.add(PATCH_INITIAL_LEVELS + ": call " + call + " names unknown level number " + levelNumber + ".");
                continue;
            }

            Integer previousFlags = flagsByLevel.get(levelCode);
            if (previousFlags != null && previousFlags.intValue() != flags)
                settings.problems.add(PATCH_INITIAL_LEVELS + ": " + levelCode + " is given flags " + previousFlags + " and later " + flags + "; the later call wins.");
            flagsByLevel.put(levelCode, flags);
        }

        settings.initialLevels = new ArrayList<>();
        settings.visibleLevels = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : flagsByLevel.entrySet()) {
            int flags = entry.getValue();
            if (flags == FLAGS_INITIAL) {
                settings.initialLevels.add(entry.getKey());
            } else if (flags == FLAGS_VISIBLE) {
                settings.visibleLevels.add(entry.getKey());
            } else {
                settings.problems.add(PATCH_INITIAL_LEVELS + ": " + entry.getKey() + " starts with flags " + flags + ", which neither "
                        + KEY_INITIAL_LEVELS + " (" + FLAGS_INITIAL + ") nor " + KEY_VISIBLE_LEVELS + " (" + FLAGS_VISIBLE + ") can express.");
            }
        }
    }

    private static void checkLevels(String key, List<String> levelCodes, Set<String> seen, List<String> problems) {
        for (String levelCode : levelCodes) {
            FroggerMapLevelID levelId = getLevelId(levelCode);
            if (levelId == null) {
                problems.add(key + " names unknown level '" + levelCode + "'.");
            } else if (levelId.name().endsWith("_MULTIPLAYER")) {
                problems.add(key + " names multiplayer level '" + levelCode + "', whose flags the race calls set.");
            } else if (!seen.add(levelId.getInternalName())) {
                problems.add(key + " repeats level '" + levelCode + "'.");
            }
        }
    }

    private static FroggerMapLevelID getLevelId(String levelCode) {
        if (levelCode == null)
            return null;

        for (FroggerMapLevelID levelId : FroggerMapLevelID.values())
            if (levelId.getInternalName().equalsIgnoreCase(levelCode.trim()))
                return levelId;
        return null;
    }

    private static Integer parseInteger(Map<String, String> values, String name, String patchId, List<String> problems) {
        String text = values.get(name);
        if (text == null) {
            problems.add(patchId + ": '" + name + "' was not read back.");
            return null;
        }

        try {
            return Integer.decode(text.trim());
        } catch (NumberFormatException ex) {
            problems.add(patchId + ": '" + name + "' is not an integer (" + text + ").");
            return null;
        }
    }

    /**
     * Holds gameplay settings recovered from bundled patches. A null value means the patch for that key was not read.
     */
    @Getter
    public static class FroggerModGameplaySettings {
        private List<String> initialLevels;
        private List<String> visibleLevels;
        private Boolean jungleSwap;
        private Integer frameRate;
        private final List<String> problems = new ArrayList<>();
    }
}
