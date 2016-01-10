/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import com.google.testing.compile.JavaFileObjects;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class PolyglotTest extends AbstractPolyglotTest {

    private static final Logger LOG = LoggerFactory.getLogger(PolyglotTest.class);

    public static final String DIR = "build/resources/test/grammars/positive";

    private void parse(@Nonnull File file) throws Exception {
        // File dst = new File("build/test/velocity/" + file.getName());
        PolyglotEngine engine = new PolyglotEngine(file, destinationDir);
        setUp(engine, file);

        // engine.setOption(Option.SLR, false);
        // engine.setOption(Option.PARALLEL, false);
        if (file.getName().equals("php4.sablecc"))
            engine.setOption(Option.ALLOWMASKEDTOKENS, true);
        if (!engine.run())
            throw new Exception("Polyglot failed:\n" + engine.getErrors().toString(engine.getInput()));
    }

    private void compile() throws MalformedURLException {
        List<JavaFileObject> javaFileObjects = new ArrayList<>();
        {
            LOG.info("Collecting file objects.");
            Stopwatch stopwatch = Stopwatch.createStarted();
            for (File javaFile : Files.fileTreeTraverser().preOrderTraversal(destinationDir)) {
                if (!javaFile.isFile())
                    continue;
                if (!javaFile.getName().endsWith(".java"))
                    continue;
                javaFileObjects.add(JavaFileObjects.forResource(javaFile.toURI().toURL()));
            }
            LOG.info("Collecting file objects took " + stopwatch);
        }

        {
            LOG.info("Compiling.");
            Stopwatch stopwatch = Stopwatch.createStarted();
            assert_().about(javaSources())
                    .that(javaFileObjects)
                    .compilesWithoutError();
            LOG.info("Compiling took " + stopwatch);
        }

        // assertTrue("Failing because conflicts exist.", automaton.getConflicts().isEmpty());
    }

    @Test
    public void testExperiments() throws Exception {
        File root = new File(DIR);
        LOG.info("Root dir is " + root);

        assertTrue(root.isDirectory());

        Stopwatch stopwatch = Stopwatch.createStarted();
        for (File file : Files.fileTreeTraverser().preOrderTraversal(root)) {
            LOG.info("File is " + file);
            if (!file.isFile())
                continue;
            if (file.getName().startsWith("."))
                continue;
            if (!file.getName().endsWith(".polyglot"))
                if (!file.getName().endsWith(".sablecc"))
                    continue;

            // if (file.getName().equals("private-vertica.polyglot")) continue;
            // if (file.getName().equals("private-plsql-compiler.polyglot")) continue;
            // if (!file.getName().equals("java-type.polyglot"))
            // if (!file.getName().equals("php4.sablecc"))
            // if (!file.getName().equals("polyglot.polyglot"))
            // if (!file.getName().equals("polyglot-simple.polyglot"))
            // if (!file.getName().equals("private-jccfe.polyglot"))
            // if (!file.getName().equals("private-pig.polyglot"))
            // if (!file.getName().equals("test-star-reduction.polyglot"))
            // if (!file.getName().equals("test-assignment.polyglot"))
            // if (!file.getName().equals("test-erasure.polyglot"))
            // if (!file.getName().equals("test-expression.polyglot"))
            // if (!file.getName().equals("test-expression-lr1.polyglot"))
            // if (!file.getName().equals("test-transform.polyglot"))
            // if (!file.getName().equals("test-no-parser.polyglot"))
            // if (!file.getName().equals("test-no-lexer.polyglot"))
            // if (!file.getName().equals("test-token-constant.polyglot"))
            // if (!file.getName().equals("test-documentation.polyglot"))
            // if (!file.getName().equals("test-notransform.polyglot"))
            // if (!file.getName().equals("test-calculator.polyglot"))
            // if (!file.getName().equals("test-nullable.polyglot"))
            // if (!file.getName().equals("test-externals.polyglot"))
            // if (!file.getName().equals("private-plsql-compiler.polyglot"))
            // if (!file.getName().equals("private-plsql-compiler-small.polyglot"))
            // if (!file.getName().equals("test-inlining.polyglot"))
            // if (!file.getName().equals("test-double-inline.polyglot"))
            // if (!file.getName().equals("test-diagnostics-root.polyglot"))
            // if (!file.getName().equals("test-cstnames.polyglot"))
            // if (!file.getName().equals("test-nonmasked.polyglot"))
            // if (!file.getName().equals("private-vertica.polyglot"))
            // if (!file.getName().equals("test-annotations.polyglot"))
            // continue;
            parse(file);
        }
        LOG.info("Generaring all parsers took " + stopwatch);

        compile();
    }
}
