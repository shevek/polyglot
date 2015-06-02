/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.gradle;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.DebugHandler;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.PolyglotEngine;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.EmptyFileVisitor;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/**
 *
 * @author shevek
 */
public class Polyglot extends ConventionTask {

    private File inputDir;
    private File outputDir;
    @CheckForNull
    private File debugDir;
    @CheckForNull
    private Map<String, File> templates;
    @CheckForNull
    private Map<Option, Boolean> options;

    @Nonnull
    @InputDirectory
    public File getInputDir() {
        return inputDir;
    }

    public void setInputDir(File inputDir) {
        this.inputDir = inputDir;
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
    public Map<String, File> getTemplates() {
        return templates;
    }

    @InputFiles
    /* pp */ Collection<? extends File> getTemplateFiles() {
        Map<?, File> t = getTemplates();    // Access through convention.
        if (t == null)
            return Collections.<File>emptyList();
        return t.values();
    }

    public void setTemplates(Map<String, File> templates) {
        this.templates = templates;
    }

    public void template(@Nonnull String path, @Nonnull Object file) {
        if (templates == null)
            templates = new HashMap<>();
        templates.put(path, getProject().file(file));
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
            options = new HashMap<>();
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

        ConfigurableFileTree inputFiles = getProject().fileTree(getInputDir());
        inputFiles.include("**/*.polyglot", "**/*.sablecc");
        getLogger().info("Polyglot file tree is " + inputFiles);
        inputFiles.visit(new EmptyFileVisitor() {
            @Override
            public void visitFile(FileVisitDetails fvd) {
                getLogger().info("Visiting " + fvd);
                try {
                    PolyglotEngine engine = new PolyglotEngine(fvd.getFile().getAbsoluteFile(), outputDir.getAbsoluteFile());
                    File debugDir = getDebugDir();
                    if (debugDir != null) {
                        PolyglotEngine.mkdirs(debugDir, "debug directory");
                        engine.setDebugHandler(new DebugHandler.File(debugDir, fvd.getName()));
                    }
                    if (options != null) {
                        for (Map.Entry<Option, Boolean> e : options.entrySet()) {
                            engine.setOption(e.getKey(), e.getValue().booleanValue());
                        }
                    }
                    Map<String, File> templates = getTemplates();
                    if (templates != null)
                        engine.addTemplates(templates);
                    if (!engine.run())
                        throw new GradleException("Failed to process " + fvd + ":\n" + engine.getErrors());
                } catch (GradleException e) {
                    throw e;
                } catch (Exception e) {
                    getLogger().error("Failed to process " + fvd + ": " + e);
                    throw new GradleException("Failed to process " + fvd, e);
                }
            }
        });
    }
}
