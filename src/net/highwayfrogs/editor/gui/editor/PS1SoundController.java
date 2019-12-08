package net.highwayfrogs.editor.gui.editor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import net.highwayfrogs.editor.file.sound.PSXSoundDummy;
import net.highwayfrogs.editor.gui.MainController;

/**
 * Controls the UI for the PS1 sounds.
 * TODO: https://github.com/simias/psxsdk/blob/master/tools/vag2wav.c
 * Created by Kneesnap on 7/24/2019.
 */
public class PS1SoundController extends EditorController<PSXSoundDummy> {

    @FXML
    private void actionExport(ActionEvent evt) {
        MainController.MAIN_WINDOW.exportFile();
    }

    @FXML
    private void actionImport(ActionEvent evt) {
        MainController.MAIN_WINDOW.importFile();
    }
}
