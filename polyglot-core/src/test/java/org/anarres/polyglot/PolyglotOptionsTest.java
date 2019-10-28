/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
@RunWith(Parameterized.class)
public class PolyglotOptionsTest extends AbstractPolyglotTest {

    @SuppressWarnings("UnusedVariable")
    private static final Logger LOG = LoggerFactory.getLogger(PolyglotOptionsTest.class);
    public static final String FILE = "build/resources/test/grammars/positive/test-calculator.polyglot";

    private static void add(@Nonnull List<Object[]> out, @Nonnull Set<Option> options) {
        out.add(new Object[]{options});
    }

    private static void add(@Nonnull List<Object[]> out, @Nonnull Option... options) {
        EnumSet<Option> set = EnumSet.noneOf(Option.class);
        set.addAll(Arrays.asList(options));
        add(out, set);
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> data() {
        List<Object[]> out = new ArrayList<>();

        add(out, EnumSet.of(Option.LR1, Option.CG_INLINE_TABLES, Option.PARALLEL));
        add(out, EnumSet.of(Option.LR1, Option.CG_PARENT, Option.PARALLEL));
        add(out, EnumSet.of(Option.LR1, Option.CG_APIDOC, Option.PARALLEL));
        add(out, EnumSet.of(Option.LR1, Option.CG_FINDBUGS, Option.PARALLEL));
        add(out, EnumSet.of(Option.LR1, Option.CG_COMMENT, Option.PARALLEL));
        add(out, EnumSet.of(Option.LR1, Option.CG_JSR305, Option.PARALLEL));
        add(out, EnumSet.of(Option.LR1, Option.CG_JSR305_INTERNAL, Option.PARALLEL));
        add(out, EnumSet.of(Option.LR1, Option.CG_COMPACT, Option.PARALLEL));
        add(out, EnumSet.of(Option.LR1, Option.CG_COMPACT, Option.INLINE_EXPLICIT));
        add(out, EnumSet.of(Option.LR1, Option.PARALLEL));
        add(out, EnumSet.of(Option.LR1, Option.CG_LEXER_BINARYSEARCH));
        add(out, EnumSet.of(Option.LR1, Option.CG_LEXER_LINEARSEARCH));
        add(out, EnumSet.of(Option.LR1, Option.CG_LARGE, Option.CG_DEBUG));
        add(out, EnumSet.of(Option.LR1, Option.CG_LISTREFS_MUTABLE, Option.CG_SERIALIZE_THAW));
        // Fairly representative.
        add(out, EnumSet.of(Option.LR1, Option.CG_PARENT, Option.CG_JSR305, Option.CG_FINDBUGS, Option.CG_APIDOC));

        return out;
    }

    private final Set<Option> options;

    public PolyglotOptionsTest(Set<Option> options) {
        this.options = options;
    }

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
    public void testOptions() throws Exception {
        parse(new File(FILE), options);
    }
}