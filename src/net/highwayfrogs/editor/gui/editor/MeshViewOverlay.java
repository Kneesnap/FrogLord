package net.highwayfrogs.editor.gui.editor;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;

/**
 * Shared 2D overlay for mesh viewers.
 * Created by Kneesnap on 6/20/2026.
 */
public class MeshViewOverlay {
    @Getter private final AnchorPane root = new AnchorPane();
    private final HBox statusBar = new HBox(8);
    private final VBox messageBox = new VBox(6);

    private static final Duration DEFAULT_MESSAGE_DURATION = Duration.seconds(2.2);
    private static final Duration MESSAGE_EXIT_DURATION = Duration.millis(250);
    private static final String STATUS_STYLE = "-fx-background-color: rgba(20, 24, 30, 0.78); -fx-background-radius: 4; -fx-border-color: rgba(255, 255, 255, 0.18); -fx-border-radius: 4; -fx-padding: 6 9 6 9; -fx-text-fill: white; -fx-font-size: 12px;";
    private static final String MESSAGE_STYLE = "-fx-background-color: rgba(20, 24, 30, 0.88); -fx-background-radius: 4; -fx-border-color: rgba(255, 255, 255, 0.20); -fx-border-radius: 4; -fx-padding: 8 10 8 10; -fx-text-fill: white; -fx-font-size: 12px;";

    public MeshViewOverlay() {
        this.root.setMouseTransparent(true);
        this.statusBar.setMouseTransparent(true);
        this.statusBar.setAlignment(Pos.BOTTOM_LEFT);
        this.messageBox.setMouseTransparent(true);
        this.messageBox.setAlignment(Pos.TOP_RIGHT);

        AnchorPane.setLeftAnchor(this.statusBar, 12.0);
        AnchorPane.setBottomAnchor(this.statusBar, 12.0);
        AnchorPane.setTopAnchor(this.messageBox, 12.0);
        AnchorPane.setRightAnchor(this.messageBox, 12.0);
        this.root.getChildren().addAll(this.statusBar, this.messageBox);
    }

    /**
     * Sets or replaces a persistent overlay status label.
     * @param id stable status id
     * @param text text to display, or null/empty to remove the status
     */
    public void setStatusText(String id, String text) {
        if (id == null)
            throw new NullPointerException("id");

        Label label = getStatusLabel(id);
        if (text == null || text.isEmpty()) {
            if (label != null)
                this.statusBar.getChildren().remove(label);

            return;
        }

        if (label == null) {
            label = createOverlayLabel(id, STATUS_STYLE);
            this.statusBar.getChildren().add(label);
        }

        label.setText(text);
    }

    /**
     * Shows a temporary message which slides away after the default display duration.
     * @param message message text
     */
    public void showTimedMessage(String message) {
        showTimedMessage(message, DEFAULT_MESSAGE_DURATION);
    }

    /**
     * Shows a temporary message which slides away after a display duration.
     * @param message message text
     * @param displayDuration amount of time to keep the message visible before leaving
     */
    public void showTimedMessage(String message, Duration displayDuration) {
        if (message == null || message.isEmpty())
            return;

        Label label = createOverlayLabel(null, MESSAGE_STYLE);
        label.setText(message);
        label.setOpacity(1);
        label.setTranslateX(0);
        VBox.setMargin(label, new Insets(0));
        this.messageBox.getChildren().add(0, label);

        PauseTransition wait = new PauseTransition(displayDuration != null ? displayDuration : DEFAULT_MESSAGE_DURATION);
        FadeTransition fadeOut = new FadeTransition(MESSAGE_EXIT_DURATION, label);
        fadeOut.setToValue(0);
        TranslateTransition slideOut = new TranslateTransition(MESSAGE_EXIT_DURATION, label);
        slideOut.setToX(260);
        SequentialTransition sequence = new SequentialTransition(wait, new ParallelTransition(fadeOut, slideOut));
        sequence.setOnFinished(event -> this.messageBox.getChildren().remove(label));
        sequence.play();
    }

    private Label getStatusLabel(String id) {
        for (int i = 0; i < this.statusBar.getChildren().size(); i++)
            if (id.equals(this.statusBar.getChildren().get(i).getUserData()))
                return (Label) this.statusBar.getChildren().get(i);

        return null;
    }

    private static Label createOverlayLabel(String id, String style) {
        Label label = new Label();
        label.setUserData(id);
        label.setStyle(style);
        label.setMinHeight(28);
        return label;
    }
}
