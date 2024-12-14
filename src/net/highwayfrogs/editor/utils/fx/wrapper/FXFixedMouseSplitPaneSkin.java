package net.highwayfrogs.editor.utils.fx.wrapper;

import javafx.scene.control.SplitPane;
import javafx.scene.control.skin.SplitPaneSkin;

/**
 *
 * In JavaFX9 this functionality can be made to work on existing skins as 'consumeMouseEvents()' is made public.
 * Created by Kneesnap on 11/8/2024.
 */
public class FXFixedMouseSplitPaneSkin extends SplitPaneSkin {
    public FXFixedMouseSplitPaneSkin(SplitPane splitPane) {
        super(splitPane);

        // Prevents SplitPane from eating mouse events.
        consumeMouseEvents(false);
    }
}
