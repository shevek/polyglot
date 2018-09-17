/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import java.io.File;
import java.util.Map;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;

/**
 *
 * @author shevek
 */
public enum OutputLanguage {

    java {
                @Override
                public OutputWriter newOutputWriter(ErrorHandler errors, File destinationDir, Map<? extends String, ? extends File> templates, OutputData data) {
                    return new JavaOutputWriter(errors, destinationDir, templates, data);
                }
            },
    html {
                @Override
                public OutputWriter newOutputWriter(ErrorHandler errors, File destinationDir, Map<? extends String, ? extends File> templates, OutputData data) {
                    return new HtmlOutputWriter(errors, destinationDir, templates, data);
                }
            },
    graphviz {
                @Override
                public OutputWriter newOutputWriter(ErrorHandler errors, File destinationDir, Map<? extends String, ? extends File> templates, OutputData data) {
                    return new GraphvizOutputWriter(errors, destinationDir, templates, data);
                }
            };

    @Nonnull
    public abstract OutputWriter newOutputWriter(@Nonnull ErrorHandler errors, @Nonnull File destinationDir, @Nonnull Map<? extends String, ? extends File> templates, @Nonnull OutputData data);
}
