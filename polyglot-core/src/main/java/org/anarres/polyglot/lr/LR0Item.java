/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import java.util.List;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementAssociativity;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;

/**
 *
 * @author shevek
 */
public class LR0Item implements LRItem {

    /** The index into the LR{0,1}ItemUniverse. */
    private final int index;
    /** The production which we are representing. */
    private final CstAlternativeModel production;
    /** The position of the dot. */
    private final int position;
    // /* pp */ Set<? extends LRItem> closure;
    // Cache this, it's used heavily.
    private final CstProductionSymbol symbol;
    private final LRAction.Reduce reduceAction;

    public LR0Item(@Nonnegative int index, @Nonnull CstAlternativeModel production, @Nonnegative int position) {
        Preconditions.checkPositionIndex(position, production.getElements().size(), "Illegal position.");
        this.index = index;
        this.production = production;
        this.position = position;
        this.symbol = (production.getElements().size() == position) ? null : production.getElements().get(position).getSymbol();
        this.reduceAction = (symbol == null) ? new LRAction.Reduce(this) : null;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public CstProductionModel getProduction() {
        return production.getProduction();
    }

    @Override
    public CstAlternativeModel getProductionAlternative() {
        return production;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public CstElementModel getElement() {
        List<? extends CstElementModel> elements = production.getElements();
        int p = getPosition();
        if (p == elements.size())
            return null;
        return elements.get(p);
    }

    @Override
    public CstElementAssociativity getAssociativity() {
        CstElementModel element = getElement();
        if (element == null)
            return CstElementAssociativity.UNSPECIFIED;
        return element.getAssociativity();
    }

    @Override
    public CstProductionSymbol getSymbol() {
        return symbol;
    }

    @Override
    public LRAction.Reduce getReduceAction() {
        return Preconditions.checkNotNull(reduceAction, "Not a Reduce LRItem.");
    }

    @Override
    public void assertFollowedBy(LRItem _follow) {
        LR0Item follow = (LR0Item) _follow;
        if (follow.getProductionAlternative() != getProductionAlternative())
            throw new IllegalStateException("Wrong production in following LRItem: " + this + " -> " + follow);
        if (follow.getPosition() != getPosition() + 1)
            throw new IllegalStateException("Wrong position in following LRItem: " + this + " -> " + follow);
    }

    @Override
    public void toStringBuilderWithoutLookahead(StringBuilder buf) {
        List<CstElementModel> elements = getProductionAlternative().elements;
        buf.append(getProductionAlternative().getName()).append(" =");
        for (int i = 0; i < getPosition(); i++)
            buf.append(" ").append(elements.get(i).getSymbolName());
        buf.append(" .");
        for (int i = getPosition(); i < elements.size(); i++)
            buf.append(" ").append(elements.get(i).getSymbolName());
    }

    @Override
    public void toStringBuilder(StringBuilder buf) {
        toStringBuilderWithoutLookahead(buf);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toStringBuilder(buf);
        return buf.toString();
    }

}
