package net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;

/**
 * Represents "MR_PART_POLY_ANIMLIST_ENTRY".
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MRMofTextureAnimationEntry implements IBinarySerializable {
    private short globalImageId; // Within .TXL, resolved to global image ID.
    private int duration = 1; // The number of game cycles (frames) which the texture will be shown for.

    @Override
    public void load(DataReader reader) {
        this.globalImageId = reader.readShort();
        this.duration = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.globalImageId);
        writer.writeUnsignedShort(this.duration);
    }
}
