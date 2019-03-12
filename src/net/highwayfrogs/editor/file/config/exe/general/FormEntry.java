package net.highwayfrogs.editor.file.config.exe.general;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents an entry in a form book.
 * Created by Kneesnap on 3/4/2019.
 */
@Getter
public class FormEntry extends GameObject {
    private int entityType; // Index into global entity book.
    private int id; // Index into theme wad.
    private int scriptId;
    private int flags;
    private long collisionReactFunction; // Form collision callback.
    private int radiusSquared; // Squared radius of bounding box.
    private FormDeathType deathType;
    private long bonusCallbackFunction; // Eaten.

    private transient MAPTheme theme;
    private transient FroggerEXEInfo config;
    private transient int globalFormId;
    private transient int localFormId;

    public static final int FLAG_GENERAL = 0x8000;
    public static final int BYTE_SIZE = (8 * Constants.INTEGER_SIZE);

    public static final int FLAG_NO_MODEL = Constants.BIT_FLAG_0; // Is a sprite, not subject to collprim collision.
    public static final int FLAG_NO_ROTATION_SNAPPING = Constants.BIT_FLAG_1; // Frog rotation is not snapped when landing on this form. (Ie: Lily pads.)
    public static final int FLAG_NO_ENTITY_ANGLE = Constants.BIT_FLAG_2; // No entity angle is calculated. (IE: Lily pads.)
    public static final int FLAG_DONT_RESET_ON_CHECKPOINT = Constants.BIT_FLAG_3; // Don't reset when a checkpoint is collected.
    public static final int FLAG_DONT_RESET_ON_DEATH = Constants.BIT_FLAG_4; // Don't reset if the frog dies.
    public static final int FLAG_THICK = Constants.BIT_FLAG_5; // Form applies beyond bottom of model to a value.
    public static final int FLAG_DONT_CENTER_X = Constants.BIT_FLAG_6; // Don't center along entity X axis, unless at end of form.
    public static final int FLAG_DONT_CENTER_Z = Constants.BIT_FLAG_7; // Don't center Z axis, unless at end of form. Used in places such as logs.
    public static final int FLAG_DONT_FADE_COLOR = Constants.BIT_FLAG_8; // Turns off color scaling for sprites. (Mainly for cave)
    public static final int FLAG_UNIT_FORM = Constants.BIT_FLAG_9; // Force form depth to 256 units.

    public FormEntry(FroggerEXEInfo config, MAPTheme theme, int formId, int globalFormId) {
        this.config = config;
        this.theme = theme;
        this.localFormId = formId;
        this.id = formId;
        this.globalFormId = globalFormId;
    }

    @Override
    public void load(DataReader reader) {
        this.entityType = reader.readInt();
        this.id = reader.readInt();
        this.scriptId = reader.readInt();
        this.flags = reader.readInt();
        this.collisionReactFunction = reader.readUnsignedIntAsLong();
        this.radiusSquared = reader.readInt();
        this.deathType = FormDeathType.values()[reader.readInt()];
        this.bonusCallbackFunction = reader.readUnsignedIntAsLong();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.entityType);
        writer.writeInt(this.id);
        writer.writeInt(this.scriptId);
        writer.writeInt(this.flags);
        writer.writeUnsignedInt(this.collisionReactFunction);
        writer.writeInt(this.radiusSquared);
        writer.writeInt(this.deathType.ordinal());
        writer.writeUnsignedInt(this.bonusCallbackFunction);
    }

    /**
     * Gets the name of the entity this represents.
     * @return entityName
     */
    public String getEntityName() {
        Config config = getConfig().getEntityBank().getConfig();
        if (config.hasChild("Override")) {
            String formName = getFormName();
            Config overrideConfig = config.getChild("Override");
            String forceEntity = overrideConfig.getString(formName, null);
            if (forceEntity != null) {
                int replaceIndex = getConfig().getEntityBank().getNames().indexOf(forceEntity);
                if (replaceIndex == -1)
                    throw new RuntimeException("Unknown replacement entity-type: '" + forceEntity + "'.");
                return getConfig().getEntityBank().getName(replaceIndex);
            }
        }

        return getConfig().getEntityBank().getName(this.entityType);
    }

    /**
     * Gets the name of this form.
     * @return formName
     */
    public String getFormName() {
        return getConfig().getFormBank().getName(this.globalFormId);
    }

    /**
     * Gets the map form id.
     * @return mapFormId
     */
    public int getMapFormId() {
        int id = this.localFormId;
        if (theme == MAPTheme.GENERAL)
            id |= FLAG_GENERAL;
        return id;
    }

    /**
     * Get the index into the wad.
     * @return wadFile
     */
    public int getWadIndex() {
        int wadIndex = getId();
        if (getTheme() == MAPTheme.GENERAL)
            wadIndex -= getTheme().getFormOffset();
        return wadIndex;
    }

    /**
     * Test if a flag is present.
     * @param flag The flag to test.
     * @return hasFlag
     */
    public boolean testFlag(int flag) {
        return (this.flags & flag) == flag;
    }
}
