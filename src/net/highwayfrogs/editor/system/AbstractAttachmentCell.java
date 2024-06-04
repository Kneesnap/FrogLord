package net.highwayfrogs.editor.system;

import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import lombok.AllArgsConstructor;

import java.util.function.BiFunction;

/**
 * An abstract attachment cell.
 * Created by Kneesnap on 2/9/2019.
 */
@AllArgsConstructor
public class AbstractAttachmentCell<T> extends ListCell<T> {
    private BiFunction<T, Integer, String> nameFunction;
    private BiFunction<T, Integer, ImageView> imageFunction;

    public AbstractAttachmentCell(BiFunction<T, Integer, String> nameFunction) {
        this.nameFunction = nameFunction;
    }

    @Override
    public void updateItem(T selection, boolean empty) {
        super.updateItem(selection, empty);
        setGraphic(imageFunction != null ? imageFunction.apply(empty ? null : selection, getIndex()) : null);
        setText(nameFunction != null ? nameFunction.apply(empty ? null : selection, getIndex()) : null);
    }
}