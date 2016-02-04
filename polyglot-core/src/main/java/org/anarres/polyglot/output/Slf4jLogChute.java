/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class Slf4jLogChute implements LogChute {

    private static final Logger LOG = LoggerFactory.getLogger(Slf4jLogChute.class);

    @Override
    public void init(RuntimeServices runtimeServices) throws Exception {
    }

    @Override
    public void log(int level, String message) {
        if (!isLevelEnabled(level))
            return;
        switch (level) {
            case TRACE_ID:
                LOG.trace(message);
                break;
            case DEBUG_ID:
                LOG.debug(message);
                break;
            case INFO_ID:
                LOG.info(message);
                break;
            case WARN_ID:
                LOG.warn(message);
                break;
            case ERROR_ID:
                LOG.error(message);
                break;
        }
    }

    @Override
    public void log(int level, String message, Throwable throwable) {
        if (!isLevelEnabled(level))
            return;
        switch (level) {
            case TRACE_ID:
                LOG.trace(message, throwable);
                break;
            case DEBUG_ID:
                LOG.debug(message, throwable);
                break;
            case INFO_ID:
                LOG.info(message, throwable);
                break;
            case WARN_ID:
                LOG.warn(message, throwable);
                break;
            case ERROR_ID:
                LOG.error(message, throwable);
                break;
        }
    }

    @Override
    public boolean isLevelEnabled(int level) {
        switch (level) {
            case TRACE_ID:
                // return LOG.isTraceEnabled();
                return false;
            case DEBUG_ID:
                // return LOG.isDebugEnabled();
                return false;
            case INFO_ID:
                return LOG.isInfoEnabled();
            case WARN_ID:
                return LOG.isWarnEnabled();
            case ERROR_ID:
                return LOG.isErrorEnabled();
            default:
                throw new IllegalArgumentException("Unknown level " + level);
        }
    }
}
