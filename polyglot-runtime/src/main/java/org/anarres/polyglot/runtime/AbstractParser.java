/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.runtime;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public abstract class AbstractParser {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractParser.class);

    public static final int SHIFT = 0;
    public static final int REDUCE = 1;
    public static final int ACCEPT = 2;
    public static final int ERROR = 3;

    /*
     protected void dump(Object token, int[] actionEntry) {
     LOG.info("Token: " + token.getClass() + ", state=" + state() + ", stackPointer=" + stackPointer + ", stack= " + stack + ", action=" + actionEntry[1] + ", " + actionEntry[2]);
     }

     @Nonnull
     protected int[] lookup(@Nonnull int[][] table, int key, boolean indexZeroIsDefault) {
     int low = indexZeroIsDefault ? 1 : 0;
     int high = table.length - 1;

     while (low <= high) {
     // LOG.info("Search [" + low + ".." + high + "] for " + key);
     int middle = (low + high) >>> 1;
     int[] entry = table[middle];
     // LOG.info("table[" + middle + "] = " + Arrays.toString(entry));

     if (key < entry[0])
     high = middle - 1;
     else if (key > entry[0])
     low = middle + 1;
     else
     return entry;
     }

     if (indexZeroIsDefault)
     return table[0];
     else
     throw new IllegalArgumentException("Table lookup failed for " + key);
     }

     @Nonnull
     protected State push(@Nonnegative int state) {
     stackPointer++;
     State out;
     if (stackPointer == stack.size()) {
     out = new State();
     stack.add(out);
     } else {
     out = stack.get(stackPointer);
     out.clear();
     }
     out.state = state;
     return out;
     }

     @Nonnegative
     protected int state() {
     return stack.get(stackPointer).state;
     }

     @Nonnull
     protected State pop() {
     return stack.remove(stackPointer--);
     }

     @Nonnull
     public Start parse() throws ParserException, LexerException, IOException {
     push(0);
     for (;;) {
     Token token = this.lexer.peek();
     int index = token.getTokenIndex();
     if (index == -1)
     continue;

     // [1, 2] = Either shift and stack state, or reduce by production number.
     int state = state();    // TODO: Inline
     int[] actionEntry = lookup(actionTable[state], index, true);
     LOG.info("Token: " + token + ", state=" + state + ", stackPointer=" + stackPointer + ", stack= " + stack + ", action=" + actionEntry[0] + ", " + actionEntry[1]);

     switch (actionEntry[1]) {
     case SHIFT:
     push(actionEntry[2]).add(this.lexer.next());
     break;
     case REDUCE:
     int reduction = actionEntry[2];
     reduce(reduction);
     break;
     case ACCEPT:
     EOF eof = (EOF) this.lexer.next();
     ${grammar.astProductionRoot.javaTypeName} root = (${grammar.astProductionRoot.javaTypeName}) pop().get(0);
     return new Start(root, eof);
     case ERROR:
     throw new ParserException(token,
     "[" + token.getLine() + "," + token.getPos() + "] " +
     errorTable[actionEntry[2]]);
     }
     }
     }

     protected abstract void reduce(@Nonnegative int reduction) throws IOException, LexerException, ParserException;
     */
    /** Implements a kind of left-associative addition for lists and items. */
    protected static class ListBuilder<T> extends ArrayList<T> {

        public ListBuilder() {
        }

        @Nonnull
        public ListBuilder<T> with(@Nonnull T value) {
            add(value);
            return this;
        }

        @Nonnull
        public ListBuilder<T> withAll(@Nonnull Collection<? extends T> value) {
            addAll(value);
            return this;
        }
    }

    protected static void readArray(@Nonnull int[] out, @Nonnull DataInputStream in) throws IOException {
        for (int i = 0; i < out.length; i++)
            out[i] = in.readInt();
    }

    @Nonnull
    protected static int[][] readTable(@Nonnull DataInputStream in, @Nonnegative int sublength) throws IOException {
        int[][] out = new int[in.readInt()][sublength];
        for (int i = 0; i < out.length; i++)
            readArray(out[i], in);
        return out;
    }
}
