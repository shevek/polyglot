/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.test.common;

import com.google.common.base.Preconditions;
import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public class IndentedOutput {

    private final StringBuilder buf;

    public IndentedOutput(@Nonnull StringBuilder buf) {
        this.buf = Preconditions.checkNotNull(buf, "Buffer was null.");
    }

    public IndentedOutput() {
        this(new StringBuilder());
    }

    private static enum State {

        NL, WORD;
    }

    private int indent = 0;
    private State state = State.NL;

    /** Called by TranspilerEmitter. */
    public void reset() {
        indent = 0;
        state = State.NL;
        buf.setLength(0);
    }

    public boolean isNewLine() {
        return state == State.NL;
    }

    @Nonnull
    public IndentedOutput nl() {
        if (!isNewLine()) {
            buf.append("\n");
            state = State.NL;
        }
        return this;
    }

    @Nonnegative
    public int getIndent() {
        return indent;
    }

    /** Newline and set indent. */
    public void setIndent(@Nonnegative int indent) {
        this.indent = indent;
        Preconditions.checkState(indent >= 0, "Ran off left margin with indent < 0.");
        nl();
    }

    /** Newline and indent. */
    @Nonnull
    public IndentedOutput indent() {
        indent++;
        return nl();
    }

    /** Unindent and newline. */
    @Nonnull
    public IndentedOutput unindent() {
        indent--;
        Preconditions.checkState(indent >= 0, "Ran off left margin with indent < 0.");
        return nl();
    }

    @Nonnull
    public IndentedOutput indent(@CheckForSigned int delta) {
        indent += delta;
        Preconditions.checkState(indent >= 0, "Ran off left margin with indent < 0.");
        return nl();
    }

    protected void ensureIndent() {
        if (state == State.NL) {
            for (int i = 0; i < indent; i++)
                buf.append("  ");
            state = State.WORD;
        }
    }

    @Nonnull
    public IndentedOutput appendRaw(@Nonnull String value) {
        ensureIndent();
        buf.append(value);
        return this;
    }

    @Nonnull
    public IndentedOutput append(@Nonnull String value) {
        Preconditions.checkNotNull(value, "Value was null.");
        if (!value.isEmpty()) {
            int start = 0;
            while (start < value.length()) {
                int end = value.indexOf('\n', start);
                switch (end) {
                    default:
                        // Append up to next newline.
                        ensureIndent();
                        buf.append(value, start, end);
                    /* fallthrough */
                    case 0:
                        // Append newline.
                        nl();
                        start = end + 1;
                        break;
                    case -1:
                        // Append remainder.
                        ensureIndent();
                        buf.append(value, start, value.length());
                        start = value.length();
                        break;
                }
            }
        }
        return this;
    }

    @Nonnegative
    public int length() {
        return buf.length();
    }

    @Nonnull
    public IndentedOutput append(char c) {
        if (c == '\n') {
            nl();
        } else {
            ensureIndent();
            buf.append(c);
        }
        return this;
    }

    @Override
    public String toString() {
        return buf.toString();
    }
}
