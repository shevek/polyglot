/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.anarres.polyglot.PolyglotExecutor;
import org.anarres.polyglot.model.GrammarModel;

/**
 *
 * @author shevek
 */
public interface OutputWriter {

    public static interface Callback {

        // We have to pull these out so that the lexerMachine variable in PolyglotEngine isn't final or effectively final.
        public static class Lexer implements Callback {

            private final PolyglotExecutor executor;
            private final GrammarModel grammar;
            private final EncodedStateMachine.Lexer lexerMachine;

            public Lexer(PolyglotExecutor executor, GrammarModel grammar, EncodedStateMachine.Lexer lexerMachine) {
                this.executor = executor;
                this.grammar = grammar;
                this.lexerMachine = lexerMachine;
            }

            @Override
            public void run(OutputWriter writer) throws ExecutionException, IOException {
                writer.writeLexerMachine(executor, grammar, lexerMachine);
            }

            @Override
            public String toString() {
                return "lexer machine '" + lexerMachine.getName() + "'";
            }
        }

        public static class Parser implements Callback {

            private final PolyglotExecutor executor;
            private final GrammarModel grammar;
            private final EncodedStateMachine.Parser parserMachine;

            public Parser(PolyglotExecutor executor, GrammarModel grammar, EncodedStateMachine.Parser parserMachine) {
                this.executor = executor;
                this.grammar = grammar;
                this.parserMachine = parserMachine;
            }

            @Override
            public void run(OutputWriter writer) throws ExecutionException, IOException {
                writer.writeParserMachine(executor, grammar, parserMachine);
            }

            @Override
            public String toString() {
                return "parser machine '" + parserMachine.getName() + "'";
            }
        }

        public void run(@Nonnull OutputWriter writer) throws ExecutionException, IOException;
    }

    @Nonnull
    public OutputLanguage getLanguage();

    public void writeModel(@Nonnull PolyglotExecutor executor, @Nonnull GrammarModel grammar, Map<? extends String, ? extends File> templates) throws ExecutionException, IOException;

    public void writeLexerMachine(@Nonnull PolyglotExecutor executor, @Nonnull GrammarModel grammar, @Nonnull EncodedStateMachine.Lexer lexerMachine) throws ExecutionException, IOException;

    public void writeParserMachine(@Nonnull PolyglotExecutor executor, @Nonnull GrammarModel grammar, @Nonnull EncodedStateMachine.Parser parserMachine) throws ExecutionException, IOException;
}
