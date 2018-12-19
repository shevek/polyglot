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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.PolyglotExecutor;
import org.anarres.polyglot.analysis.StartChecker;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class JavaOutputWriter extends AbstractOutputWriter {

    private static final Logger LOG = LoggerFactory.getLogger(JavaOutputWriter.class);

    /** Syntactic sugar. */
    private static class PackageDirectoryMapper {

        private final String packagePath;

        public PackageDirectoryMapper(@Nonnull GrammarModel grammar) {
            this.packagePath = grammar.getPackage().getPackagePath();
        }

        @Nonnull
        public String map(String input) {
            return packagePath + File.separator + input;
        }
    }

    public JavaOutputWriter(ErrorHandler errors, String grammarName, File destinationDir, Set<? extends Option> options) {
        super(errors, OutputLanguage.java, grammarName, destinationDir, options);
    }

    @Nonnull
    private ImmutableMap<String, Object> newGlobalContext(@Nonnull GrammarModel grammar) {
        Map<String, EncodedStateMachine.ParserMetadata> parserMachines = new HashMap<>();
        for (final CstProductionModel root : grammar.getCstProductionRoots()) {
            final String name = StartChecker.getMachineName(root);
            parserMachines.put(name, new EncodedStateMachine.ParserMetadata() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public CstProductionModel getCstProductionRoot() {
                    return root;
                }
            });
        }

        JavaHelper helper = new JavaHelper(getOptions(), grammar);
        return ImmutableMap.<String, Object>of(
                "generated", "@javax.annotation.Generated(\"org.anarres.Polyglot\")",
                "helper", helper,
                "grammar", grammar,
                "package", grammar.getPackage().getPackageName(),
                "parserMachines", parserMachines
        );
    }

    @Override
    public void writeModel(PolyglotExecutor executor, final GrammarModel grammar, Map<? extends String, ? extends File> templates) throws ExecutionException, IOException {
        PackageDirectoryMapper p = new PackageDirectoryMapper(grammar);
        ImmutableMap<String, Object> context = newGlobalContext(grammar);

        for (Map.Entry<? extends String, ? extends File> e : templates.entrySet()) {
            processSource(executor, Files.asCharSource(e.getValue(), StandardCharsets.UTF_8), p.map(e.getKey()), context, ImmutableMap.<String, Object>of());
        }

        // Parser
        processResource(executor, "parserexception.vm", p.map("parser/ParserException.java"), context);
        for (EncodedStateMachine.ParserMetadata parserMachine : ((Map<?, EncodedStateMachine.ParserMetadata>) context.get("parserMachines")).values()) {
            processResource(executor, "start.vm", p.map("node/" + parserMachine.getStartClassName() + ".java"), context, ImmutableMap.of("parserMachine", parserMachine));
        }

        // Lexer
        processResource(executor, "ilexer.vm", p.map("lexer/ILexer.java"), context);
        processResource(executor, "lexerexception.vm", p.map("lexer/LexerException.java"), context);
        // if (grammar.isLrk())
        // processResource(executor, "lookaheadlexer.vm", p.apply("lexer/LookaheadLexer.java"), context);

        // Nodes and tokens
        processResource(executor, "inode.vm", p.map("node/INode.java"), context);
        processResource(executor, "node.vm", p.map("node/Node.java"), context);
        processResource(executor, "itoken.vm", p.map("node/IToken.java"), context);
        processResource(executor, "token.vm", p.map("node/Token.java"), context);

        processResource(executor, "token-fixed.vm", p.map("node/EOF.java"), context, ImmutableMap.<String, Object>of("token", TokenModel.EOF.INSTANCE));
        processResource(executor, "token-variable.vm", p.map("node/InvalidToken.java"), context, ImmutableMap.<String, Object>of("token", TokenModel.Invalid.INSTANCE));
        for (TokenModel token : grammar.tokens.values()) {
            // LOG.info("Generating " + token + " from " + token.isFixed());
            if (token.isFixed())
                processResource(executor, "token-fixed.vm", p.map("node/" + token.getJavaTypeName() + ".java"), context, ImmutableMap.<String, Object>of("token", token));
            else
                processResource(executor, "token-variable.vm", p.map("node/" + token.getJavaTypeName() + ".java"), context, ImmutableMap.<String, Object>of("token", token));
        }

        // Analyses
        processResource(executor, "clonelistener.vm", p.map("node/CloneListener.java"), context);
        processResource(executor, "switchable.vm", p.map("node/Switchable.java"), context);
        processResource(executor, "switch.vm", p.map("node/Switch.java"), context);

        processResource(executor, "visitable.vm", p.map("node/Visitable.java"), context);
        processResource(executor, "visitor.vm", p.map("analysis/Visitor.java"), context);
        processResource(executor, "abstractvisitoradapter.vm", p.map("analysis/AbstractVisitorAdapter.java"), context);
        processResource(executor, "visitoradapter.vm", p.map("analysis/VisitorAdapter.java"), context);

        processResource(executor, "analysis.vm", p.map("analysis/Analysis.java"), context);
        processResource(executor, "analysisadapter.vm", p.map("analysis/AnalysisAdapter.java"), context);

        // It makes little sense to visit a lexer-only grammar depth-first, but
        // there's no reason why we wouldn't emit the degenerate case. It makes
        // things like Locator, ASTPrinter, and so forth work for lexer-only machines.
        processResource(executor, "depthfirstadapter.vm", p.map("analysis/DepthFirstAdapter.java"), context);
        processResource(executor, "reverseddepthfirstadapter.vm", p.map("analysis/ReversedDepthFirstAdapter.java"), context);
        processResource(executor, "treevisitoradapter.vm", p.map("analysis/TreeVisitorAdapter.java"), context);
        processResource(executor, "depthfirstvisitor.vm", p.map("analysis/DepthFirstVisitor.java"), context);

        if (!grammar.astProductions.isEmpty()) {

            // processResource(executor, "reverseddepthfirstadapter.vm", "analysis/ReversedDepthFirstAdapter.java"), context);
            processResource(executor, "iproduction.vm", p.map("node/IProduction.java"), context);
            processResource(executor, "ialternative.vm", p.map("node/IAlternative.java"), context);
            for (AstProductionModel production : grammar.astProductions.values()) {
                processResource(executor, "production.vm", p.map("node/" + production.getJavaTypeName() + ".java"), context, ImmutableMap.<String, Object>of("production", production));
                for (AstAlternativeModel alternative : production.alternatives.values()) {
                    processResource(executor, "alternative.vm", p.map("node/" + alternative.getJavaTypeName() + ".java"), context, ImmutableMap.<String, Object>of("production", production, "alternative", alternative));
                }
            }
        }

        // Documentation
        String packageJavadoc = grammar.getPackage().getJavadocComment();
        if (packageJavadoc != null)
            processResource(executor, "package-info.vm", p.map("package-info.java"), context, ImmutableMap.<String, Object>of("subpackage", "", "javadoc", packageJavadoc));
        processResource(executor, "package-info.vm", p.map("node/package-info.java"), context, ImmutableMap.<String, Object>of("subpackage", ".node", "javadoc", "/** Autogenerated abstract syntax tree model classes. */"));
        processResource(executor, "package-info.vm", p.map("analysis/package-info.java"), context, ImmutableMap.<String, Object>of("subpackage", ".analysis", "javadoc", "/** Autogenerated abstract syntax tree analysis and visitor classes. */"));
        processResource(executor, "package-info.vm", p.map("parser/package-info.java"), context, ImmutableMap.<String, Object>of("subpackage", ".parser", "javadoc", "/** Autogenerated parser classes. */"));
        processResource(executor, "package-info.vm", p.map("lexer/package-info.java"), context, ImmutableMap.<String, Object>of("subpackage", ".lexer", "javadoc", "/** Autogenerated lexer classes. */"));
    }

    @Override
    public void writeLexerMachine(@Nonnull PolyglotExecutor executor, @Nonnull GrammarModel grammar, @Nonnull EncodedStateMachine.Lexer lexerMachine) throws ExecutionException, IOException {
        PackageDirectoryMapper p = new PackageDirectoryMapper(grammar);
        ImmutableMap<String, Object> contextGlobal = newGlobalContext(grammar);
        ImmutableMap<String, Object> contextLocal = ImmutableMap.<String, Object>of("lexerMachine", lexerMachine);
        processResource(executor, "lexer.vm", p.map("lexer/" + lexerMachine.getLexerClassName() + ".java"), contextGlobal, contextLocal);
        processResource(executor, "abstractlexer.vm", p.map("lexer/" + lexerMachine.getLexerClassName("Abstract", "") + ".java"), contextGlobal, contextLocal); // requires property 'inline'.
        processResource(executor, "stringlexer.vm", p.map("lexer/" + lexerMachine.getLexerClassName("", "String") + ".java"), contextGlobal, contextLocal);
        write(executor, lexerMachine.getEncodedData(), p.map("lexer/" + lexerMachine.getLexerClassName() + ".dat"));
    }

    @Override
    public void writeParserMachine(@Nonnull PolyglotExecutor executor, @Nonnull GrammarModel grammar, @Nonnull EncodedStateMachine.Parser parserMachine) throws ExecutionException, IOException {
        PackageDirectoryMapper p = new PackageDirectoryMapper(grammar);
        ImmutableMap<String, Object> contextGlobal = newGlobalContext(grammar);
        ImmutableMap<String, Object> contextLocal = ImmutableMap.<String, Object>of("parserMachine", parserMachine);
        // LRAutomaton automaton = parserMachine.getAutomaton();
        processResource(executor, "parser.vm", p.map("parser/" + parserMachine.getParserClassName() + ".java"), contextGlobal, contextLocal);
        write(executor, parserMachine.getEncodedData(), p.map("parser/" + parserMachine.getParserClassName() + ".dat"));
    }
}
