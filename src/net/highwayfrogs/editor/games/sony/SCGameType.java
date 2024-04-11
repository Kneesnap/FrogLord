package net.highwayfrogs.editor.games.sony;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2GameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * A registry of different game types supported by FrogLord by Sony Cambridge.
 * Created by Kneesnap on 9/6/2023.
 */
@Getter
public enum SCGameType implements IGameType {
    //HEADRUSH(null), // Aka Brains In Planes
    OLD_FROGGER(OldFroggerGameInstance::new),
    BEAST_WARS(BeastWarsInstance::new),
    FROGGER(FroggerGameInstance::new),
    //TAX_MAN(null),
    MEDIEVIL(MediEvilGameInstance::new),
    //COMMON_TALES(null),
    MOONWARRIOR(null),
    MEDIEVIL2(MediEvil2GameInstance::new),
    C12(null);

    private final Supplier<SCGameInstance> instanceMaker;
    private final String identifier;

    SCGameType(Supplier<SCGameInstance> instanceMaker) {
        this.instanceMaker = instanceMaker;
        this.identifier = name().toLowerCase(Locale.ROOT).replace("_", "");
    }

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

    @Override
    public String getDisplayName() {
        return Utils.capitalize(name());
    }

    @Override
    public SCGameInstance createGameInstance() {
        return this.instanceMaker != null ? this.instanceMaker.get() : new SCGameUnimplemented(this);
    }

    /**
     * Check if the MWI contains a file checksum.
     */
    public boolean doesMwiHaveChecksum() {
        return isAtLeast(MEDIEVIL2);
    }
}