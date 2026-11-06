package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.shared.mods.ModFormatConstants;
import net.highwayfrogs.editor.games.shared.mods.ModPackage;
import net.highwayfrogs.editor.games.shared.mods.ModProblem;
import net.highwayfrogs.editor.system.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The outcome of exporting a Frogger mod package: the package, the difference it was built from, the problems found and the export log.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
public class ModExportResult {
    @NonNull private final ModPackage modPackage;
    @NonNull private final FroggerBaseDifference difference;
    @NonNull private final List<ModProblem> problems;
    @NonNull private final String exportLog;

    public ModExportResult(@NonNull ModPackage modPackage, @NonNull FroggerBaseDifference difference, @NonNull List<ModProblem> problems, @NonNull String exportLog) {
        this.modPackage = modPackage;
        this.difference = difference;
        this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
        this.exportLog = exportLog;
    }

    /**
     * Tests whether any problem is an error, meaning the package does not reproduce the open game.
     * @return true iff at least one problem is an error
     */
    public boolean hasErrors() {
        return ModProblem.hasErrors(this.problems);
    }

    /**
     * Tests whether the package holds nothing beyond [Mod], [Targets] and [Files].
     * @return true iff no content file and no Frogger section was exported
     */
    public boolean isEmpty() {
        if (!this.modPackage.getEntries().isEmpty())
            return false;

        for (Config section : this.modPackage.getManifest().getConfig().getChildConfigNodes()) {
            String name = section.getSectionName();
            if (!ModFormatConstants.SECTION_MOD.equals(name) && !ModFormatConstants.SECTION_TARGETS.equals(name) && !ModFormatConstants.SECTION_FILES.equals(name))
                return false;
        }

        return true;
    }
}
