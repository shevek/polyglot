/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.model.GrammarModel;

/**
 *
 * @author shevek
 */
public class OutputData {

    private final String name;
    private final GrammarModel grammar;
    private final EncodedStateMachine.Lexer lexerMachine;
    private final List<? extends EncodedStateMachine.Parser> parserMachines;
    private final Set<? extends Option> options;

    public OutputData(
            @Nonnull String name,
            @Nonnull GrammarModel grammar,
            @CheckForNull EncodedStateMachine.Lexer lexerMachine,
            @Nonnull List<? extends EncodedStateMachine.Parser> parserMachines,
            @Nonnull Set<? extends Option> options) {
        this.name = name;
        this.grammar = grammar;
        this.lexerMachine = lexerMachine;
        this.parserMachines = parserMachines;
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
    public EncodedStateMachine.Lexer getLexerMachine() {
        return lexerMachine;
    }

    @Nonnull
    public List<? extends EncodedStateMachine.Parser> getParserMachines() {
        return parserMachines;
    }

    @Nonnull
    public Set<? extends Option> getOptions() {
        return options;
    }

}
