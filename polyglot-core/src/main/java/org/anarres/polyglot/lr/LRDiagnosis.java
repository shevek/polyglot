/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.lr.LRDiagnosis.Frame;
import org.anarres.polyglot.lr.LRDiagnosis.Path;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionSymbol;

/**
 *
 * @author shevek
 */
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
public class LRDiagnosis extends HashMap<LRAction, Path> {

    public static interface Frame {

        public void toStringBuilder(@Nonnull StringBuilder buf);
    }

    public static class ItemFrame implements Frame {

        /** The production which we are representing. */
        private final CstAlternativeModel alternative;
        /** The position of the dot. */
        private final int position;
        private final String message;

        public ItemFrame(@Nonnull CstAlternativeModel alternative, @Nonnegative int position, @CheckForNull String message) {
            this.alternative = alternative;
            this.position = position;
            this.message = message;
        }

        public ItemFrame(@Nonnull CstAlternativeModel alternative, @Nonnegative int position) {
            this(alternative, position, null);
        }

        @Nonnull
        public CstAlternativeModel getAlternative() {
            return alternative;
        }

        @Override
        public void toStringBuilder(StringBuilder buf) {
            List<CstElementModel> elements = alternative.elements;
            buf.append(alternative.getProduction().getName()).append(" =");
            for (int i = 0; i < position; i++)
                buf.append(" ").append(elements.get(i).getSymbolName());
            buf.append(" .");
            for (int i = position; i < elements.size(); i++)
                buf.append(" ").append(elements.get(i).getSymbolName());

            buf.append("    [").append(alternative.getLocation().getLine()).append(":").append(alternative.getLocation().getColumn()).append("]");

            if (message != null)
                buf.append(": ").append(message);
        }

    }

    public static class MessageFrame implements Frame {

        private final String message;

        public MessageFrame(@Nonnull String message) {
            this.message = message;
        }

        @Override
        public void toStringBuilder(StringBuilder buf) {
            buf.append("Failed: ").append(message);
        }
    }

    public static class Path extends ArrayList<Frame> {

        public Path(@Nonnull Collection<? extends Frame> c) {
            super(c);
        }

        public void toStringBuilder(@Nonnull StringBuilder buf) {
            int indent = 1;
            for (Frame frame : this) {
                for (int i = 0; i < indent; i++)
                    buf.append("  ");
                buf.append("-> ");
                frame.toStringBuilder(buf);
                buf.append("\n");
                indent++;
            }
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            toStringBuilder(buf);
            return buf.toString();
        }
    }

    private transient final LRConflict conflict;

    public LRDiagnosis(@Nonnull LRConflict conflict) {
        this.conflict = conflict;
    }

    public void toStringBuilder(@Nonnull StringBuilder buf) {
        buf.append("Diagnosis for conflict in state ").append(conflict.getState().getName()).append(":\n");

        buf.append("  Stack is ");
        for (CstProductionSymbol symbol : conflict.getState().getStack())
            buf.append(symbol.getName()).append(' ');
        buf.append("\n");
        buf.append("  Lookahead token is ").append(conflict.getToken().getName()).append("\n");
        conflict.toStringBuilderBody(buf);

        for (Map.Entry<LRAction, Path> e : entrySet()) {
            buf.append("Justification for ").append(e.getKey()).append(":\n");
            e.getValue().toStringBuilder(buf);
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toStringBuilder(buf);
        return buf.toString();
    }
}
