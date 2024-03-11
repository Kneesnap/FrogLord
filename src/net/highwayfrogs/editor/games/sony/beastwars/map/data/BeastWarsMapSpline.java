package net.highwayfrogs.editor.games.sony.beastwars.map.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.games.sony.shared.spline.MRBezierCurve;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a spline entry in a Beast Wars map.
 * Created by Kneesnap on 9/22/2023.
 */
@Getter
public class BeastWarsMapSpline extends SCGameData<BeastWarsInstance> {
    private final BeastWarsMapFile mapFile;
    private final List<MRBezierCurve> bezierCurves = new ArrayList<>();

    public BeastWarsMapSpline(BeastWarsMapFile mapFile) {
        super(mapFile.getGameInstance());
        this.mapFile = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        int bezierCurveCount = reader.readUnsignedShortAsInt();

        this.bezierCurves.clear();
        for (int i = 0; i < bezierCurveCount; i++) {
            MRBezierCurve curve = new MRBezierCurve(getGameInstance());
            curve.load(reader);
            this.bezierCurves.add(curve);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.bezierCurves.size());

        // Write curves
        for (int i = 0; i < this.bezierCurves.size(); i++)
            this.bezierCurves.get(i).save(writer);
    }
}