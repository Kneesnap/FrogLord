package net.highwayfrogs.editor.games.konami.greatquest.math;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;

/**
 * This represents the '_kcBox4' struct in kcMath3D.h
 * NOTE: The 'kcBox4' type in the exported header file uses kcVector3. Not sure why, as ghidra sees it as a kcVector4.
 * Created by Kneesnap on 7/12/2023.
 */
public class kcBox4 extends GameObject implements IMultiLineInfoWriter {
    private kcVector4 min;
    private kcVector4 max;

    @Override
    public void load(DataReader reader) {
        if (this.min == null)
            this.min = new kcVector4();
        this.min.load(reader);
        if (this.max == null)
            this.max = new kcVector4();
        this.max.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.min.save(writer);
        this.max.save(writer);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        this.min.writePrefixedInfoLine(builder, "Box Min", padding);
        this.max.writePrefixedInfoLine(builder, "Box Max", padding);
    }
}