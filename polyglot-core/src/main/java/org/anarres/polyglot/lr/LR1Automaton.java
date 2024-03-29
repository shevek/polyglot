/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import java.util.Collections;
import javax.annotation.Nonnull;
import org.anarres.polyglot.analysis.StartChecker;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.TokenModel;

/**
 *
 * @author shevek
 */
public class LR1Automaton extends LRAutomaton {

    public LR1Automaton(@Nonnull CstProductionModel cstProductionRoot) {
        super(StartChecker.getMachineName(cstProductionRoot));
    }

    @Override
    protected Iterable<? extends TokenModel> getLookaheads(LRItem item) {
        return Collections.singletonList(((LR1Item) item).getLookahead());
    }
}
