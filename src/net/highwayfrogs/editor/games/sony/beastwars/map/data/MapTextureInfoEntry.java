package net.highwayfrogs.editor.games.sony.beastwars.map.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * A single entry representing a texture choice which can be applied to the Beast Wars map grid.
 * Created by Kneesnap on 9/22/2023.
 */
@Setter
@Getter
public class MapTextureInfoEntry extends SCGameData<BeastWarsInstance> {
    private boolean active;
    private int textureId;
    private int flags;

    // The following are the only flags values which exist in the detail game:
    public static final int FLAGS_DEADLY_LIQUID = 70; // If the user stands on this, they will fall under the surface and be damaged.
    public static final int FLAGS_ENERGON_CRYSTAL = 71; // If the user stands on this, they will drain energon energy regardless of if they are in beast mode or not.
    public static final int FLAGS_HURT_GROUND = 94; // If the user stands on this, they will be hurt.
    // These seems to be indices NOT FLAGS into a global table with function callbacks. This must apply to something other than tile logic, and tiles are just hooking into this system, because there are way too many entries.

    public MapTextureInfoEntry(BeastWarsInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        int readStart = reader.getIndex();
        short someBoolValue = reader.readShort();
        this.textureId = reader.readInt();
        this.flags = reader.readInt();
        int unknown1Zero = reader.readInt(); // These are probably runtime pointers.
        int unknown2Zero = reader.readInt();

        if (someBoolValue == 1) {
            this.active = true;
        } else if (someBoolValue == 0) {
            this.active = false;
        } else if (getGameInstance().isPSX()) {
            // TODO: SUPPORT THIS PROPERLY ON PSX.
            System.err.println("Unexpected value for what was thought to be if the texture info entry was active. (Value: " + someBoolValue + ", Location: " + NumberUtils.toHexString(readStart) + ")");
            this.active = (someBoolValue != 0);
        } else {
            throw new RuntimeException("Unexpected value for what was thought to be if the texture info entry was active. (Value: " + someBoolValue + ", Location: " + NumberUtils.toHexString(readStart) + ")");
        }

        if (getGameInstance().isPC()) {
            // TODO: SUPPORT THIS PROPERLY ON PSX.
            if (unknown1Zero != 0)
                throw new RuntimeException("Unknown Value1 Expected to be 0 was " + unknown1Zero + " near " + NumberUtils.toHexString(readStart) + ".");
            if (unknown2Zero != 0)
                throw new RuntimeException("Unknown Value2 Expected to be 0 was " + unknown2Zero + " near " + NumberUtils.toHexString(readStart) + ".");
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort((short) (this.active ? 1 : 0));
        writer.writeInt(this.textureId);
        writer.writeInt(this.flags);
        writer.writeInt(0);
        writer.writeInt(0);
    }
}