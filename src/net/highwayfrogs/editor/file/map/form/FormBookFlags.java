package net.highwayfrogs.editor.file.map.form;

import net.highwayfrogs.editor.Constants;

/**
 * Flags for FormBook entries.
 * Created by Kneesnap on 8/24/2018.
 */
public class FormBookFlags {
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

    public static final int CODE_NULL_OFFSET = Integer.MIN_VALUE;
}
