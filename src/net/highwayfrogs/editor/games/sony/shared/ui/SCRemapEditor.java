package net.highwayfrogs.editor.games.sony.shared.ui;

import javafx.collections.FXCollections;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mwd.MWDFile;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.fx.wrapper.LazyFXListCell;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class SCRemapEditor<TFile extends SCGameFile<?>> {
    @NonNull private final Label remapListLabel;
    @NonNull private final ListView<Short> remapList;
    @NonNull private final Supplier<TFile> fileSupplier;
    @NonNull private final Function<TFile, VLOArchive> vloGetter;
    @NonNull private final Function<TFile, TextureRemapArray> remapGetter;

    /**
     * Sets up the editor for the given file.
     * @param file the file to setup the editor for
     */
    public void setupEditor(TFile file) {
        this.remapListLabel.setText("No Texture Remap");
        this.remapList.setDisable(true);
        if (this.remapList.getItems() != null)
            this.remapList.getItems().clear();

        TextureRemapArray textureRemap = this.remapGetter.apply(file);

        // Setup Remap Editor.
        if (textureRemap != null) {
            List<Short> textureRemapIdList = textureRemap.getTextureIds() != null ? textureRemap.getTextureIds() : Collections.emptyList();
            this.remapListLabel.setText(textureRemap.getDebugName() + " (" + textureRemapIdList.size() + " texture" + (textureRemapIdList.size() != 1 ? "s" : "") + ")");
            this.remapList.setDisable(false);
            this.remapList.setItems(FXCollections.observableArrayList(textureRemapIdList));
            this.remapList.getSelectionModel().selectFirst();
            this.remapList.setCellFactory(param -> new LazyFXListCell<>((textureId, index) -> {
                GameImage image = resolveGameImage(textureId);
                String originalName = image != null ? image.getOriginalName() : null;
                return index + ": " + (originalName != null ? originalName + " (" + textureId + ")" : "Texture " + textureId);
            }, (textureId, index) -> {
                GameImage image = resolveGameImage(textureId);
                return image != null ? image.toFXImage(MWDFile.VLO_ICON_SETTING) : null;
            }));

            // Handle double-click to change.
            this.remapList.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    promptChangeTexture();
                    event.consume();
                }
            });
        }
    }

    private GameImage resolveGameImage(Short textureId) {
        if (textureId == null)
            return null;

        TFile file = this.fileSupplier.get();
        if (file == null)
            return null;

        VLOArchive vloArchive = this.vloGetter.apply(file);
        GameImage temp = vloArchive != null ? vloArchive.getImageByTextureId(textureId, false) : null;
        if (temp != null)
            return temp;

        return file.getArchive().getImageByTextureId(textureId);
    }

    private void promptChangeTexture() {
        TFile file = this.fileSupplier.get();
        if (file == null)
            return;

        TextureRemapArray textureRemap = this.remapGetter.apply(file);
        if (textureRemap == null)
            return;

        // Validate selection index.
        int selectionIndex = this.remapList.getSelectionModel().getSelectedIndex();
        if (selectionIndex < 0 || selectionIndex >= textureRemap.getTextureIds().size())
            return;

        // Ensure we've got the VLO to find textures from.
        VLOArchive vloFile = this.vloGetter.apply(file);
        if (vloFile == null) {
            FXUtils.makePopUp("Cannot edit remaps for a map which has no associated VLO!", AlertType.WARNING);
            return;
        }

        // Ask the user which texture to apply.
        vloFile.promptImageSelection(newImage -> {
            TFile safeFile = this.fileSupplier.get();
            TextureRemapArray safeRemap = this.remapGetter.apply(safeFile);

            int index = this.remapList.getSelectionModel().getSelectedIndex();
            safeRemap.setRemappedTextureId(index, newImage.getTextureId());
            this.remapList.setItems(FXCollections.observableArrayList(safeRemap.getTextureIds())); // Refresh remap.
            this.remapList.getSelectionModel().select(index);
        }, false);
    }
}
