/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.lr.LRAutomaton;
import org.anarres.polyglot.model.AnnotationModel;
import org.anarres.polyglot.model.AstModel;
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

    @Nonnull
    private final Set<? extends Option> options;
    @Nonnull
    private final GrammarModel grammar;
    @CheckForNull
    private final LRAutomaton automaton;

    public JavaHelper(@Nonnull Set<? extends Option> options, @Nonnull GrammarModel grammar, @CheckForNull LRAutomaton automaton) {
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
        if (grammar.cstProductions.size() > ALTERNATIVE_GROUP_SIZE)
            return true;
        if (automaton != null)
            if (automaton.getStates().size() > ALTERNATIVE_GROUP_SIZE)
                return true;
        return false;
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

    @Nonnull
    public boolean hasAnnotations(@Nonnull AstModel model, @Nonnull String name) {
        return !model.getAnnotations().get(name).isEmpty();
    }

    /**
     * Returns the list of values of annotations on the given model with the given name.
     *
     * The returned list may contain nulls for annotations which did not specify a value.
     *
     * @param model The model from which to retrieve annotations.
     * @param name The name of the annotations to retrieve.
     * @return The list of values of annotations on the given model with the given name.
     */
    @Nonnull
    public List<String> getAnnotations(@Nonnull AstModel model, @Nonnull String name) {
        List<String> out = new ArrayList<>();
        for (AnnotationModel annotation : model.getAnnotations().get(name))
            out.add(annotation.getValue());
        return out;
    }

}
