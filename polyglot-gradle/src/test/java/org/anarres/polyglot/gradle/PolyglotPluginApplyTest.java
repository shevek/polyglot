package org.anarres.polyglot.gradle;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
@RunWith(Parameterized.class)
public class PolyglotPluginApplyTest {

    private static final Logger LOG = LoggerFactory.getLogger(PolyglotPluginApplyTest.class);

    @Nonnull
    private static Object[] A(Object... in) {
        return in;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() throws Exception {
        return Arrays.asList(
                // A("2.12"),
                // A("2.14"),
                // A("3.0"),
                // A("3.2.1"),
                // A("3.4.1"),
                // A("4.5.1"),
                // A("4.10.3"),
                A("5.4.1"),
                A("5.6"),
                A("5.6.3")
        );
    }

    private final String gradleVersion;
    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();
    public File testProjectBuildFile;

    @Before
    public void setUp() throws Exception {
        testProjectBuildFile = testProjectDir.newFile("build.gradle");
    }

    public PolyglotPluginApplyTest(String gradleVersion) {
        this.gradleVersion = gradleVersion;
    }

    @Test
    public void testApply() throws Exception {
        String text = "plugins { id 'java';\nid 'org.anarres.polyglot' }\n";
        Files.write(testProjectBuildFile.toPath(), Collections.singletonList(text));

        GradleRunner runner = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withPluginClasspath()
                .withDebug(true)
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("--stacktrace", "tasks");
        LOG.info("Building...\n\n");
        // System.out.println("ClassPath is " + runner.getPluginClasspath());
        BuildResult result = runner.build();
        LOG.info("Output:\n\n" + result.getOutput() + "\n\n");

        /*
         assertTrue("Project is missing plugin", project.getPlugins().hasPlugin(PolyglotPlugin.class));
         {
         Task task = project.getTasks().findByName("polyglotGrammar");
         assertNotNull("Project is missing polyglotGrammar task", task);
         assertTrue("Polyglot grammar task is the wrong type", task instanceof VelocityTask);
         assertTrue("Polyglot grammar task should be enabled", ((AbstractTask) task).isEnabled());
         }
         {
         Task task = project.getTasks().findByName("polyglotParser");
         assertNotNull("Project is missing polyglotParser task", task);
         assertTrue("Polyglot parser task is the wrong type", task instanceof Polyglot);
         assertTrue("Polyglot parser task should be enabled", ((AbstractTask) task).isEnabled());
         }
         */
    }
}
