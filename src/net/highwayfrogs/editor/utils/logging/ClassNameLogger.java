package net.highwayfrogs.editor.utils.logging;

import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.GameInstanceLogger;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Represents a logger using the name of the class.
 * Created by Kneesnap on 12/13/2024.
 */
public class ClassNameLogger extends GameInstanceLogger {
    private final String className;
    private static final WeakHashMap<GameInstance, Map<Class<?>, WeakReference<ClassNameLogger>>> CACHED_LOGGERS_BY_GAME_INSTANCE = new WeakHashMap<>();

    public ClassNameLogger(GameInstance instance, Class<?> clazz) {
        super(instance);
        this.className = getNameForClass(clazz);
    }

    @Override
    public String getName() {
        return this.className;
    }

    @Override
    public String getLoggerInfo() {
        return this.className;
    }

    /**
     * Gets the logger for a particular GameInstance/class combo.
     * @param instance the game instance to get the logger for
     * @param objClass the class to get the logger for
     * @return logger
     */
    public static ClassNameLogger getLogger(GameInstance instance, Class<?> objClass) {
        Map<Class<?>, WeakReference<ClassNameLogger>> loggersByClass = CACHED_LOGGERS_BY_GAME_INSTANCE.computeIfAbsent(instance, key -> new HashMap<>());
        WeakReference<ClassNameLogger> loggerRef = loggersByClass.get(objClass);
        ClassNameLogger logger = loggerRef != null ? loggerRef.get() : null;
        if (logger == null)
            loggersByClass.put(objClass, new WeakReference<>(logger = new ClassNameLogger(instance, objClass)));

        return logger;
    }

    private static String getNameForClass(Class<?> objClass) {
        return objClass != null ? objClass.getSimpleName() : "NULL_CLASS";
    }
}
