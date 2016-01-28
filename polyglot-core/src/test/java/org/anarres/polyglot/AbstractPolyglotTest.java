/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import java.io.File;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.anarres.polyglot.output.OutputLanguage;
import org.apache.commons.lang.SystemUtils;
import org.junit.Before;

/**
 *
 * @author shevek
 */
public abstract class AbstractPolyglotTest {

    protected final File destinationDir = new File("../polyglot-tests/build/generated-sources/polyglot-java");

    @Before
    public void setUp() {
        destinationDir.mkdirs();
    }

    public static void setUp(@Nonnull PolyglotEngine engine, @Nonnull File file) throws IOException {
        File debugDir = new File(SystemUtils.getUserDir(), "build/polyglot-debug");
        PolyglotEngine.mkdirs(debugDir, "Debug directory");
        engine.setDebugHandler(new DebugHandler.File(debugDir, file.getName()));

        File htmlDir = new File(SystemUtils.getUserDir(), "build/polyglot-output/" + file.getName() + "/html");
        PolyglotEngine.mkdirs(htmlDir, "HTML directory");
        engine.setOutputDir(OutputLanguage.html, htmlDir);

        File graphvizDir = new File(SystemUtils.getUserDir(), "build/polyglot-output/" + file.getName() + "/graphviz");
        PolyglotEngine.mkdirs(graphvizDir, "GraphViz directory");
        engine.setOutputDir(OutputLanguage.graphviz, graphvizDir);
    }
}
