/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.analysis.DepthFirstAdapter;
import org.anarres.polyglot.model.AbstractElementModel;
import org.anarres.polyglot.model.AbstractModel;
import org.anarres.polyglot.model.AbstractNamedJavaModel;
import org.anarres.polyglot.model.AbstractNamedModel;
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
import org.anarres.polyglot.node.ACharChar;
import org.anarres.polyglot.node.AConcatMatcher;
import org.anarres.polyglot.node.ADecChar;
import org.anarres.polyglot.node.ADifferenceMatcher;
import org.anarres.polyglot.node.AHelperMatcher;
import org.anarres.polyglot.node.AHexChar;
import org.anarres.polyglot.node.AIntervalMatcher;
import org.anarres.polyglot.node.APlusMatcher;
import org.anarres.polyglot.node.AQuestionMatcher;
import org.anarres.polyglot.node.AStarMatcher;
import org.anarres.polyglot.node.AStringMatcher;
import org.anarres.polyglot.node.AUnionMatcher;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.PMatcher;

/**
 *
 * @author shevek
 */
public class HtmlHelper {

    private final GrammarModel grammar;

    public HtmlHelper(OutputData data) {
        this.grammar = data.getGrammar();
        buildUsage();
    }

    public static enum ListGroup {

        Helpers, Tokens, CstProductions, Externals, AstProductions;
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

    public boolean isListExternals(@Nonnull Set<? extends ListGroup> groups) {
        return groups.contains(ListGroup.Externals) && !grammar.getExternals().isEmpty();
    }

    public boolean isListAstProductions(@Nonnull Set<? extends ListGroup> groups) {
        return groups.contains(ListGroup.AstProductions) && !grammar.getAstProductions().isEmpty();
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
        public void caseAStringMatcher(AStringMatcher node) {
            buf.append(node.getString().getText());
        }

        @Override
        public void caseACharChar(ACharChar node) {
            buf.append(node.getToken().getText());
        }

        @Override
        public void caseADecChar(ADecChar node) {
            buf.append(node.getToken().getText());
        }

        @Override
        public void caseAHexChar(AHexChar node) {
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
        if (text == null)
            return "No summary.";
        // Pattern.compile("\n\\s*\\**").matcher(text).replaceAll("");
        return text;
    }

    @Nonnull
    public String toJavadocDetail(@Nonnull AbstractModel model) {
        String text = model.getJavadocComment();
        if (text == null)
            return "No description.";
        // Pattern.compile("\n\\s*\\**").matcher(text).replaceAll("");
        return text;
    }

    private final Multimap<HelperModel, HelperModel> helperHelperUsage = HashMultimap.create();
    private final Multimap<HelperModel, TokenModel> tokenHelperUsage = HashMultimap.create();
    // Uses of given token in CST.
    private final Multimap<TokenModel, CstAlternativeModel> cstTokenUsage = HashMultimap.create();
    private final Multimap<CstProductionModel, CstAlternativeModel> cstCstUsage = HashMultimap.create();
    // Construction of given AST in CST.
    private final Multimap<AstAlternativeModel, CstAlternativeModel> cstAstUsage = HashMultimap.create();
    // Uses of given token in AST.
    private final Multimap<TokenModel, AstAlternativeModel> astTokenUsage = HashMultimap.create();
    private final Multimap<AstProductionModel, AstAlternativeModel> astAstUsage = HashMultimap.create();

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
        }

        for (final TokenModel token : grammar.tokens.values()) {
            token.getMatcher().apply(new DepthFirstAdapter() {
                @Override
                public void caseAHelperMatcher(AHelperMatcher node) {
                    String name = node.getHelperName().getText();
                    HelperModel helper = grammar.getHelper(name);
                    tokenHelperUsage.put(helper, token);
                }
            });
        }

        for (final CstProductionModel cstProduction : grammar.cstProductions.values()) {
            for (final CstAlternativeModel cstAlternative : cstProduction.getAlternatives().values()) {
                for (CstElementModel cstElement : cstAlternative.getElements()) {
                    if (cstElement.isTerminal())
                        cstTokenUsage.put(cstElement.getToken(), cstAlternative);
                    else
                        cstCstUsage.put(cstElement.getCstProduction(), cstAlternative);
                }

                for (final CstTransformExpressionModel cstTransformExpression : cstAlternative.getTransformExpressions()) {
                    cstTransformExpression.apply(new CstTransformExpressionModel.AbstractVisitor<Void, Void, RuntimeException>() {
                        @Override
                        public Void visitNew(CstTransformExpressionModel.New expression, Void input) throws RuntimeException {
                            cstAstUsage.put(expression.getAstAlternative(), cstAlternative);
                            return null;
                        }
                    }, null);
                }
            }
        }

        for (final AstProductionModel astProduction : grammar.astProductions.values()) {
            for (final AstAlternativeModel astAlternative : astProduction.getAlternatives()) {
                for (final AstElementModel astElement : astAlternative.getElements()) {
                    if (astElement.isTerminal())
                        astTokenUsage.put(astElement.getToken(), astAlternative);
                    else
                        astAstUsage.put(astElement.getAstProduction(), astAlternative);
                }
            }
        }
    }

    @Nonnull
    public Collection<HelperModel> getHelperHelperUsage(@Nonnull HelperModel m) {
        return helperHelperUsage.get(m);
    }

    @Nonnull
    public Collection<TokenModel> getTokenHelperUsage(@Nonnull HelperModel m) {
        return tokenHelperUsage.get(m);
    }

    /** Returns CstAlternativeModels which use the given TokenModel. */
    @Nonnull
    public Collection<CstAlternativeModel> getCstTokenUsage(@Nonnull TokenModel m) {
        return cstTokenUsage.get(m);
    }

    /** Returns CstAlternativeModels which use the given CstProductionModel. */
    @Nonnull
    public Collection<CstAlternativeModel> getCstCstUsage(@Nonnull CstProductionModel m) {
        return cstCstUsage.get(m);
    }

    /** Returns CstAlternativeModels which create the given AstAlternativeModel. */
    @Nonnull
    public Collection<CstAlternativeModel> getCstAstUsage(@Nonnull AstAlternativeModel m) {
        return cstAstUsage.get(m);
    }

    /** Returns AstAlternativeModels which use the given TokenModel. */
    @Nonnull
    public Collection<AstAlternativeModel> getAstTokenUsage(@Nonnull TokenModel m) {
        return astTokenUsage.get(m);
    }

    /** Returns AstAlternativeModels which use the given AstProductionModel. */
    @Nonnull
    public Collection<AstAlternativeModel> getAstAstUsage(@Nonnull AstProductionModel m) {
        return astAstUsage.get(m);
    }

}
