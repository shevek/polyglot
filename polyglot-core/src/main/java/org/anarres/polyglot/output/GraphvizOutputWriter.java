/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.anarres.graphviz.builder.GraphVizGraph;
import org.anarres.graphviz.builder.GraphVizUtils;
import org.anarres.graphviz.builder.GraphVizable;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.PolyglotExecutor;

/**
 *
 * @author shevek
 */
public class GraphvizOutputWriter extends AbstractOutputWriter {

    public GraphvizOutputWriter(ErrorHandler errors, File destinationDir, Map<? extends String, ? extends File> templates, OutputData data) {
        super(errors, OutputLanguage.graphviz, destinationDir, templates, data);
    }

    private void write(@Nonnull File file, @Nonnull GraphVizable object) throws IOException {
        GraphVizGraph graph = GraphVizUtils.toGraphVizGraph(object);
        // This would be a good time to mutate the graph, highlight orphans, etc.
        graph.writeTo(file);
    }

    @Override
    public void run(PolyglotExecutor executor) throws InterruptedException, ExecutionException, IOException {
        write(newDestinationFile("ast.dot"), getGrammar().getAstGraphVizable());
        write(newDestinationFile("cst.dot"), getGrammar().getCstGraphVizable());
        URL resource = Resources.getResource(getClass(), "graphviz/Makefile");
        Resources.asByteSource(resource).copyTo(Files.asByteSink(newDestinationFile("Makefile")));
        processTemplates(executor);
    }

}
