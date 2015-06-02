/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import javax.annotation.Nonnull;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
public interface AstProductionSymbol extends ProductionSymbol, AstModel {

    @Nonnull
    @TemplateProperty
    public String getJavaTypeName();
}
