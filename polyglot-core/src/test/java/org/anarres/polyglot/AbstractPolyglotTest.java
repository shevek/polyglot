/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import com.google.testing.compile.JavaFileObjects;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.tools.JavaFileObject;
import org.anarres.polyglot.output.OutputLanguage;
import org.apache.commons.lang.SystemUtils;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

/**
 *
 * @author shevek
 */
public abstract class AbstractPolyglotTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPolyglotTest.class);
    private static final String TEST_PARAMETER_NAME = "test.parameter";

    @CheckForNull
    public static String getTestParameterValue() {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(TEST_PARAMETER_NAME);
            }
        });
    }

    @Nonnull
    public static Predicate<CharSequence> getTestPredicate() {
        final String testParameterValue = getTestParameterValue();
        if (testParameterValue == null)
            return Predicates.alwaysTrue();
        return Predicates.containsPattern(testParameterValue);
    }

    protected static class TestFilePredicate implements Predicate<File> {

        private final boolean fast = System.getProperty("test.fast") != null;
        private final Predicate<CharSequence> testPredicate = getTestPredicate();

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
            if (!testPredicate.apply(file.getName()))
                return false;
            LOG.info("Accepting " + file);
            return true;
        }
    }

    protected final File destinationDir = new File("../polyglot-tests/build/generated-sources/polyglot-java");

    @Before
    public void setUp() throws IOException {
        PolyglotEngine.deleteChildren(destinationDir, "Test cleanup");
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

    protected void compile() throws MalformedURLException {
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
            // Compilation.Result result = Compilation.compile(Collections.<Processor>emptySet(), javaSources().getSubject(Truth.THROW_ASSERTION_ERROR, javaFileObjects));
            assert_().about(javaSources())
                    .that(javaFileObjects)
                    .compilesWithoutError();
            LOG.info("Compiling took " + stopwatch);
        }

        // assertTrue("Failing because conflicts exist.", automaton.getConflicts().isEmpty());
    }
}
