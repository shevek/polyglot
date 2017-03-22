/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.base.Charsets;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.PolyglotEngine;
import org.anarres.polyglot.PolyglotExecutor;
import org.anarres.polyglot.model.GrammarModel;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.implement.ReportInvalidReferences;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.log.LogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.EscapeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public abstract class AbstractOutputWriter implements OutputWriter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractOutputWriter.class);
    private final OutputLanguage language;
    private final ErrorHandler errors;
    private final File destinationDir;
    private final Map<? extends String, ? extends File> templates;
    private final OutputData outputData;
    private final LogChute logChute = new Slf4jLogChute();
    private final EscapeTool escapeTool = new EscapeTool();

    public AbstractOutputWriter(
            @Nonnull ErrorHandler errors,
            @Nonnull OutputLanguage language,
            @Nonnull File destinationDir,
            @Nonnull Map<? extends String, ? extends File> templates,
            @Nonnull OutputData outputData) {
        this.errors = errors;
        this.language = language;
        this.destinationDir = destinationDir;
        this.templates = templates;
        this.outputData = outputData;
    }

    @Nonnull
    public ErrorHandler getErrors() {
        return errors;
    }

    @Nonnull
    public OutputLanguage getLanguage() {
        return language;
    }

    @Nonnull
    public File getDestinationDir() {
        return destinationDir;
    }

    @Nonnull
    public OutputData getOutputData() {
        return outputData;
    }

    @Nonnull
    protected GrammarModel getGrammar() {
        return getOutputData().getGrammar();
    }

    protected void setProperty(@Nonnull VelocityEngine engine, @Nonnull String name, @Nonnull Object value) {
        // if (LOG.isDebugEnabled()) LOG.debug("VelocityEngine property: " + name + " = " + value);
        engine.setProperty(name, value);
    }

    @Nonnull
    protected File newDestinationFile(@Nonnull File dstFile) throws IOException {
        // Reconstruct this as dstFilePath may contain a slash.
        File dstDir = dstFile.getParentFile();
        PolyglotEngine.mkdirs(dstDir, "output directory");
        return dstFile;
    }

    @Nonnull
    protected File newDestinationFile(@Nonnull String dstFilePath) throws IOException {
        File dstFile = new File(getDestinationDir(), dstFilePath);
        return newDestinationFile(dstFile);
    }

    @OverridingMethodsMustInvokeSuper
    protected void initContext(@Nonnull VelocityContext context) {
    }

    public static class Loader extends ClasspathResourceLoader {

        // private static final Loader INSTANCE = new Loader();
        @Override
        public InputStream getResourceStream(String name) throws ResourceNotFoundException {
            // LOG.info("Loading " + name);
            return super.getResourceStream(name);
        }
    }

    protected void process(@Nonnull CharSource source, @Nonnull String dstFilePath, @Nonnull Map<String, Object> contextValues) throws IOException {
        VelocityEngine engine = new VelocityEngine();
        // setProperty(engine, VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS, SystemLogChute.class.getName());
        setProperty(engine, VelocityEngine.RUNTIME_LOG_LOGSYSTEM, logChute);
        setProperty(engine, VelocityEngine.RESOURCE_LOADER, "classpath");
        setProperty(engine, "classpath.resource.loader.class", Loader.class.getName());  // Needs to be in this JAR file.
        // setProperty(engine, VelocityEngine.FILE_RESOURCE_LOADER_CACHE, "true");
        setProperty(engine, VelocityEngine.EVENTHANDLER_INVALIDREFERENCES, ReportInvalidReferences.class.getName());
        setProperty(engine, VelocityEngine.RUNTIME_REFERENCES_STRICT, true);
        // setProperty(engine, VelocityEngine.FILE_RESOURCE_LOADER_PATH, includeBuf.toString());
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

        context.put("header", "This file was generated automatically by Polyglot. Edits will be lost.");
        context.put("grammarName", getOutputData().getName());
        context.put("grammar", getGrammar());
        context.put("lexerMachine", getOutputData().getLexerMachine());
        context.put("parserMachines", getOutputData().getParserMachines());
        context.put("package", getGrammar().getPackage().getPackageName());
        initContext(context);
        for (Map.Entry<String, Object> e : contextValues.entrySet())
            context.put(e.getKey(), e.getValue());
        File dstFile = newDestinationFile(dstFilePath);
        StringWriter writer = new StringWriter();
        try (Reader reader = source.openBufferedStream()) {
            // try (Writer writer = sink.openBufferedStream()) {
            engine.evaluate(context, writer, dstFilePath, reader);
            // }
        }
        // This trick lets us do the write in a single IOP.
        CharSink sink = Files.asCharSink(dstFile, Charsets.UTF_8);
        sink.write(writer.getBuffer());
    }

    protected void process(@Nonnull PolyglotExecutor executor, @Nonnull final CharSource source, @Nonnull final String dstFilePath, @Nonnull final Map<String, Object> contextValues) throws ExecutionException, IOException {
        executor.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                process(source, dstFilePath, contextValues);
                return null;
            }
        });
    }

    protected void process(@Nonnull PolyglotExecutor executor, @Nonnull String srcResourceName, @Nonnull final String dstFilePath, @Nonnull final Map<String, Object> contextValues) throws ExecutionException, IOException {
        URL resource = Resources.getResource(getClass(), language.name() + "/" + srcResourceName);
        final CharSource source = Resources.asCharSource(resource, Charsets.UTF_8);
        process(executor, source, dstFilePath, contextValues);
    }

    protected void process(@Nonnull PolyglotExecutor executor, @Nonnull String srcResourceName, @Nonnull String dstFilePath) throws ExecutionException, IOException {
        process(executor, srcResourceName, dstFilePath, ImmutableMap.<String, Object>of());
    }

    protected void processTemplates(@Nonnull PolyglotExecutor executor) throws ExecutionException, IOException {
        for (Map.Entry<? extends String, ? extends File> e : templates.entrySet()) {
            String dstFilePath = e.getKey();
            File template = e.getValue();
            process(executor, Files.asCharSource(template, StandardCharsets.UTF_8), dstFilePath, ImmutableMap.<String, Object>of());
        }
    }

    protected void write(@Nonnull PolyglotExecutor executor, @CheckForNull byte[] data, String dstFilePath) throws IOException {
        if (data != null) {
            Files.write(data, newDestinationFile(dstFilePath));
        }
    }
}
