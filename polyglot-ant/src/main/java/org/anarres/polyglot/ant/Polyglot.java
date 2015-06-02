/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.ant;

import java.io.File;
import org.anarres.polyglot.DebugHandler;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.anarres.polyglot.PolyglotEngine;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;

/**
 *
 * @author shevek
 */
public class Polyglot extends MatchingTask {

    private File srcDir;
    private File outputDirectory = new File(".");
    private File debugDirectory;

    public void setSrc(String d) {
        srcDir = getProject().resolveFile(d);
    }

    public void setOutputDirectory(String d) {
        outputDirectory = getProject().resolveFile(d);
    }

    public void setDebugDirectory(String d) {
        debugDirectory = getProject().resolveFile(d);
    }

    @Override
    public void execute() throws BuildException {
        if (srcDir == null)
            throw new BuildException("src is required.");

        DirectoryScanner ds = getDirectoryScanner(srcDir);
        for (String fileName : ds.getIncludedFiles()) {
            File file = new File(srcDir, fileName);
            try {
                PolyglotEngine engine = new PolyglotEngine(file, outputDirectory);
                if (debugDirectory != null) {
                    PolyglotEngine.mkdirs(debugDirectory, "debug directory");
                    engine.setDebugHandler(new DebugHandler.File(debugDirectory, file.getName()));
                }
                if (!engine.run())
                    throw new BuildException("PolyglotEngine failed:\n" + engine.getErrors());
            } catch (BuildException e) {
                throw e;
            } catch (Exception e) {
                throw new BuildException("Failed to process " + file, e);
            }
        }
    }

}
