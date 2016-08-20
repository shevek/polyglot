/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import javax.annotation.Nonnull;
import org.anarres.polyglot.analysis.StartChecker;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;

/**
 * Actually, this is an SLR automaton, not a pure LR(0).
 *
 * @author shevek
 */
public class LR0Automaton extends LRAutomaton {

    private final FirstFunction firstFunction;
    private final FollowFunction followFunction;

    public LR0Automaton(@Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot, @Nonnull IgnoredProductionsSet ignoredProductions) {
        super(StartChecker.getMachineName(cstProductionRoot));
        this.firstFunction = new FirstFunction(grammar, ignoredProductions);
        this.followFunction = new FollowFunction(grammar, cstProductionRoot, firstFunction);
    }

    /**
     * The fact that we only reduce on FOLLOW() makes us SLR, not LR(0).
     *
     * @param item The item for which to get lookaheads.
     * @return The set of lookaheads, from the {@link FollowFunction}.
     */
    @Override
    protected Iterable<? extends TokenModel> getLookaheads(LRItem item) {
        CstAlternativeModel alternative = item.getProductionAlternative();
        return followFunction.apply(alternative.getProduction());
    }
}
