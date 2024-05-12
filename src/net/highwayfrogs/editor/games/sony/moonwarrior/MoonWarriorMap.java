package net.highwayfrogs.editor.games.sony.moonwarrior;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygonPacket;
import net.highwayfrogs.editor.gui.GameUIController;

/**
 * Implements a MoonWarrior map.
 * Created by Kneesnap on 5/7/2024.
 */
public class MoonWarriorMap extends SCMapFile<MoonWarriorInstance> {
    @Getter private final SCMapPolygonPacket<MoonWarriorInstance> polygonPacket;
    private MoonWarriorLevelTableEntry cachedLevelTableEntry;

    public MoonWarriorMap(MoonWarriorInstance instance) {
        super(instance);
        addFilePacket(this.polygonPacket = new MoonWarriorPolygonPacket(this));
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return loadEditor(getGameInstance(), new MoonWarriorMapUIController(getGameInstance()), this);
    }

    @Override
    public ISCLevelTableEntry getLevelTableEntry() {
        if (this.cachedLevelTableEntry == null)
            this.cachedLevelTableEntry = new MoonWarriorLevelTableEntry(this);

        return this.cachedLevelTableEntry;
    }

    public static class MoonWarriorPolygonPacket extends SCMapPolygonPacket<MoonWarriorInstance> {
        public MoonWarriorPolygonPacket(SCMapFile<MoonWarriorInstance> parentFile) {
            super(parentFile);
        }

        @Override
        public SCMapPolygon createPolygon() {
            return new SCMapPolygon(getParentFile());
        }
    }
}