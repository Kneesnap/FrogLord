package net.highwayfrogs.editor.file.patch.reference;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;

/**
 * Gets the variable value.
 * Created by Kneesnap on 1/15/2020.
 */
@Getter
@AllArgsConstructor
public class PatchTextReference implements PatchValueReference {
    private String textData;

    @Override
    public PatchValue getValue(PatchRuntime runtime) {
        return runtime.getVariables().get(this.textData);
    }
}
