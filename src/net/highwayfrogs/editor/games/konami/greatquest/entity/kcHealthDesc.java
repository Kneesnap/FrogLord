package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the kcHealthDesc struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcHealthDesc extends GameObject implements IMultiLineInfoWriter {
    private int durability;
    private int startHealth;
    private int immuneMask;

    @Override
    public void load(DataReader reader) {
        this.durability = reader.readInt();
        this.startHealth = reader.readInt();
        this.immuneMask = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.durability);
        writer.writeInt(this.startHealth);
        writer.writeInt(this.immuneMask);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Durability: ").append(this.durability).append(Constants.NEWLINE);
        builder.append(padding).append("Start Health: ").append(this.startHealth).append(Constants.NEWLINE);
        builder.append(padding).append("Immune Mask: ").append(Utils.toHexString(this.immuneMask)).append(Constants.NEWLINE);
    }
}