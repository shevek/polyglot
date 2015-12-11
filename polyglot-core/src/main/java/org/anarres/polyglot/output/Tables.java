/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.StateModel;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.runtime.AbstractParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class Tables {

    public static final int MAX_INLINE_TABLE_LENGTH = 64000;

    @CheckForNull
    private final byte[] lexerData;
    @CheckForNull
    private final String lexerDataText;
    @CheckForNull
    private final byte[] parserData;
    @CheckForNull
    private final String parserDataText;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Tables(byte[] lexerData, String lexerDataText, byte[] parserData, String parserDataText) {
        this.lexerData = lexerData;
        this.lexerDataText = lexerDataText;
        this.parserData = parserData;
        this.parserDataText = parserDataText;
    }

    @CheckForNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte[] getLexerData() {
        return lexerData;
    }

    @TemplateProperty
    public boolean isLexerDataInline() {
        return lexerDataText != null;
    }

    @CheckForNull
    @TemplateProperty
    public String getLexerDataText() {
        return lexerDataText;
    }

    @CheckForNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte[] getParserData() {
        return parserData;
    }

    @TemplateProperty
    public boolean isParserDataInline() {
        return parserDataText != null;
    }

    @CheckForNull
    @TemplateProperty
    public String getParserDataText() {
        return parserDataText;
    }

    private static void toStringBuilder(@Nonnull StringBuilder buf, @CheckForNull byte[] data, @CheckForNull String text) {
        if (data != null) {
            buf.append(data.length).append("->");
            if (text != null)
                buf.append(text.length());
            else
                buf.append("no-inline");
        } else {
            buf.append("no-data");
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("lexer=");
        toStringBuilder(buf, lexerData, lexerDataText);

        buf.append(", parser=");
        toStringBuilder(buf, parserData, parserDataText);

        return buf.toString();
    }

    public static class Builder {

        @Nonnull
        private final GrammarModel grammar;
        @CheckForNull
        private final LRAutomaton automaton;
        private final boolean inline;

        public Builder(@Nonnull GrammarModel grammar, @CheckForNull LRAutomaton automaton, boolean inline) {
            this.grammar = grammar;
            this.automaton = automaton;
            this.inline = inline;
        }

        private static final Logger LOG = LoggerFactory.getLogger(Builder.class);

        @Nonnull
        private byte[] newLexerTable() throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(8192);
            try (DataOutputStream out = new DataOutputStream(buf)) {
                out.writeInt(grammar.states.size());
                for (StateModel lexerState : grammar.states.values()) {
                    DFA dfa = lexerState.dfa;
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

        // This takes an argument with a different nullability annotation than the field.
        @Nonnull
        private byte[] newParserTable(@Nonnull LRAutomaton automaton) throws IOException {
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
                        // LRAction value = e.getValue();
                        out.writeInt(e.getKey().getIndex());
                        out.writeInt(e.getValue().getAction().getOpcode());
                        Indexed target = e.getValue().getValue();
                        out.writeInt(target == null ? -1 : target.getIndex());
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
        private String newStringTable(@Nonnull byte[] table) throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (GZIPOutputStream out = new GZIPOutputStream(buf)) {
                out.write(table);
            }
            // return BaseEncoding.base64().encode(buf.toByteArray());
            return DatatypeConverter.printBase64Binary(buf.toByteArray());
        }

        @Nonnull
        public Tables build() throws IOException {
            byte[] lexerData = newLexerTable();
            String lexerDataText = newStringTable(lexerData);
            // LOG.debug("Lexer table is " + lexerDataText.length() + " characters from " + lexerData.length + " bytes.");
            if (lexerDataText.length() > MAX_INLINE_TABLE_LENGTH)
                lexerDataText = null;
            if (!inline)
                lexerDataText = null;

            byte[] parserData = null;
            String parserDataText = null;
            if (automaton != null) {
                parserData = newParserTable(automaton);
                parserDataText = newStringTable(parserData);
                // LOG.debug("Parser table is " + parserDataText.length() + " characters from " + parserData.length + " bytes.");
                if (parserDataText.length() > MAX_INLINE_TABLE_LENGTH)
                    parserDataText = null;
                if (!inline)
                    parserDataText = null;
            }

            return new Tables(lexerData, lexerDataText, parserData, parserDataText);
        }
    }
}
