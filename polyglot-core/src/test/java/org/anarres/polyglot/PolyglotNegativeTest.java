/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import com.google.common.io.Files;
import java.io.File;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class PolyglotNegativeTest extends AbstractPolyglotTest {

    private static final Logger LOG = LoggerFactory.getLogger(PolyglotNegativeTest.class);

    public static final String DIR = "build/resources/test/grammars/negative";

    private void parse(@Nonnull File file) throws Exception {
        // File dst = new File("build/test/velocity/" + file.getName());
        PolyglotEngine engine = new PolyglotEngine(file, destinationDir);
        setUp(engine, file);
        engine.getOptions().add(Option.DIAGNOSIS);
        engine.getOptions().add(Option.VERBOSE);

        if (engine.run())
            throw new Exception("Polyglot succeeded unexpectedly");
        LOG.info("Diagnostics are:\n" + engine.getErrors().toString(engine.getInput()));
        // assertTrue("Failing because conflicts exist.", automaton.getConflicts().isEmpty());
    }

    @Test
    public void testExperiments() throws Exception {
        File root = new File(DIR);
        LOG.info("Root dir is " + root);

        assertTrue(root.isDirectory());

        for (File file : Files.fileTreeTraverser().preOrderTraversal(root)) {
            LOG.info("File is " + file);
            if (!file.isFile())
                continue;
            if (file.getName().startsWith("."))
                continue;
            if (!file.getName().endsWith(".polyglot"))
                continue;

            // if (!file.getName().equals("test-diagnostics-root.polyglot")) continue;
            parse(file);
        }
    }
}
