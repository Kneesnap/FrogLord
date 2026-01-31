package net.highwayfrogs.editor.utils.logging;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Represents an ILogger which is hooked up to the java.util.Logger logging system.
 * Created by Kneesnap on 11/12/2024.
 */
public abstract class InstanceLogger implements ILogger {
    protected final Logger wrappedJavaLogger;
    protected final ILogger wrappedLogger;

    public InstanceLogger(Logger wrappedJavaLogger) {
        this.wrappedJavaLogger = wrappedJavaLogger;
        this.wrappedLogger = null;
    }

    public InstanceLogger(ILogger wrappedLogger) {
        this.wrappedJavaLogger = null;
        this.wrappedLogger = wrappedLogger;
    }

    protected InstanceLogger() {
        this.wrappedJavaLogger = null;
        this.wrappedLogger = null;
    }

    @Override
    public void log(LogRecord lr) {
        if (this.wrappedJavaLogger != null) {
            final ResourceBundle bundle = this.wrappedJavaLogger.getResourceBundle();
            final String bundleName = this.wrappedJavaLogger.getResourceBundleName();
            if (bundleName != null && bundle != null) {
                lr.setResourceBundleName(bundleName);
                lr.setResourceBundle(bundle);
            }

            this.wrappedJavaLogger.log(lr);
        } else if (this.wrappedLogger != null) {
            this.wrappedLogger.log(lr);
        }
    }

    @Override
    public boolean isLoggable(Level level) {
        if (this.wrappedJavaLogger != null) {
            return this.wrappedJavaLogger.isLoggable(level);
        } else if (this.wrappedLogger != null) {
            return this.wrappedLogger.isLoggable(level);
        } else {
            return true;
        }
    }

    @Override
    public String getName() {
        if (this.wrappedJavaLogger != null) {
            return this.wrappedJavaLogger.getName();
        } else if (this.wrappedLogger != null) {
            return this.wrappedLogger.getName();
        } else {
            return Utils.getSimpleName(this);
        }
    }

    /**
     * Allows wrapping a logger and overriding the name/info.
     */
    @Getter
    public static class BasicWrappedLogger extends WrappedLogger {
        private final String overrideName;
        private final String overrideInfo;

        public BasicWrappedLogger(GameInstance gameInstance, Logger logger, String overrideName) {
            this(gameInstance, logger, overrideName, null);
        }

        public BasicWrappedLogger(GameInstance gameInstance, Logger logger, String overrideName, String overrideInfo) {
            super(gameInstance, logger);
            this.overrideName = overrideName;
            this.overrideInfo = overrideInfo;
        }

        public BasicWrappedLogger(ILogger logger, String overrideName) {
            this(logger, overrideName, null);
        }

        public BasicWrappedLogger(ILogger logger, String overrideName, String overrideInfo) {
            super(logger);
            this.overrideName = overrideName;
            this.overrideInfo = overrideInfo;
        }

        public BasicWrappedLogger(GameInstance gameInstance, String overrideName) {
            this(gameInstance, overrideName, null);
        }

        public BasicWrappedLogger(GameInstance gameInstance, String overrideName, String overrideInfo) {
            super(gameInstance);
            this.overrideName = overrideName;
            this.overrideInfo = overrideInfo;
        }

        /**
         * Gets the original Logger name.
         */
        public String getOriginalName() {
            return super.getName();
        }

        /**
         * Gets the original Logger info.
         */
        public String getOriginalLoggerInfo() {
            return super.getLoggerInfo();
        }

        @Override
        public String getName() {
            return this.overrideName != null ? this.overrideName : getOriginalName();
        }

        @Override
        public String getLoggerInfo() {
            return this.overrideInfo != null ? this.overrideInfo : getOriginalLoggerInfo();
        }
    }

    /**
     * Allows wrapping a logger and overriding the name/info.
     * The only advantage to using this over BasicWrappedLogger is that it allows for the parent logger to change.
     */
    public static class AppendInfoLoggerWrapper extends BasicWrappedLogger {
        private final GameInstance gameInstance;
        private final String template;

        public static final String TEMPLATE_BAR_SEPARATOR = "{original}|{override}";
        public static final String TEMPLATE_COMMA_SEPARATOR = "{original},{override}";
        public static final String TEMPLATE_OVERRIDE_AT_ORIGINAL = "{override}@{original}";

        public AppendInfoLoggerWrapper(GameInstance gameInstance, Logger logger, String overrideName, @NonNull String template) {
            super(gameInstance, logger, overrideName);
            this.gameInstance = gameInstance;
            this.template = template;
        }

        public AppendInfoLoggerWrapper(GameInstance gameInstance, Logger logger, String overrideName, String overrideInfo, @NonNull String template) {
            super(gameInstance, logger, overrideName, overrideInfo);
            this.gameInstance = gameInstance;
            this.template = template;
        }

        public AppendInfoLoggerWrapper(ILogger logger, String overrideName, @NonNull String template) {
            super(logger, overrideName);
            this.gameInstance = logger instanceof IGameInstanceLogger ? ((IGameInstanceLogger) logger).getGameInstance() : null;
            this.template = template;
        }

        public AppendInfoLoggerWrapper(ILogger logger, String overrideName, String overrideInfo, @NonNull String template) {
            super(logger, overrideName, overrideInfo);
            this.gameInstance = logger instanceof IGameInstanceLogger ? ((IGameInstanceLogger) logger).getGameInstance() : null;
            this.template = template;
        }

        @Override
        public String getName() {
            String originalName = getOriginalName();
            String overrideName = getOverrideName();
            if (originalName != null && overrideName != null) {
                return this.template
                        .replace("{original}", originalName)
                        .replace("{override}", overrideName);
            } else if (overrideName != null) {
                return overrideName;
            } else {
                return originalName;
            }
        }

        @Override
        public String getLoggerInfo() {
            String originalInfo = getOriginalLoggerInfo();
            String overrideInfo = getOverrideInfo();
            if (originalInfo != null && overrideInfo != null) {
                return this.template
                        .replace("{original}", originalInfo)
                        .replace("{override}", overrideInfo);
            } else if (overrideInfo != null) {
                return overrideInfo;
            } else {
                return originalInfo;
            }
        }

        @Override
        public LogRecord createLogRecord(Level level, String message) {
            return new GameInstanceLogRecord(this.gameInstance, level, message);
        }
    }

    /**
     * Allows wrapping a logger.
     */
    @Getter
    public static class WrappedLogger extends InstanceLogger implements IGameInstanceLogger {
        private final GameInstance gameInstance;

        public WrappedLogger(GameInstance gameInstance, Logger logger) {
            super(logger);
            this.gameInstance = gameInstance;
        }

        public WrappedLogger(ILogger logger) {
            super(logger);
            this.gameInstance = logger instanceof IGameInstanceLogger ? ((IGameInstanceLogger) logger).getGameInstance() : null;
        }

        public WrappedLogger(GameInstance gameInstance) {
            super();
            this.gameInstance = gameInstance;
        }

        @Override
        public String getLoggerInfo() {
            if (this.wrappedJavaLogger != null) {
                return this.wrappedJavaLogger.getName();
            } else if (this.wrappedLogger != null) {
                String wrappedInfo = this.wrappedLogger.getLoggerInfo();
                return wrappedInfo != null ? wrappedInfo : this.wrappedLogger.getName();
            } else {
                return null;
            }
        }

        @Override
        public LogRecord createLogRecord(Level level, String message) {
            if (this.gameInstance != null) {
                return new GameInstanceLogRecord(this.gameInstance, level, message);
            } else {
                return super.createLogRecord(level, message);
            }
        }
    }

    /**
     * Represents a logger which is easy to create.
     */
    public static class LazyInstanceLogger extends GameInstanceLogger {
        private final Function<?, String> loggerInfoGetter;
        private final Object loggerInfoGetterParam;
        private final String constantLoggerName;

        public <TParam> LazyInstanceLogger(GameInstance instance, Function<TParam, String> loggerInfoGetter, TParam loggerInfoGetterParam) {
            this(instance, loggerInfoGetter, loggerInfoGetterParam, null);
        }

        public LazyInstanceLogger(GameInstance instance, String loggerName) {
            this(instance, null, null, loggerName);
        }

        public <TParam> LazyInstanceLogger(GameInstance instance, Function<TParam, String> loggerInfoGetter, TParam loggerInfoGetterParam, String loggerName) {
            super(instance);
            this.loggerInfoGetter = loggerInfoGetter;
            this.loggerInfoGetterParam = loggerInfoGetterParam;
            this.constantLoggerName = loggerName;
        }

        @Override
        @SuppressWarnings("unchecked")
        public String getName() {
            if (this.loggerInfoGetter != null) {
                try {
                    Function<? super Object, String> loggerInfoGetter = (Function<? super Object, String>) this.loggerInfoGetter;
                    String result = loggerInfoGetter.apply(this.loggerInfoGetterParam);
                    if (!StringUtils.isNullOrEmpty(result))
                        return result;
                } catch (Throwable th) {
                    th.printStackTrace(System.err); // We don't want to pass this to the logger, since it could infinitely recurse, so we'll print it the default way.
                    return "ERROR";
                }
            }

            return this.constantLoggerName;
        }

        @Override
        public String getLoggerInfo() {
            return getName();
        }
    }

    /**
     * Represents a logger for a particular game instance.
     */
    @Getter
    public static abstract class GameInstanceLogger extends InstanceLogger implements IGameInstanceLogger {
        private final GameInstance gameInstance;

        private static final Logger NULL_LOGGER = Logger.getLogger("NullGameInstance");

        public GameInstanceLogger(GameInstance instance) {
            super(instance != null ? instance.getLogger().wrappedJavaLogger : NULL_LOGGER);
            this.gameInstance = instance;
        }

        @Override
        public LogRecord createLogRecord(Level level, String message) {
            return new GameInstanceLogRecord(this.gameInstance, level, message);
        }
    }

    /**
     * Represents an ILogger which has a GameInstance
     */
    public interface IGameInstanceLogger {
        GameInstance getGameInstance();
    }

    @Getter
    public static class GameInstanceLogRecord extends LogRecord {
        private transient final GameInstance instance;

        public GameInstanceLogRecord(GameInstance instance, Level level, String msg) {
            super(level, msg);
            this.instance = instance;
        }
    }
}
