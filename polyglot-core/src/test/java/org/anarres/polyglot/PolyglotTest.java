/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import java.io.File;
import javax.annotation.Nonnull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class PolyglotTest extends AbstractPolyglotTest {

    private static final Logger LOG = LoggerFactory.getLogger(PolyglotTest.class);

    public static final String DIR = "build/resources/test/grammars/positive";
    private static final File ROOT = new File(DIR);

    @BeforeClass
    public static void setUpClass() {
        LOG.info("Root dir is " + ROOT);
        assertTrue(ROOT.isDirectory());
    }

    private void parse(@Nonnull File file, boolean allow_slr) throws Exception {
        // File dst = new File("build/test/velocity/" + file.getName());
        PolyglotEngine engine = new PolyglotEngine(file, destinationDir);
        setUp(engine, file);

        // engine.setOption(Option.SLR, false);
        // engine.setOption(Option.PARALLEL, false);
        if (file.getName().equals("php4.sablecc"))
            engine.setOption(Option.ALLOWMASKEDTOKENS, true);
        engine.setOption(Option.SLR, allow_slr);
        if (!engine.run())
            fail("Polyglot failed on " + file + ":\n" + engine.getErrors().toString(engine.getInput()));
    }

    private static class FilePredicate implements Predicate<File> {

        private final boolean fast = System.getProperty("test.fast") != null;

        @Override
        public boolean apply(File file) {
            if (!file.isFile())
                return false;
            if (file.getName().startsWith("."))
                return false;
            if (!file.getName().endsWith(".polyglot"))
                if (!file.getName().endsWith(".sablecc"))
                    return false;
            if (fast)
                if (file.length() > 16384)
                    // if (file.getName().startsWith("private-"))
                    return false;
            // if (!file.getName().equals("java-type.polyglot"))
            // if (!file.getName().equals("php4.sablecc"))
            // if (!file.getName().equals("polyglot.polyglot"))
            // if (!file.getName().equals("polyglot-simple.polyglot"))
            // if (!file.getName().equals("private-jccfe.polyglot"))
            // if (!file.getName().equals("private-pig.polyglot"))
            // if (!file.getName().equals("private-plsql-compiler-medium.polyglot"))
            // if (!file.getName().equals("private-plsql-compiler.polyglot"))
            // if (!file.getName().equals("private-plsql-compiler.polyglot")) continue;
            // if (!file.getName().equals("private-plsql-compiler-small.polyglot"))
            // if (!file.getName().equals("private-vertica.polyglot"))
            // if (!file.getName().equals("test-abstracts.polyglot"))
            // if (!file.getName().equals("test-annotations.polyglot"))
            // if (!file.getName().equals("test-assignment.polyglot"))
            // if (!file.getName().equals("test-calculator.polyglot"))
            // if (!file.getName().equals("test-cstnames.polyglot"))
            // if (!file.getName().equals("test-diagnostics-root.polyglot"))
            // if (!file.getName().equals("test-documentation.polyglot"))
            // if (!file.getName().equals("test-double-inline.polyglot"))
            // if (!file.getName().equals("test-erasure.polyglot"))
            // if (!file.getName().equals("test-expression-lr1.polyglot"))
            // if (!file.getName().equals("test-expression.polyglot"))
            // if (!file.getName().equals("test-externals.polyglot"))
            // if (!file.getName().equals("test-inlining.polyglot"))
            // if (!file.getName().equals("test-lookaheads.polyglot"))
            // if (!file.getName().equals("test-multilexers.polyglot"))
            // if (!file.getName().equals("test-multistart.polyglot"))
            // if (!file.getName().equals("test-parserignore.polyglot"))
            // if (!file.getName().equals("test-no-lexer.polyglot"))
            // if (!file.getName().equals("test-nonmasked.polyglot"))
            // if (!file.getName().equals("test-no-parser.polyglot"))
            // if (!file.getName().equals("test-notransform.polyglot"))
            // if (!file.getName().equals("test-nullable.polyglot"))
            // if (!file.getName().equals("test-star-reduction.polyglot"))
            // if (!file.getName().equals("test-token-constant.polyglot"))
            // if (!file.getName().equals("test-transform.polyglot"))
            // if (!file.getName().equals("test-weak.polyglot"))
            // if (!file.getName().equals("test-lexer.polyglot"))
            // return false;
            LOG.info("Accepting " + file);
            return true;
        }
    }

    @Test
    public void testSLR() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (File file : Files.fileTreeTraverser().preOrderTraversal(ROOT).filter(new FilePredicate()))
            parse(file, true);
        LOG.info("Generaring all parsers took " + stopwatch);
        compile();
    }

    @Test
    public void testLR1() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (File file : Files.fileTreeTraverser().preOrderTraversal(ROOT).filter(new FilePredicate()))
            parse(file, false);
        LOG.info("Generaring all parsers took " + stopwatch);
        compile();
    }

}
