package net.highwayfrogs.editor.gui.components;

import javafx.scene.Node;
import javafx.scene.image.ImageView;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.utils.FXUtils;

import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

/**
 * Represents a component which is just a wrapper around a supplied JavaFX node.
 * Created by Kneesnap on 9/26/2025.
 */
public class FXNodeWrapperComponent<TGameInstance extends GameInstance, TNode extends Node> extends GameUIController<TGameInstance> {
    private final BiConsumer<FXNodeWrapperComponent<TGameInstance, TNode>, TNode> controllerLoadHandler;

    public FXNodeWrapperComponent(TGameInstance instance, TNode javaFxNode) {
        this(instance, javaFxNode, null);
    }

    public FXNodeWrapperComponent(TGameInstance instance, TNode javaFxNode, BiConsumer<FXNodeWrapperComponent<TGameInstance, TNode>, TNode> controllerLoadHandler) {
        super(instance);
        this.controllerLoadHandler = controllerLoadHandler;
        loadController(javaFxNode);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onControllerLoad(Node rootNode) {
        if (this.controllerLoadHandler != null)
            this.controllerLoadHandler.accept(this, (TNode) rootNode);
    }

    /**
     * Creates a component wrapping an ImageView for a BufferedImage.
     * @param instance the game instance to create the component for
     * @param awtImage the image to create a view for
     * @return the image view component
     * @param <TGameInstance> the type of game instance to create the UI component for
     */
    public static <TGameInstance extends GameInstance> FXNodeWrapperComponent<TGameInstance, ImageView> createImageView(TGameInstance instance, BufferedImage awtImage) {
        if (instance == null)
            throw new NullPointerException("instance");

        ImageView imageView = new ImageView(awtImage != null ? FXUtils.toFXImage(awtImage, false) : null);
        return new FXNodeWrapperComponent<>(instance, imageView);
    }
}
