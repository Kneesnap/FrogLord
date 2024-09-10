package net.highwayfrogs.editor.gui.components;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.GameObject;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.DefaultFileEditorUISoundListComponent.IBasicSoundList;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;

import javax.sound.sampled.Clip;
import java.util.Collection;
import java.util.Collections;

/**
 * Represents editor UI for a sound list.
 * TODO: Allow adding / removing sounds based on configuration of this component. (Button UI)
 * TODO: Allow exporting all sounds to a folder. (Add a button somewhere in the UI)
 * Created by Kneesnap on 9/9/2024.
 */
@Getter
public class DefaultFileEditorUISoundListComponent<TGameFile extends GameObject<?> & ICollectionViewEntry & IPropertyListCreator & IBasicSoundList, TGameInstance extends GameInstance> extends DefaultFileUIController<TGameInstance, TGameFile> {
    private final CollectionEditorComponent<TGameInstance, ? extends IBasicSoundList> collectionEditorComponent;
    private final BasicSoundListViewComponent<? extends IBasicSoundList, TGameFile, TGameInstance> soundListComponent;

    public DefaultFileEditorUISoundListComponent(TGameInstance instance) {
        this(instance, "Sound List", ImageResource.MUSIC_NOTE_16.getFxImage());
    }

    public DefaultFileEditorUISoundListComponent(TGameInstance instance, String fileNameText) {
        this(instance, fileNameText, ImageResource.MUSIC_NOTE_16.getFxImage());
    }

    public DefaultFileEditorUISoundListComponent(TGameInstance instance, String fileNameText, Image icon) {
        super(instance, fileNameText, icon);
        this.soundListComponent = new BasicSoundListViewComponent<>(this);
        this.collectionEditorComponent = new CollectionEditorComponent<>(instance, this.soundListComponent);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);

        if (getLeftSidePanelFreeArea() != null) {
            Node propertyListViewRootNode = this.collectionEditorComponent.getRootNode();
            HBox.setHgrow(propertyListViewRootNode, Priority.ALWAYS);
            getLeftSidePanelFreeArea().getChildren().add(propertyListViewRootNode);
            addController(this.collectionEditorComponent);
        }
    }

    public interface IBasicSoundList {
        /**
         * Gets a collection of the sounds kept by the sound list.
         */
        Collection<? extends IBasicSound> getSounds();

        /**
         * Controls whether the add/remove sound UI is shown for this UI.
         * TODO: USE
         */
        boolean isAllowAddRemoveOperationUI();
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
    }

    public interface IBasicSoundVariableSampleRate extends IBasicSound {
        /**
         * Sets the new sample rate of the sound.
         */
        void setSampleRate(Clip clip, int newSampleRate);
    }

    public static class BasicSoundListViewComponent<TSound extends IBasicSound, TGameFile extends GameObject<?> & ICollectionViewEntry & IPropertyListCreator & IBasicSoundList, TGameInstance extends GameInstance> extends CollectionListViewComponent<TGameInstance, TSound> {
        private final DefaultFileEditorUISoundListComponent<TGameFile, TGameInstance> listComponent;

        public BasicSoundListViewComponent(DefaultFileEditorUISoundListComponent<TGameFile, TGameInstance> listComponent) {
            super(listComponent.getGameInstance());
            this.listComponent = listComponent;
        }

        @Override
        protected void onSelect(TSound viewEntry) {
            if (viewEntry != null) {
                this.listComponent.getPropertyListViewer().showProperties(viewEntry.createPropertyList());
                viewEntry.getClip().start(); // TODO: Implement an actual UI for playing selected sounds, and have selecting this activate/deactivate its visibility.
                // TODO: I like keeping it playing sounds when you use the arrow keys. Perhaps we can have a checkbox to enable playing on click.
            } else if (this.listComponent.getFile() != null) {
                this.listComponent.getPropertyListViewer().showProperties(this.listComponent.getFile().createPropertyList());
            } else {
                this.listComponent.getPropertyListViewer().clear();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public Collection<TSound> getViewEntries() {
            return this.listComponent != null && this.listComponent.getFile() != null
                    ? (Collection<TSound>) this.listComponent.getFile().getSounds()
                    : Collections.emptyList();
        }
    }
}
