/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import javax.annotation.Nonnull;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.Token;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
public interface ProductionSymbol {

    @Nonnull
    public Token getLocation();

    @Nonnull
    public String getName();

    @Nonnull
    public TIdentifier toNameToken();

    @TemplateProperty
    public boolean isTerminal();
}
