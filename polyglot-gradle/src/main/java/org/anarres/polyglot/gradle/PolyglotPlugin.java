package org.anarres.polyglot.gradle;

import com.google.common.base.Throwables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Closure;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.anarres.gradle.plugin.velocity.VelocityTask;
import org.anarres.jdiagnostics.ProductMetadata;
import org.apache.velocity.tools.generic.EscapeTool;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.CopySpec;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

/**
 *
 * @author shevek
 */
public class PolyglotPlugin implements Plugin<Project> {

    private final FileResolver fileResolver;

    @Inject
    public PolyglotPlugin(@Nonnull FileResolver fileResolver) {
        this.fileResolver = fileResolver;
    }

    @Override
    public void apply(final Project project) {
        // project.getPluginManager().apply(JavaPlugin.class);

        final PolyglotPluginExtension extension = project.getExtensions().create("polyglot", PolyglotPluginExtension.class);
        final Configuration configuration = project.getConfigurations().create("polyglot")
                // .setVisible(false)
                .setDescription("The Polyglot parser generator used by this build.");

        try {
            ProductMetadata product = new ProductMetadata(PolyglotPlugin.class.getClassLoader());
            // project.getLogger().info("Polyglot ProductMetadata is:\n" + product);
            ProductMetadata.ModuleMetadata module = product.getModule("org.anarres.polyglot:polyglot-runtime");
            String version = (module != null) ? module.getVersion() : "+";

            project.getDependencies().add(configuration.getName(), "org.anarres.polyglot:polyglot-core:" + version);
            // project.getDependencies().add(JavaPlugin.COMPILE_CONFIGURATION_NAME, "org.anarres.polyglot:polyglot-runtime:" + version);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(
                new Action<SourceSet>() {
                    @Override
                    public void execute(SourceSet t) {
                        apply(project, t, extension);
                    }
                });
    }

    public static class SerializableEscapeTool extends EscapeTool implements Serializable {
    }

    private void apply(@Nonnull final Project project, @Nonnull final SourceSet sourceSet, @Nonnull final PolyglotPluginExtension extension) {
        final PolyglotSourceSet polyglotSourceSet = new PolyglotSourceSet(sourceSet.getName(), fileResolver);
        new DslObject(sourceSet).getConvention().getPlugins().put("polyglot", polyglotSourceSet);
        final String srcDir = String.format("src/%s/polyglot", sourceSet.getName());
        polyglotSourceSet.getPolyglot().srcDir(srcDir);
        sourceSet.getAllSource().source(polyglotSourceSet.getPolyglot());

        final String intermediateDir = String.format("build/generated-sources/polyglot/%s/grammar", sourceSet.getName());
        final String outputDir = String.format("build/generated-sources/polyglot/%s/java", sourceSet.getName());

        String polyglotGrammarTaskName = sourceSet.getTaskName("polyglot", "Grammar");
        final VelocityTask polyglotGrammarTask = project.getTasks().create(polyglotGrammarTaskName, VelocityTask.class, new Action<VelocityTask>() {

            @Override
            public void execute(VelocityTask task) {

                task.conventionMapping("includeDirs", new Callable<List<File>>() {
                    @Override
                    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
                    public List<File> call() {
                        List<Object> includeDirs = extension.includeDirs;
                        if (includeDirs == null)
                            return null;
                        List<File> out = new ArrayList<File>();
                        for (Object includeDir : includeDirs)
                            out.add(project.file(includeDir));
                        return out;
                    }
                });

                Map<String, Object> context = new HashMap<>();
                context.put("esc", new SerializableEscapeTool());
                task.setContextValues(context);
            }
        });
        polyglotGrammarTask.setSource(polyglotSourceSet.getPolyglot());
        polyglotGrammarTask.setOutputDir(project.file(intermediateDir));

        String polyglotParserTaskName = sourceSet.getTaskName("polyglot", "Parser");
        final Polyglot polyglotParserTask = project.getTasks().create(polyglotParserTaskName, Polyglot.class, new Action<Polyglot>() {
            @Override
            public void execute(Polyglot task) {
                task.dependsOn(polyglotGrammarTask);
                task.setDescription("Preprocesses Polyglot grammar files.");

                task.conventionMapping("debugDir", new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        if (extension.debugDir == null)
                            return null;
                        return project.file(extension.debugDir);
                    }
                });

                // task.conventionMapping("options", new Callable<>(){});
            }
        });
        polyglotParserTask.setSource(polyglotGrammarTask);
        polyglotParserTask.setOutputDir(project.file(outputDir));
        sourceSet.getJava().srcDir(polyglotParserTask.getOutputDir());

        project.getTasks().getByName(sourceSet.getCompileJavaTaskName()).dependsOn(polyglotParserTask);

        CopySpec processResourcesTask = (CopySpec) project.getTasks().getByName(sourceSet.getProcessResourcesTaskName());
        processResourcesTask.with(
                project.copySpec(Closure.IDENTITY)
                .from(polyglotParserTask.getOutputDir())
                .include("**/*.dat")
        );
    }
}
