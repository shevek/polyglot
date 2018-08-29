/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

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

    @Test
    public void testSLR() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (File file : Files.fileTreeTraverser().preOrderTraversal(ROOT).filter(new TestFilePredicate()))
            parse(file, true);
        LOG.info("Generaring all parsers took " + stopwatch);
        compile();
    }

    @Test
    public void testLR1() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (File file : Files.fileTreeTraverser().preOrderTraversal(ROOT).filter(new TestFilePredicate()))
            parse(file, false);
        LOG.info("Generaring all parsers took " + stopwatch);
        compile();
    }

}
