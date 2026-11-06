package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.file.patch.PatchWriteRecorder.PatchWriteRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The classified differences between an open Frogger game and the base fingerprint of its build, used by mod export and install.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
public class FroggerBaseDifference {
    private final String baseBuild;
    private final String currentBuild;
    private final FroggerBaseSectionDifference files = new FroggerBaseSectionDifference("Files", "file");
    private final FroggerBaseSectionDifference wadChildren = new FroggerBaseSectionDifference("WadChildren", "WAD child");
    private final List<String> dummiedWadChildren = new ArrayList<>();
    private final FroggerBaseSectionDifference images = new FroggerBaseSectionDifference("Images", "image");
    private final FroggerBaseSectionDifference levelSelect = new FroggerBaseSectionDifference("LevelSelect", "arcade row");
    private final FroggerBaseSectionDifference raceSelect = new FroggerBaseSectionDifference("RaceSelect", "race row");
    private final FroggerBaseSectionDifference demos = new FroggerBaseSectionDifference("Demos", "demo row");
    private final FroggerBaseSectionDifference themes = new FroggerBaseSectionDifference("Themes", "theme");
    private final FroggerBaseSectionDifference remaps = new FroggerBaseSectionDifference("Remaps", "remap");
    private final FroggerBaseSectionDifference ownedRegions = new FroggerBaseSectionDifference("ExeOwned", "FrogLord-owned executable region");
    private final FroggerBaseSectionDifference patchSites = new FroggerBaseSectionDifference("PatchSites", "bundled patch");
    private final List<FroggerExeBlockChange> exeBlocks = new ArrayList<>();
    @Setter private boolean arcadeOrderChanged;
    @Setter private boolean raceOrderChanged;
    @Setter private int baseExecutableLength;
    @Setter private int currentExecutableLength;

    public FroggerBaseDifference(String baseBuild, String currentBuild) {
        this.baseBuild = baseBuild;
        this.currentBuild = currentBuild;
    }

    /**
     * Tests whether the build names of the base and the current game disagree.
     * @return buildMismatch
     */
    public boolean isBuildMismatch() {
        return this.baseBuild != null && !this.baseBuild.equals(this.currentBuild);
    }

    /**
     * Gets the executable blocks which differ from the base outside both the FrogLord-owned regions and the recorded patch sites.
     * @return unexplainedBlocks
     */
    public List<FroggerExeBlockChange> getUnexplainedExeBlocks() {
        List<FroggerExeBlockChange> results = new ArrayList<>();
        for (FroggerExeBlockChange change : this.exeBlocks)
            if (!change.isExplainedByPatchSites())
                results.add(change);
        return results;
    }

    /**
     * Tests whether nothing differs: no section has a change and only dummied WAD children (unused models) may be present.
     * Dummied children are not changes; they are reported separately.
     * @return empty
     */
    public boolean isEmpty() {
        return !isBuildMismatch() && this.files.isEmpty() && this.wadChildren.isEmpty() && this.images.isEmpty()
                && this.levelSelect.isEmpty() && this.raceSelect.isEmpty() && !this.arcadeOrderChanged && !this.raceOrderChanged
                && this.demos.isEmpty() && this.themes.isEmpty() && this.remaps.isEmpty() && this.ownedRegions.isEmpty()
                && this.patchSites.isEmpty() && this.exeBlocks.isEmpty() && this.baseExecutableLength == this.currentExecutableLength;
    }

    /**
     * Describes every difference, one headline per section followed by its items indented by two spaces.
     * @return lines
     */
    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        if (isBuildMismatch())
            lines.add("The base fingerprint is for '" + this.baseBuild + "', but the open game is '" + this.currentBuild + "'.");

        this.files.describe(lines);
        this.wadChildren.describe(lines);
        if (!this.dummiedWadChildren.isEmpty()) {
            lines.add("WadChildren: " + this.dummiedWadChildren.size() + " dummied (base model now DUMY, classified as unused, not as changed)");
            for (String key : this.dummiedWadChildren)
                lines.add("  dummied " + key);
        }

        this.images.describe(lines);
        this.levelSelect.describe(lines);
        if (this.arcadeOrderChanged)
            lines.add("LevelSelect: the order of the arcade rows changed");
        this.raceSelect.describe(lines);
        if (this.raceOrderChanged)
            lines.add("RaceSelect: the order of the race rows changed");
        this.demos.describe(lines);
        this.themes.describe(lines);
        this.remaps.describe(lines);
        this.ownedRegions.describe(lines);
        this.patchSites.describe(lines);

        if (this.baseExecutableLength != this.currentExecutableLength)
            lines.add("ExeBlocks: the executable (without its FrogLord trailer) is " + this.currentExecutableLength + " bytes, the base is " + this.baseExecutableLength);

        if (!this.exeBlocks.isEmpty()) {
            int unexplained = getUnexplainedExeBlocks().size();
            lines.add("ExeBlocks: " + this.exeBlocks.size() + " changed outside FrogLord-owned regions, " + (this.exeBlocks.size() - unexplained)
                    + " explained by bundled patch sites, " + unexplained + " unexplained");
            for (FroggerExeBlockChange change : this.exeBlocks)
                lines.add("  " + change);
        }

        if (lines.isEmpty())
            lines.add("No differences from the base fingerprint of " + this.baseBuild + ".");
        return lines;
    }

    /**
     * Gets the headline lines of describe(), for a short summary.
     * @return summaryLines
     */
    public List<String> getSummaryLines() {
        List<String> lines = new ArrayList<>();
        for (String line : describe())
            if (!line.startsWith("  "))
                lines.add(line);
        return lines;
    }

    /**
     * The changed, added and removed keys of one fingerprint section.
     */
    @Getter
    public static class FroggerBaseSectionDifference {
        private final String sectionName;
        private final String itemName;
        private final List<String> changed = new ArrayList<>();
        private final List<String> added = new ArrayList<>();
        private final List<String> removed = new ArrayList<>();

        public FroggerBaseSectionDifference(@NonNull String sectionName, @NonNull String itemName) {
            this.sectionName = sectionName;
            this.itemName = itemName;
        }

        /**
         * Tests whether the section has no difference.
         * @return empty
         */
        public boolean isEmpty() {
            return this.changed.isEmpty() && this.added.isEmpty() && this.removed.isEmpty();
        }

        /**
         * Gets every key that differs in any way, changed first, then removed, then added.
         * @return keys
         */
        public List<String> getAllKeys() {
            List<String> keys = new ArrayList<>(this.changed);
            keys.addAll(this.removed);
            keys.addAll(this.added);
            return Collections.unmodifiableList(keys);
        }

        private void describe(List<String> lines) {
            if (isEmpty())
                return;

            lines.add(this.sectionName + ": " + this.changed.size() + " changed, " + this.removed.size() + " removed, " + this.added.size() + " added (" + this.itemName + ")");
            for (String key : this.changed)
                lines.add("  changed " + key);
            for (String key : this.removed)
                lines.add("  removed " + key);
            for (String key : this.added)
                lines.add("  added " + key);
        }
    }

    /**
     * A 1 KiB executable block whose bytes outside the FrogLord-owned regions differ from the base.
     */
    @Getter
    public static class FroggerExeBlockChange {
        private final PatchWriteRange block;
        private final boolean explainedByPatchSites;
        private final List<String> patchIds;

        public FroggerExeBlockChange(@NonNull PatchWriteRange block, boolean explainedByPatchSites, @NonNull List<String> patchIds) {
            this.block = block;
            this.explainedByPatchSites = explainedByPatchSites;
            this.patchIds = Collections.unmodifiableList(new ArrayList<>(patchIds));
        }

        @Override
        public String toString() {
            String range = String.format("0x%08X-0x%08X", this.block.getAddress(), this.block.getEndAddress() - 1);
            if (this.explainedByPatchSites)
                return range + " explained by patch sites of " + String.join(", ", this.patchIds);
            if (!this.patchIds.isEmpty())
                return range + " unexplained (holds patch sites of " + String.join(", ", this.patchIds) + ", but also differs outside them)";
            return range + " unexplained";
        }
    }
}
