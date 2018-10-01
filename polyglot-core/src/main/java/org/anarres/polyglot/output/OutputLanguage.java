/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import java.io.File;
import java.util.Set;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.Option;

/**
 *
 * @author shevek
 */
public enum OutputLanguage {

    java {
        @Override
        public OutputWriter newOutputWriter(ErrorHandler errors, String grammarName, File destinationDir, Set<? extends Option> options) {
            return new JavaOutputWriter(errors, grammarName, destinationDir, options);
        }
    },
    html {
        @Override
        public OutputWriter newOutputWriter(ErrorHandler errors, String grammarName, File destinationDir, Set<? extends Option> options) {
            return new HtmlOutputWriter(errors, grammarName, destinationDir, options);
        }
    },
    graphviz {
        @Override
        public OutputWriter newOutputWriter(ErrorHandler errors, String grammarName, File destinationDir, Set<? extends Option> options) {
            return new GraphvizOutputWriter(errors, grammarName, destinationDir, options);
        }
    };

    @Nonnull
    public abstract OutputWriter newOutputWriter(@Nonnull ErrorHandler errors, @Nonnull String grammarName, @Nonnull File destinationDir, @Nonnull Set<? extends Option> options);
}
