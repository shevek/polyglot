/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.analysis.MatcherParserVisitor;
import org.anarres.polyglot.dfa.NFA;
import org.anarres.polyglot.lr.Indexed;
import org.anarres.polyglot.node.ALiteralMatcher;
import org.anarres.polyglot.node.AToken;
import org.anarres.polyglot.node.ATokenState;
import org.anarres.polyglot.node.PMatcher;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.TJavadocComment;
import org.anarres.polyglot.output.JavaHelper;
import org.anarres.polyglot.output.TemplateProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class TokenModel extends AbstractNamedJavaModel implements CstProductionSymbol, AstProductionSymbol, Indexed {

    private static final Logger LOG = LoggerFactory.getLogger(TokenModel.class);

    public static class IndexComparator implements Comparator<TokenModel> {

        public static final IndexComparator INSTANCE = new IndexComparator();

        @Override
        public int compare(TokenModel o1, TokenModel o2) {
            return Integer.compare(o1.getIndex(), o2.getIndex());
        }
    }

    // public static class Comparator implements java.util.Comparator<TokenModel> {
    // public static final Comparator INSTANCE = new Comparator();
    // @Override public int compare(TokenModel o1, TokenModel o2) { return Integer.compare(o1.getIndex(), o2.getIndex()); }
    // }
    public static class EOF extends TokenModel {

        public static final EOF INSTANCE = new EOF();
        public static final int INDEX = 0;

        public EOF() {
            super(INDEX, new TIdentifier("<eof>"), new ALiteralMatcher(), null, ImmutableMultimap.<String, AnnotationModel>of());
            setJavadocComment(new TJavadocComment("/** A magic end-of-input token returned from the Lexer. */"));
        }

        @Override
        public String getJavaText() {
            return "";
        }

        @Override
        public String getJavaTypeName() {
            return "EOF";
        }

        @Override
        public String getJavaMethodName() {
            return "EOF";
        }

        @Override
        public String getJavaFieldName() {
            return "eof";
        }

        @Override
        public boolean isFixed() {
            return true;
        }
    }

    public static class Invalid extends TokenModel {

        public static final Invalid INSTANCE = new Invalid();

        public Invalid() {
            super(Integer.MAX_VALUE, new TIdentifier("<invalid>"), new ALiteralMatcher(), null, ImmutableMultimap.<String, AnnotationModel>of());
            setJavadocComment(new TJavadocComment("/** A magic 'invalid' token returned from the Lexer. */"));
        }

        @Override
        public String getJavaTypeName() {
            return "InvalidToken";
        }

        @Override
        public String getJavaMethodName() {
            return "InvalidToken";
        }

        @Override
        public boolean isFixed() {
            return false;
        }
    }

    @CheckForNull
    private static String toText(@Nonnull ErrorHandler errors, @Nonnull PMatcher matcher, @Nonnull Multimap<String, ? extends AnnotationModel> annotations) {
        if (matcher instanceof ALiteralMatcher) {
            // :-(
            ALiteralMatcher literalMatcher = (ALiteralMatcher) matcher;
            // A special case in our code, e.g. EOF, etc.
            if (literalMatcher.getLiteral() == null)
                return null;
            MatcherParserVisitor parser = new MatcherParserVisitor(errors);
            literalMatcher.getLiteral().apply(parser);
            return parser.getString(literalMatcher.getLiteral());
        }
        for (AnnotationModel annotation : annotations.get(AnnotationName.Text.name())) {
            String value = annotation.getValue();
            if (value != null)
                return value;
        }
        return null;
    }

    @Nonnull
    public static TokenModel forNode(@Nonnull ErrorHandler errors, @Nonnegative int index, @Nonnull AToken node) {
        Multimap<String, ? extends AnnotationModel> annotations = annotations(errors, node.getAnnotations());
        String text = toText(errors, node.getMatcher(), annotations);
        return new TokenModel(index, node.getName(), node.getMatcher(), text, annotations);
    }

    private final int index;
    // If empty, then NORMAL only.
    private final PMatcher matcher;
    public final Map<StateModel, StateModel> transitions = new HashMap<>();
    private final CstTransformPrototypeModel transformPrototype;
    public boolean ignored;
    public NFA nfa;
    // Cached
    private final String javaTypeName;
    private final String text;

    public TokenModel(int index, @Nonnull TIdentifier name, @Nonnull PMatcher matcher, @CheckForNull String text, @Nonnull Multimap<String, ? extends AnnotationModel> annotations) {
        super(name, annotations);
        this.index = index;
        this.matcher = matcher;
        this.transformPrototype = new CstTransformPrototypeModel(name, Specifier.TOKEN, name, UnaryOperator.NONE);
        this.transformPrototype.symbol = this;
        this.javaTypeName = "T" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, getName());
        this.text = text;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Nonnull
    @TemplateProperty("html")
    public PMatcher getMatcher() {
        return matcher;
    }

    @Nonnull
    public CstTransformPrototypeModel getTransformPrototype() {
        return transformPrototype;
    }

    @Override
    public List<CstTransformPrototypeModel> getTransformPrototypes() {
        return Collections.singletonList(getTransformPrototype());
    }

    @Override
    public String getJavaTypeName() {
        return javaTypeName;
    }

    @TemplateProperty
    public boolean isIgnored() {
        return ignored;
    }

    @TemplateProperty
    public boolean isFixed() {
        // This relies on
        // * Never generate a concat of length 1.
        // * Never generate an alternate unless required.
        // Both of these properties are embedded in the CST grammar itself.
        // LOG.info("Matcher in " + this + " is " + matcher + " of " + matcher.getClass());
        // return matcher instanceof ALiteralMatcher || hasAnnotation(AnnotationName.Text);
        return (text != null);
    }

    @Nonnull
    public String getText() {
        if (text == null)
            throw new IllegalStateException("Not a known fixed token type (or @Text used without value): " + matcher.getClass());
        return text;
    }

    @Nonnull
    public String getJavaText() {
        return JavaHelper.ESCAPER.escape(getText());
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @TemplateProperty
    public boolean isTransitional() {
        // We require at least one non-identity transition.
        for (Map.Entry<StateModel, StateModel> e : transitions.entrySet()) {
            if (e.getKey() != e.getValue())
                return true;
        }
        return false;
    }

    @Nonnull
    @TemplateProperty
    public Collection<? extends Map.Entry<StateModel, StateModel>> getTransitions() {
        return transitions.entrySet();
    }

    @Override
    public AToken toNode() {
        List<ATokenState> states = new ArrayList<>();
        for (Map.Entry<StateModel, StateModel> e : this.transitions.entrySet())
            states.add(new ATokenState(e.getKey().toNameToken(), e.getValue().toNameToken()));
        return new AToken(
                newJavadocCommentToken(),
                states,
                toNameToken(),
                matcher.clone(),
                toAnnotations(getAnnotations()));
    }
}
