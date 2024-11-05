package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;

/**
 * Represents the 'CPropDesc' struct.
 * Loaded by CProp::Init.
 * Props have gravity disabled.
 * Created by Kneesnap on 8/21/2023.
 */
public class CPropDesc extends kcActorBaseDesc {
    private static final int PADDING_VALUES = 64;
    private static final int EVENT_VALUE = -1; // Event is a hash of an event name to trigger when a hit occurs. (CProp::TRiggerHitCallback) But it never appears to be used since why would we want to trigger an event on hit when we can use scripts instead.

    public CPropDesc(@NonNull kcCResourceGeneric resource) {
        super(resource);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // mode - Seems to always be zero, and it doesn't appear to be used by the code.
        int event = reader.readInt();
        if (event != EVENT_VALUE)
            throw new RuntimeException("Expected 'event' value to always be -1, but was " + event + "!");

        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(0); // mode - Seems to always be zero.
        writer.writeInt(EVENT_VALUE); // event - Seems to always be -1.
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.PROP.getClassId();
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.PROP_DESCRIPTION;
    }
}