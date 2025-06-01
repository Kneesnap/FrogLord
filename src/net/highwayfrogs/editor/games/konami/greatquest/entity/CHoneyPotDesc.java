package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the 'CHoneyPotDesc' struct.
 * CHoneyPot::Init is what loads this, but there's no data actually used.
 * Created by Kneesnap on 8/22/2023.
 */
public class CHoneyPotDesc extends CItemDesc {
    private static final int PADDING_VALUES = 8;

    public CHoneyPotDesc(@NonNull kcCResourceGeneric resource) {
        super(resource, kcEntityDescType.HONEY_POT);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }
}