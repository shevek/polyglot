/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Function;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
public interface ProductionSymbol extends AnnotatedModel {

    // @Nonnull
    // public Token getLocation();
    @Nonnull
    public String getName();

    public static final Function<ProductionSymbol, String> FUNCTION_GET_DESCRIPTIVE_NAME = new Function<ProductionSymbol, String>() {
        @Override
        @SuppressFBWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public String apply(ProductionSymbol input) {
            return input.getDescriptiveName();
        }
    };

    @Nonnull
    public String getDescriptiveName();

    @Nonnull
    public TIdentifier toNameToken();

    @TemplateProperty
    public boolean isTerminal();

    // @Nonnull
    // @TemplateProperty
    // public Multimap<String, ? extends AnnotationModel> getAnnotations();
}
