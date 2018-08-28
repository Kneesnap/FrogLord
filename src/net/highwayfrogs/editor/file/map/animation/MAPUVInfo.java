package net.highwayfrogs.editor.file.map.animation;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the MAP_UV_INFO struct.
 * Created by Kneesnap on 8/28/2018.
 */
@Getter
public class MAPUVInfo extends GameObject {
    private ByteUV[] uvs = new ByteUV[4]; // The UVs to apply to the poly address.

    @Override
    public void load(DataReader reader) {
        int polyPointer = reader.readInt(); //TODO: Need to point to the right pointer.

        for (int i = 0; i < uvs.length; i++) {
            ByteUV uv = new ByteUV();
            uv.load(reader);
            uvs[i] = uv;
        }
    }

    @Override
    public void save(DataWriter writer) {
        //TODO: Write Pointer

        for (ByteUV uvWrite : this.uvs)
            uvWrite.save(writer);
    }
}
