/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.collect.Iterables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.xml.bind.DatatypeConverter;
import org.anarres.polyglot.dfa.DFA;
import org.anarres.polyglot.lr.Indexed;
import org.anarres.polyglot.lr.LRAction;
import org.anarres.polyglot.lr.LRAutomaton;
import org.anarres.polyglot.lr.LRState;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstTransformPrototypeModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.runtime.AbstractParser;

/**
 *
 * @author shevek
 */
public class EncodedStateMachine {

    public static final int MAX_INLINE_TABLE_LENGTH = 64000;

    @CheckForNull
    public static EncodedStateMachine.Lexer forLexer(@Nonnull String name, @Nonnull GrammarModel grammar, List<DFA> dfas, boolean inline) throws IOException {
        if (grammar.tokens.isEmpty())
            return null;
        byte[] encodedData = newLexerTable(grammar, dfas);
        String encodedText = newStringTable(encodedData, inline ? MAX_INLINE_TABLE_LENGTH : 0);
        return new Lexer(name, encodedData, encodedText);
    }

    @Nonnull
    private static byte[] newLexerTable(@Nonnull GrammarModel grammar, @Nonnull List<DFA> dfas) throws IOException {
        if (grammar.states.size() != dfas.size())
            throw new IllegalArgumentException("Bad DFA count: states.size=" + grammar.states.size() + ", dfas.size=" + dfas.size());
        ByteArrayOutputStream buf = new ByteArrayOutputStream(8192);
        try (DataOutputStream out = new DataOutputStream(buf)) {
            out.writeInt(dfas.size());
            for (DFA dfa : dfas) {
                if (dfa == null) {
                    out.writeInt(0);
                    continue;
                }

                out.writeInt(dfa.getStates().size());

                for (DFA.State dfaState : dfa.getStates()) {
                    // Write gotoTable.
                    out.writeInt(dfaState.getTransitions().size());
                    for (DFA.Transition dfaTransition : dfaState.getTransitions()) {
                        out.writeInt(dfaTransition.getStart());
                        out.writeInt(dfaTransition.getEnd());
                        out.writeInt(dfaTransition.getDestination().getIndex());
                    }

                    // Write acceptTable.
                    out.writeInt(dfaState.getAcceptTokenIndex());
                }
            }
        }
        return buf.toByteArray();
    }

    @CheckForNull
    public static EncodedStateMachine.Parser forParser(@Nonnull String name, @Nonnull LRAutomaton automaton, @Nonnull CstProductionModel cstProductionRoot, boolean inline) throws IOException {
        IntOpenHashSet cstAlternativeSet = new IntOpenHashSet();
        byte[] encodedData = newParserTable(automaton, cstAlternativeSet);
        String encodedText = newStringTable(encodedData, inline ? MAX_INLINE_TABLE_LENGTH : 0);
        return new Parser(name, /* automaton, */ cstProductionRoot, cstAlternativeSet, encodedData, encodedText);
    }

    // This takes an argument with a different nullability annotation than the field.
    @Nonnull
    private static byte[] newParserTable(@Nonnull LRAutomaton automaton, @Nonnull IntSet cstAlternativeSet) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(8192);
        try (DataOutputStream out = new DataOutputStream(buf)) {
            out.writeInt(automaton.getStates().size()); // parserStateCount

            for (LRState state : automaton.getStates()) {

                out.writeInt(state.getActionMap().size() + 1);
                // Error is slot 0.
                out.writeInt(-1);
                out.writeInt(AbstractParser.ERROR);
                out.writeInt(state.getErrorIndex());

                for (Map.Entry<TokenModel, LRAction> e : state.getActionMap().entrySet()) {
                    // TokenModel key = e.getKey();
                    LRAction action = e.getValue();
                    out.writeInt(e.getKey().getIndex());
                    out.writeInt(action.getAction().getOpcode());
                    Indexed target = action.getTarget();
                    out.writeInt(target == null ? -1 : target.getIndex());

                    // Keep track of all the CstProductionModels which are reachable from this Parser.
                    if (action instanceof LRAction.Reduce) {
                        LRAction.Reduce reduceAction = (LRAction.Reduce) action;
                        cstAlternativeSet.add(reduceAction.getRule().getIndex());
                    }
                }

                out.writeInt(state.getGotoMap().size());
                for (Map.Entry<CstProductionModel, LRState> e : state.getGotoMap().entrySet()) {
                    // CstProductionModel key = e.getKey();
                    // LRState value = e.getValue();
                    out.writeInt(e.getKey().getIndex());
                    out.writeInt(e.getValue().getIndex());
                }
            }

            // Errors.
            out.writeInt(automaton.getErrors().size());
            for (String error : automaton.getErrors())
                out.writeUTF(error);
        }
        return buf.toByteArray();
    }

    @Nonnull
    private static String newStringTable(@Nonnull byte[] table) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(buf)) {
            out.write(table);
        }
        // return BaseEncoding.base64().encode(buf.toByteArray());
        return DatatypeConverter.printBase64Binary(buf.toByteArray());
    }

    @CheckForNull
    private static String newStringTable(@Nonnull byte[] table, int maxTextLength) throws IOException {
        if (maxTextLength <= 0)
            return null;
        String encodedText = newStringTable(table);
        if (encodedText.length() > maxTextLength)
            return null;
        return encodedText;
    }

    public static class Lexer extends EncodedStateMachine {

        private final String name;

        public Lexer(@Nonnull String name,
                @Nonnull byte[] encodedData, @CheckForNull String encodedText) {
            super(encodedData, encodedText);
            this.name = name;
        }

        @Nonnull
        public String getName() {
            return name;
        }

        @Nonnull
        public String getLexerClassName(@Nonnull String prefix, @Nonnull String infix) {
            return prefix + getName() + infix + "Lexer";
        }

        @Nonnull
        public String getLexerClassName() {
            return getLexerClassName("", "");
        }
    }

    public static class Parser extends EncodedStateMachine {

        private final String name;
        // private final LRAutomaton automaton;
        private final CstProductionModel cstProductionRoot;
        private final IntSet cstAlternativeSet;

        public Parser(@Nonnull String name, /* @Nonnull LRAutomaton automaton, */
                @Nonnull CstProductionModel cstProductionRoot, @Nonnull IntSet cstAlternativeSet,
                @Nonnull byte[] encodedData, @CheckForNull String encodedText) {
            super(encodedData, encodedText);
            this.name = name;
            // this.automaton = automaton;
            this.cstProductionRoot = cstProductionRoot;
            this.cstAlternativeSet = cstAlternativeSet;
        }

        @Nonnull
        public String getName() {
            return name;
        }

        @Nonnull
        public String getParserClassName() {
            return getName() + "Parser";
        }

        @Nonnull
        public String getStartClassName() {
            if (getName().isEmpty())
                return "Start";
            return "S" + getName();
        }

        // See also JavaHelper.
        // @Nonnull public LRAutomaton getAutomaton() { return automaton; }
        @Nonnull
        public CstProductionModel getCstProductionRoot() {
            return cstProductionRoot;
        }

        public boolean isCstAlternativeReachable(@Nonnull CstAlternativeModel cstAlternative) {
            return cstAlternativeSet.contains(cstAlternative.getIndex());
        }

        @Nonnull
        public AstProductionModel getAstProductionRoot() {
            CstTransformPrototypeModel transformPrototype = Iterables.getFirst(getCstProductionRoot().getTransformPrototypes(), null);
            if (transformPrototype == null)
                throw new IllegalStateException("CST production " + cstProductionRoot + " has no transform prototypes.");
            return transformPrototype.getAstProduction();
        }
    }

    @Nonnull
    private final byte[] encodedData;
    @CheckForNull
    private final String encodedText;

    private EncodedStateMachine(@Nonnull byte[] encodedData, @CheckForNull String encodedText) {
        this.encodedData = encodedData;
        this.encodedText = encodedText;
    }

    @Nonnull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte[] getEncodedData() {
        return encodedData;
    }

    @CheckForNull
    public String getEncodedText() {
        return encodedText;
    }

    public boolean isInline() {
        return encodedText != null;
    }

    private static void toStringBuilder(@Nonnull StringBuilder buf, @CheckForNull byte[] encodedData, @CheckForNull String encodedText) {
        if (encodedData != null) {
            buf.append(encodedData.length).append("->");
            if (encodedText != null)
                buf.append(encodedText.length());
            else
                buf.append("no-inline");
        } else {
            buf.append("no-data");
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Table(");
        toStringBuilder(buf, encodedData, encodedText);
        buf.append(")");
        return buf.toString();
    }
}
