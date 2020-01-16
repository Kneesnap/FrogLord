package net.highwayfrogs.editor.file.patch.reference;

import lombok.AllArgsConstructor;
import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;

/**
 * A wrapper around a PatchValue.
 * Created by Kneesnap on 1/15/2020.
 */
@AllArgsConstructor
public class PatchWrapperValue implements PatchValueReference {
    private PatchValue value;

    @Override
    public PatchValue getValue(PatchRuntime runtime) {
        return this.value;
    }
}
