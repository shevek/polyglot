/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes the set of possible first-terminals of a of a symbol subsequence.
 * The returned value may also indicate the inclusion of epsilon.
 *
 * @author shevek
 */
public class FirstFunction implements Function<CstProductionSymbol, Set<TokenModel>> {

    private static final Logger LOG = LoggerFactory.getLogger(FirstFunction.class);
    private static final boolean DEBUG = false;

    private static class Result extends TokenSet {

        private boolean nullable = false;

        public Result(@Nonnull TokenUniverse universe) {
            super(universe);
        }

        public void setNullable() {
            this.nullable = true;
        }

        public boolean isNullable() {
            return nullable;
        }

        @Override
        public String toString() {
            return super.toString() + (isNullable() ? "+nullable" : "");
        }
    }

    private static boolean isPossiblyEmptyProduction(@Nonnull CstProductionModel production) {
        if (production.alternatives.isEmpty())
            return true;    // I hope this is never reached? The grammar forbids it.
        for (CstAlternativeModel alternative : production.alternatives.values())
            if (alternative.elements.isEmpty())
                return true;
        return false;
    }

    private final TokenUniverse universe;
    private final IgnoredProductionsSet ignoredProductions;
    private final Map<CstProductionModel, Result> firstMap = new HashMap<>();

    public FirstFunction(@Nonnull TokenUniverse universe, @Nonnull GrammarModel grammar, @Nonnull IgnoredProductionsSet ignoredProductions) {
        this.universe = Preconditions.checkNotNull(universe, "TokenUniverse was null.");
        this.ignoredProductions = Preconditions.checkNotNull(ignoredProductions, "IgnoredProductionsSet was null.");
        build(grammar, ignoredProductions);
    }

    public FirstFunction(@Nonnull GrammarModel grammar, @Nonnull IgnoredProductionsSet ignoredProductions) {
        this(new TokenUniverse(grammar), grammar, ignoredProductions);
    }

    @Nonnull
    public TokenUniverse getUniverse() {
        return universe;
    }

    @Nonnull
    public IgnoredProductionsSet getIgnoredProductions() {
        return ignoredProductions;
    }

    // Dragon book page 189.
    // Mogensen page 55.
    private void build(@Nonnull GrammarModel grammar, @Nonnull IgnoredProductionsSet ignoredProductions) {
        for (CstProductionModel production : grammar.getCstProductions()) {
            if (ignoredProductions.isIgnored(production))
                continue;
            Result firstSet = new Result(universe);
            if (isPossiblyEmptyProduction(production))
                firstSet.setNullable();
            // if (DEBUG) LOG.info("First-0: " + production.getName() + " = " + firstSet);
            firstMap.put(production, firstSet);
        }

        boolean modified;
        do {
            modified = false;
            for (CstProductionModel production : grammar.getCstProductions()) {
                if (ignoredProductions.isIgnored(production))
                    continue;
                // if (production == null) throw new IllegalStateException("No Production");
                Result firstSet = firstMap.get(production);
                // if (firstSet == null) throw new IllegalStateException("No FirstSet (Result) for " + production);
                for (CstAlternativeModel alternative : production.getAlternatives()) {
                    if (ignoredProductions.isIgnored(alternative))
                        continue;
                    ELEMENT:
                    {
                        for (CstElementModel element : alternative.elements) {
                            if (element.isTerminal()) {
                                if (firstSet.add(element.getToken()))
                                    modified = true;
                                // Tokens are never empty.
                                break ELEMENT;
                            } else {
                                CstProductionModel subproduction = element.getCstProduction();
                                // if (subproduction == null) throw new IllegalStateException("No SubProduction for " + element);
                                // if (grammar.cstProductions.get(subproduction.getName()) == null) throw new IllegalStateException("Nonexistent SubProduction for " + element);
                                // if (subproduction != grammar.cstProductions.get(subproduction.getName())) throw new IllegalStateException("Illegal SubProduction for " + element);
                                Result subFirstSet = firstMap.get(subproduction);
                                // if (subFirstSet == null) throw new IllegalStateException("No SubFirstSet (Result) for " + subproduction);
                                if (firstSet.addAll(subFirstSet))
                                    modified = true;
                                if (!subFirstSet.isNullable())
                                    break ELEMENT;
                            }
                        }

                        // Epsilon was in every element.
                        if (!firstSet.isNullable()) {
                            firstSet.setNullable();
                            modified = true;
                        }
                    }
                }

                // if (DEBUG) LOG.info("First-I: " + production.getName() + " = " + firstSet);
            }
        } while (modified);

        if (DEBUG) {
            for (CstProductionModel production : grammar.cstProductions.values()) {
                if (ignoredProductions.isIgnored(production))
                    continue;
                LOG.info("First-O: " + production.getName() + " = " + firstMap.get(production));
            }
        }
    }

    public boolean isNullable(@Nonnull CstProductionSymbol symbol) {
        if (symbol instanceof TokenModel)
            return false;
        CstProductionModel production = (CstProductionModel) symbol;
        return firstMap.get(production).isNullable();
    }

    public boolean isNullable(@Nonnull CstAlternativeModel alternative) {
        for (int i = 0; i < alternative.elements.size(); i++)
            if (!isNullable(alternative.elements.get(i).getSymbol()))
                return false;
        return true;
    }

    /**
     * Returns FIRST(symbol).
     *
     * @param symbol The symbol, either {@link TokenModel terminal} or {@link CstProductionModel nonterminal}.
     * @return The set of tokens which may follow the given symbol in any production.
     */
    @Nonnull
    @Override
    public Set<TokenModel> apply(CstProductionSymbol symbol) {
        if (symbol instanceof TokenModel)
            return Collections.singleton((TokenModel) symbol);
        CstProductionModel production = (CstProductionModel) symbol;
        return firstMap.get(production);
    }

    /**
     * Adds first(WXYZ) to out.
     *
     * WXYZ is held in elements.subList(start).
     *
     * @param out the set into which to add tokens.
     * @param elements The vector of symbols to analyze.
     * @param start the starting point in the vector.
     * @return true iff the symbol sequence was nullable.
     */
    // @CheckReturnValue
    public boolean addFirst(@Nonnull Set<TokenModel> out, @Nonnull List<? extends CstElementModel> elements, @Nonnegative int start) {
        for (int i = start; i < elements.size(); i++) {
            CstElementModel element = elements.get(i);
            if (element.isTerminal()) {
                out.add(element.getToken());
                // Clearly not epsilon.
                return false;
            } else {
                Result firstSet = firstMap.get(element.getCstProduction());
                if (DEBUG)
                    LOG.info("First-X: " + element.getCstProduction().getName() + " = " + firstSet);
                out.addAll(firstSet);
                // If the sub-production cannot reduce to epsilon:
                if (!firstSet.isNullable())
                    return false;
            }
        }
        return true;
    }

    // @CheckReturnValue
    public boolean addFirst(@Nonnull IntSet out, @Nonnull List<? extends CstElementModel> elements, @Nonnegative int start) {
        for (int i = start; i < elements.size(); i++) {
            CstElementModel element = elements.get(i);
            if (element.isTerminal()) {
                out.add(element.getToken().getIndex());
                // Clearly not epsilon.
                return false;
            } else {
                Result firstSet = firstMap.get(element.getCstProduction());
                if (DEBUG)
                    LOG.info("First-X: " + element.getCstProduction().getName() + " = " + firstSet);
                out.addAll(firstSet.getIndices());
                // If the sub-production cannot reduce to epsilon:
                if (!firstSet.isNullable())
                    return false;
            }
        }
        return true;
    }

}
