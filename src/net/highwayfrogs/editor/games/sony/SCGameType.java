package net.highwayfrogs.editor.games.sony;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MedievilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil2.Medievil2GameInstance;

import java.util.function.Supplier;

/**
 * A registry of different game types supported by FrogLord by Sony Cambridge.
 * Created by Kneesnap on 9/6/2023.
 */
@Getter
@AllArgsConstructor
public enum SCGameType {
    //HEADRUSH(null), // Aka Brains In Planes
    OLD_FROGGER(null),
    BEAST_WARS(BeastWarsInstance::new),
    FROGGER(FroggerGameInstance::new),
    //TAX_MAN(null),
    MEDIEVIL(MedievilGameInstance::new),
    //COMMON_TALES(null),
    MOONWARRIOR(null),
    MEDIEVIL2(Medievil2GameInstance::new),
    C12(null);

    private final Supplier<SCGameInstance> instanceMaker;

    /**
     * Test if this game was developed at or after the provided game.
     * This may not work perfectly with prototypes, but the general use of this is testing technical capabilities.
     * @param otherType The other game to test.
     * @return True iff the game was developed at or after the provided game.
     */
    public boolean isAtLeast(SCGameType otherType) {
        if (otherType == null)
            throw new RuntimeException("Cannot compare to null game type.");
        return ordinal() >= otherType.ordinal();
    }

    /**
     * Test if this game was developed before the provided game.
     * This may not work perfectly with prototypes, but the general use of this is testing technical capabilities.
     * @param otherType The other game to test.
     * @return True iff the game was developed before the provided game.
     */
    public boolean isBefore(SCGameType otherType) {
        if (otherType == null)
            throw new RuntimeException("Cannot compare to null game type.");
        return otherType.ordinal() > ordinal();
    }

    /**
     * Creates a new instance of the game.
     */
    public SCGameInstance createInstance() {
        return this.instanceMaker != null ? this.instanceMaker.get() : new SCGameUnimplemented(this);
    }

    /**
     * Check if the MWI contains a file checksum.
     */
    public boolean doesMwiHaveChecksum() {
        return isAtLeast(MEDIEVIL2);
    }
}