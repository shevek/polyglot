package org.anarres.polyglot.gradle;

import java.util.Collections;
import org.anarres.gradle.plugin.velocity.VelocityTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.AbstractTask;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class PolyglotPluginApplyTest {

    private Project project;

    @Before
    public void setUp() {
        project = ProjectBuilder.builder().build();
    }

    @Test
    public void testApply() {
        project.apply(Collections.singletonMap("plugin", "java"));
        project.apply(Collections.singletonMap("plugin", "org.anarres.polyglot"));
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
    }
}
