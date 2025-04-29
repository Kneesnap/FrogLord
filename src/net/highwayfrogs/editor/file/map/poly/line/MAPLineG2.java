package net.highwayfrogs.editor.file.map.poly.line;

import lombok.Getter;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Not really used in the retail game, I think at one point it was for debugging.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MAPLineG2 extends MAPLine {
    private PSXColorVector color1 = new PSXColorVector();
    private PSXColorVector color2 = new PSXColorVector();

    public MAPLineG2() {
        super(MAPLineType.G2, 2);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.color1.load(reader);
        this.color2.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        this.color1.save(writer);
        this.color2.save(writer);
    }
}
