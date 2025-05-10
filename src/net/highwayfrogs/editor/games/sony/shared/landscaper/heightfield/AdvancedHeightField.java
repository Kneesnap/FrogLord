package net.highwayfrogs.editor.games.sony.shared.landscaper.heightfield;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.landscaper.Landscape;
import net.highwayfrogs.editor.games.sony.shared.landscaper.SCWorldGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a height-field with advanced editing capabilities.
 * Used for Frogger map editing and potentially other games too.
 * Created by Kneesnap on 7/16/2024.
 */
public class AdvancedHeightField extends SCWorldGrid<AdvancedHeightFieldVertexColumn> {
    @Getter private final Landscape landscape;

    public AdvancedHeightField(SCGameInstance instance, Landscape landscape, Class<? extends AdvancedHeightFieldVertexColumn> entryClass) {
        super(instance, entryClass);
        this.landscape = landscape;
    }

    public AdvancedHeightField(SCGameInstance instance, Landscape landscape, Class<? extends AdvancedHeightFieldVertexColumn> entryClass, int xSquareCount, int zSquareCount, float xSquareSize, float zSquareSize) {
        super(instance, entryClass, xSquareCount, zSquareCount, xSquareSize, zSquareSize);
        this.landscape = landscape;
    }

    @Override
    protected AdvancedHeightFieldVertexColumn createNewGridEntry(int x, int z) {
        return new AdvancedHeightFieldVertexColumn(this, x, z);
    }

    @Override
    protected void readDependantGridData(DataReader reader) {
        // Don't need to read anything by default.
    }

    @Override
    protected void writeDependantGridData(DataWriter writer) {
        // Don't need to write anything by default.
    }
}
