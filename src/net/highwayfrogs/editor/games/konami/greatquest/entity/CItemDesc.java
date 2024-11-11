package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;

/**
 * Represents the 'CItemDesc' struct.
 * Loaded by 'CItem::Init'
 * This has a hardcoded collision proxy, also created in CItem::Init. This explains why the items like the goblet in The Lost Trail Ruins have such small hitboxes.
 * Additionally, it may also explain why coins have weird hitboxes when viewed in editor.
 * TODO: Communicate this in FrogLord.
 * Created by Kneesnap on 8/21/2023.
 */
public class CItemDesc extends kcActorBaseDesc {
    private static final int VALUES_ALWAYS_ZERO = 3; // value, properties, attributes
    private static final int PADDING_VALUES = 32;

    public CItemDesc(@NonNull kcCResourceGeneric resource, kcEntityDescType descType) {
        super(resource, descType);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        reader.skipBytesRequireEmpty(VALUES_ALWAYS_ZERO * Constants.INTEGER_SIZE);
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeNull(VALUES_ALWAYS_ZERO * Constants.INTEGER_SIZE);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.ITEM_DESCRIPTION;
    }
}