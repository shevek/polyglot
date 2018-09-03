/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.graphviz.builder.GraphVizGraph;
import org.anarres.graphviz.builder.GraphVizNode;
import org.anarres.graphviz.builder.GraphVizScope;
import org.anarres.graphviz.builder.GraphVizable;
import org.anarres.polyglot.node.AAstProduction;
import org.anarres.polyglot.node.AAstSection;
import org.anarres.polyglot.node.ACstProduction;
import org.anarres.polyglot.node.AGrammar;
import org.anarres.polyglot.node.AHelper;
import org.anarres.polyglot.node.AHelpersSection;
import org.anarres.polyglot.node.AProductionsSection;
import org.anarres.polyglot.node.AStatesSection;
import org.anarres.polyglot.node.AToken;
import org.anarres.polyglot.node.ATokensSection;
import org.anarres.polyglot.node.EOF;
import org.anarres.polyglot.node.PSection;
import org.anarres.polyglot.node.Start;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
public class GrammarModel implements GraphVizScope {

    private static class DefaultMap<K, V> extends LinkedHashMap<K, V> {
    }

    private PackageModel _package;
    private final Map<String, ExternalModel> externals = new DefaultMap<>();
    private final Map<String, HelperModel> helpers = new DefaultMap<>(); // Temporarily preserve order.
    public int stateIndex = 0;
    public final Map<String, StateModel> states = new DefaultMap<>();   // Must be linked/sorted.
    public final PrecedenceComparator precedenceComparator = new PrecedenceComparator();
    public int tokenIndex = 1;  // Allow for EOF.
    // public final TokenModel.EOF tokenEof = new TokenModel.EOF();
    public final Map<String, TokenModel> tokens = new DefaultMap<>();
    public int cstProductionIndex = 0;
    private CstProductionModel cstProductionRoot;
    public final Map<String, CstProductionModel> cstProductions = new DefaultMap<>();
    public int cstAlternativeIndex = 0;
    // private AstProductionModel astProductionRoot;
    public final Map<String, AstProductionModel> astProductions = new DefaultMap<>();
    // public final Map<String, AlternativeModel> productionAlternatives = new HashMap<>();
    public int inlineIndex = 0;

    @Nonnull
    @TemplateProperty
    public PackageModel getPackage() {
        return _package;
    }

    public void setPackage(@Nonnull PackageModel _package) {
        this._package = _package;
    }

    @Nonnull
    @TemplateProperty("html")
    public List<ExternalModel> getExternalsAlphabetical() {
        return getExternals();
    }

    @Nonnull
    @TemplateProperty
    public List<ExternalModel> getExternals() {
        List<ExternalModel> out = new ArrayList<>(externals.values());
        Collections.sort(out, ExternalModel.NameComparator.INSTANCE);
        return out;
    }

    @CheckForNull
    public ExternalModel getExternal(@Nonnull String name) {
        return externals.get(name);
    }

    public boolean addExternal(@Nonnull ExternalModel model) {
        Object prev = externals.put(model.getName(), model);
        return prev == null;
    }

    @TemplateProperty("html")
    public List<HelperModel> getHelpersAlphabetical() {
        List<HelperModel> out = new ArrayList<>(helpers.values());
        Collections.sort(out, HelperModel.NameComparator.INSTANCE);
        return out;
    }

    @Nonnull
    public Collection<? extends HelperModel> getHelpers() {
        return helpers.values();
    }

    @CheckForNull
    public HelperModel getHelper(@Nonnull String name) {
        return helpers.get(name);
    }

    public boolean addHelper(@Nonnull HelperModel model) {
        HelperModel prev = helpers.put(model.getName(), model);
        return prev == null || prev.hasAnnotation(AnnotationName.Weak);
    }

    @Nonnull
    public Set<String> getLexerMachineNames() {
        if (tokens.isEmpty())
            return Collections.emptySet();
        Set<String> out = new HashSet<>();
        for (TokenModel token : tokens.values()) {
            if (token.hasAnnotation(AnnotationName.LexerInclude))
                for (AnnotationModel annotation : token.getAnnotations(AnnotationName.LexerInclude))
                    out.add(MoreObjects.firstNonNull(annotation.getValue(), ""));
            if (token.hasAnnotation(AnnotationName.LexerExclude))
                for (AnnotationModel annotation : token.getAnnotations(AnnotationName.LexerExclude))
                    out.add(MoreObjects.firstNonNull(annotation.getValue(), ""));
        }
        if (out.isEmpty())
            return Collections.singleton("");
        return out;
    }

    @Nonnull
    public StateModel addState(@Nonnull TIdentifier node) {
        String name = StateModel.name(node);
        StateModel model = new StateModel(stateIndex++, node);
        return states.put(name, model);
    }

    @Nonnull
    public List<StateModel> getStates() {
        List<StateModel> out = new ArrayList<>(states.values());
        Collections.sort(out, StateModel.IndexComparator.INSTANCE);
        return out;
    }

    /** Sorted by index. */
    @Nonnull
    @TemplateProperty("java")
    public List<TokenModel> getTokens() {
        List<TokenModel> out = new ArrayList<>(tokens.values());
        Collections.sort(out, TokenModel.IndexComparator.INSTANCE);
        return out;
    }

    @Nonnull
    @TemplateProperty("html")
    public List<TokenModel> getTokensAlphabetical() {
        List<TokenModel> out = new ArrayList<>(tokens.values());
        Collections.sort(out, TokenModel.NameComparator.INSTANCE);
        return out;
    }

    @Nonnull
    public Map<String, TokenModel> getTokenMap() {
        return tokens;
    }

    @CheckForNull
    public TokenModel getToken(@Nonnull String name) {
        return tokens.get(name);
    }

    /**
     * Returns the CST productions in definition order.
     *
     * @return the CST productions in definition order.
     */
    @Nonnull
    @TemplateProperty("parser.vm")
    public List<CstProductionModel> getCstProductions() {
        List<CstProductionModel> out = new ArrayList<>(cstProductions.values());
        Collections.sort(out, CstProductionModel.IndexComparator.INSTANCE);
        return out;
    }

    @Nonnull
    @TemplateProperty("html")
    public List<CstProductionModel> getCstProductionsAlphabetical() {
        List<CstProductionModel> out = new ArrayList<>(cstProductions.values());
        Collections.sort(out, CstProductionModel.NameComparator.INSTANCE);
        return out;
    }

    @Nonnull
    public List<? extends CstProductionModel> getCstProductionRoots() {
        List<CstProductionModel> out = new ArrayList<CstProductionModel>();
        for (CstProductionModel cstProduction : cstProductions.values())
            if (cstProduction.hasAnnotation(AnnotationName.ParserStart))
                out.add(cstProduction);
        if (!out.isEmpty())
            return out;
        CstProductionModel cstProduction = cstProductionRoot;
        if (cstProduction != null)
            return Collections.singletonList(cstProduction);
        return Collections.emptyList();
    }

    @Nonnull
    public List<? extends CstProductionModel> getCstProductionRootsAlphabetical() {
        List<CstProductionModel> out = new ArrayList<CstProductionModel>(getCstProductionRoots());
        Collections.sort(out, CstProductionModel.NameComparator.INSTANCE);
        return out;
    }

    @CheckForNull
    public CstProductionModel getCstProduction(@Nonnull String name) {
        return cstProductions.get(name);
    }

    public boolean addCstProduction(@Nonnull CstProductionModel cstProduction) {
        if (cstProductionRoot == null)
            cstProductionRoot = cstProduction;
        CstProductionModel prev = cstProductions.put(cstProduction.getName(), cstProduction);
        return prev == null || prev.hasAnnotation(AnnotationName.Weak);
    }

    public boolean removeCstProduction(@Nonnull CstProductionModel cstProduction) {
        return cstProductions.remove(cstProduction.getName()) != null;
    }

    @Nonnegative
    public int getCstAlternativeCount() {
        int out = 0;
        for (CstProductionModel cstProduction : cstProductions.values())
            out += cstProduction.getAlternatives().size();
        return out;
    }

    @Nonnull
    public Collection<? extends AstProductionModel> getAstProductions() {
        return astProductions.values();
    }

    @Nonnull
    public List<AstProductionModel> getAstProductionsAlphabetical() {
        List<AstProductionModel> out = new ArrayList<>(astProductions.values());
        Collections.sort(out, AstProductionModel.NameComparator.INSTANCE);
        return out;
    }

    // @Nonnull @TemplateProperty public AstProductionModel getAstProductionRoot() { return astProductionRoot; }
    @CheckForNull
    @TemplateProperty
    public AstProductionModel getAstProduction(@Nonnull String name) {
        return astProductions.get(name);
    }

    public boolean addAstProduction(@Nonnull AstProductionModel astProduction) {
        // if (astProductionRoot == null) astProductionRoot = astProduction;
        AstProductionModel prev = astProductions.put(astProduction.getName(), astProduction);
        return prev == null || prev.hasAnnotation(AnnotationName.Weak);
    }

    public boolean removeAstProduction(@Nonnull AstProductionModel astProduction) {
        return astProductions.remove(astProduction.getName()) != null;
    }

    private boolean isSingleAlternativeProduction(@Nonnull CstProductionModel production) {
        return production.alternatives.size() == 1;
    }

    private boolean isSingleAlternativeProduction(CstProductionSymbol symbol) {
        if (!(symbol instanceof CstProductionModel))
            return false;
        return isSingleAlternativeProduction((CstProductionModel) symbol);
    }

    /** Elides all Production nodes which have exactly one alternative. */
    public void toGraphVizCst(@Nonnull GraphVizGraph graph) {
        if (false)
            for (Map.Entry<String, TokenModel> e : tokens.entrySet()) {
                TokenModel token = e.getValue();
                if (!token.isIgnored())
                    graph.node(this, token).label(e.getKey());
            }
        for (Map.Entry<String, CstProductionModel> e : cstProductions.entrySet()) {
            CstProductionModel production = e.getValue();
            if (production.isIgnored(null))
                continue;
            if (!isSingleAlternativeProduction(production))
                graph.node(this, production).label(e.getKey());
            for (Map.Entry<String, CstAlternativeModel> f : production.alternatives.entrySet()) {
                CstAlternativeModel alternative = f.getValue();
                if (alternative.isIgnored(null))
                    continue;
                graph.node(this, alternative).label(f.getKey());
                if (!isSingleAlternativeProduction(production))
                    graph.edge(this, production, alternative);
                for (CstElementModel element : alternative.elements) {
                    if (element.symbol instanceof TokenModel)
                        // graph.node(this, element.symbol).label(((TokenModel) element.symbol).getName());
                        continue;
                    if (!isSingleAlternativeProduction(element.symbol))
                        graph.edge(this, alternative, element.symbol);
                    else
                        graph.edge(this, alternative, ((CstProductionModel) element.symbol).alternatives.values().iterator().next());
                }
            }
        }
    }

    @Nonnull
    public GraphVizable getCstGraphVizable() {
        return new GraphVizable() {
            @Override
            public void toGraphViz(GraphVizGraph graph) {
                toGraphVizCst(graph);
            }
        };
    }

    private boolean isSingleAlternativeProduction(@Nonnull AstProductionModel production) {
        return production.alternatives.size() == 1;
    }

    private boolean isSingleAlternativeProduction(AstProductionSymbol symbol) {
        if (!(symbol instanceof AstProductionModel))
            return false;
        return isSingleAlternativeProduction((AstProductionModel) symbol);
    }

    private boolean isLeafAlternative(@Nonnull AstAlternativeModel alternative) {
        for (AstElementModel element : alternative.elements)
            if (!element.isTerminal())
                return false;
        return true;
    }

    public void toGraphVizAst(@Nonnull GraphVizGraph graph) {
        if (false)
            for (Map.Entry<String, TokenModel> e : tokens.entrySet()) {
                TokenModel token = e.getValue();
                if (!token.isIgnored())
                    graph.node(this, token).label(e.getKey());
            }
        for (Map.Entry<String, AstProductionModel> e : astProductions.entrySet()) {
            AstProductionModel production = e.getValue();
            GraphVizNode productionNode = null;
            if (!isSingleAlternativeProduction(production))
                productionNode = graph.node(this, production).label(e.getKey());
            ALTERNATIVE:
            for (Map.Entry<String, AstAlternativeModel> f : production.alternatives.entrySet()) {
                AstAlternativeModel alternative = f.getValue();
                if (productionNode != null && isLeafAlternative(alternative)) {
                    productionNode.label().append("\n(").append(f.getKey()).append(")");
                } else {
                    graph.node(this, alternative).label(f.getKey());
                    if (!isSingleAlternativeProduction(production))
                        graph.edge(this, production, alternative);
                    for (AstElementModel element : alternative.elements) {
                        if (element.symbol instanceof TokenModel)
                            // graph.node(this, element.symbol).label(((TokenModel) element.symbol).getName());
                            continue;
                        if (!isSingleAlternativeProduction(element.symbol))
                            graph.edge(this, alternative, Preconditions.checkNotNull(element.symbol, "Null symbol in " + f.getKey()));
                        else
                            graph.edge(this, alternative, ((AstProductionModel) element.symbol).alternatives.values().iterator().next());
                    }
                }
            }
        }
    }

    @Nonnull
    public GraphVizable getAstGraphVizable() {
        return new GraphVizable() {
            @Override
            public void toGraphViz(GraphVizGraph graph) {
                toGraphVizAst(graph);
            }
        };
    }

    /**
     * Converts this grammar model back into a partial AST.
     *
     * @return the constructed AST.
     * @see #toTree()
     */
    @Nonnull
    @SuppressFBWarnings("BC_VACUOUS_INSTANCEOF")    // LinkedHashMap below.
    public AGrammar toNode() {
        AGrammar grammar = new AGrammar();

        PACKAGE:
        {
            grammar.setPackage(_package.toNode());
        }

        List<PSection> sections = new ArrayList<>();

        HELPERS:
        {
            List<AHelper> helpers = new ArrayList<>();
            for (HelperModel e : this.helpers.values())
                helpers.add(e.toNode());
            sections.add(new AHelpersSection(helpers));
        }

        STATES:
        {
            List<TIdentifier> states = new ArrayList<>();
            for (StateModel e : getStates())
                states.add(e.toNode());
            sections.add(new AStatesSection(states));
        }

        TOKENS:
        {
            List<AToken> tokens = new ArrayList<>();
            for (TokenModel e : getTokens())
                tokens.add(e.toNode());
            sections.add(new ATokensSection(tokens));
        }

        CST:
        {
            List<ACstProduction> productions = new ArrayList<>();
            for (CstProductionModel e : getCstProductions())
                productions.add(e.toNode());
            sections.add(new AProductionsSection(productions));
        }

        AST:
        {
            List<AAstProduction> productions = new ArrayList<>();
            // This one has to be first.
            // if (astProductionRoot != null) productions.add(astProductionRoot.toNode());
            List<Map.Entry<String, AstProductionModel>> astProductions = new ArrayList<>(this.astProductions.entrySet());
            // If order wasn't preserved, let's put some sensible order on it.
            if (!(this.astProductions instanceof LinkedHashMap)) {
                Collections.sort(astProductions, new Comparator<Map.Entry<String, AstProductionModel>>() {
                    @Override
                    public int compare(Map.Entry<String, AstProductionModel> o1, Map.Entry<String, AstProductionModel> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                });
            }
            for (Map.Entry<String, AstProductionModel> e : astProductions)
                // if (e.getValue() != astProductionRoot)
                productions.add(e.getValue().toNode());
            sections.add(new AAstSection(productions));
        }

        grammar.setSections(sections);

        return grammar;
    }

    /**
     * Converts this grammar model back into a full AST.
     *
     * @return the constructed AST.
     */
    @Nonnull
    public Start toTree() {
        return new Start(toNode(), new EOF());
    }

    /**
     * Checks internal invariants of the grammar model.
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    public void check() {
        for (Map.Entry<String, CstProductionModel> e : cstProductions.entrySet()) {
            if (!e.getKey().equals(e.getValue().getName()))
                throw new IllegalStateException("Mis-named CST production " + e.getKey() + " -> " + e.getValue());
            for (CstAlternativeModel cstAlternative : e.getValue().getAlternatives().values()) {
                for (CstElementModel cstElement : cstAlternative.getElements()) {
                    if (!cstElement.isTerminal()) {
                        CstProductionModel subProduction = cstElement.getCstProduction();
                        if (subProduction == null)
                            throw new IllegalStateException("Null CST production in " + cstElement);
                        if (cstProductions.get(subProduction.getName()) == null)
                            throw new IllegalStateException("Missing CST production in " + cstElement);
                        if (cstProductions.get(subProduction.getName()) != subProduction)
                            throw new IllegalStateException("Bad pointer to CST production in " + cstElement);
                    }
                }
            }
        }

        for (Map.Entry<String, AstProductionModel> e : astProductions.entrySet()) {
            if (!e.getKey().equals(e.getValue().getName()))
                throw new IllegalStateException("Bad AST production " + e.getKey() + " -> " + e.getValue());
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("Externals:\n");
        buf.append(externals).append("\n");

        buf.append("Helpers:\n");
        buf.append(helpers).append("\n");

        buf.append("States:\n");
        buf.append(states).append("\n");

        buf.append("Tokens:\n");
        buf.append(tokens).append("\n");

        buf.append("Productions:");
        buf.append(cstProductions).append("\n");

        return buf.toString();
    }
}
