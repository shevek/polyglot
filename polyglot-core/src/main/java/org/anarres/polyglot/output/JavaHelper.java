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
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.lr.LRAutomaton;
import org.anarres.polyglot.model.AnnotationModel;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstElementModel;
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
    @CheckForNull
    private final LRAutomaton automaton;

    public JavaHelper(@Nonnull Set<? extends Option> options, @Nonnull GrammarModel grammar, @CheckForNull LRAutomaton automaton) {
        this.options = options;
        this.grammar = grammar;
        this.automaton = automaton;
    }

    @Nonnull
    public String escape(String in) {
        return ESCAPER.escape(Preconditions.checkNotNull(in, "Cannot escape null string."));
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

    @Nonnull
    public boolean hasAnnotation(@Nonnull AstModel model, @Nonnull String name) {
        return hasAnnotations(model, name);
    }

    /**
     * Returns the value of the unique annotation on the given model with the given name.
     *
     * If the annotation is missing or not unique, an exception is thrown.
     * If the annotation has a null or no value, a null is returned.
     * Note: A null return value does NOT mean that the annotation was missing.
     *
     * @param model The model from which to retrieve annotations.
     * @param name The name of the annotations to retrieve.
     * @return The value of the unique annotation on the given model with the given name.
     */
    @CheckForNull
    public String getAnnotation(@Nonnull AstModel model, @Nonnull String name) {
        AnnotationModel annotation = Iterables.getOnlyElement(model.getAnnotations().get(name));
        return annotation.getValue();
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
        return escape(getAnnotation(model, name));
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

    @Nonnull
    private AstElementModel lexGetElement(@Nonnull AstAlternativeModel model, @Nonnull CharSequence name) {
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

    @Nonnull
    public List<FormatToken> lexFormat(@Nonnull AstAlternativeModel model, @Nonnull String format) {
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
                        default:
                            break;
                    }
                case VAR:
                    switch (c) {
                        case '}':
                            out.add(new FormatToken(lexGetElement(model, buf)));
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
                throw new IllegalStateException("Unterminated variable at end of '" + format + "'.");
        }
        return out;
    }
}
