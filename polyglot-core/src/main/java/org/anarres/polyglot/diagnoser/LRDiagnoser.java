/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.diagnoser;

import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.lr.LRConflict;
import org.anarres.polyglot.lr.LRDiagnosis;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;

/**
 *
 * @author shevek
 */
public interface LRDiagnoser {

    public static interface Factory {

        @CheckForNull
        public LRDiagnoser newDiagnoser(@Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot, @Nonnull Set<? extends Option> options);
    }

    @Nonnull
    public LRDiagnosis diagnose(@Nonnull LRConflict conflict);
}
