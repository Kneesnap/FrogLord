package net.highwayfrogs.editor.games.shared.coupled;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameObject.SharedGameObject;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

/**
 * Represents two objects who depend on each other to load successfully.
 * An example of this would be the .VB/.VH sound files, where one is a header file, and the other contains audio data.
 * Created by Kneesnap on 5/13/2024.
 */
@Getter
public class CoupledDataParent extends SharedGameObject {
    private final CoupledDataEntry<?> firstData;
    private final CoupledDataEntry<?> secondData;

    public CoupledDataParent(SCGameInstance instance, CoupledDataEntry<?> firstData, CoupledDataEntry<?> secondData) {
        super(instance);
        this.firstData = firstData;
        this.secondData = secondData;
        this.firstData.setParent(this, true);
        this.secondData.setParent(this, false);
    }
}