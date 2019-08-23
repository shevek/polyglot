/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

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

/**
 *
 * @author shevek
 */
@RunWith(Parameterized.class)
public class PolyglotNegativeTest extends AbstractPolyglotTest {

    private static final Logger LOG = LoggerFactory.getLogger(PolyglotNegativeTest.class);

    public static final String DIR = "build/resources/test/grammars/negative";
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

    public PolyglotNegativeTest(@Nonnull File file) {
        this.file = file;
    }

    @Test
    public void parse() throws Exception {
        // File dst = new File("build/test/velocity/" + file.getName());
        PolyglotEngine engine = new PolyglotEngine(file, destinationDir);
        setUp(engine, file);
        engine.getOptions().add(Option.DIAGNOSIS);
        engine.getOptions().add(Option.VERBOSE);
        engine.getOptions().add(Option.LRK);

        if (engine.run())
            throw new Exception("Polyglot succeeded unexpectedly");
        LOG.info("Diagnostics are:\n" + engine.getErrors().toString(engine.getInput()));
        // assertTrue("Failing because conflicts exist.", automaton.getConflicts().isEmpty());
    }
}
