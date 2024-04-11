package net.highwayfrogs.editor.games.greatquest;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * A TGQ dummy file.
 * Created by Kneesnap on 8/17/2019.
 */
@Getter
public class TGQDummyFile extends TGQFile {
    private byte[] data;
    private final int length;

    public TGQDummyFile(TGQBinFile mainArchive, int length) {
        super(mainArchive);
        this.length = length;
    }

    @Override
    public void load(DataReader reader) {
        this.data = reader.readBytes(this.length);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(this.data);
    }

    @Override
    public String getExtension() {
        if (this.data.length >= 3) {
            String useString = new String(new byte[]{data[0], data[1], data[2]});
            if (Utils.isAlphanumeric(useString))
                return useString;
        }

        return super.getExtension();
    }

    @Override
    public String getDefaultFolderName() {
        return "Dummy";
    }
}