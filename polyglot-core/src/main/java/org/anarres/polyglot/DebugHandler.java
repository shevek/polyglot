/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import com.google.common.io.CharSink;
import com.google.common.io.Files;
import java.nio.charset.StandardCharsets;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public interface DebugHandler {

    public static class None implements DebugHandler {

        public static final None INSTANCE = new None();

        @Override
        public CharSink forTarget(Target target, String suffix) {
            return null;
        }
    }

    public static class File implements DebugHandler {

        private final java.io.File dir;
        private final String prefix;

        /**
         * Constructs a new File DebugHandler.
         *
         * @param dir The output directory, which must pre-exist.
         * @param prefix The prefix to attach to all generated files.
         */
        public File(@Nonnull java.io.File dir, @Nonnull String prefix) {
            this.dir = dir;
            this.prefix = prefix;
        }

        @Override
        public CharSink forTarget(Target target, String suffix) {
            return Files.asCharSink(new java.io.File(dir, prefix + suffix), StandardCharsets.UTF_8);
        }
    }

    /** Metadata about the type of a debug target. */
    public static enum TargetType {

        GRAMMAR, DOT, TEXT;
    }

    /** Metadata about a debug target. */
    public static enum Target {

        GRAMMAR_PARSED(TargetType.GRAMMAR), GRAMMAR_LINKED(TargetType.GRAMMAR), GRAMMAR_NORMALIZED(TargetType.GRAMMAR), GRAMMAR_SUBSTITUTED(TargetType.GRAMMAR),
        GRAMMAR_CST(TargetType.DOT), GRAMMAR_AST(TargetType.DOT),
        STATE_NFA(TargetType.DOT), STATE_DFA(TargetType.DOT),
        FUNCTIONS(TargetType.TEXT),
        AUTOMATON_LR0_DESC(TargetType.TEXT), AUTOMATON_LR0(TargetType.DOT),
        AUTOMATON_LR1_DESC(TargetType.TEXT), AUTOMATON_LR1(TargetType.DOT),;
        private final TargetType type;

        private Target(@Nonnull TargetType type) {
            this.type = type;
        }

        @Nonnull
        public TargetType getType() {
            return type;
        }
    }

    @CheckForNull
    public CharSink forTarget(@Nonnull Target target, @Nonnull String suffix);
}
