package net.highwayfrogs.editor.system;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.function.Supplier;

/**
 * An abstract JavaFX service.
 * Created by Kneesnap on 12/4/2018.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AbstractService<T> extends Service<T> {
    private Supplier<Task<T>> taskSupplier;

    @Override
    protected Task<T> createTask() {
        return taskSupplier.get();
    }

    /**
     * Run an abstract task.
     * @param supplier The task maker.
     */
    public static <T> void runAbstractTask(Supplier<Task<T>> supplier) {
        AbstractService<T> task = new AbstractService<>(supplier);
        task.start();
    }
}
