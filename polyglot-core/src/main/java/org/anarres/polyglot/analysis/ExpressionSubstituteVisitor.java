/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.AstProductionSymbol;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstTransformExpressionModel;
import org.anarres.polyglot.model.CstTransformPrototypeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class ExpressionSubstituteVisitor implements CstTransformExpressionModel.Visitor<ExpressionSubstituteVisitor.SubstitutionMap, CstTransformExpressionModel, RuntimeException> {

    private static final Logger LOG = LoggerFactory.getLogger(ExpressionSubstituteVisitor.class);

    private static class Needle {

        @Nonnull
        private final CstElementModel element;
        @Nonnull
        private final AstProductionSymbol symbol;
        // private final boolean list;

        public Needle(@Nonnull CstElementModel element, @Nonnull CstTransformPrototypeModel transform) {
            this.element = Preconditions.checkNotNull(element, "CstElementModel was null.");
            this.symbol = Preconditions.checkNotNull(transform.getSymbol(), "CstTransformPrototypeModel was null.");
            // this.list = transform.isList();
        }

        @Override
        public int hashCode() {
            return element.hashCode() ^ symbol.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (null == obj)
                return false;
            if (!getClass().equals(obj.getClass()))
                return false;
            Needle o = (Needle) obj;
            return element.equals(o.element)
                    && symbol.equals(o.symbol);
            // && list == o.list;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            // if (list) buf.append('[');
            buf.append(element.getName()).append('.').append(symbol.getName());
            // if (list) buf.append(']');
            return buf.toString();
        }
    }

    public static class SubstitutionMap extends HashMap<Needle, CstTransformExpressionModel> {

        public void addSubstitution(@Nonnull CstElementModel needleElement, @Nonnull CstTransformPrototypeModel needleTransform, @Nonnull CstTransformExpressionModel replacement) {
            put(new Needle(needleElement, needleTransform), replacement);
        }

        /*
         @Override
         public CstTransformExpressionModel get(Object key) {
         CstTransformExpressionModel value = super.get(key);
         LOG.info("get: " + key + " -> " + value);
         return value;
         }
         */
    }

    @Override
    public CstTransformExpressionModel visitNull(CstTransformExpressionModel.Null expression, SubstitutionMap input) throws RuntimeException {
        return expression;
    }

    @Override
    public CstTransformExpressionModel visitReference(CstTransformExpressionModel.Reference expression, SubstitutionMap input) throws RuntimeException {
        CstTransformExpressionModel replacement = input.get(new Needle(expression.element, expression.transform));
        if (replacement != null)
            return replacement;
        return expression;
    }

    @CheckForNull
    private List<? extends CstTransformExpressionModel> visitChildren(@Nonnull List<? extends CstTransformExpressionModel> inList, @Nonnull SubstitutionMap input, boolean excludeNulls) {
        List<CstTransformExpressionModel> outList = new ArrayList<>(inList.size());
        boolean mutated = false;
        for (CstTransformExpressionModel in : inList) {
            CstTransformExpressionModel out = in.apply(this, input);
            // Allow lists to remove items transformed to null.
            ADD:
            {
                // We might need ANOTHER boolean to turn this on for production normalization but off for user inlining.
                if (excludeNulls)
                    if (out != in)
                        if (out instanceof CstTransformExpressionModel.Null)
                            break ADD;
                outList.add(out);
            }
            mutated |= (out != in);
        }
        if (!mutated)
            return null;
        return outList;
    }

    @Override
    public CstTransformExpressionModel visitNew(CstTransformExpressionModel.New expression, SubstitutionMap input) throws RuntimeException {
        List<? extends CstTransformExpressionModel> arguments = visitChildren(expression.getArguments(), input, false);
        if (arguments == null)
            return expression;
        CstTransformExpressionModel.New out = new CstTransformExpressionModel.New(expression.productionName, expression.alternativeName);
        out.astAlternative = expression.astAlternative;
        out.getArguments().addAll(arguments);
        return out;
    }

    private void addItem(@Nonnull CstTransformExpressionModel.List out, @Nonnull CstTransformExpressionModel item) {
        if (item instanceof CstTransformExpressionModel.List) {
            CstTransformExpressionModel.List in = (CstTransformExpressionModel.List) item;
            for (CstTransformExpressionModel subitem : in.getItems()) {
                // This should never recurse a second time - if it does, we screwed up elsewhere.
                // addItem(out, subitem);
                out.addItem(subitem);
            }
        } else {
            out.addItem(item);
        }
    }

    @Override
    public CstTransformExpressionModel visitList(CstTransformExpressionModel.List expression, SubstitutionMap input) throws RuntimeException {
        List<? extends CstTransformExpressionModel> arguments = visitChildren(expression.getItems(), input, true);
        if (arguments == null)
            return expression;
        CstTransformExpressionModel.List out = new CstTransformExpressionModel.List(expression.getLocation());
        out.elementType = expression.elementType;
        // If we are substituting a list-valued expression into an expression which also expresses the list nature,
        // we can accidentally double the list nature.
        // Since a list can never be embedded in a list, we just decapsulate once here.
        for (CstTransformExpressionModel argument : arguments)
            addItem(out, argument);
        return out;
    }

}
