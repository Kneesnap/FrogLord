package net.highwayfrogs.editor.games.tgq.proxy;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.kcClassID;

/**
 * Implements the 'kcProxyCapsuleDesc' struct.
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public class kcProxyCapsuleDesc extends kcProxyDesc {
    private float radius;
    private float length;
    private float offset;

    @Override
    protected int getTargetClassID() {
        return kcClassID.PROXY_CAPSULE.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.radius = reader.readFloat();
        this.length = reader.readFloat();
        this.offset = reader.readFloat();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeFloat(this.radius);
        writer.writeFloat(this.length);
        writer.writeFloat(this.offset);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Radius: ").append(this.radius).append(Constants.NEWLINE);
        builder.append(padding).append("Length: ").append(this.length).append(Constants.NEWLINE);
        builder.append(padding).append("Offset: ").append(this.offset).append(Constants.NEWLINE);
    }
}
