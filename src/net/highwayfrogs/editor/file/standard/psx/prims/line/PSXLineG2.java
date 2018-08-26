package net.highwayfrogs.editor.file.standard.psx.prims.line;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Not really used in the retail game, I think at one point it was for debugging.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class PSXLineG2 extends PSXLine {
    private PSXColorVector color1 = new PSXColorVector();
    private PSXColorVector color2 = new PSXColorVector();

    public PSXLineG2() {
        super(2);
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
