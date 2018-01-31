package ch.tripod.minecraft.simply_utilities;

import java.util.logging.Logger;

public class Log {

    private static Logger logger;

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        Log.logger = logger;
    }

    public static void info(String fmt, Object ... args) {
        if (logger != null) {
            logger.info(String.format(fmt, args));
        }
    }

    public static void warn(String fmt, Object ... args) {
        if (logger != null) {
            logger.warning(String.format(fmt, args));
        }
    }
}
