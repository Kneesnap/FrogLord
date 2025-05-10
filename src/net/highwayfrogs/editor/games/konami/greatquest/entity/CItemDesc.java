package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.ProxyReact;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyCapsuleDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc.kcCollisionGroup;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the 'CItemDesc' struct.
 * Loaded by 'CItem::Init'
 * This has a hardcoded collision proxy, also created in CItem::Init. This explains why the items like the goblet in The Lost Trail Ruins have such small hitboxes, and how coins setup their hitboxes.
 * Created by Kneesnap on 8/21/2023.
 */
public class CItemDesc extends kcActorBaseDesc {
    private static final int VALUES_ALWAYS_ZERO = 3; // value, properties, attributes
    private static final int PADDING_VALUES = 32;

    // ALL CItem instances are overridden to use this setup.
    public static final kcProxyCapsuleDesc ITEM_CAPSULE_DESCRIPTION;

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
    public kcProxyCapsuleDesc getCollisionProxyDescription() {
        return ITEM_CAPSULE_DESCRIPTION; // These may be an item assigned, but this one is hardcoded to be used by the game instead.
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.ITEM_DESCRIPTION;
    }

    static {
        ITEM_CAPSULE_DESCRIPTION = new kcProxyCapsuleDesc(null);
        // Seen in CItem::Init
        ITEM_CAPSULE_DESCRIPTION.setReaction(ProxyReact.NOTIFY);
        ITEM_CAPSULE_DESCRIPTION.setCollisionGroup(kcCollisionGroup.ITEM.getBitMask());
        ITEM_CAPSULE_DESCRIPTION.setCollideWith(kcCollisionGroup.PLAYER.getBitMask());
        ITEM_CAPSULE_DESCRIPTION.setRadius(.35F);
        ITEM_CAPSULE_DESCRIPTION.setLength(0F);
        ITEM_CAPSULE_DESCRIPTION.setOffset(-.35F);
    }
}