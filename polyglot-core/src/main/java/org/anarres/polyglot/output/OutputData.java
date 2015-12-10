/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.lr.LRAutomaton;
import org.anarres.polyglot.model.GrammarModel;

/**
 *
 * @author shevek
 */
public class OutputData {

    private final GrammarModel grammar;
    private final LRAutomaton automaton;
    private final Tables tables;

    public OutputData(@Nonnull GrammarModel grammar, @CheckForNull LRAutomaton automaton, @Nonnull Tables tables) {
        this.grammar = grammar;
        this.automaton = automaton;
        this.tables = tables;
    }

    @Nonnull
    public GrammarModel getGrammar() {
        return grammar;
    }

    @CheckForNull
    public LRAutomaton getAutomaton() {
        return automaton;
    }

    @Nonnull
    public Tables getTables() {
        return tables;
    }

}
