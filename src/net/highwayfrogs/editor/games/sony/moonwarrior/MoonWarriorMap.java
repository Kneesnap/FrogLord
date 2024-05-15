package net.highwayfrogs.editor.games.sony.moonwarrior;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;

/**
 * Implements a MoonWarrior map.
 * Created by Kneesnap on 5/7/2024.
 */
public class MoonWarriorMap extends SCMapFile<MoonWarriorInstance> {
    @Getter private final MoonWarriorMapEntityPacket moonWarriorEntityPacket;
    private MoonWarriorLevelTableEntry cachedLevelTableEntry;

    public MoonWarriorMap(MoonWarriorInstance instance) {
        super(instance);
        addFilePacket(this.moonWarriorEntityPacket = new MoonWarriorMapEntityPacket(this));
    }

    @Override
    public MoonWarriorMap getParentMap() {
        // MoonWarrior maps are not split up.
        return null;
    }

    @Override
    public ISCLevelTableEntry getLevelTableEntry() {
        if (this.cachedLevelTableEntry == null)
            this.cachedLevelTableEntry = new MoonWarriorLevelTableEntry(this);

        return this.cachedLevelTableEntry;
    }
}