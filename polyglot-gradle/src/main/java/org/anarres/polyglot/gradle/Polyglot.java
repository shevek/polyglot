/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.gradle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.jdiagnostics.DefaultQuery;
import org.anarres.polyglot.DebugHandler;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.PolyglotEngine;
import org.anarres.polyglot.output.OutputLanguage;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.EmptyFileVisitor;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
@CacheableTask
public class Polyglot extends SourceTask {

    private static final Logger LOG = LoggerFactory.getLogger(Polyglot.class);
    private static final ReentrantLock mutex = new ReentrantLock();
    private File outputDir;
    @CheckForNull
    private File debugDir;
    @Nonnull
    private final Map<String, PolyglotTemplateSet> templates = new HashMap<>();
    @CheckForNull
    private Map<Option, Boolean> options;
    @Nonnegative
    private int maxThreads = Integer.MAX_VALUE;

    @Deprecated // Use setSource.
    public void setInputDir(File inputDir) {
        getLogger().warn("Polyglot.setInputDir is deprecated. Please use Polyglot.setSource() from SourceTask.");
        setSource(inputDir);
    }

    @Nonnull
    @OutputDirectory
    // I don't think this is relevant for output directories.
    // @PathSensitive(PathSensitivity.RELATIVE)
    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(@Nonnull File outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Returns the directory into which to output debug information.
     * If not set, then extra debugging output is disabled.
     *
     * @return the directory into which to output debug information.
     */
    @CheckForNull
    @Optional
    @Input  // We only care about the directory name, not the contents.
    // @PathSensitive(PathSensitivity.RELATIVE)
    public File getDebugDir() {
        return debugDir;
    }

    /**
     * Sets the directory into which to output debug information.
     * If not set, then extra debugging output is disabled.
     */
    public void setDebugDir(@Nonnull Object debugDir) {
        this.debugDir = getProject().file(debugDir);
    }

    // The override exists only for the PathSensitive annotation.
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    @Override
    public FileTree getSource() {
        return super.getSource();
    }

    @Input
    public Map<String, PolyglotTemplateSet> getTemplates() {
        return templates;
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    /* pp */ Collection<? extends File> getTemplateFiles() {
        try {
            List<File> out = new ArrayList<>();
            for (PolyglotTemplateSet templateSet : templates.values())
                for (Object templateFile : templateSet.getTemplates().values())
                    out.add(getProject().file(templateFile));
            return out;
        } catch (LinkageError e) {
            // If we have a conflict on Guava versions, we can get a NoSuchMethodError here.
            LOG.warn(new DefaultQuery(e).call().toString());
            throw e;
        }
    }

    public void templates(@Nonnull String glob, @Nonnull Action<? super PolyglotTemplateSet> action) {
        PolyglotTemplateSet templateSet = templates.get(glob);
        if (templateSet == null) {
            templateSet = new PolyglotTemplateSet(glob);
            templates.put(glob, templateSet);
        }
        action.execute(templateSet);
    }

    public void templates(@Nonnull Action<? super PolyglotTemplateSet> action) {
        templates("*", action);
    }

    @Input
    @Optional
    public Map<Option, Boolean> getOptions() {
        return options;
    }

    public void setOptions(Map<Option, Boolean> options) {
        this.options = options;
    }

    public void option(@Nonnull String... names) {
        if (options == null)
            options = new EnumMap<>(Option.class);
        for (String name : names) {
            Boolean value = Boolean.TRUE;
            if (name.startsWith("-")) {
                name = name.substring(1);
                value = Boolean.FALSE;
            } else if (name.startsWith("+")) {
                name = name.substring(1);
            }
            Option option = Option.valueOf(name.toUpperCase());
            options.put(option, value);
        }
    }

    public void options(@Nonnull String... names) {
        option(names);
    }

    @Nonnegative
    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(@Nonnegative int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public void maxThreads(@Nonnegative int maxThreads) {
        setMaxThreads(maxThreads);
    }

    public void runPolyglot() throws IOException {
        // println "Reading from $inputDir"
        final File outputDir = getOutputDir();
        // println "Writing to $outputDir"
        PolyglotEngine.deleteChildren(outputDir, "output directory");
        PolyglotEngine.mkdirs(outputDir, "output directory");

        FileTree inputFiles = getSource();
        getLogger().info("Polyglot file tree is " + inputFiles);
        inputFiles.visit(new EmptyFileVisitor() {
            @Override
            public void visitFile(FileVisitDetails fvd) {
                File file = fvd.getFile();
                getLogger().info("Visiting " + file);
                try {
                    PolyglotEngine engine = new PolyglotEngine(file.getAbsoluteFile(), outputDir.getAbsoluteFile());
                    final File reportsDir = new File(getProject().getBuildDir(), "reports/polyglot");
                    engine.setOutputDir(OutputLanguage.html, new File(reportsDir, engine.getName()));
                    // engine.setOutputDir(OutputLanguage.graphviz, new File(reportsDir, engine.getName()));
                    File debugDir = getDebugDir();
                    if (debugDir != null) {
                        PolyglotEngine.mkdirs(debugDir, "debug directory");
                        engine.setDebugHandler(new DebugHandler.File(debugDir, file.getName()));
                    }
                    if (options != null) {
                        for (Map.Entry<Option, Boolean> e : getOptions().entrySet()) {
                            engine.setOption(e.getKey(), e.getValue().booleanValue());
                        }
                    }
                    engine.setMaxThreads(getMaxThreads());
                    for (PolyglotTemplateSet templateSet : templates.values()) {
                        if (!templateSet.toSpec().isSatisfiedBy(file))
                            continue;
                        for (Map.Entry<OutputLanguage, ? extends Map<? extends String, ? extends Object>> e : templateSet.getTemplates().rowMap().entrySet()) {
                            Map<String, File> temp = new HashMap<>(e.getValue().size());
                            // Resolve all files late against the project
                            for (Map.Entry<? extends String, ? extends Object> f : e.getValue().entrySet())
                                temp.put(f.getKey(), getProject().file(f.getValue()));
                            engine.addTemplates(e.getKey(), temp);
                        }
                    }
                    if (!engine.run())
                        throw new GradleException("Failed to process " + file + ":\n" + engine.getErrors().toString(engine.getInput()));
                } catch (GradleException e) {
                    throw e;
                } catch (Exception e) {
                    getLogger().error("Failed to process " + file + ": " + e);
                    throw new GradleException("Failed to process " + file, e);
                }
            }
        });
    }

    @TaskAction
    public void runPolyglotExclusively() throws IOException, InterruptedException {
        try {
            for (;;) {
                getLogger().debug("Attempting to acquire Polyglot mutex.");
                if (mutex.tryLock(10, TimeUnit.SECONDS))
                    break;
            }
            getLogger().debug("Successfully acquired Polyglot mutex.");
            runPolyglot();
        } finally {
            mutex.unlock();
            getLogger().debug("Successfully released Polyglot mutex.");
        }
    }
}
