package net.highwayfrogs.editor.games.sony.beastwars.map.data;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.psx.math.vector.CVector;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents Beast Wars map lighting data.
 * The current guess is that this is for entities.
 * Created by Kneesnap on 3/5/2025.
 */
@Getter
public class BeastWarsMapLight extends SCGameData<BeastWarsInstance> {
    private final CVector color = new CVector();
    private final SVector direction = new SVector(); // The first light is hardcoded as ambient (Direction is zero).

    public static final int SIZE_IN_BYTES = CVector.BYTE_LENGTH + SVector.PADDED_BYTE_SIZE;

    public BeastWarsMapLight(BeastWarsInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.color.load(reader);
        this.direction.loadWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.color.save(writer);
        this.direction.saveWithPadding(writer);
    }

    /**
     * Test if the map light is currently active/would have an impact on the map rendering.
     */
    public boolean isActive() {
        return this.color.getRed() != 0 || this.color.getGreen() != 0 || this.color.getBlue() != 0;
    }
}