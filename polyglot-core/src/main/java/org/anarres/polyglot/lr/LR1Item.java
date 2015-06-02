/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.TokenModel;

/**
 *
 * @author shevek
 */
public class LR1Item extends LR0Item implements LRItem {

    private final TokenModel lookahead;

    public LR1Item(
            @Nonnegative int index,
            @Nonnull CstAlternativeModel production,
            @Nonnegative int position,
            @Nonnull TokenModel lookahead) {
        super(index, production, position);
        this.lookahead = Preconditions.checkNotNull(lookahead, "TokenModel (lookahead) was null.");
    }

    @Nonnull
    public TokenModel getLookahead() {
        return lookahead;
    }

    @Override
    public void assertFollowedBy(LRItem _follow) {
        super.assertFollowedBy(_follow);
        LR1Item follow = (LR1Item) _follow;
        if (follow.getLookahead() != getLookahead())
            throw new IllegalStateException("Wrong lookahead in following LRItem: " + this + " -> " + follow);
    }

    @Override
    public void toStringBuilder(StringBuilder buf) {
        super.toStringBuilder(buf);
        buf.append(" / ").append(lookahead.getName());
    }
}
