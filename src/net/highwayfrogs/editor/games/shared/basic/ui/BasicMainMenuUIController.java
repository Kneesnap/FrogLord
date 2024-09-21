package net.highwayfrogs.editor.games.shared.basic.ui;

import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.file.writer.LargeFileReceiver;
import net.highwayfrogs.editor.games.shared.basic.BasicGameInstance;
import net.highwayfrogs.editor.games.shared.basic.file.BasicGameFile;
import net.highwayfrogs.editor.gui.MainMenuController;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;

/**
 * Manages the main menu UI for a basic game instance.
 * Created by Kneesnap on 8/12/2024.
 */
public class BasicMainMenuUIController<TGameInstance extends BasicGameInstance> extends MainMenuController<TGameInstance, BasicGameFile<?>> {
    public BasicMainMenuUIController(TGameInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);

        // Allow exporting MWI.
        addMenuItem(this.menuBarFile, "Export Files", () -> { // TODO: Make this use the progress bar later.
            File exportFolder = Utils.promptChooseDirectory(getGameInstance(), "Choose the folder to export files into...", true);
            if (exportFolder == null)
                return; // Cancel.

            try {
                for (BasicGameFile<?> file : getGameInstance().getFiles()) {
                    if (file.getFileDefinition().getFile() == null) {
                        getLogger().severe("File '" + file.getDisplayName() + "' was defined by a(n) " + Utils.getSimpleName(file) + ", which should not have been possible.");
                        continue;
                    }

                    file.export(new File(exportFolder, "Export [" + file.getDisplayName() + "]"));
                }

                getLogger().info("Export complete.");
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, true, "Failed to export files.");
            }
        });

        /*
        TODO: Implement.
        addMenuItem(this.menuBarFile, "Import File", this::importFile); // Ctrl + I
        addMenuItem(this.menuBarFile, "Export File", this::exportFile); // Ctrl + O
         */
    }

    @Override
    protected void saveMainGameData() {
        ProgressBarComponent.openProgressBarWindow(getGameInstance(), "Saving Files", progressBar -> {
            progressBar.update(0, getGameInstance().getFiles().size(), "");

            for (BasicGameFile<?> file : getGameInstance().getFiles()) {
                if (file.getFileDefinition().getFile() == null) {
                    getLogger().severe("File '" + file.getDisplayName() + "' was defined by a(n) " + Utils.getSimpleName(file) + ", which should not have been possible.");
                    continue;
                }

                File targetFile = file.getFileDefinition().getFile();
                progressBar.setStatusMessage("Saving '" + targetFile.getName() + "'");
                DataWriter writer = new DataWriter(targetFile.getName().equalsIgnoreCase("gamedata.bin") ? new LargeFileReceiver(targetFile) : new FileReceiver(targetFile)); // TODO: Allow specifying if it needs a large receiver.
                file.save(writer);
                writer.closeReceiver();

                progressBar.addCompletedProgress(1);
            }
        });
    }

    @Override
    protected CollectionEditorComponent<TGameInstance, BasicGameFile<?>> createFileListEditor() {
        return new CollectionEditorComponent<>(getGameInstance(), new BasicFileListViewComponent<>(getGameInstance()), false);
    }

    protected static MenuItem addMenuItem(Menu menuBar, String title, Runnable action) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(event -> Utils.reportErrorIfFails(action));
        menuBar.getItems().add(menuItem);
        return menuItem;
    }
}