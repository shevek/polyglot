/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.anarres.graphviz.builder.GraphVizGraph;
import org.anarres.graphviz.builder.GraphVizUtils;
import org.anarres.graphviz.builder.GraphVizable;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.PolyglotExecutor;
import org.anarres.polyglot.model.GrammarModel;

/**
 *
 * @author shevek
 */
public class GraphvizOutputWriter extends AbstractOutputWriter {

    public GraphvizOutputWriter(ErrorHandler errors, String grammarName, File destinationDir, Set<? extends Option> options) {
        super(errors, OutputLanguage.graphviz, grammarName, destinationDir, options);
    }

    private void write(@Nonnull File file, @Nonnull GraphVizable object) throws IOException {
        GraphVizGraph graph = GraphVizUtils.toGraphVizGraph(object);
        // This would be a good time to mutate the graph, highlight orphans, etc.
        graph.writeTo(file);
    }

    @Override
    public void writeModel(PolyglotExecutor executor, GrammarModel grammar, Map<? extends String, ? extends File> templates) throws ExecutionException, IOException {
        write(newDestinationFile("ast.dot"), grammar.getAstGraphVizable());
        write(newDestinationFile("cst.dot"), grammar.getCstGraphVizable());
        URL resource = Resources.getResource(getClass(), "graphviz/Makefile");
        Resources.asByteSource(resource).copyTo(Files.asByteSink(newDestinationFile("Makefile")));
        processTemplates(executor, templates, ImmutableMap.<String, Object>of());
    }

    @Override
    public void writeLexerMachine(PolyglotExecutor executor, GrammarModel grammar, EncodedStateMachine.Lexer lexerMachine) throws ExecutionException, IOException {
    }

    @Override
    public void writeParserMachine(PolyglotExecutor executor, GrammarModel grammar, EncodedStateMachine.Parser parserMachine) throws ExecutionException, IOException {
    }
}
