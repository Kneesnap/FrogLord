package net.highwayfrogs.editor.utils.logging;

import lombok.Getter;
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
            lr.setLoggerName(getName()); // setLoggerName() will be overridden when we pass to the wrappedLogger.
            lr.setSourceClassName(getLoggerInfo()); // setLoggerName() will be overridden when we pass to the wrappedLogger.
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
    public static class BasicWrappedLogger extends WrappedLogger {
        private final String overrideName;
        private final String overrideInfo;

        public BasicWrappedLogger(Logger logger, String overrideName) {
            this(logger, overrideName, null);
        }

        public BasicWrappedLogger(Logger logger, String overrideName, String overrideInfo) {
            super(logger);
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

        public BasicWrappedLogger(String overrideName) {
            this(overrideName, null);
        }

        public BasicWrappedLogger(String overrideName, String overrideInfo) {
            super();
            this.overrideName = overrideName;
            this.overrideInfo = overrideInfo;
        }

        @Override
        public String getName() {
            return this.overrideName != null ? this.overrideName : super.getName();
        }

        @Override
        public String getLoggerInfo() {
            return this.overrideInfo != null ? this.overrideInfo : super.getLoggerInfo();
        }
    }

    /**
     * Allows wrapping a logger.
     */
    public static class WrappedLogger extends InstanceLogger {
        public WrappedLogger(Logger logger) {
            super(logger);
        }

        public WrappedLogger(ILogger logger) {
            super(logger);
        }

        public WrappedLogger() {
            super();
        }

        @Override
        public String getLoggerInfo() {
            return this.wrappedJavaLogger.getName();
        }
    }

    /**
     * Represents a logger which is easy to create.
     */
    public static class LazyInstanceLogger extends GameInstanceLogger {
        private final Function<? extends Object, String> loggerInfoGetter;
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
    public static abstract class GameInstanceLogger extends InstanceLogger {
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

    @Getter
    public static class GameInstanceLogRecord extends LogRecord {
        private transient final GameInstance instance;

        public GameInstanceLogRecord(GameInstance instance, Level level, String msg) {
            super(level, msg);
            this.instance = instance;
        }
    }
}
