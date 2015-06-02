/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * Either a terminal or a nonterminal.
 *
 * @author shevek
 */
public interface CstProductionSymbol extends ProductionSymbol {

    // Could add getTransformPrototypes, which for TokenModel, would return 'self', thus simplifying a lot.
    @Nonnull
    public List<CstTransformPrototypeModel> getTransformPrototypes();
}
