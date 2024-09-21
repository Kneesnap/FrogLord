package net.highwayfrogs.editor.gui.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.GameObject;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.DefaultFileEditorUISoundListComponent.IBasicSoundList;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.utils.Utils;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents editor UI for a sound list.
 * Created by Kneesnap on 9/9/2024.
 */
public class DefaultFileEditorUISoundListComponent<TGameInstance extends GameInstance, TGameFile extends GameObject<?> & ICollectionViewEntry & IPropertyListCreator & IBasicSoundList> extends DefaultFileUIController<TGameInstance, TGameFile> {
    @Getter private final CollectionEditorComponent<TGameInstance, ? extends IBasicSound> collectionEditorComponent;
    @Getter private final BasicSoundListViewComponent<TGameInstance, TGameFile, ? extends IBasicSound> soundListComponent;
    private final CheckBox playSoundOnSelectCheckBox = new CheckBox("Auto-Play");
    private final Button playSelectedSoundButton = new Button("Play");
    private final CheckBox forceRepeatCheckBox = new CheckBox("Force Repeat");
    private final HBox audioPlaybackControlBar = new HBox(10);
    private Clip activeAudioClip;
    private final LineListener audioEventListener = event -> {
        if (event.getType() != Type.STOP || event.getSource() != this.activeAudioClip)
            return;

        Utils.runOnFXThread(this::resetUIOnPlaybackStop);
    };

    public DefaultFileEditorUISoundListComponent(TGameInstance instance) {
        this(instance, "Sound List");
    }

    public DefaultFileEditorUISoundListComponent(TGameInstance instance, String fileNameText) {
        this(instance, fileNameText, ImageResource.MUSIC_NOTE_16.getFxImage());
    }

    public DefaultFileEditorUISoundListComponent(TGameInstance instance, String fileNameText, Image icon) {
        super(instance, fileNameText, icon);
        this.soundListComponent = createListViewComponent();
        this.collectionEditorComponent = new CollectionEditorComponent<>(instance, this.soundListComponent, true);
    }

    /**
     * Creates the list view component to use for this editor.
     */
    protected BasicSoundListViewComponent<TGameInstance, TGameFile, ? extends IBasicSound> createListViewComponent() {
        return new BasicSoundListViewComponent<>(this);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);

        // Create audio playback controls.
        this.audioPlaybackControlBar.setFillHeight(true); // Force the HBox to be the same height as the row
        this.audioPlaybackControlBar.setAlignment(Pos.CENTER_LEFT); // Vertically center the nodes within.
        this.audioPlaybackControlBar.getChildren().addAll(this.playSoundOnSelectCheckBox, this.forceRepeatCheckBox, this.playSelectedSoundButton);
        VBox.setVgrow(this.audioPlaybackControlBar, Priority.NEVER);
        getLeftSidePanelFreeArea().getChildren().add(0, this.audioPlaybackControlBar);

        // Register the editor.
        if (getLeftSidePanelFreeArea() != null) {
            this.soundListComponent.applyDefaultEditor(this.collectionEditorComponent);
            Node propertyListViewRootNode = this.collectionEditorComponent.getRootNode();
            VBox.setVgrow(propertyListViewRootNode, Priority.ALWAYS);
            getLeftSidePanelFreeArea().getChildren().add(propertyListViewRootNode);
            addController(this.collectionEditorComponent);
        }

        // Setup Playback UI.
        this.playSelectedSoundButton.setDisable(this.soundListComponent.getSelectedViewEntry() == null);
        this.playSelectedSoundButton.setOnAction(evt -> {
            evt.consume();
            if (this.activeAudioClip != null && this.activeAudioClip.isRunning()) {
                stopActiveAudioClip();
            } else {
                playSelectedSound();
            }
        });
    }

    @Override
    public void onSceneRemove(Scene oldScene) {
        super.onSceneRemove(oldScene);
        stopActiveAudioClip();
    }

    private void playSelectedSound() {
        playSound(this.soundListComponent.getSelectedViewEntry());
    }

    @Override
    public void setTargetFile(TGameFile newSoundList) {
        TGameFile oldSoundList = getFile();
        super.setTargetFile(newSoundList);
        if (oldSoundList != newSoundList) {
            this.soundListComponent.refreshDisplay();
            this.playSelectedSoundButton.setDisable(this.soundListComponent.getSelectedViewEntry() == null);
            this.collectionEditorComponent.updateEditorControls();
        }
    }

    /**
     * Plays the provided sound.
     * @param sound the sound to play
     */
    public void playSound(IBasicSound sound) {
        if (sound != null) {
            Clip audioClip = sound.getClip();
            if (audioClip != null)
                playNewAudioClip(audioClip, sound.isRepeatEnabled() || this.forceRepeatCheckBox.isSelected());
        } else {
            stopActiveAudioClip();
        }
    }

    /**
     * Stops the active audio clip.
     */
    public void stopActiveAudioClip() {
        if (this.activeAudioClip == null)
            return;

        // Shutdown old audio clip.
        // NOTE: WE DO NOT CALL Clip.close()!!!!!!
        // Clip.close() for some reason makes it so obtaining future clips becomes extremely slow!
        // There does appear to be a memory leak if we do this though, so our solution is to cache the Clip objects, and never close them.
        // This does sacrifice memory, but at least it will always be a constant amount, where-as uncached would cause the same sounds to get leaked multiple times in memory.
        this.activeAudioClip.removeLineListener(this.audioEventListener);
        this.activeAudioClip.stop();
        resetUIOnPlaybackStop();

        // Stop tracking it.
        this.activeAudioClip = null;
    }

    /**
     * Plays the new audio clip.
     * @param newAudioClip the new audio clip to play
     * @param repeat if true, the sound will repeat
     */
    public void playNewAudioClip(Clip newAudioClip, boolean repeat) {
        if (newAudioClip == null)
            throw new NullPointerException("newAudioClip");

        stopActiveAudioClip();
        this.activeAudioClip = newAudioClip;
        this.activeAudioClip.setMicrosecondPosition(0); // Reset play position.
        if (repeat) {
            this.activeAudioClip.loop(Clip.LOOP_CONTINUOUSLY);
        } else {
            this.activeAudioClip.start();
        }

        // Update UI.
        this.playSelectedSoundButton.setText("Stop");
        this.forceRepeatCheckBox.setDisable(true);
        this.activeAudioClip.addLineListener(this.audioEventListener);
    }

    private void resetUIOnPlaybackStop() {
        this.playSelectedSoundButton.setText("Play");
        this.forceRepeatCheckBox.setDisable(false);
    }

    public interface IBasicSoundList {
        /**
         * Gets a collection of the sounds kept by the sound list.
         */
        Collection<? extends IBasicSound> getSounds();
    }

    public interface IBasicSound extends ICollectionViewEntry, IPropertyListCreator {
        /**
         * Gets a playable audio clip.
         */
        Clip getClip();

        /**
         * Gets the file name to export the sound as if an export were to occur, with or without the extension.
         */
        String getExportFileName();

        /**
         * Reports whether the sound is configured to repeat by default.
         */
        boolean isRepeatEnabled();
    }

    @Getter
    public static class BasicSoundListViewComponent<TGameInstance extends GameInstance, TGameFile extends GameObject<?> & ICollectionViewEntry & IPropertyListCreator & IBasicSoundList, TSound extends IBasicSound> extends ListViewComponent<TGameInstance, TSound> {
        private final DefaultFileEditorUISoundListComponent<TGameInstance, TGameFile> listComponent;

        public BasicSoundListViewComponent(DefaultFileEditorUISoundListComponent<TGameInstance, TGameFile> listComponent) {
            super(listComponent.getGameInstance());
            this.listComponent = listComponent;
        }

        @Override
        protected void onSelect(TSound viewEntry) {
            if (this.listComponent.playSoundOnSelectCheckBox.isSelected())
                this.listComponent.stopActiveAudioClip();

            // Update controls based on selection.
            this.listComponent.getCollectionEditorComponent().updateEditorControls();
            this.listComponent.playSelectedSoundButton.setDisable(viewEntry == null);

            if (viewEntry != null) {
                this.listComponent.getPropertyListViewer().showProperties(viewEntry.createPropertyList());
                if (this.listComponent.playSoundOnSelectCheckBox.isSelected())
                    this.listComponent.playSound(viewEntry);
            } else if (this.listComponent.getFile() != null) {
                this.listComponent.getPropertyListViewer().showProperties(this.listComponent.getFile().createPropertyList());
            } else {
                this.listComponent.getPropertyListViewer().clear();
            }
        }

        @Override
        protected void onDoubleClick(TSound viewEntry) {
            this.listComponent.playSound(viewEntry);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<? extends TSound> getViewEntries() {
            return this.listComponent != null && this.listComponent.getFile() != null
                    ? (List<TSound>) this.listComponent.getFile().getSounds()
                    : Collections.emptyList();
        }
    }
}
