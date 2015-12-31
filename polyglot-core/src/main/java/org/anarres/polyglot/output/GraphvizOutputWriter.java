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
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.anarres.graphviz.builder.GraphVizUtils;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.PolyglotExecutor;

/**
 *
 * @author shevek
 */
public class GraphvizOutputWriter extends AbstractOutputWriter {

    public GraphvizOutputWriter(File destinationDir, Set<? extends Option> options, Map<? extends String, ? extends File> templates, OutputData data) {
        super(OutputLanguage.graphviz, destinationDir, options, templates, data);
    }

    @Override
    public void run(PolyglotExecutor executor) throws InterruptedException, ExecutionException, IOException {
        GraphVizUtils.toGraphVizFile(newDestinationFile("ast.dot"), getGrammar().getAstGraphVizable());
        GraphVizUtils.toGraphVizFile(newDestinationFile("cst.dot"), getGrammar().getCstGraphVizable());
        URL resource = Resources.getResource(getClass(), "graphviz/Makefile");
        Resources.asByteSource(resource).copyTo(Files.asByteSink(newDestinationFile("Makefile")));
        processTemplates(executor);
    }

}
