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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.DebugHandler;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.PolyglotEngine;
import org.anarres.polyglot.output.OutputLanguage;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.EmptyFileVisitor;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

/**
 *
 * @author shevek
 */
public class Polyglot extends SourceTask {

    private File inputDir;
    private File outputDir;
    @CheckForNull
    private File debugDir;
    @Nonnull
    private final Map<String, PolyglotTemplateSet> templates = new HashMap<>();
    @CheckForNull
    private Map<Option, Boolean> options;

    @Deprecated // Use setSource.
    public void setInputDir(File inputDir) {
        setSource(inputDir);
    }

    @Nonnull
    @OutputDirectory
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
    @OutputDirectory
    public File getDebugDir() {
        return debugDir;
    }

    /**
     * Sets the directory into which to output debug information.
     * If not set, then extra debugging output is disabled.
     */
    public void setDebugDir(@Nonnull File debugDir) {
        this.debugDir = debugDir;
    }

    @Input
    public Map<String, PolyglotTemplateSet> getTemplates() {
        return templates;
    }

    @InputFiles
    /* pp */ Collection<? extends File> getTemplateFiles() {
        List<File> out = new ArrayList<>();
        for (PolyglotTemplateSet templateSet : templates.values())
            out.addAll(templateSet.getTemplates().values());
        return out;
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

    @TaskAction
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
                        for (Map.Entry<Option, Boolean> e : options.entrySet()) {
                            engine.setOption(e.getKey(), e.getValue().booleanValue());
                        }
                    }
                    for (PolyglotTemplateSet templateSet : templates.values()) {
                        if (!templateSet.toSpec().isSatisfiedBy(file))
                            continue;
                        for (Map.Entry<OutputLanguage, Map<String, File>> e : templateSet.getTemplates().rowMap().entrySet())
                            engine.addTemplates(e.getKey(), e.getValue());
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
}
