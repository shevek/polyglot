/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.analysis.DepthFirstAdapter;
import org.anarres.polyglot.model.AbstractElementModel;
import org.anarres.polyglot.model.AbstractModel;
import org.anarres.polyglot.model.AbstractNamedModel;
import org.anarres.polyglot.model.AnnotationModel;
import org.anarres.polyglot.model.AnnotationName;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstElementModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstTransformExpressionModel;
import org.anarres.polyglot.model.CstTransformPrototypeModel;
import org.anarres.polyglot.model.ExternalModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.HelperModel;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.node.AAlternateMatcher;
import org.anarres.polyglot.node.AConcatMatcher;
import org.anarres.polyglot.node.ADecCharLiteral;
import org.anarres.polyglot.node.ADifferenceMatcher;
import org.anarres.polyglot.node.AHelperMatcher;
import org.anarres.polyglot.node.AHexCharLiteral;
import org.anarres.polyglot.node.AIntervalMatcher;
import org.anarres.polyglot.node.APlusMatcher;
import org.anarres.polyglot.node.AQuestionMatcher;
import org.anarres.polyglot.node.AStarMatcher;
import org.anarres.polyglot.node.AStringLiteral;
import org.anarres.polyglot.node.AUnionMatcher;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.PMatcher;

/**
 *
 * @author shevek
 */
public class HtmlHelper extends AbstractHelper {

    private final GrammarModel grammar;

    public HtmlHelper(GrammarModel grammar) {
        this.grammar = grammar;
        buildUsage();
    }

    public static enum ListGroup {

        Helpers, Tokens, CstProductions, CstAlternatives, Externals, AstProductions, AstAlternatives, Unused;
    }

    public boolean isListUnused(@Nonnull Set<? extends ListGroup> groups) {
        return groups.contains(ListGroup.Unused);
    }

    public boolean isListHelpers(@Nonnull Set<? extends ListGroup> groups) {
        return groups.contains(ListGroup.Helpers) && !grammar.getHelpers().isEmpty();
    }

    public boolean isListTokens(@Nonnull Set<? extends ListGroup> groups) {
        return groups.contains(ListGroup.Tokens) && !grammar.getTokens().isEmpty();
    }

    public boolean isListCstProductions(@Nonnull Set<? extends ListGroup> groups) {
        return groups.contains(ListGroup.CstProductions) && !grammar.getCstProductions().isEmpty();
    }

    public boolean isListCstAlternatives(@Nonnull Set<? extends ListGroup> groups) {
        return groups.contains(ListGroup.CstAlternatives) && !grammar.getCstProductions().isEmpty();
    }

    public boolean isListExternals(@Nonnull Set<? extends ListGroup> groups) {
        return groups.contains(ListGroup.Externals) && !grammar.getExternals().isEmpty();
    }

    public boolean isListAstProductions(@Nonnull Set<? extends ListGroup> groups) {
        return groups.contains(ListGroup.AstProductions) && !grammar.getAstProductions().isEmpty();
    }

    public boolean isListAstAlternatives(@Nonnull Set<? extends ListGroup> groups) {
        return groups.contains(ListGroup.AstAlternatives) && !grammar.getAstProductions().isEmpty();
    }

    @Nonnull
    public List<AstProductionModel> getAstProductionRootsAlphabetical() {
        List<AstProductionModel> out = new ArrayList<>();
        for (CstProductionModel cstProduction : grammar.getCstProductionRoots()) {
            for (CstTransformPrototypeModel cstTransform : cstProduction.getTransformPrototypes()) {
                out.add(cstTransform.getAstProduction());
            }
        }
        Collections.sort(out, AstProductionModel.NameComparator.INSTANCE);
        return out;
    }

    @Nonnull
    public List<String> getAstProductionParserStartAnnotations(@Nonnull AstProductionModel model) {
        List<String> out = new ArrayList<>();
        for (CstProductionModel cstProduction : astCstProductionUsage.get(model)) {
            for (AnnotationModel annotation : cstProduction.getAnnotations(AnnotationName.ParserStart)) {
                out.add(annotation.getValue());
            }
        }
        return out;
    }

    @Nonnull
    public String a(@Nonnull AbstractNamedModel m) {
        Preconditions.checkNotNull(m, "AbstractNamedModel was null.");
        if (m instanceof ExternalModel)
            return "E-" + m.getName();
        if (m instanceof HelperModel)
            return "H-" + m.getName();
        if (m instanceof TokenModel)
            return "T-" + m.getName();
        if (m instanceof CstProductionModel)
            return "CP-" + m.getName();
        if (m instanceof CstAlternativeModel)
            return "CA-" + m.getName();
        if (m instanceof AstProductionModel)
            return "AP-" + m.getName();
        if (m instanceof AstAlternativeModel)
            return "AA-" + m.getName();
        if (m instanceof CstTransformPrototypeModel)
            return "CT-" + m.getName();
        throw new IllegalArgumentException("Unknown model " + m.getClass().getSimpleName());
    }

    private class MatcherVisitor extends DepthFirstAdapter {

        private final String aprefix;
        private final String asuffix;
        private final StringBuilder buf = new StringBuilder();

        public MatcherVisitor(@CheckForNull String aprefix, @CheckForNull String asuffix) {
            this.aprefix = aprefix;
            this.asuffix = asuffix;
        }

        @Override
        public void defaultCase(Node node) {
            throw new UnsupportedOperationException("Illegal node type " + node.getClass().getSimpleName());
        }

        public void applyToAtomic(@Nonnull Node node) {
            boolean wrap = false;
            if (node instanceof AConcatMatcher)
                wrap = true;
            else if (node instanceof AAlternateMatcher)
                wrap = true;
            if (wrap)
                buf.append("(");
            node.apply(this);
            if (wrap)
                buf.append(")");
        }

        @Override
        public void caseAHelperMatcher(AHelperMatcher node) {
            String name = node.getHelperName().getText();
            HelperModel helper = grammar.getHelper(name);
            boolean link = (helper != null) && (aprefix != null) && (asuffix != null);
            if (link) {
                buf.append("<a href=\"").append(aprefix).append(a(helper)).append(asuffix).append("\"");
                MatcherVisitor m = new MatcherVisitor(null, null);
                helper.getMatcher().apply(m);
                buf.append(" title=\"= ").append(m.buf).append("\"");
                buf.append(">");
            }
            buf.append(name);
            if (link) {
                buf.append("</a>");
            }
        }

        private void caseAListMatcher(@Nonnull String infix, @Nonnull Iterable<? extends PMatcher> nodes) {
            boolean b = false;
            for (PMatcher matcher : nodes) {
                if (b)
                    buf.append(infix);
                else
                    b = true;
                applyToAtomic(matcher);
            }
        }

        @Override
        public void caseAConcatMatcher(AConcatMatcher node) {
            caseAListMatcher(" ", node.getMatchers());
        }

        @Override
        public void caseAAlternateMatcher(AAlternateMatcher node) {
            caseAListMatcher(" | ", node.getMatchers());
        }

        private void caseAPostfixMatcher(@Nonnull String postfix, @Nonnull Node node) {
            applyToAtomic(node);
            buf.append(postfix);
        }

        @Override
        public void caseAQuestionMatcher(AQuestionMatcher node) {
            caseAPostfixMatcher("?", node.getMatcher());
        }

        @Override
        public void caseAPlusMatcher(APlusMatcher node) {
            caseAPostfixMatcher("+", node.getMatcher());
        }

        @Override
        public void caseAStarMatcher(AStarMatcher node) {
            caseAPostfixMatcher("*", node.getMatcher());
        }

        private void caseABinaryMatcher(@Nonnull String infix, @Nonnull Node left, @Nonnull Node right) {
            buf.append("[");
            applyToAtomic(left);
            buf.append(infix);
            applyToAtomic(right);
            buf.append("]");
        }

        @Override
        public void caseAUnionMatcher(AUnionMatcher node) {
            caseABinaryMatcher(" + ", node.getLeft(), node.getRight());
        }

        @Override
        public void caseADifferenceMatcher(ADifferenceMatcher node) {
            caseABinaryMatcher(" - ", node.getLeft(), node.getRight());
        }

        @Override
        public void caseAIntervalMatcher(AIntervalMatcher node) {
            caseABinaryMatcher("..", node.getLeft(), node.getRight());
        }

        @Override
        public void caseAStringLiteral(AStringLiteral node) {
            buf.append(node.getToken().getText());
        }

        @Override
        public void caseADecCharLiteral(ADecCharLiteral node) {
            buf.append(node.getToken().getText());
        }

        @Override
        public void caseAHexCharLiteral(AHexCharLiteral node) {
            buf.append(node.getToken().getText());
        }
    }

    @Nonnull
    public String toRegex(@Nonnull PMatcher matcher, @Nonnull String aprefix, @Nonnull String asuffix) {
        MatcherVisitor visitor = new MatcherVisitor(aprefix, asuffix);
        matcher.apply(visitor);
        return visitor.buf.toString();
    }

    @Nonnull
    public String toJavaMethodPrototype(@Nonnull AbstractElementModel<?> model) {
        StringBuilder buf = new StringBuilder();
        if (false) {
            // I don't really like this.
            if (model.isList())
                buf.append("@Nonnull ");
            else if (model.isNullable())
                buf.append("@CheckForNull ");
            else
                buf.append("@Nonnull ");
        }

        if (model.isList())
            buf.append("List&lt;");
        buf.append(model.getJavaTypeName());
        if (model.isList())
            buf.append("&gt;");
        buf.append(" get").append(model.getJavaMethodName()).append("()");
        return buf.toString();
    }

    @Nonnull
    public String toJavadocText(@Nonnull AbstractModel model) {
        String text = model.getJavadocComment();
        if (text == null)
            return "";
        // Pattern.compile("\n\\s*\\**").matcher(text).replaceAll("");
        return text;
    }

    @Nonnull
    public String toJavadocSummary(@Nonnull AbstractModel model) {
        String text = model.getJavadocComment();
        if (text != null)
            return text;
        // Pattern.compile("\n\\s*\\**").matcher(text).replaceAll("");
        if (model instanceof TokenModel)
            return "A lexical token matching " + toRegex(((TokenModel) model).getMatcher(), "", ".html");
        return "No summary.";
    }

    @Nonnull
    public String toJavadocDetail(@Nonnull AbstractModel model) {
        String text = model.getJavadocComment();
        if (text == null)
            return "No description.";
        // Pattern.compile("\n\\s*\\**").matcher(text).replaceAll("");
        return text;
    }

    private static final Function<AbstractNamedModel, Iterable<String>> FUNCTION_GET_ANNOTATION_NAMES = new Function<AbstractNamedModel, Iterable<String>>() {
        @Override
        @SuppressFBWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public Iterable<String> apply(AbstractNamedModel input) {
            return input.getAnnotations().keySet();
        }
    };

    private final Multimap<HelperModel, HelperModel> helperHelperUsage = HashMultimap.create();
    private final Multimap<HelperModel, TokenModel> helperTokenUsage = HashMultimap.create();
    // Uses of given token in CST.
    private final Multimap<TokenModel, CstProductionModel> tokenCstProductionUsage = HashMultimap.create();
    private final Multimap<TokenModel, CstAlternativeModel> tokenCstAlternativeUsage = HashMultimap.create();
    private final Set<CstProductionModel> cstRootUsage = new HashSet<>();
    private final Multimap<CstProductionModel, CstAlternativeModel> cstCstUsage = HashMultimap.create();
    // Construction of given AST in CST.
    private final Multimap<AstProductionModel, CstProductionModel> astCstProductionUsage = HashMultimap.create();
    private final Multimap<AstAlternativeModel, CstAlternativeModel> astCstAlternativeUsage = HashMultimap.create();
    // Uses of given token in AST.
    private final Multimap<TokenModel, AstAlternativeModel> tokenAstUsage = HashMultimap.create();
    private final Multimap<AstProductionModel, AstAlternativeModel> astAstUsage = HashMultimap.create();
    private final ModelMap<String> annotationUsage = new ModelMap<>();

    private void buildUsage() {
        for (final HelperModel token : grammar.getHelpers()) {
            token.getMatcher().apply(new DepthFirstAdapter() {
                @Override
                public void caseAHelperMatcher(AHelperMatcher node) {
                    String name = node.getHelperName().getText();
                    HelperModel helper = grammar.getHelper(name);
                    helperHelperUsage.put(helper, token);
                }
            });
            annotationUsage.put(token, FUNCTION_GET_ANNOTATION_NAMES);
        }

        for (final TokenModel token : grammar.tokens.values()) {
            token.getMatcher().apply(new DepthFirstAdapter() {
                @Override
                public void caseAHelperMatcher(AHelperMatcher node) {
                    String name = node.getHelperName().getText();
                    HelperModel helper = grammar.getHelper(name);
                    helperTokenUsage.put(helper, token);
                }
            });
            annotationUsage.put(token, FUNCTION_GET_ANNOTATION_NAMES);
        }

        cstRootUsage.addAll(grammar.getCstProductionRoots());
        for (final CstProductionModel cstProduction : grammar.getCstProductions()) {
            for (final CstTransformPrototypeModel cstTransformPrototype : cstProduction.getTransformPrototypes()) {
                if (cstTransformPrototype.symbol.isTerminal())
                    tokenCstProductionUsage.put(cstTransformPrototype.getToken(), cstProduction);
                else
                    astCstProductionUsage.put(cstTransformPrototype.getAstProduction(), cstProduction);
            }
            annotationUsage.put(cstProduction, FUNCTION_GET_ANNOTATION_NAMES);

            for (final CstAlternativeModel cstAlternative : cstProduction.getAlternatives()) {
                for (CstElementModel cstElement : cstAlternative.getElements()) {
                    if (cstElement.isTerminal())
                        tokenCstAlternativeUsage.put(cstElement.getToken(), cstAlternative);
                    else
                        cstCstUsage.put(cstElement.getCstProduction(), cstAlternative);
                }
                annotationUsage.put(cstAlternative, FUNCTION_GET_ANNOTATION_NAMES);

                for (final CstTransformExpressionModel cstTransformExpression : cstAlternative.getTransformExpressions()) {
                    cstTransformExpression.apply(new CstTransformExpressionModel.AbstractVisitor<Void, Void, RuntimeException>() {
                        @Override
                        public Void visitNew(CstTransformExpressionModel.New expression, Void input) throws RuntimeException {
                            astCstAlternativeUsage.put(expression.getAstAlternative(), cstAlternative);
                            for (CstTransformExpressionModel argument : expression.getArguments())
                                argument.apply(this, input);
                            return null;
                        }

                        /*
                         @Override
                         public Void visitReference(CstTransformExpressionModel.Reference expression, Void input) throws RuntimeException {
                         AstProductionSymbol symbol = expression.transform.getSymbol();
                         if (symbol.isTerminal())
                         tokenCstAlternativeUsage.put((TokenModel) symbol, cstAlternative);
                         // else
                         // astCstAlternativeUsage.put((AstProductionModel) symbol, cstAlternative);
                         return null;
                         }
                         */
                    }, null);
                }
            }
        }

        for (final AstProductionModel astProduction : grammar.astProductions.values()) {
            annotationUsage.put(astProduction, FUNCTION_GET_ANNOTATION_NAMES);
            for (final AstAlternativeModel astAlternative : astProduction.getAlternatives()) {
                for (final AstElementModel astElement : astAlternative.getElements()) {
                    if (astElement.isTerminal())
                        tokenAstUsage.put(astElement.getToken(), astAlternative);
                    else
                        astAstUsage.put(astElement.getAstProduction(), astAlternative);
                }
                annotationUsage.put(astAlternative, FUNCTION_GET_ANNOTATION_NAMES);
            }
        }
    }

    @Nonnull
    public Collection<HelperModel> getHelperHelperUsage(@Nonnull HelperModel m) {
        return helperHelperUsage.get(m);
    }

    @Nonnull
    public Collection<TokenModel> getHelperTokenUsage(@Nonnull HelperModel m) {
        return helperTokenUsage.get(m);
    }

    @Nonnull
    public Collection<CstProductionModel> getTokenCstProductionUsage(@Nonnull TokenModel m) {
        return tokenCstProductionUsage.get(m);
    }

    /** Returns CstAlternativeModels which use the given TokenModel. */
    @Nonnull
    public Collection<CstAlternativeModel> getTokenCstAlternativeUsage(@Nonnull TokenModel m) {
        return tokenCstAlternativeUsage.get(m);
    }

    /** Returns CstAlternativeModels which use the given CstProductionModel. */
    @Nonnull
    public Collection<CstAlternativeModel> getCstCstUsage(@Nonnull CstProductionModel m) {
        return cstCstUsage.get(m);
    }

    @Nonnull
    public Collection<CstProductionModel> getAstCstProductionUsage(@Nonnull AstProductionModel m) {
        return astCstProductionUsage.get(m);
    }

    /** Returns CstAlternativeModels which create the given AstAlternativeModel. */
    @Nonnull
    public Collection<CstAlternativeModel> getAstCstAlternativeUsage(@Nonnull AstAlternativeModel m) {
        return astCstAlternativeUsage.get(m);
    }

    /** Returns AstAlternativeModels which use the given TokenModel. */
    @Nonnull
    public Collection<AstAlternativeModel> getTokenAstUsage(@Nonnull TokenModel m) {
        return tokenAstUsage.get(m);
    }

    /** Returns AstAlternativeModels which use the given AstProductionModel. */
    @Nonnull
    public Collection<AstAlternativeModel> getAstAstUsage(@Nonnull AstProductionModel m) {
        return astAstUsage.get(m);
    }

    /**
     * Returns true if the model is used.
     *
     * For an AST production, this may return false if the alternative is constructed
     * by the CST but subsequently dropped on the floor and not referenced in the AST.
     *
     * @param m The model for which to find usages.
     * @return true if the model is used.
     */
    public boolean isUsed(@Nonnull AbstractModel m) {
        if (m instanceof HelperModel)
            return helperHelperUsage.containsKey(m) || helperTokenUsage.containsKey(m);
        if (m instanceof TokenModel)
            return tokenCstAlternativeUsage.containsKey(m) || tokenAstUsage.containsKey(m) || ((TokenModel) m).isIgnored();
        if (m instanceof CstProductionModel)
            return cstRootUsage.contains(m) || cstCstUsage.containsKey(m);
        if (m instanceof CstAlternativeModel)
            return true;
        if (m instanceof AstProductionModel)
            return astAstUsage.containsKey(m) || astCstProductionUsage.containsKey(m); // || m == grammar.getAstProductionRoot();
        if (m instanceof AstAlternativeModel)
            return astCstAlternativeUsage.containsKey(m);
        throw new IllegalArgumentException("Unknown model " + m.getClass());
    }

    public boolean isListModel(@Nonnull Set<? extends ListGroup> groups, @Nonnull AbstractModel m) {
        if (groups.contains(ListGroup.Unused))
            return !isUsed(m);
        return true;
    }

    @Nonnull
    public Set<? extends String> getAnnotationNames() {
        return annotationUsage.getKeys();
    }

    @Nonnull
    public ModelMap<String>.ModelSet getAnnotationUsage(@Nonnull String annotationName) {
        return annotationUsage.getValues(annotationName);
    }

    @Nonnull
    public Collection<? extends AnnotationModel> getAnnotations(@Nonnull AbstractNamedModel m) {
        return m.getAnnotations().values();
    }
}
