/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.maven;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.anarres.polyglot.DebugHandler;
import org.anarres.polyglot.PolyglotEngine;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;

/**
 *
 * @author shevek
 */
@Mojo(
        name = "polyglot",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresProject = true)
public class PolyglotMojo extends AbstractMojo {

    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${basedir}/src/main/polyglot")
    protected File sourceDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/polyglot")
    protected File outputDirectory;

    @Parameter
    protected File debugDirectory;

    @Parameter
    protected Set<String> includes = new HashSet<>();
    @Parameter
    protected Set<String> excludes = new HashSet<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Log log = getLog();

        try {
            if (!outputDirectory.exists())
                PolyglotEngine.mkdirs(outputDirectory, "output directory");

            SourceMapping mapping = new SuffixMapping("polyglot", Collections.<String>emptySet());
            SourceInclusionScanner scan = new SimpleSourceInclusionScanner(includes, excludes);
            scan.addSourceMapping(mapping);
            Set<File> files = scan.getIncludedSources(sourceDirectory, null);
            for (File file : files) {
                PolyglotEngine engine = new PolyglotEngine(file, outputDirectory);
                if (debugDirectory != null) {
                    PolyglotEngine.mkdirs(debugDirectory, "debug directory");
                    engine.setDebugHandler(new DebugHandler.File(debugDirectory, file.getName()));
                }
                if (!engine.run())
                    throw new MojoExecutionException("Polyglot failed:\n" + engine.getErrors());
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Polyglot failed", e);
        }
    }

}
