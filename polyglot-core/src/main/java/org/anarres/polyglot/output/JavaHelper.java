/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstElementModel;
import org.anarres.polyglot.model.AstModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstTransformPrototypeModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class JavaHelper extends AbstractHelper {

    private static final int LEXER_GROUP_SHIFT = 8;
    private static final int LEXER_GROUP_SIZE = 1 << LEXER_GROUP_SHIFT;
    /** If this is 9, then the generated methods should not exceed the JIT method size limit. */
    private static final int ALTERNATIVE_GROUP_SHIFT = 9;
    private static final int ALTERNATIVE_GROUP_SIZE = 1 << ALTERNATIVE_GROUP_SHIFT;

    public static class AbstractGroup<T> extends ArrayList<T> {

        private final int index;

        public AbstractGroup(@Nonnegative int index) {
            this.index = index;
        }

        @Nonnegative
        @TemplateProperty("stringlexer.vm")
        public int getIndex() {
            return index;
        }

        @Nonnull
        @TemplateProperty("stringlexer.vm")
        public String getJavaMethodName() {
            return "G" + index;
        }
    }

    public static class LexerGroup extends AbstractGroup<TokenModel> {

        public LexerGroup(int index) {
            super(index);
        }

        @Nonnull
        @TemplateProperty("stringlexer.vm")
        public List<TokenModel> getTokens() {
            return this;
        }
    }

    public static class CstAlternativeGroup extends AbstractGroup<CstAlternativeModel> {

        public CstAlternativeGroup(@Nonnegative int index) {
            super(index);
        }

        /**
         * For naming consistency with {@link CstProductionModel#getAlternatives()}.
         *
         * @return this object.
         */
        @Nonnull
        @TemplateProperty("parser.vm")
        public List<CstAlternativeModel> getAlternatives() {
            return this;
        }
    }

    /*new Escaper() { @Override public String escape(String string) { return StringEscapeUtils.escapeJava(string); } };*/
    public static final Escaper ESCAPER = Escapers.builder()
            .addEscape('\t', "\\t")
            .addEscape('\r', "\\r")
            .addEscape('\n', "\\n")
            .addEscape('"', "\\\"")
            .addEscape('\\', "\\\\")
            .build();

    @Nonnull
    private final Set<? extends Option> options;
    @Nonnull
    private final GrammarModel grammar;
    private final ThreadLocal<Map<String, String>> locals = new ThreadLocal<Map<String, String>>() {
        @Override
        protected Map<String, String> initialValue() {
            return new HashMap<>();
        }
    };

    public JavaHelper(@Nonnull Set<? extends Option> options, @Nonnull GrammarModel grammar) {
        this.options = options;
        this.grammar = grammar;
    }

    @Nonnull
    public String escape(@Nonnull String in) {
        return ESCAPER.escape(Preconditions.checkNotNull(in, "Cannot escape null string."));
    }

    /**
     * Used to choose a different emit-strategy for the switch in the parser.
     *
     * @return true if the grammar or the automaton is "large".
     */
    @TemplateProperty("stringlexer.vm")
    public boolean isLarge(EncodedStateMachine.Lexer lexerMachine) {
        if (options.contains(Option.CG_LARGE))
            return true;
        if (grammar.tokenIndex > LEXER_GROUP_SIZE)
            return true;
        return false;
    }

    @TemplateProperty("stringlexer.vm")
    public int getLexerGroupShift() {
        return LEXER_GROUP_SHIFT;
    }

    @Nonnull
    @TemplateProperty("stringlexer.vm")
    public List<LexerGroup> getLexerGroups() {
        List<LexerGroup> out = new ArrayList<>();
        for (TokenModel token : grammar.tokens.values()) {
            int groupIndex = token.getIndex() >> LEXER_GROUP_SHIFT;
            while (out.size() <= groupIndex)
                out.add(new LexerGroup(out.size()));
            out.get(groupIndex).add(token);
        }
        return out;
    }

    /**
     * Used to choose a different emit-strategy for the switch in the parser.
     *
     * @return true if the grammar or the automaton is "large".
     */
    @TemplateProperty("parser.vm")
    public boolean isLarge(EncodedStateMachine.Parser parserMachine) {
        // return isLarge(parserMachine.getAutomaton());
        if (options.contains(Option.CG_LARGE))
            return true;
        if (grammar.cstAlternativeIndex > ALTERNATIVE_GROUP_SIZE)
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

    @Nonnull
    @TemplateProperty("parser.vm")
    public String beginLocalVariables() {
        locals.remove();
        return "";
    }

    @Nonnull
    @TemplateProperty("parser.vm")
    public String endLocalVariables() {
        locals.remove();
        return "";
    }

    @Nonnull
    @TemplateProperty("parser.vm")
    public String getReduceMethodName(@Nonnull CstAlternativeModel model) {
        if (options.contains(Option.CG_COMPACT))
            return "r" + model.getIndex();
        else
            return "reduce" + model.getJavaMethodName();
    }

    @Nonnull
    private String newCompactLocalName(@Nonnull String in) {
        Map<String, String> locals = this.locals.get();
        String out = locals.get(in);
        if (out == null) {
            out = "v" + locals.size();
            locals.put(in, out);
        }
        // out = "/*" + in + "*/" + out;
        return out;
    }

    @Nonnull
    @TemplateProperty("parser.vm")
    public String getLocalVariableName(@Nonnull CstElementModel element) {
        String out = element.getJavaFieldName() + "_nodes";
        if (options.contains(Option.CG_COMPACT))
            out = newCompactLocalName(out);
        return out;
    }

    @Nonnull
    @TemplateProperty("parser.vm")
    public String getLocalVariableName(@Nonnull CstElementModel element, @Nonnull CstTransformPrototypeModel transformPrototype) {
        String out = element.getJavaFieldName() + "__" + transformPrototype.getName();
        if (options.contains(Option.CG_COMPACT))
            out = newCompactLocalName(out);
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

    public boolean isOption(@Nonnull Option option) {
        return options.contains(option);
    }

    @TemplateProperty
    public boolean isOption(@Nonnull String name) {
        return isOption(Option.valueOf(name));
    }

    /**
     * Returns the value of the unique annotation on the given model with the given name,
     * escaped as a Java literal without encapsulating quotation marks.
     *
     * @param model The model from which to retrieve annotations.
     * @param name The name of the annotations to retrieve.
     * @return The escaped value of the unique annotation on the given model with the given name.
     */
    @Nonnull
    public String getAnnotationJavaText(@Nonnull AstModel model, @Nonnull String name) {
        String text = getAnnotation(model, name);
        if (text == null)
            throw new IllegalArgumentException("Annotation '" + name + "' has no value on " + model + ".");
        return escape(text);
    }

    @Nonnull
    public String getSuperClass(@Nonnull AstModel model, @Nonnull String defaultValue) {
        List<String> values = getAnnotations(model, "javaExtends");
        return Iterables.getFirst(values, defaultValue);
    }

    @Nonnull
    public List<String> getExternalTypes() {
        // return Arrays.asList("boolean", "byte", "char", "short", "int", "long", "float", "double", "String", "Object");
        return Arrays.asList("long", "double", "Object");
    }

    @Nonnull
    private static String capitalize(@Nonnull String word) {
        if (word.isEmpty())
            return word;
        StringBuilder buf = new StringBuilder();
        buf.append(Character.toUpperCase(word.charAt(0)));
        buf.append(word, 1, word.length());
        return buf.toString();
    }

    @Nonnull
    public String getExternalMethodName(@Nonnull String externalType) {
        switch (externalType) {
            case "byte":
            case "char":
            case "short":
            case "int":
            case "long":
                // All widen losslessly to long.
                return "Long";
            case "float":
            case "double":
                // All widen losslessly to double.
                return "Double";
            // Autoboxing booleans is allocation-free.
            case "boolean":
            default:
                return "Object";
        }
    }

    private static enum State {

        CONST, @Deprecated
        DOLLAR, PERCENT, VAR;
    }

    public static class FormatToken {

        private final Object value;

        public FormatToken(Object value) {
            this.value = value;
        }

        public boolean isText() {
            return value instanceof String;
        }

        @Nonnull
        public String getJavaText() {
            return ESCAPER.escape(String.valueOf(value));
        }

        public boolean isIndent() {
            return value instanceof Integer;
        }

        @CheckForSigned
        public int getIndent() {
            return ((Integer) value).intValue();
        }

        public boolean isElement() {
            return value instanceof AstElementModel;
        }

        @Nonnull
        public AstElementModel getElement() {
            return (AstElementModel) value;
        }

        @Override
        public String toString() {
            if (isText())
                return "string:\"" + getJavaText() + "\"";
            if (isIndent())
                return "indent:" + getIndent();
            return "element:" + getElement();
        }
    }

    public static abstract class FormatLexer {

        private static final Logger LOG = LoggerFactory.getLogger(FormatLexer.class);

        @Nonnull
        protected abstract AstElementModel getElement(@Nonnull CharSequence name);

        @Nonnull
        public List<FormatToken> lex(@Nonnull String format) {
            List<FormatToken> out = new ArrayList<>();
            StringBuilder buf = new StringBuilder();
            State state = State.CONST;

            CHAR:
            for (int i = 0; i < format.length(); i++) {
                char c = format.charAt(i);
                switch (state) {
                    case CONST:
                        switch (c) {
                            case '%':
                                state = State.PERCENT;
                                continue CHAR;
                            default:
                                buf.append(c);
                                continue CHAR;
                        }
                    case PERCENT:
                        if (c == '%') {
                            buf.append('%');
                            state = State.CONST;
                            continue CHAR;
                        }
                        if (buf.length() > 0) {
                            out.add(new FormatToken(buf.toString()));
                            buf.setLength(0);
                        }
                        switch (c) {
                            case '{':
                                state = State.VAR;
                                continue CHAR;
                            case '>':
                            case '<':
                                out.add(new FormatToken(c == '<' ? -1 : +1));
                                state = State.CONST;
                                continue CHAR;
                        }
                        break;
                    case VAR:
                        switch (c) {
                            case '}':
                                out.add(new FormatToken(getElement(buf)));
                                buf.setLength(0);
                                state = State.CONST;
                                continue CHAR;
                            default:
                                buf.append(c);
                                continue CHAR;
                        }
                }
                throw new IllegalStateException("Unexpected character '" + Character.toString(c) + "' in state " + state);
            }

            switch (state) {
                case CONST:
                    if (buf.length() > 0)
                        out.add(new FormatToken(buf.toString()));
                    break;
                default:
                    throw new IllegalStateException("Unterminated format code at end of '" + format + "'.");
            }
            return out;
        }
    }

    @Nonnull
    public List<FormatToken> lexFormat(@Nonnull final AstAlternativeModel model, @Nonnull String format) {
        FormatLexer lexer = new FormatLexer() {
            @Override
            protected AstElementModel getElement(CharSequence name) {
                for (AstElementModel element : model.getElements()) {
                    if (element.getName().contentEquals(name))
                        return element;
                }
                for (AstElementModel element : model.getExternals()) {
                    if (element.getName().contentEquals(name))
                        return element;
                }
                throw new IllegalArgumentException("No such element '" + name + "' in AST alternative '" + model.getName() + "'.");
            }
        };
        return lexer.lex(format);
    }

    @Nonnull
    public List<FormatToken> lexFormat(@Nonnull final AstElementModel model, @Nonnull String format) {
        FormatLexer lexer = new FormatLexer() {
            @Override
            protected AstElementModel getElement(CharSequence name) {
                if ("self".contentEquals(name))
                    return model;
                if ("this".contentEquals(name))
                    return model;
                if (model.getName().contentEquals(name))
                    return model;
                throw new IllegalArgumentException("Cannot refer to element element '" + name + "' from format string on element '" + model.getName() + "'.");
            }
        };
        return lexer.lex(format);
    }
}
