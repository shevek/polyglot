/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.lr.LRAutomaton;
import org.anarres.polyglot.model.GrammarModel;

/**
 *
 * @author shevek
 */
public class OutputData {

    private final String name;
    private final GrammarModel grammar;
    private final LRAutomaton automaton;
    private final Tables tables;
    private final Set<? extends Option> options;

    public OutputData(@Nonnull String name, @Nonnull GrammarModel grammar, @CheckForNull LRAutomaton automaton, @Nonnull Tables tables, @Nonnull Set<? extends Option> options) {
        this.name = name;
        this.grammar = grammar;
        this.automaton = automaton;
        this.tables = tables;
        this.options = options;
    }

    @Nonnull
    public String getName() {
        return name;
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

    @Nonnull
    public Set<? extends Option> getOptions() {
        return options;
    }

}
