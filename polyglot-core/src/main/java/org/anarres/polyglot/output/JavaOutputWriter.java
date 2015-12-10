/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.PolyglotExecutor;
import org.anarres.polyglot.lr.LRAutomaton;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class JavaOutputWriter extends AbstractOutputWriter {

    private static final Logger LOG = LoggerFactory.getLogger(JavaOutputWriter.class);
    private final JavaHelper helper;

    public JavaOutputWriter(@Nonnull File destinationDir, @Nonnull Map<? extends String, ? extends File> templates,
            @Nonnull Set<? extends Option> options,
            @Nonnull GrammarModel grammar,
            @CheckForNull LRAutomaton automaton, @Nonnull Tables tables) {
        super(OutputLanguage.java, destinationDir, templates, options, grammar, automaton, tables);
        this.helper = new JavaHelper(options, grammar, automaton);
    }

    @Override
    protected void initContext(@Nonnull VelocityContext context) {
        context.put("helper", helper);
    }

    @Override
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
