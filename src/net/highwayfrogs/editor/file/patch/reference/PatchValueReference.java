package net.highwayfrogs.editor.file.patch.reference;

import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;

/**
 * Represents a reference to a PatchValue.
 * Created by Kneesnap on 1/15/2020.
 */
public interface PatchValueReference {

    /**
     * Gets the value from the runtime.
     * @param runtime The runtime to get the value from.
     * @return value
     */
    public PatchValue getValue(PatchRuntime runtime);

}
