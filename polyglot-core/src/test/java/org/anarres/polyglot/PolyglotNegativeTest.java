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
public class PolyglotNegativeTest {

    private static final Logger LOG = LoggerFactory.getLogger(PolyglotNegativeTest.class);

    public static final String DIR = "build/resources/test/grammars/negative";

    private void parse(@Nonnull File file) throws Exception {
        // File dst = new File("build/test/velocity/" + file.getName());
        File dst = new File("../polyglot-tests/build/generated-sources/polyglot-java");
        dst.mkdirs();
        PolyglotEngine engine = new PolyglotEngine(file, dst);
        // engine.setDebugHandler(new DebugHandler.File(SystemUtils.getUserDir(), file.getName()));
        // engine.setOption(Option.SLR, false);
        // engine.setOption(Option.PARALLEL, false);
        if (engine.run())
            throw new Exception("Polyglot succeeded unexpectedly");
            // LOG.error("Polyglot succeeded unexpectedly.");
        LOG.info("Diagnostics are:\n" + engine.getErrors());

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

            // if (!file.getName().equals("test-diagnostics-root.polyglot"))
            // continue;
            parse(file);
        }
    }
}
