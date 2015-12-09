package org.anarres.polyglot.gradle;

import com.google.common.base.Throwables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Closure;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.anarres.gradle.plugin.velocity.VelocityTask;
import org.anarres.jdiagnostics.ProductMetadata;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.CopySpec;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

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
        final PolyglotPluginExtension extension = project.getExtensions().create("polyglot", PolyglotPluginExtension.class);
        final Configuration configuration = project.getConfigurations().create("polyglot");

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

        final Task polyglotGrammarTask = project.getTasks().create("polyglotGrammar", VelocityTask.class, new Action<VelocityTask>() {

            @Override
            public void execute(VelocityTask task) {

                task.conventionMapping("inputDir", new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        return project.file(extension.inputDir);
                    }
                });

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

                task.conventionMapping("outputDir", new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        return project.file(extension.intermediateDir);
                    }
                });

                task.setIncludeFilter("**/*.polyglot", "**/*.sablecc");
            }
        });

        final Task polyglotParserTask = project.getTasks().create("polyglotParser", Polyglot.class, new Action<Polyglot>() {
            @Override
            public void execute(Polyglot task) {
                task.dependsOn(polyglotGrammarTask);
                task.setDescription("Preprocesses Polyglot grammar files.");

                task.conventionMapping("inputDir", new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        return project.file(extension.intermediateDir);
                    }
                });

                task.conventionMapping("outputDir", new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        return project.file(extension.outputDir);
                    }
                });

                task.conventionMapping("debugDir", new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        if (extension.debugDir == null)
                            return null;
                        return project.file(extension.debugDir);
                    }
                });

                task.conventionMapping("templates", new Callable<Map<String, File>>() {
                    @Override
                    public Map<String, File> call() throws Exception {
                        Map<String, File> out = new HashMap<>();
                        for (Map.Entry<String, Object> e : extension.templates.entrySet())
                            out.put(e.getKey(), project.file(e.getValue()));
                        return out;
                    }
                });
            }
        });

        project.getTasks().getByName("compileJava").dependsOn(polyglotParserTask);
        SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();

        /*
         sourceSets.all(new Action<SourceSet>() {
         @Override
         public void execute(SourceSet t) {
         // Add a source set convention?
         DefaultPolyglotSourceSet polyglotSourceSet = new DefaultPolyglotSourceSet(((DefaultSourceSet) t).getDisplayName(), fileResolver);
         new DslObject(t).getConvention().getPlugins().put("polyglot", polyglotSourceSet);
         polyglotSourceSet.getPolyglot().srcDir(String.format("src/%s/groovy", sourceSet.getName()));
         }
         });
         */
        final SourceSet mainSourceSet = sourceSets.getByName("main");
        mainSourceSet.getJava().srcDir(extension.outputDir);

        Task polyglotResourcesTask = project.getTasks().create("polyglotResources", Copy.class, new Action<Copy>() {

            @Override
            @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")   // into, from and include do have side effects.
            public void execute(Copy task) {
                task.setDescription("Copies Polyglot resource files.");
                task.into(mainSourceSet.getOutput().getResourcesDir());
                task.from(extension.outputDir, new Closure<Void>(PolyglotPlugin.this) {
                    @Override
                    public Void call(Object... args) {
                        CopySpec spec = (CopySpec) args[0];
                        return doCall(spec);
                    }

                    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")   // include does have side effects.
                    private Void doCall(CopySpec spec) {
                        spec.include("**/*.dat");
                        return null;
                    }
                });
            }
        });
        project.getTasks().getByName("classes").dependsOn(polyglotResourcesTask);

    }

}
