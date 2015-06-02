package org.anarres.polyglot.gradle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class PolyglotPluginExtension {

    private static final Logger LOG = LoggerFactory.getLogger(PolyglotPluginExtension.class);
    @Nonnull
    public Object inputDir = "src/main/polyglot";
    @Nonnull
    public List<Object> includeDirs = new ArrayList<Object>();
    @Nonnull
    public Object intermediateDir = "build/generated-sources/polyglot-grammar";
    @Nonnull
    public Object outputDir = "build/generated-sources/polyglot-java";
    @CheckForNull
    public Object debugDir;
    @Nonnull
    public Map<String, Object> templates = new HashMap<>();
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

    public void template(@Nonnull String dstFilePath, @Nonnull Object srcTemplate) {
        templates.put(dstFilePath, srcTemplate);
        // LOG.info("Templates are now " + templates);
    }

    public void option(@Nonnull String... options) {
        this.options.addAll(Arrays.asList(options));
    }
}
