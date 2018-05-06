/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class PolyglotOptionsTest extends AbstractPolyglotTest {

    private static final Logger LOG = LoggerFactory.getLogger(PolyglotOptionsTest.class);
    public static final String FILE = "build/resources/test/grammars/positive/test-calculator.polyglot";

    private void parse(@Nonnull File file, Set<Option> options) throws Exception {
        // File dst = new File("build/test/velocity/" + file.getName());
        PolyglotEngine engine = new PolyglotEngine(file, destinationDir);
        setUp(engine, file);
        engine.getOptions().clear();
        engine.getOptions().addAll(options);

        PolyglotEngine.deleteChildren(destinationDir, "option test destination dir");

        if (!engine.run())
            throw new Exception("Polyglot failed:\n" + engine.getErrors().toString(engine.getInput()));
        compile();
    }

    @Test
    public void testExperiments() throws Exception {
        File file = new File(FILE);
        LOG.info("File is " + file);

        assertTrue(file.isFile());
        parse(file, EnumSet.of(Option.LR1, Option.CG_INLINE_TABLES, Option.PARALLEL));
        parse(file, EnumSet.of(Option.LR1, Option.CG_PARENT, Option.PARALLEL));
        parse(file, EnumSet.of(Option.LR1, Option.CG_APIDOC, Option.PARALLEL));
        parse(file, EnumSet.of(Option.LR1, Option.CG_FINDBUGS, Option.PARALLEL));
        parse(file, EnumSet.of(Option.LR1, Option.CG_COMMENT, Option.PARALLEL));
        parse(file, EnumSet.of(Option.LR1, Option.CG_JSR305, Option.PARALLEL));
        parse(file, EnumSet.of(Option.LR1, Option.CG_JSR305_INTERNAL, Option.PARALLEL));
        parse(file, EnumSet.of(Option.LR1, Option.CG_COMPACT, Option.PARALLEL));
        parse(file, EnumSet.of(Option.LR1, Option.PARALLEL));
        parse(file, EnumSet.of(Option.LR1, Option.CG_LEXER_BINARYSEARCH));
        parse(file, EnumSet.of(Option.LR1, Option.CG_LEXER_LINEARSEARCH));
        parse(file, EnumSet.of(Option.LR1, Option.CG_LARGE));
        parse(file, EnumSet.of(Option.LR1, Option.CG_LISTREFS_MUTABLE));
        // Fairly representative.
        parse(file, EnumSet.of(Option.LR1, Option.CG_PARENT, Option.CG_JSR305, Option.CG_FINDBUGS, Option.CG_APIDOC));
    }
}
