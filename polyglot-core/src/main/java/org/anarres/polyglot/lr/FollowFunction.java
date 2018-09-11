/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Function;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes the set of terminals which can possibly follow a nonterminal.
 *
 * This function is itself a function of the chosen CST root.
 *
 * @author shevek
 */
public class FollowFunction implements Function<CstProductionModel, Set<TokenModel>> {

    private static final Logger LOG = LoggerFactory.getLogger(FollowFunction.class);

    /** In a value, TokenModel.EOF means $ (end of input). */
    private final Map<CstProductionModel, Set<TokenModel>> followMap = new HashMap<>();

    public FollowFunction(@Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot, @Nonnull FirstFunction firstFunction) {
        build(grammar, cstProductionRoot, firstFunction);
    }

    // Dragon book Page 189.
    private void build(@Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot, @Nonnull FirstFunction firstFunction) {
        for (CstProductionModel production : grammar.cstProductions.values()) {
            Set<TokenModel> followSet = new TokenSet(firstFunction.getUniverse());
            followMap.put(production, followSet);
        }

        // Rule 1. The root production may be followed by EOF.
        // This means $.
        followMap.get(cstProductionRoot).add(TokenModel.EOF.INSTANCE);

        // Rule 2. Within a rule, excluding epsilon and EOF.
        for (CstProductionModel production : grammar.cstProductions.values()) {
            for (CstAlternativeModel alternative : production.alternatives.values()) {  // A
                for (int i = 0; i < alternative.elements.size(); i++) {
                    CstElementModel element = alternative.elements.get(i);  // B
                    if (element.isTerminal())
                        continue;
                    Set<TokenModel> followSet = followMap.get(element.getCstProduction());  // FOLLOW(B)
                    firstFunction.addFirst(followSet, alternative.elements, i + 1);
                    // LOG.info("F0 " + element.getCstProduction().getName() + " -> " + followSet + " because of " + alternative);
                }
            }
        }

        // for (CstProductionModel production : grammar.cstProductions.values()) {
        // LOG.info("Fx " + production.getName() + " -> " + followMap.get(production));
        // }
        // LOG.info("FA " + followMap);
        // Rule 3.
        boolean modified;
        do {
            modified = false;
            for (CstProductionModel production : grammar.cstProductions.values()) {
                Set<TokenModel> followSet = followMap.get(production);  // FOLLOW(A)
                // LOG.info("Evaluating FOLLOW for " + production.getName() + " -> ... with initial follow " + followSet);
                ALTERNATIVE:
                for (CstAlternativeModel alternative : production.alternatives.values()) {
                    // Walking BACKWARDS, and stop as soon as we know the tail-end of this production cannot result in epsilon.
                    ELEMENT:
                    for (int i = alternative.elements.size() - 1; i >= 0; i--) {
                        CstElementModel element = alternative.elements.get(i);  // B
                        if (element.isTerminal()) {
                            // This element is a terminal, and no previous
                            // nonterminal may be followed by epsilon, as they
                            // will all be followed by at least one terminal.
                            break ELEMENT;
                        }

                        // Given that A is the current production, and
                        // B is the nonterminal element of the current production,, and
                        // B is followed by a potential epsilon:
                        // Everything in FOLLOW(A) is in FOLLOW(B).
                        if (followMap.get(element.getCstProduction()).addAll(followSet)) {
                            // LOG.info("Modified FOLLOW(" + element.getCstProduction() + ") with " + followSet);
                            modified = true;
                        }

                        // If this element may not reduce to epsilon
                        if (!firstFunction.isNullable(element.symbol)) {
                            // LOG.info("Breaking from " + alternative + " -> " + alternative.elements + " at " + i + " - no epsilon in " + element.symbol);
                            break ELEMENT;
                        }
                    }
                }
                // LOG.info("F1 " + production.getName() + " -> " + followSet);
            }
        } while (modified);
    }

    @Nonnull
    @Override
    public Set<TokenModel> apply(CstProductionModel input) {
        return followMap.get(input);
    }
}
