/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import javax.annotation.Nonnull;
import org.anarres.polyglot.analysis.StartChecker;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;

/**
 *
 * @author shevek
 */
public class IgnoredProductionsSet {

    public static final IgnoredProductionsSet EMPTY = new IgnoredProductionsSet(true);

    private final String machineName;
    private final IntOpenHashSet ignoredProductionIndices = new IntOpenHashSet();
    private final IntOpenHashSet ignoredAlternativeIndices = new IntOpenHashSet();

    public IgnoredProductionsSet(@Nonnull GrammarModel grammar, @Nonnull String machineName) {
        this.machineName = machineName;
        for (CstProductionModel production : grammar.cstProductions.values()) {
            boolean ignored = production.isIgnored(machineName);
            if (ignored)
                ignoredProductionIndices.add(production.getIndex());
            for (CstAlternativeModel alternative : production.alternatives.values()) {
                if (ignored || alternative.isIgnored(machineName))
                    ignoredAlternativeIndices.add(alternative.getIndex());
            }
        }
    }

    public IgnoredProductionsSet(@Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot) {
        this(grammar, StartChecker.getMachineName(cstProductionRoot));
    }

    private IgnoredProductionsSet(boolean dummy) {
        this.machineName = "<global>";
    }

    @Nonnull
    public String getMachineName() {
        return machineName;
    }

    public boolean isIgnored(@Nonnull CstProductionModel cstProduction) {
        return ignoredProductionIndices.contains(cstProduction.getIndex());
    }

    public boolean isIgnored(@Nonnull CstAlternativeModel cstAlternative) {
        return ignoredAlternativeIndices.contains(cstAlternative.getIndex());
    }
}
