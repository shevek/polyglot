/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.lr.LRAutomaton;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;

/**
 *
 * @author shevek
 */
public class JavaHelper {

    private static final int ALTERNATIVE_GROUP_SHIFT = 9;
    private static final int ALTERNATIVE_GROUP_SIZE = 1 << ALTERNATIVE_GROUP_SHIFT;

    public static class CstAlternativeGroup extends ArrayList<CstAlternativeModel> {

        private final int index;

        public CstAlternativeGroup(@Nonnegative int index) {
            this.index = index;
        }

        @Nonnegative
        @TemplateProperty("parser.vm")
        public int getIndex() {
            return index;
        }

        @Nonnull
        @TemplateProperty("parser.vm")
        public String getJavaMethodName() {
            return "Group" + index;
        }

        /**
         * For naming consistency with {@link CstProductionModel#getAlternatives()}.
         *
         * @return this object.
         */
        @Nonnull
        @TemplateProperty
        public List<CstAlternativeModel> getAlternatives() {
            return this;
        }
    }

    private final Set<? extends Option> options;
    private final GrammarModel grammar;
    private final LRAutomaton automaton;

    public JavaHelper(@Nonnull Set<? extends Option> options, @Nonnull GrammarModel grammar, @Nonnull LRAutomaton automaton) {
        this.options = options;
        this.grammar = grammar;
        this.automaton = automaton;
    }

    /**
     * Since our template engine is slow, this allows us not to emit uninteresting tables.
     *
     * @return false if the grammar or the automaton is "large".
     */
    @TemplateProperty
    public boolean isLarge() {
        return grammar.cstProductions.size() > ALTERNATIVE_GROUP_SIZE || automaton.getStates().size() > ALTERNATIVE_GROUP_SIZE;
    }

    @Nonnegative
    @TemplateProperty("parser.vm")
    public int getAlternativeGroupShift() {
        return ALTERNATIVE_GROUP_SHIFT;
    }

    @Nonnull
    @TemplateProperty("parser.vm")
    public List<CstAlternativeGroup> getAlternativeGroups() {
        List<CstAlternativeGroup> out = new ArrayList<>();
        for (CstProductionModel production : grammar.getCstProductions()) {
            for (CstAlternativeModel alternative : production.getAlternatives().values()) {
                int groupIndex = alternative.getIndex() >> ALTERNATIVE_GROUP_SHIFT;
                while (out.size() <= groupIndex)
                    out.add(new CstAlternativeGroup(out.size()));
                out.get(groupIndex).add(alternative);
            }
        }
        return out;
    }

    @TemplateProperty
    public boolean isTrue() {
        return true;
    }

    @TemplateProperty
    public boolean isFalse() {
        return false;
    }

    public boolean isOption(@Nonnull String name) {
        return options.contains(Option.valueOf(name));
    }

}
