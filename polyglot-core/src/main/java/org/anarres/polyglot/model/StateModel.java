/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import javax.annotation.Nonnull;
import org.anarres.polyglot.dfa.DFA;
import org.anarres.polyglot.dfa.NFA;
import org.anarres.polyglot.lr.Indexed;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
public class StateModel extends AbstractNamedModel implements Indexed {

    @Nonnull
    public static String name(@Nonnull TIdentifier identifier) {
        return identifier.getText().toUpperCase();
    }

    private final int index;
    public NFA nfa;
    public DFA dfa;

    public StateModel(int index, TIdentifier name) {
        super(name, name(name));
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getSourceName() {
        return super.getSourceName().toLowerCase();
    }

    public boolean isInitialState() {
        return getIndex() == 0;
    }

    @TemplateProperty
    public DFA getDfa() {
        return dfa;
    }

    @Override
    public TIdentifier toNode() {
        return toNameToken();
    }
}
