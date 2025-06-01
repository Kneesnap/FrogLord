package net.highwayfrogs.editor.games.konami.greatquest.generic;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a generic resource which was unable to be processed successfully.
 * Created by Kneesnap on 10/26/2024.
 */
public class kcCResourceGenericDummy extends GameData<GreatQuestInstance> implements kcIGenericResourceData {
    @Getter private final kcCResourceGeneric resource;
    @Getter private final int tag;
    private byte[] rawData;

    public kcCResourceGenericDummy(kcCResourceGeneric resource, int tag, byte[] rawData) {
        super(resource.getGameInstance());
        this.resource = resource;
        this.tag = tag;
        this.rawData = rawData;
    }

    @Override
    public void load(DataReader reader) {
        this.rawData = reader.readBytes(reader.getRemaining());
    }

    @Override
    public void save(DataWriter writer) {
        if (this.rawData != null)
            writer.writeBytes(this.rawData);
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return null;
    }
}
