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
import java.nio.charset.StandardCharsets;
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
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;
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
public class OutputWriter {

    private static final Logger LOG = LoggerFactory.getLogger(OutputWriter.class);

    public static enum Language {

        java
    }

    private final File destinationDir;
    private final Language language;
    private final Map<? extends String, ? extends File> templates;
    private final Set<? extends Option> options;
    private final GrammarModel grammar;
    private final LRAutomaton automaton;
    private final Tables tables;

    public OutputWriter(@Nonnull File destinationDir,
            @Nonnull Language language,
            @Nonnull Map<? extends String, ? extends File> templates,
            @Nonnull Set<? extends Option> options,
            @Nonnull GrammarModel grammar,
            @CheckForNull LRAutomaton automaton, @Nonnull Tables tables) {
        this.destinationDir = destinationDir;
        this.language = language;
        this.templates = templates;
        this.options = options;
        this.grammar = grammar;
        this.automaton = automaton;
        this.tables = tables;
    }

    private void setProperty(@Nonnull VelocityEngine engine, @Nonnull String name, @Nonnull Object value) {
        // if (LOG.isDebugEnabled()) LOG.debug("VelocityEngine property: " + name + " = " + value);
        engine.setProperty(name, value);
    }

    @Nonnull
    private File newDestinationFile(@Nonnull String dstFilePath) throws IOException {
        File dstRoot = new File(destinationDir, grammar._package.getPackagePath());
        File dstFile = new File(dstRoot, dstFilePath);
        // Reconstruct this as dstFilePath may contain a slash.
        File dstDir = dstFile.getParentFile();
        PolyglotEngine.mkdirs(dstDir, "output directory");
        return dstFile;
    }

    public void process(@Nonnull CharSource source, @Nonnull String dstFilePath, @Nonnull Map<String, Object> contextValues) throws IOException {
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
        context.put("helper", new JavaHelper(options, grammar, automaton));

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

    public void process(@Nonnull PolyglotExecutor executor, @Nonnull final CharSource source, @Nonnull final String dstFilePath, @Nonnull final Map<String, Object> contextValues) throws ExecutionException, IOException {
        executor.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                process(source, dstFilePath, contextValues);
                return null;
            }
        });
    }

    public void process(PolyglotExecutor executor, String srcResourceName, final String dstFilePath, final Map<String, Object> contextValues) throws ExecutionException, IOException {
        URL resource = Resources.getResource(OutputWriter.class, language.name() + "/" + srcResourceName);
        final CharSource source = Resources.asCharSource(resource, Charsets.UTF_8);
        process(executor, source, dstFilePath, contextValues);
    }

    public void process(PolyglotExecutor executor, String srcResourceName, String dstFilePath) throws ExecutionException, IOException {
        process(executor, srcResourceName, dstFilePath, ImmutableMap.<String, Object>of());
    }

    private void write(@CheckForNull byte[] data, String dstFilePath) throws IOException {
        if (data != null)
            Files.write(data, newDestinationFile(dstFilePath));
    }

    public void run(@Nonnull PolyglotExecutor executor) throws InterruptedException, ExecutionException, IOException {
        try {
            // Kick this off first, as it's the long pole.
            if (automaton != null) {
                // Parser
                process(executor, "parser.vm", "parser/Parser.java");
                process(executor, "parserexception.vm", "parser/ParserException.java");
            }

            // Lexer
            process(executor, "ilexer.vm", "lexer/ILexer.java");
            process(executor, "lexer.vm", "lexer/Lexer.java");
            process(executor, "lexerexception.vm", "lexer/LexerException.java");

            // Nodes and tokens
            process(executor, "node.vm", "node/Node.java");
            process(executor, "token.vm", "node/Token.java");

            process(executor, "token-fixed.vm", "node/EOF.java", ImmutableMap.<String, Object>of("token", TokenModel.EOF.INSTANCE));
            process(executor, "token-variable.vm", "node/InvalidToken.java", ImmutableMap.<String, Object>of("token", TokenModel.Invalid.INSTANCE));
            for (TokenModel token : grammar.tokens.values()) {
                // LOG.info("Generating " + token + " from " + token.isFixed());
                if (token.isFixed())
                    process(executor, "token-fixed.vm", "node/" + token.getJavaTypeName() + ".java", ImmutableMap.<String, Object>of("token", token));
                else
                    process(executor, "token-variable.vm", "node/" + token.getJavaTypeName() + ".java", ImmutableMap.<String, Object>of("token", token));
            }

            // Analyses
            process(executor, "clonelistener.vm", "node/CloneListener.java");
            process(executor, "switchable.vm", "node/Switchable.java");
            process(executor, "switch.vm", "node/Switch.java");

            process(executor, "visitable.vm", "node/Visitable.java");
            process(executor, "visitor.vm", "analysis/Visitor.java");
            process(executor, "visitoradapter.vm", "analysis/VisitorAdapter.java");

            process(executor, "analysis.vm", "analysis/Analysis.java");
            process(executor, "analysisadapter.vm", "analysis/AnalysisAdapter.java");

            if (grammar.astProductionRoot != null) {
                process(executor, "depthfirstadapter.vm", "analysis/DepthFirstAdapter.java");
                process(executor, "reverseddepthfirstadapter.vm", "analysis/ReversedDepthFirstAdapter.java");

                process(executor, "depthfirstvisitor.vm", "analysis/DepthFirstVisitor.java");
                // process(executor, "reverseddepthfirstadapter.vm", "analysis/ReversedDepthFirstAdapter.java");

                process(executor, "start.vm", "node/Start.java");
                for (AstProductionModel production : grammar.astProductions.values()) {
                    process(executor, "production.vm", "node/" + production.getJavaTypeName() + ".java", ImmutableMap.<String, Object>of("production", production));
                    for (AstAlternativeModel alternative : production.alternatives.values()) {
                        process(executor, "alternative.vm", "node/" + alternative.getJavaTypeName() + ".java", ImmutableMap.<String, Object>of("production", production, "alternative", alternative));
                    }
                }
            }

            for (Map.Entry<? extends String, ? extends File> e : templates.entrySet()) {
                String dstFilePath = e.getKey();
                File template = e.getValue();
                process(executor, Files.asCharSource(template, StandardCharsets.UTF_8), dstFilePath, ImmutableMap.<String, Object>of());
            }

            // Documentation
            String packageJavadoc = grammar._package.getJavadocComment();
            if (packageJavadoc != null)
                process(executor, "package-info.vm", "package-info.java", ImmutableMap.<String, Object>of("subpackage", "", "javadoc", packageJavadoc));
            process(executor, "package-info.vm", "node/package-info.java", ImmutableMap.<String, Object>of("subpackage", ".node", "javadoc", "/** Autogenerated abstract syntax tree model classes. */"));
            process(executor, "package-info.vm", "analysis/package-info.java", ImmutableMap.<String, Object>of("subpackage", ".analysis", "javadoc", "/** Autogenerated abstract syntax tree analysis and visitor classes. */"));
            process(executor, "package-info.vm", "parser/package-info.java", ImmutableMap.<String, Object>of("subpackage", ".parser", "javadoc", "/** Autogenerated parser classes. */"));
            process(executor, "package-info.vm", "lexer/package-info.java", ImmutableMap.<String, Object>of("subpackage", ".lexer", "javadoc", "/** Autogenerated lexer classes. */"));

            write(tables.getLexerData(), "lexer/lexer.dat");
            write(tables.getParserData(), "parser/parser.dat");

        } finally {
            executor.await();
        }
    }
}
