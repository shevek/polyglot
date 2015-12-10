/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import java.io.File;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;

/**
 *
 * @author shevek
 */
public enum OutputLanguage {

    java {
                @Override
                public OutputWriter newOutputWriter(File destinationDir, Set<? extends Option> options, Map<? extends String, ? extends File> templates, OutputData data) {
                    return new JavaOutputWriter(destinationDir, options, templates, data);
                }
            }, html {
                @Override
                public OutputWriter newOutputWriter(File destinationDir, Set<? extends Option> options, Map<? extends String, ? extends File> templates, OutputData data) {
                    return new HtmlOutputWriter(destinationDir, options, templates, data);
                }
            };

    public abstract OutputWriter newOutputWriter(@Nonnull File destinationDir, @Nonnull Set<? extends Option> options, @Nonnull Map<? extends String, ? extends File> templates, @Nonnull OutputData data);
}
