/**
 *
 */
package org.microshed.testing.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalLogger {

    private static boolean checkedEnabled;
    private static boolean loggingEnabled;

    private static boolean isLog4jEnabled() {
        if (!checkedEnabled) {
            checkedEnabled = true;
            try {
                Class.forName("org.slf4j.impl.StaticMDCBinder");
            } catch (Throwable t) {
                return loggingEnabled = false;
            }
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            loggingEnabled = tccl.getResource("log4j.properties") != null ||
                             tccl.getResource("log4j.xml") != null ||
                             System.getProperty("log4j.configuration") != null;
        }
        return loggingEnabled;
    }

    public static InternalLogger get(Class<?> clazz) {
        return new InternalLogger(clazz);
    }

    public final boolean LOG_ENABLED = isLog4jEnabled();
    public final Logger log;

    private InternalLogger(Class<?> clazz) {
        if (LOG_ENABLED)
            log = LoggerFactory.getLogger(clazz);
        else
            log = null;
    }

    public void debug(String msg) {
        if (LOG_ENABLED)
            log.debug(msg);
        // No-op if SLF4j is not enabled
    }

    public void debug(String msg, Throwable t) {
        if (LOG_ENABLED)
            log.debug(msg, t);
        // No-op if SLF4j is not enabled
    }

    public void info(String msg) {
        if (LOG_ENABLED)
            log.info(msg);
        else
            System.out.println("[INFO] " + msg);
    }

    public void warn(String msg) {
        if (LOG_ENABLED)
            log.warn(msg);
        else
            System.out.println("[WARN] " + msg);
    }

    public void warn(String msg, Throwable t) {
        if (LOG_ENABLED)
            log.warn(msg, t);
        else {
            System.out.println("[WARN] " + msg);
            t.printStackTrace();
        }
    }

    public void error(String msg) {
        if (LOG_ENABLED)
            log.error(msg);
        else
            System.out.println("[ERROR] " + msg);
    }

    public void error(String msg, Throwable t) {
        if (LOG_ENABLED)
            log.error(msg, t);
        else {
            System.out.println("[ERROR] " + msg);
            t.printStackTrace();
        }
    }

}
