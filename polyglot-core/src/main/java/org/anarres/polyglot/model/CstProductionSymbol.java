/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import java.util.List;
import javax.annotation.Nonnull;
import org.anarres.polyglot.output.TemplateProperty;

/**
 * Either a terminal or a nonterminal.
 *
 * @author shevek
 */
public interface CstProductionSymbol extends ProductionSymbol {

    /**
     * Returns the list of transform prototypes.
     *
     * @see #getTransformPrototype(int)
     * @see CstAlternativeModel#getTransformExpressions()
     * @see CstAlternativeModel#getTransformExpression(int)
     * @return The list of transform prototypes.
     */
    @Nonnull
    @TemplateProperty("parser.vm")
    public List<CstTransformPrototypeModel> getTransformPrototypes();
}
