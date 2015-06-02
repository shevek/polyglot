/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.cmd;

import java.io.File;
import javax.annotation.Nonnull;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.anarres.polyglot.DebugHandler;
import org.anarres.polyglot.PolyglotEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(@Nonnull String[] args) throws Exception {
        OptionParser parser = new OptionParser();
        OptionSpec<?> helpOption = parser.accepts("help", "Displays command-line help.").forHelp();
        OptionSpec<?> versionOption = parser.accepts("version", "Displays the product version and exits.").forHelp();

        OptionSpec<File> inputOption = parser.accepts("input", "Specifies the input grammar file or files.").withRequiredArg().ofType(File.class).describedAs("file").required();
        OptionSpec<File> outputOption = parser.accepts("output", "Specifies the output directory.").withRequiredArg().ofType(File.class).describedAs("dir").required();
        OptionSpec<File> debugOption = parser.accepts("debug", "Specifies the debug output directory.").withRequiredArg().ofType(File.class).describedAs("dir");

        OptionSet o = parser.parse(args);
        if (o.has(helpOption)) {
            parser.printHelpOn(System.err);
            System.exit(1);
        }
        if (o.has(versionOption)) {
            System.err.println("Polyglot Parser Generator.");
            System.exit(1);
        }

        File inputFile = o.valueOf(inputOption);

        File outputDir = o.valueOf(outputOption);
        PolyglotEngine.mkdirs(outputDir, "output directory");

        PolyglotEngine engine = new PolyglotEngine(inputFile, outputDir);
        if (o.hasArgument(debugOption)) {
            File debugDir = o.valueOf(debugOption);
            PolyglotEngine.mkdirs(debugDir, "debug directory");
            engine.setDebugHandler(new DebugHandler.File(debugDir, "polyglot-debug-" + inputFile.getName()));
        }

        if (!engine.run())
            LOG.error("Polyglot failed:\n" + engine.getErrors());
    }
}
