package net.highwayfrogs.editor.file.map.form;

/**
 * Flags for FormBook entries.
 * Created by Kneesnap on 8/24/2018.
 */
public class FormBookFlags {
    public static final int FLAG_NO_MODEL = 1; // Is a sprite, not subject to collprim collision.
    public static final int FLAG_NO_ROTATION_SNAPPING = 2; // Frog rotation is not snapped when landing on this form. (Ie: Lily pads.)
    public static final int FLAG_NO_ENTITY_ANGLE = 4; // No entity angle is calculated. (IE: Lily pads.)
    public static final int FLAG_DONT_RESET_ON_CHECKPOINT = 8; // Don't reset when a checkpoint is collected.
    public static final int FLAG_DONT_RESET_ON_DEATH = 16; // Don't reset if the frog dies.
    public static final int FLAG_THICK = 32; // Form applies beyond bottom of model to a value.
    public static final int FLAG_DONT_CENTER_X = 64; // Don't center along entity X axis, unless at end of form.
    public static final int FLAG_DONT_CENTER_Z = 128; // Don't center Z axis, unless at end of form. Used in places such as logs.
    public static final int FLAG_DONT_FADE_COLOR = 256; // Turns off color scaling for sprites. (Mainly for cave)
    public static final int FLAG_UNIT_FORM = 512; // Force form depth to 256 units.

    public static final int CODE_NULL_OFFSET = Integer.MIN_VALUE;
}
