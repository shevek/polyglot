/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

/**
 *
 * @author shevek
 */
@RunWith(Parameterized.class)
public class PolyglotTest extends AbstractPolyglotTest {

    private static final Logger LOG = LoggerFactory.getLogger(PolyglotTest.class);

    public static final String DIR = "build/resources/test/grammars/positive";
    private static final File ROOT = new File(DIR);

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> data() {
        List<Object[]> out = new ArrayList<>();
        for (File file : Iterables.filter(Files.fileTraverser().depthFirstPreOrder(ROOT), new TestFilePredicate())) {
            out.add(new Object[]{file});
        }
        return out;
    }

    @BeforeClass
    public static void setUpClass() {
        LOG.info("Root dir is " + ROOT);
        assertTrue(ROOT.isDirectory());
    }

    private final File file;

    public PolyglotTest(File file) {
        this.file = file;
    }

    private void parse(@Nonnull File file, boolean is_slr) throws Exception {
        // File dst = new File("build/test/velocity/" + file.getName());
        PolyglotEngine engine = new PolyglotEngine(file, destinationDir);
        setUp(engine, file);

        // engine.setOption(Option.SLR, false);
        // engine.setOption(Option.PARALLEL, false);
        if (file.getName().equals("php4.sablecc"))
            engine.setOption(Option.ALLOWMASKEDTOKENS, true);
        engine.setOption(Option.SLR, is_slr);
        engine.setOption(Option.LR1, !is_slr);
        if (!engine.run())
            fail("Polyglot failed on " + file + ":\n" + engine.getErrors().toString(engine.getInput()));
    }

    @Test
    public void testSLR() throws Exception {
        // Some grammars can't be generated in SLR.
        assumeFalse("test-assignment.polyglot".equals(file.getName()));
        assumeFalse("test-double-inline.polyglot".equals(file.getName()));
        assumeFalse("test-inlining.polyglot".equals(file.getName()));
        assumeFalse("test-star-reduction.polyglot".equals(file.getName()));

        assumeFalse(file.getName().startsWith("private-"));

        Stopwatch stopwatch = Stopwatch.createStarted();
        parse(file, true);
        LOG.info("Generating parser took " + stopwatch);
        compile();
    }

    @Test
    public void testLR1() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        parse(file, false);
        LOG.info("Generating parser took " + stopwatch);
        compile();
    }

}
