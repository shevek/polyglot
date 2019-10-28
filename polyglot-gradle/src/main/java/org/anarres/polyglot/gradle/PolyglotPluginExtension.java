package org.anarres.polyglot.gradle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class PolyglotPluginExtension {

    @SuppressWarnings("UnusedVariable")
    private static final Logger LOG = LoggerFactory.getLogger(PolyglotPluginExtension.class);
    @Nonnull
    public List<Object> includeDirs = new ArrayList<Object>();
    @CheckForNull
    public Object debugDir;
    @Nonnull
    public List<String> options = new ArrayList<>();

    public void setDebug(boolean debug) {
        if (debug) {
            if (debugDir == null)
                debugDir = "build/reports/polyglot";
        } else {
            debugDir = null;
        }
    }

    public void option(@Nonnull String... options) {
        this.options.addAll(Arrays.asList(options));
    }
}
