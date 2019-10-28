/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.PolyglotEngine;
import org.anarres.polyglot.PolyglotExecutor;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.implement.ReportInvalidReferences;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.EscapeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public abstract class AbstractOutputWriter implements OutputWriter {

    @SuppressWarnings("UnusedVariable")
    private static final Logger LOG = LoggerFactory.getLogger(AbstractOutputWriter.class);

    protected static void setProperty(@Nonnull VelocityEngine engine, @Nonnull String name, @Nonnull Object value) {
        // if (LOG.isDebugEnabled()) LOG.debug("VelocityEngine property: " + name + " = " + value);
        engine.setProperty(name, value);
    }

    @Nonnull
    private static VelocityEngine newVelocityEngine() {
        VelocityEngine engine = new VelocityEngine();
        // setProperty(engine, VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS, SystemLogChute.class.getName());
        setProperty(engine, VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new Slf4jLogChute());
        setProperty(engine, VelocityEngine.RESOURCE_LOADER, "classpath");
        setProperty(engine, "classpath.resource.loader.class", Loader.class.getName());  // Needs to be in this JAR file.
        // setProperty(engine, VelocityEngine.FILE_RESOURCE_LOADER_CACHE, "true");
        setProperty(engine, VelocityEngine.EVENTHANDLER_INVALIDREFERENCES, ReportInvalidReferences.class.getName());
        setProperty(engine, VelocityEngine.RUNTIME_REFERENCES_STRICT, true);
        // setProperty(engine, VelocityEngine.FILE_RESOURCE_LOADER_PATH, includeBuf.toString());
        engine.init();
        return engine;
    }

    private final OutputLanguage language;
    private final ErrorHandler errors;
    private final String grammarName;
    private final File destinationDir;
    private final Set<? extends Option> options;
    private final EscapeTool escapeTool = new EscapeTool();

    public AbstractOutputWriter(
            @Nonnull ErrorHandler errors,
            @Nonnull OutputLanguage language,
            @Nonnull String grammarName,
            @Nonnull File destinationDir,
            @Nonnull Set<? extends Option> options) {
        this.errors = Preconditions.checkNotNull(errors, "ErrorHandler was null.");
        this.language = Preconditions.checkNotNull(language, "OutputLanguage was null.");
        this.grammarName = Preconditions.checkNotNull(grammarName, "Grammar name was null.");
        this.destinationDir = Preconditions.checkNotNull(destinationDir, "Destination dir was null.");
        this.options = Preconditions.checkNotNull(options, "Options was null.");
    }

    @Nonnull
    public ErrorHandler getErrors() {
        return errors;
    }

    @Override
    public OutputLanguage getLanguage() {
        return language;
    }

    @Nonnull
    public String getGrammarName() {
        return grammarName;
    }

    @Nonnull
    public File getDestinationDir() {
        return destinationDir;
    }

    @Nonnull
    public Set<? extends Option> getOptions() {
        return options;
    }

    @Nonnull
    protected File newDestinationFile(@Nonnull String dstFilePath) throws IOException {
        // TODO: Reconstruct this as dstFilePath may contain a slash.
        File dstFile = new File(getDestinationDir(), dstFilePath);
        File dstDir = dstFile.getParentFile();
        if (dstDir != null)
            PolyglotEngine.mkdirs(dstDir, "output directory");
        return dstFile;
    }

    public static class Loader extends ClasspathResourceLoader {

        // private static final Loader INSTANCE = new Loader();
        @Override
        public InputStream getResourceStream(String name) throws ResourceNotFoundException {
            // LOG.info("Loading " + name);
            return super.getResourceStream(name);
        }
    }

    protected void processSync(@Nonnull CharSource source, @Nonnull String dstFilePath, @Nonnull Map<? extends String, ? extends Object> contextValuesGlobal, @Nonnull Map<? extends String, ? extends Object> contextValuesLocal) throws IOException {
        // We'd love to share here, but it seems to cause all sorts of issues.
        VelocityEngine engine = newVelocityEngine();
        VelocityContext context = new VelocityContext() {
            /** The superclass calls key.intern(). */
            @Override
            public Object put(String key, Object value) {
                if (key == null)
                    return null;
                return internalPut(key, value);
            }
        };
        context.put("esc", escapeTool);

        context.put("header", "This file was generated automatically by Polyglot from " + getGrammarName() + ". Edits will be lost.");
        context.put("grammarName", getGrammarName());
        context.put("grammarOptions", String.valueOf(getOptions()));
        // context.put("grammar", getOutputData().getGrammar());
        // context.put("package", getGrammar().getPackage().getPackageName());
        for (Map.Entry<? extends String, ? extends Object> e : contextValuesGlobal.entrySet())
            context.put(e.getKey(), e.getValue());
        for (Map.Entry<? extends String, ? extends Object> e : contextValuesLocal.entrySet())
            context.put(e.getKey(), e.getValue());
        File dstFile = newDestinationFile(dstFilePath);
        StringWriter writer = new StringWriter();
        try (Reader reader = source.openBufferedStream()) {
            // try (Writer writer = sink.openBufferedStream()) {
            engine.evaluate(context, writer, dstFilePath, reader);
            // }
        } catch (RuntimeException e) {
            // e.g. Velocity throwing a ConcurrentModificationException?
            // at org.apache.velocity.runtime.directive.Foreach.render(Foreach.java:393)
            throw new IOException(getClass().getName() + " failed to process " + source + " -> " + dstFile + " with contextValuesGlobal=" + contextValuesGlobal + ", contextValuesLocal=" + contextValuesLocal + ": " + e, e);
        }
        // This trick lets us do the write in a single IOP.
        CharSink sink = Files.asCharSink(dstFile, Charsets.UTF_8);
        sink.write(writer.getBuffer());
    }

    protected void processSource(@Nonnull PolyglotExecutor executor, @Nonnull final CharSource source, @Nonnull final String dstFilePath,
            @Nonnull final Map<? extends String, ? extends Object> contextValuesGlobal,
            @Nonnull final Map<? extends String, ? extends Object> contextValuesLocal) throws ExecutionException, IOException {
        executor.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                processSync(source, dstFilePath, contextValuesGlobal, contextValuesLocal);
                return null;
            }
        });
    }

    protected void processResource(@Nonnull PolyglotExecutor executor, @Nonnull String srcResourceName, @Nonnull final String dstFilePath,
            @Nonnull final Map<? extends String, ? extends Object> contextValuesGlobal,
            @Nonnull final Map<? extends String, ? extends Object> contextValuesLocal) throws ExecutionException, IOException {
        URL resource = Resources.getResource(getClass(), language.name() + "/" + srcResourceName);
        final CharSource source = Resources.asCharSource(resource, Charsets.UTF_8);
        processSource(executor, source, dstFilePath, contextValuesGlobal, contextValuesLocal);
    }

    protected void processResource(@Nonnull PolyglotExecutor executor, @Nonnull String srcResourceName, @Nonnull final String dstFilePath,
            @Nonnull final Map<? extends String, ? extends Object> contextValuesGlobal) throws ExecutionException, IOException {
        processResource(executor, srcResourceName, dstFilePath, contextValuesGlobal, ImmutableMap.<String, Object>of());
    }

    protected void processTemplates(@Nonnull PolyglotExecutor executor, @Nonnull Map<? extends String, ? extends File> templates,
            @Nonnull final Map<? extends String, ? extends Object> contextValuesGlobal) throws ExecutionException, IOException {
        for (Map.Entry<? extends String, ? extends File> e : templates.entrySet()) {
            processSource(executor, Files.asCharSource(e.getValue(), StandardCharsets.UTF_8), e.getKey(), contextValuesGlobal, ImmutableMap.<String, Object>of());
        }
    }

    protected void write(@Nonnull PolyglotExecutor executor, @CheckForNull byte[] data, String dstFilePath) throws IOException {
        if (data != null) {
            Files.write(data, newDestinationFile(dstFilePath));
        }
    }
}
