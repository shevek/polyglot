/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
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

    public JavaOutputWriter(
            @Nonnull ErrorHandler errors,
            @Nonnull File destinationDir,
            @Nonnull Map<? extends String, ? extends File> templates,
            @Nonnull OutputData outputData) {
        super(errors, OutputLanguage.java, destinationDir, templates, outputData);
        this.helper = new JavaHelper(outputData.getOptions(), outputData.getGrammar());
    }

    @Override
    protected File newDestinationFile(@Nonnull String dstFilePath) throws IOException {
        File dstRoot = new File(getDestinationDir(), getGrammar().getPackage().getPackagePath());
        File dstFile = new File(dstRoot, dstFilePath);
        // Reconstruct this as dstFilePath may contain a slash.
        return newDestinationFile(dstFile);
    }

    @Override
    protected void initContext(@Nonnull VelocityContext context) {
        super.initContext(context);
        context.put("helper", helper);
    }

    @Override
    public void run(@Nonnull PolyglotExecutor executor) throws InterruptedException, ExecutionException, IOException {
        GrammarModel grammar = getGrammar();

        // Kick this off first, as it's the long pole.
        // Parser
        // if (!parserTables.isEmpty())
        process(executor, "parserexception.vm", "parser/ParserException.java");
        List<? extends EncodedStateMachine.Parser> parserMachines = getOutputData().getParserMachines();
        for (EncodedStateMachine.Parser parserMachine : parserMachines) {
            // LRAutomaton automaton = parserMachine.getAutomaton();
            process(executor, "parser.vm", "parser/" + parserMachine.getParserClassName() + ".java", ImmutableMap.<String, Object>of("parserMachine", parserMachine));
            process(executor, "start.vm", "node/" + parserMachine.getStartClassName() + ".java", ImmutableMap.<String, Object>of("parserMachine", parserMachine));
            write(executor, parserMachine.getEncodedData(), "parser/" + parserMachine.getParserClassName() + ".dat");
        }

        processTemplates(executor);

        // Lexer
        process(executor, "ilexer.vm", "lexer/ILexer.java");
        process(executor, "lexerexception.vm", "lexer/LexerException.java");
        EncodedStateMachine.Lexer lexerMachine = getOutputData().getLexerMachine();
        if (lexerMachine != null) {
            process(executor, "abstractlexer.vm", "lexer/AbstractLexer.java");
            process(executor, "lexer.vm", "lexer/Lexer.java");
            process(executor, "stringlexer.vm", "lexer/StringLexer.java");
            write(executor, lexerMachine.getEncodedData(), "lexer/Lexer.dat");
        }

        // Nodes and tokens
        process(executor, "inode.vm", "node/INode.java");
        process(executor, "node.vm", "node/Node.java");
        process(executor, "itoken.vm", "node/IToken.java");
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
        process(executor, "abstractvisitoradapter.vm", "analysis/AbstractVisitorAdapter.java");
        process(executor, "visitoradapter.vm", "analysis/VisitorAdapter.java");

        process(executor, "analysis.vm", "analysis/Analysis.java");
        process(executor, "analysisadapter.vm", "analysis/AnalysisAdapter.java");

        // It makes little sense to visit a lexer-only grammar depth-first, but
        // there's no reason why we wouldn't emit the degenerate case. It makes
        // things like Locator, ASTPrinter, and so forth work for lexer-only machines.
        process(executor, "depthfirstadapter.vm", "analysis/DepthFirstAdapter.java");
        process(executor, "treevisitoradapter.vm", "analysis/TreeVisitorAdapter.java");
        process(executor, "depthfirstvisitor.vm", "analysis/DepthFirstVisitor.java");

        if (!grammar.astProductions.isEmpty()) {
            process(executor, "reverseddepthfirstadapter.vm", "analysis/ReversedDepthFirstAdapter.java");

            // process(executor, "reverseddepthfirstadapter.vm", "analysis/ReversedDepthFirstAdapter.java");
            process(executor, "iproduction.vm", "node/IProduction.java");
            process(executor, "ialternative.vm", "node/IAlternative.java");
            for (AstProductionModel production : grammar.astProductions.values()) {
                process(executor, "production.vm", "node/" + production.getJavaTypeName() + ".java", ImmutableMap.<String, Object>of("production", production));
                for (AstAlternativeModel alternative : production.alternatives.values()) {
                    process(executor, "alternative.vm", "node/" + alternative.getJavaTypeName() + ".java", ImmutableMap.<String, Object>of("production", production, "alternative", alternative));
                }
            }
        }

        // Documentation
        String packageJavadoc = grammar.getPackage().getJavadocComment();
        if (packageJavadoc != null)
            process(executor, "package-info.vm", "package-info.java", ImmutableMap.<String, Object>of("subpackage", "", "javadoc", packageJavadoc));
        process(executor, "package-info.vm", "node/package-info.java", ImmutableMap.<String, Object>of("subpackage", ".node", "javadoc", "/** Autogenerated abstract syntax tree model classes. */"));
        process(executor, "package-info.vm", "analysis/package-info.java", ImmutableMap.<String, Object>of("subpackage", ".analysis", "javadoc", "/** Autogenerated abstract syntax tree analysis and visitor classes. */"));
        process(executor, "package-info.vm", "parser/package-info.java", ImmutableMap.<String, Object>of("subpackage", ".parser", "javadoc", "/** Autogenerated parser classes. */"));
        process(executor, "package-info.vm", "lexer/package-info.java", ImmutableMap.<String, Object>of("subpackage", ".lexer", "javadoc", "/** Autogenerated lexer classes. */"));
    }
}
