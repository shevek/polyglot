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
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.PolyglotEngine;
import org.anarres.polyglot.PolyglotExecutor;
import org.anarres.polyglot.lr.LRAutomaton;
import org.anarres.polyglot.model.GrammarModel;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.implement.ReportInvalidReferences;
import org.apache.velocity.runtime.log.SystemLogChute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public abstract class AbstractOutputWriter implements OutputWriter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractOutputWriter.class);
    private final OutputLanguage language;
    private final File destinationDir;
    protected final Map<? extends String, ? extends File> templates;
    protected final Set<? extends Option> options;
    protected final GrammarModel grammar;
    protected final LRAutomaton automaton;
    protected final Tables tables;

    public AbstractOutputWriter(
            @Nonnull OutputLanguage language,
            @Nonnull File destinationDir,
            @Nonnull Map<? extends String, ? extends File> templates,
            @Nonnull Set<? extends Option> options,
            @Nonnull GrammarModel grammar,
            @CheckForNull LRAutomaton automaton, @Nonnull Tables tables) {
        this.language = language;
        this.destinationDir = destinationDir;
        this.templates = templates;
        this.options = options;
        this.grammar = grammar;
        this.automaton = automaton;
        this.tables = tables;
    }

    protected void setProperty(@Nonnull VelocityEngine engine, @Nonnull String name, @Nonnull Object value) {
        // if (LOG.isDebugEnabled()) LOG.debug("VelocityEngine property: " + name + " = " + value);
        engine.setProperty(name, value);
    }

    @Nonnull
    protected File newDestinationFile(@Nonnull String dstFilePath) throws IOException {
        File dstRoot = new File(destinationDir, grammar._package.getPackagePath());
        File dstFile = new File(dstRoot, dstFilePath);
        // Reconstruct this as dstFilePath may contain a slash.
        File dstDir = dstFile.getParentFile();
        PolyglotEngine.mkdirs(dstDir, "output directory");
        return dstFile;
    }

    protected abstract void initContext(@Nonnull VelocityContext context);

    protected void process(@Nonnull CharSource source, @Nonnull String dstFilePath, @Nonnull Map<String, Object> contextValues) throws IOException {
        VelocityEngine engine = new VelocityEngine();
        setProperty(engine, VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS, SystemLogChute.class.getName());
        setProperty(engine, VelocityEngine.RESOURCE_LOADER, "file");
        setProperty(engine, VelocityEngine.FILE_RESOURCE_LOADER_CACHE, "true");
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
        context.put("header", "This file was generated automatically by Polyglot. Edits will be lost.");
        context.put("grammar", grammar);
        context.put("automaton", automaton);
        context.put("tables", tables);
        context.put("package", grammar.getPackage().getPackageName());
        initContext(context);
        for (Map.Entry<String, Object> e : contextValues.entrySet())
            context.put(e.getKey(), e.getValue());
        File dstFile = newDestinationFile(dstFilePath);
        StringWriter writer = new StringWriter();
        try (final Reader reader = source.openBufferedStream()) {
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

    protected void process(PolyglotExecutor executor, String srcResourceName, final String dstFilePath, final Map<String, Object> contextValues) throws ExecutionException, IOException {
        URL resource = Resources.getResource(JavaOutputWriter.class, language.name() + "/" + srcResourceName);
        final CharSource source = Resources.asCharSource(resource, Charsets.UTF_8);
        process(executor, source, dstFilePath, contextValues);
    }

    protected void process(PolyglotExecutor executor, String srcResourceName, String dstFilePath) throws ExecutionException, IOException {
        process(executor, srcResourceName, dstFilePath, ImmutableMap.<String, Object>of());
    }

    protected void write(@CheckForNull byte[] data, String dstFilePath) throws IOException {
        if (data != null)
            Files.write(data, newDestinationFile(dstFilePath));
    }
}