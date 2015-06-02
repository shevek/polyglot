/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import javax.annotation.Nonnull;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.Token;

/**
 *
 * @author shevek
 */
public interface Model {

    @Nonnull
    public Token getLocation();

    @Nonnull
    public Node toNode();
}
