package net.highwayfrogs.editor.games.sony.moonwarrior;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygonPacket;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Implements a MoonWarrior map.
 * Created by Kneesnap on 5/7/2024.
 */
public class MoonWarriorMap extends SCMapFile<MoonWarriorInstance> {
    @Getter private final SCMapPolygonPacket<MoonWarriorInstance> polygonPacket;
    @Getter private final MoonWarriorMapEntityPacket entityPacket;
    private MoonWarriorLevelTableEntry cachedLevelTableEntry;

    public MoonWarriorMap(MoonWarriorInstance instance) {
        super(instance);
        addFilePacket(this.polygonPacket = new SCMapPolygonPacket<>(this));
        addFilePacket(this.entityPacket = new MoonWarriorMapEntityPacket(this));
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        if (this.entityPacket != null && this.entityPacket.isActive())
            propertyList.add("Entity Count", this.entityPacket.getEntities().size());
        return propertyList;
    }

    @Override
    public ISCLevelTableEntry getLevelTableEntry() {
        if (this.cachedLevelTableEntry == null)
            this.cachedLevelTableEntry = new MoonWarriorLevelTableEntry(this);

        return this.cachedLevelTableEntry;
    }
}