package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.*;

/**
 * The choices an author makes when exporting a Frogger mod package: which detected changes to leave out, whether to package raw
 * executable writes, and the level names to write.
 * Created by jimbolina on 11/6/2026.
 */
public class FroggerModExportOptions {
    private final Map<String, Set<String>> excludedChanges = new LinkedHashMap<>();
    private final Map<String, String> levelNames = new LinkedHashMap<>();
    @Getter @Setter private boolean includeRawExecutableWrites;

    /**
     * Leaves a detected change out of the package.
     * @param section the FroggerBaseDifference section holding the change, such as "Images" or "Files"
     * @param key the key of the change in that section, as FroggerBaseDifference lists it, such as "LS_ALL:org1pic"
     */
    public void excludeChange(@NonNull String section, @NonNull String key) {
        this.excludedChanges.computeIfAbsent(section, name -> new LinkedHashSet<>()).add(key);
    }

    /**
     * Puts a change that was left out back into the package.
     * @param section the FroggerBaseDifference section holding the change
     * @param key the key of the change in that section
     */
    public void includeChange(@NonNull String section, @NonNull String key) {
        Set<String> keys = this.excludedChanges.get(section);
        if (keys != null && keys.remove(key) && keys.isEmpty())
            this.excludedChanges.remove(section);
    }

    /**
     * Tests whether a detected change is left out of the package.
     * @param section the FroggerBaseDifference section holding the change
     * @param key the key of the change in that section
     * @return true iff the author left the change out
     */
    public boolean isExcluded(String section, String key) {
        Set<String> keys = this.excludedChanges.get(section);
        return keys != null && keys.contains(key);
    }

    /**
     * Gets the left-out changes, keyed by section.
     * @return excludedChanges
     */
    public Map<String, Set<String>> getExcludedChanges() {
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : this.excludedChanges.entrySet())
            copy.put(entry.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(entry.getValue())));
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Sets the level name the author gives a level, written as name= of its [LevelSelect] child.
     * @param levelCode the level code, such as ORG1
     * @param name the level name as text, or null to write none
     */
    public void setLevelName(@NonNull String levelCode, String name) {
        if (name == null || name.trim().isEmpty()) {
            this.levelNames.remove(levelCode);
        } else {
            this.levelNames.put(levelCode, name.trim());
        }
    }

    /**
     * Gets the level name the author gave a level.
     * @param levelCode the level code
     * @return levelName, or null when the author gave none
     */
    public String getLevelName(String levelCode) {
        return this.levelNames.get(levelCode);
    }

    /**
     * Gets every level name the author gave, keyed by level code.
     * @return levelNames
     */
    public Map<String, String> getLevelNames() {
        return Collections.unmodifiableMap(this.levelNames);
    }
}
