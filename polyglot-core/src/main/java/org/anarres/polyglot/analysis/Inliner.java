/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.lr.LRAction;
import org.anarres.polyglot.lr.LRConflict;
import org.anarres.polyglot.lr.LRItem;
import org.anarres.polyglot.lr.LRState;
import org.anarres.polyglot.model.AnnotationName;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstTransformExpressionModel;
import org.anarres.polyglot.model.CstTransformPrototypeModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.node.TIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class Inliner {

    private static final Logger LOG = LoggerFactory.getLogger(Inliner.class);
    private static final boolean DEBUG = false;
    private final ExpressionSubstituteVisitor substituteVisitor = new ExpressionSubstituteVisitor();
    private final ErrorHandler errors;
    private final GrammarModel grammar;
    private int substitutions = 0;

    public Inliner(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    @Nonnegative
    public int getSubstitutions() {
        return substitutions;
    }

    /**
     * haystackAlternative = [a]:needle { -> New foo(a.bar) }
     * needleProduction { -> bar } = other { -> New bar() }
     *
     * haystack = New foo(a.bar)
     * needle = a.bar
     * replacement = New bar()
     * result = New foo(New bar())
     *
     * where Substitution: needleElement = a, needleTansform = bar, replacement = 'New bar()'
     */
    @Nonnull
    private CstAlternativeModel substitute(@Nonnull CstAlternativeModel haystackAlternative, CstElementModel needleElement, CstAlternativeModel replacementAlternative) {
        /*
         if (DEBUG) {
         LOG.debug("    Haystack = " + haystackAlternative);
         // LOG.debug("    Needle = " + needleElement);
         LOG.debug("    Replacement = " + replacementAlternative);
         }
         */

        if (DEBUG)
            LOG.debug("Inlining " + haystackAlternative.getName() + " <- " + replacementAlternative.getName());

        String name = haystackAlternative.getSourceName() + "$inl_" + (grammar.inlineIndex++) + "_" + needleElement.getSourceName();
        CstAlternativeModel out = CstAlternativeModel.forName(grammar.cstAlternativeIndex++, haystackAlternative.getProduction(), new TIdentifier(name, haystackAlternative.getLocation()));
        ExpressionSubstituteVisitor.SubstitutionMap elementSubstitutions = new ExpressionSubstituteVisitor.SubstitutionMap();
        for (CstElementModel haystackElement : haystackAlternative.getElements()) {
            if (haystackElement == needleElement) {
                for (CstElementModel sourceElement : replacementAlternative.getElements()) {
                    // We have to rename the element(s) of the replacementAlternative so that they don't clash in the Java code.
                    String replacementElementName = needleElement.getName() + "$inl_" + (grammar.inlineIndex++) + "_" + sourceElement.getName();
                    CstElementModel replacementElement = new CstElementModel(
                            new TIdentifier(replacementElementName, sourceElement.getLocation()),
                            sourceElement.getSpecifier(),
                            new TIdentifier(sourceElement.getSymbolName(), sourceElement.getLocation()), // TODO: Accurate location loss.
                            sourceElement.getUnaryOperator());
                    replacementElement.symbol = sourceElement.symbol;

                    out.elements.add(replacementElement);

                    // The renaming requires us to substitute the element references in the transform expressions.
                    for (CstTransformPrototypeModel replacementTransform : replacementElement.getSymbol().getTransformPrototypes()) {
                        CstTransformExpressionModel.Reference replacementReference = new CstTransformExpressionModel.Reference(replacementElement.toNameToken(), replacementTransform.toNameToken());
                        replacementReference.element = replacementElement;
                        replacementReference.transform = replacementTransform;
                        elementSubstitutions.addSubstitution(sourceElement, replacementTransform, replacementReference);
                    }
                }
            } else {
                out.elements.add(haystackElement);
            }
        }

        ExpressionSubstituteVisitor.SubstitutionMap transformSubstitutions = new ExpressionSubstituteVisitor.SubstitutionMap();
        int transformCount = needleElement.getCstProduction().getTransformPrototypes().size();
        for (int i = 0; i < transformCount; i++) {
            // For each transform of the needle:
            CstTransformPrototypeModel needleTransform = needleElement.getCstProduction().getTransformPrototype(i);
            // Find the expression corresponding to the transform:
            CstTransformExpressionModel replacementExpression = replacementAlternative.getTransformExpression(i);
            replacementExpression = replacementExpression.apply(substituteVisitor, elementSubstitutions);
            transformSubstitutions.addSubstitution(needleElement, needleTransform, replacementExpression);
        }

        for (CstTransformExpressionModel haystackExpression : haystackAlternative.getTransformExpressions()) {
            // LOG.info("      Substitution input is " + haystackExpression);
            haystackExpression = haystackExpression.apply(substituteVisitor, transformSubstitutions);
            // LOG.info("      Substitution result is " + haystackExpression);
            out.addTransformExpression(haystackExpression);
        }

        if (DEBUG)
            LOG.debug("  Newly generated CstAlternativeModel is " + out);

        return out;
    }

    @SuppressWarnings("unused")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private void substitute(@Nonnull CstAlternativeModel haystackAlternative, CstElementModel needleElement, CstProductionModel replacementProduction) {
        CstProductionModel haystackProduction = haystackAlternative.getProduction();
        for (CstAlternativeModel replacementAlternative : replacementProduction.getAlternatives().values()) {
            CstAlternativeModel resultAlternative = substitute(haystackAlternative, needleElement, replacementAlternative);
            if (haystackProduction.alternatives.put(resultAlternative.getName(), resultAlternative) != null)
                throw new IllegalStateException("Name clash!");
        }
        if (!haystackProduction.removeAlterative(haystackAlternative))
            throw new IllegalStateException("Alt-removal failure.");
        if (DEBUG)
            LOG.debug("Removed alternative " + haystackAlternative.getName() + " from " + haystackProduction.getName());
    }

    private boolean isRecursive(@Nonnull CstAlternativeModel alternative) {
        CstProductionModel production = alternative.getProduction();
        for (CstElementModel element : alternative.getElements())
            if (element.getSymbol() == production)
                return true;
        return false;
    }

    private boolean isRecursive(@Nonnull CstProductionModel production) {
        for (CstAlternativeModel alternative : production.getAlternatives().values())
            if (isRecursive(alternative))
                return true;
        return false;
    }

    /*
     * A recursive erasing production A -> | A b can become
     *  A -> | A$nonerasing
     *  A$nonerasing -> b | A$nonerasing b
     * where the second A$nonerasing rule is gotten by inlining
     * A into A$nonerasing.
     */
    @SuppressWarnings("unused")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private boolean isErasing(@Nonnull CstProductionModel production) {
        for (CstAlternativeModel alternative : production.getAlternatives().values()) {
            if (alternative.getElements().isEmpty())
                return true;
        }
        return false;
    }

    @CheckReturnValue
    private boolean substitute(@Nonnull CstAlternativeModel inlineAlternative) {
        CstProductionModel inlineProduction = inlineAlternative.getProduction();

        // [k] CstAlternativeModel == the alternatives in which we replaced elements
        // [v] CstAlternativeModel == result, when replaced
        Map<CstAlternativeModel, CstAlternativeModel> inlinedAlternatives = new HashMap<>();

        // Strictly, we only need to do this in the production in the state pointing to the conflict state.
        // So we should really walk states, not the entire grammar here.
        for (CstProductionModel haystackProduction : grammar.getCstProductions()) {
            for (CstAlternativeModel haystackAlternative : haystackProduction.getAlternatives().values()) {
                CstAlternativeModel resultAlternative = haystackAlternative;
                for (CstElementModel haystackElement : haystackAlternative.getElements()) {
                    if (haystackElement.symbol != inlineProduction)
                        continue;
                    // We only inline the one alternative here, thus to avoid over-explosive growth.
                    resultAlternative = substitute(resultAlternative, haystackElement, inlineAlternative);
                    inlinedAlternatives.put(haystackAlternative, resultAlternative);
                }
                if (resultAlternative != haystackAlternative) {
                    // We just signal failure on overload; the caller can work out what to do with this.
                    if (++substitutions > 4096)
                        return false;
                }
            }
        }

        // Now we apply those results to the grammar:
        // The original alternative was inlined everywhere.
        if (DEBUG)
            LOG.debug("Removing inlined alternative " + inlineAlternative.getName());
        if (!inlineProduction.removeAlterative(inlineAlternative))
            throw new IllegalStateException("Failed to remove inlined alternative " + inlineAlternative.getName() + " from " + inlineProduction.getName());

        // The original production might have no alternatives left.
        boolean inlineProductionRemoved = inlineProduction.getAlternatives().isEmpty();
        if (inlineProductionRemoved) {
            if (DEBUG)
                LOG.debug("Removing now-empty production " + inlineProduction.getName());
            if (!grammar.removeCstProduction(inlineProduction))
                throw new IllegalStateException("Failed to remove production " + inlineProduction.getName() + " from grammar.");
        }

        // Augment each haystackProduction with its new alternative.
        for (Map.Entry<CstAlternativeModel, CstAlternativeModel> e : inlinedAlternatives.entrySet()) {
            CstProductionModel cstProduction = e.getKey().getProduction();
            // This references a now nonexistent production.
            if (inlineProductionRemoved) {
                if (DEBUG)
                    LOG.debug("Removing now-useless alternative " + e.getKey().getName());
                if (!cstProduction.removeAlterative(e.getKey()))
                    throw new IllegalStateException("Failed to remove superceded alternative " + e.getKey().getName() + " from " + cstProduction.getName());
            }
            if (DEBUG)
                LOG.debug("Adding alternative " + e.getValue().getName());
            cstProduction.addAlternative(e.getValue());
        }

        return true;
    }

    // public boolean inline(@Nonnull Iterable<? extends CstProductionModel> inlineProductions) { }
    /**
     * Returns false on exceptional return (abort).
     *
     * @param inlineAlternatives The candidates for attempted inlining.
     * @return false on exceptional return (abort).
     */
    @CheckReturnValue
    public boolean substitute(@Nonnull Iterable<? extends CstAlternativeModel> inlineAlternatives) {
        grammar.check();

        for (CstAlternativeModel inlineAlternative : inlineAlternatives) {
            CstProductionModel inlineProduction = inlineAlternative.getProduction();
            // It is possible that a substitute() inlines into an otherwise conflicting alternative.
            // In that case, a CstAlternativeModel in this Iterable will apparently "vanish".
            if (inlineProduction.alternatives.get(inlineAlternative.getName()) != inlineAlternative)
                continue;
            // Let's not inline this.
            if (inlineProduction == grammar.cstProductionRoot)
                continue;
            // If we inline one alternative of a recursive production, we break it
            // when we remove the alternative from the production itself.
            if (isRecursive(inlineProduction)) {
                if (!inlineProduction.getAnnotations(AnnotationName.Inline).isEmpty())
                    errors.addError(inlineProduction.getLocation(), "Production '" + inlineProduction.getName() + "' is recursive, and may not be inlined.");
                continue;
            }
            // Do the dirty.
            if (!substitute(inlineAlternative))
                return false;
            grammar.check();
        }

        return substitutions > 0;
    }

    /**
     * Returns false on exceptional return (abort).
     *
     * @param conflicts The set of conflicts from which to draw inlining candidates.
     * @return false on exceptional return (abort).
     */
    @CheckReturnValue
    public boolean substitute(@Nonnull LRConflict.Map conflicts) {
        return substitute(conflicts.getConflictingAlternatives());
    }

    @SuppressWarnings("unused")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private int substituteParallel(LRConflict.Map conflicts) {

        // It is definitely possible to write this in a single pass over all rules.
        // However, doing so leads to complex and mistake-prone code, and
        // will probably not give any significant performance gains.
        // [k] CstAlternativeModel == the alternatives in which we replaced elements
        // [v] CstAlternativeModel == result, when replaced
        Map<CstAlternativeModel, CstAlternativeModel> inlinedAlternatives = new HashMap<>();
        Set<? extends CstAlternativeModel> inlineAlternatives = conflicts.getConflictingAlternatives();
        Multimap<CstProductionModel, CstAlternativeModel> inlineProductions = ArrayListMultimap.create();
        for (CstAlternativeModel inlineAlternative : inlineAlternatives)
            if (!isRecursive(inlineAlternative))
                inlineProductions.put(inlineAlternative.getProduction(), inlineAlternative);

        // Strictly, we only need to do this in the production in the state pointing to the conflict state.
        // So we should really walk states, not the entire grammar here.
        for (CstProductionModel haystackProduction : grammar.getCstProductions()) {
            for (CstAlternativeModel haystackAlternative : haystackProduction.getAlternatives().values()) {
                CstAlternativeModel resultAlternative = haystackAlternative;
                for (CstElementModel haystackElement : haystackAlternative.getElements()) {
                    if (!(haystackElement.symbol instanceof CstProductionModel))
                        continue;
                    // Remember, Multimap returns empty collection, not null.
                    // So this iterates only if haystackElement.symbol is to be replaced.
                    for (CstAlternativeModel inlineAlternative : inlineProductions.get((CstProductionModel) haystackElement.symbol)) {
                        resultAlternative = substitute(resultAlternative, haystackElement, inlineAlternative);
                        inlinedAlternatives.put(haystackAlternative, resultAlternative);
                    }
                }
            }
        }

        // Augment each haystack with its replacement.
        for (Map.Entry<CstAlternativeModel, CstAlternativeModel> e : inlinedAlternatives.entrySet()) {
            CstProductionModel cstProduction = e.getKey().getProduction();
            // cstProduction.removeAlterative(e.getKey());
            cstProduction.addAlternative(e.getValue());
        }

        // These replacements were inlined everywhere.
        for (CstAlternativeModel alternative : inlineProductions.values())
            alternative.getProduction().removeAlterative(alternative);

        // Some productions might have no alternatives left.
        for (CstProductionModel production : inlineProductions.keySet())
            if (production.getAlternatives().isEmpty())
                grammar.removeCstProduction(production);

        grammar.check();

        return inlinedAlternatives.size();
    }

    @SuppressWarnings("unused")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private void substituteUgly(@Nonnull LRState state, @Nonnull LRConflict.Map conflicts) {
        Table<LRItem, CstAlternativeModel, CstAlternativeModel> inlinedAlternatives = HashBasedTable.create();
        for (LRConflict conflict : conflicts.values()) {
            for (Map.Entry<? extends LRAction, ? extends LRItem> e : conflict.getItems().entrySet()) {
                if (e.getKey().getAction() != LRAction.Action.REDUCE)
                    continue;
                LRItem conflictItem = e.getValue();
                LOG.info("Conflict item is " + conflictItem);
                // This discards the specific conflict alternative, as we have to
                // inline all alternatives for the production. The other option would
                // be just to inline the conflicting alternative, but then we would still have
                // the original production rule present, so would not have eliminated the conflict.
                CstProductionModel conflictProduction = conflictItem.getProduction();
                // LOG.info("Conflict production is " + conflictProduction);
                if (isRecursive(conflictProduction))
                    continue;

                for (LRItem haystackItem : state.getItems()) {
                    if (haystackItem.getSymbol() != conflictProduction)
                        continue;
                    LOG.info("  Substituting into " + haystackItem);

                    // Now we reintroduce the alternative(s) we abolished above.
                    CstProductionModel haystackProduction = haystackItem.getProduction();
                    CstAlternativeModel haystackAlternative = haystackItem.getProductionAlternative();
                    CstElementModel haystackElement = haystackItem.getElement();
                    for (CstAlternativeModel replacementAlternative : conflictProduction.getAlternatives().values()) {
                        CstAlternativeModel resultAlternative = inlinedAlternatives.get(haystackItem, replacementAlternative);
                        // Avoid substituting the same thing twice, once per conflictItem.
                        if (resultAlternative == null) {
                            resultAlternative = substitute(haystackAlternative, haystackElement, replacementAlternative);
                            inlinedAlternatives.put(haystackItem, replacementAlternative, resultAlternative);
                        }
                        haystackProduction.alternatives.put(resultAlternative.getName(), resultAlternative);
                    }
                    haystackProduction.alternatives.remove(haystackAlternative.getName());
                }
            }
        }
    }
}
