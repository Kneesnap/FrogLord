package net.highwayfrogs.editor.games.tgq.entity;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.kcClassID;

/**
 * Represents the 'CHoneyPotDesc' struct.
 * Created by Kneesnap on 8/22/2023.
 */
public class CHoneyPotDesc extends CItemDesc {
    private final int[] padHoneyPot = new int[8];

    @Override
    protected int getTargetClassID() {
        return kcClassID.HONEY_POT.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        for (int i = 0; i < this.padHoneyPot.length; i++)
            this.padHoneyPot[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        for (int i = 0; i < this.padHoneyPot.length; i++)
            writer.writeInt(this.padHoneyPot[i]);
    }
}
