package net.highwayfrogs.editor.games.sony.shared.vlo2;

import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

/**
 * This does not map onto any known concepts in the original MR API / Vorg, but exist in FrogLord to simplify cross-platform texture importing for the user.
 * Since padding differs per-platform, it's desirable to allow having a way to choose padding behavior regardless of platform.
 * This enum represents those discrete choices for cross-platform padding behavior.
 * Created by Kneesnap on 1/27/2026.
 */
public enum VloPadding {
    // No padding -> Null.
    NONE, MINIMUM, DEFAULT, MAXIMUM;

    private static final int[] OLD_FROGGER_SIZES = {0, 2, 2}; // There's a question of if "MINIMUM" should contain any padding at all.
    private static final int[] FROGGER_PSX_SIZES = {2, 2, 2};
    private static final int[] FROGGER_PC_SIZES = {2, 4, 8};
    private static final int[] FROGGER_PC_PROTOTYPE_SIZES = {2, 4, 4};
    private static final int[] BEAST_WARS_PC_SIZES = {2, 4, 4};
    private static final int[] DEFAULT_PSX_SIZES = {1, 2, 2};

    /**
     * Get the padding amount to use for the given vlo file.
     * @param vloFile the vlo file to get the padding amount for
     * @return paddingAmount
     */
    public int getPaddingAmount(VloFile vloFile) {
        if (vloFile == null)
            throw new NullPointerException("vloFile");
        if (this == NONE)
            return 0;

        SCGameInstance instance = vloFile.getGameInstance();
        boolean psxMode = vloFile.isPsxMode();

        switch (instance.getGameType()) {
            case OLD_FROGGER:
                if (psxMode) { // Old Frogger PSX: 0 or 2.
                    // Everything except some water particles in MULTIPLAYER1.VLO and ID71 unit ruler in FRONT_0.ULR (an outdated VLO), AND everything in FIXED.ULR have padding.
                    return getTexturePaddingAmount(this, OLD_FROGGER_SIZES);
                } else { // Old Frogger PC: 0 or 2.
                    // Very inconsistent, I don't have any clear patterns to describe other than this returns either 0 or 2.
                    return getTexturePaddingAmount(this, OLD_FROGGER_SIZES);
                }
            case FROGGER:
                // Rough explanation:
                // Terrain & Texture Remaps: 8
                // Bugs, Level Select Images: 4
                // Text: 2

                FroggerGameInstance frogger = (FroggerGameInstance) instance;
                if (psxMode) { // Frogger PSX: 0 or 2.
                    return getTexturePaddingAmount(this, FROGGER_PSX_SIZES);
                } else if (frogger.getVersionConfig().isAtLeastRetailWindows()) { // Frogger Windows: 0, 2, 4, 8
                    // I suspect the 8 padding value was added to the game for Bi-linear filtering to ensure it cannot possibly show an outline on the edge of a texture.
                    // This is why the prototype builds don't have padding=8, since they either didn't have bi-linear filtering or didn't see the need to fix the issue until late.
                    return getTexturePaddingAmount(this, FROGGER_PC_SIZES);
                } else { // Frogger Windows: 0, 2, 4
                    // Confirmed: PC Alpha (June), Windows Build 1 (July), Kao's Prototype (September 3)
                    return getTexturePaddingAmount(this, FROGGER_PC_PROTOTYPE_SIZES);
                }
            case BEAST_WARS: // Beast Wars PC: 0, 2, 4, Nearly the entirety of the game uses padding=0 here though.
                if (psxMode) {
                    return getTexturePaddingAmount(this, DEFAULT_PSX_SIZES);
                } else {
                    return getTexturePaddingAmount(this, BEAST_WARS_PC_SIZES);
                }
            // None of these games have PC ports,
            case MEDIEVIL: // MediEvil: 0, 1, or 2.
            case MOONWARRIOR: // MoonWarrior: 0, 1, or 2. (1 is never seen in vanilla)
            case MEDIEVIL2: // MediEvil II: 0, 1, or 2.
            case C12: // C-12 Final Resistance: 0, 1, or 2.
            default:
                return getTexturePaddingAmount(this, DEFAULT_PSX_SIZES);
        }
    }

    private static int getTexturePaddingAmount(VloPadding padding, int[] paddingArray) {
        if (paddingArray.length != values().length - 1)
            throw new IllegalArgumentException("paddingArray contained " + paddingArray.length + " values when it was expected to contain " + (values().length - 1) + "!");

        return paddingArray[padding.ordinal() - 1];
    }
}
