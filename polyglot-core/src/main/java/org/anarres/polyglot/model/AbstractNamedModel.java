/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.analysis.NFABuilderVisitor;
import org.anarres.polyglot.node.AAnnotation;
import org.anarres.polyglot.node.PAnnotation;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.TString;
import org.anarres.polyglot.node.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public abstract class AbstractNamedModel extends AbstractModel implements AnnotatedModel {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNamedModel.class);

    public static class NameComparator implements Comparator<AbstractNamedModel> {

        public static final NameComparator INSTANCE = new NameComparator();

        @Override
        public int compare(AbstractNamedModel o1, AbstractNamedModel o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    @Nonnull
    public static String name(@Nonnull AbstractNamedModel parent, @CheckForNull TIdentifier name) {
        String text = parent.getName();
        if (name != null)
            text = text + "." + name.getText();
        return text;
    }

    @Nonnull
    public static String name(@Nonnull AbstractNamedModel parent, @CheckForNull String name) {
        String text = parent.getName();
        if (name != null)
            text = text + "." + name;
        return text;
    }

    @Nonnull
    protected static Multimap<String, ? extends AnnotationModel> annotations(@Nonnull ErrorHandler errors, @CheckForNull Collection<? extends PAnnotation> nodes) {
        if (nodes == null || nodes.isEmpty())
            return ImmutableMultimap.of();
        // LOG.info("Annotations in: " + Iterables.size(nodes));
        Multimap<String, AnnotationModel> out = HashMultimap.create();
        for (PAnnotation node : nodes) {
            AAnnotation a = (AAnnotation) node;
            TString value = a.getValue();
            AnnotationModel m = new AnnotationModel(a.getName(), value == null ? null : NFABuilderVisitor.parse(errors, value));
            // LOG.info("Annotation: " + m);
            out.put(m.getName(), m);
        }
        // LOG.info("Annotations out: " + out);
        return out;
    }

    @Nonnull
    public List<? extends AAnnotation> toAnnotations(@Nonnull Multimap<? extends String, ? extends AnnotationModel> annotations) {
        if (annotations.isEmpty())
            return Collections.<AAnnotation>emptyList();
        List<AAnnotation> out = new ArrayList<>();
        for (AnnotationModel a : annotations.values())
            out.add(a.toNode());
        return out;
    }

    private final String name;
    private final Multimap<String, ? extends AnnotationModel> annotations;

    public AbstractNamedModel(@Nonnull Token location, @Nonnull String name, @Nonnull Multimap<String, ? extends AnnotationModel> annotations) {
        super(location);
        this.name = name;
        this.annotations = annotations;
    }

    public AbstractNamedModel(@Nonnull TIdentifier name, @Nonnull Multimap<String, ? extends AnnotationModel> annotations) {
        super(name);
        this.name = name.getText();
        this.annotations = annotations;
    }

    /** The qualified name of this object, in the object model. */
    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public String getDescriptiveName() {
        AnnotationModel annotation = Iterables.getFirst(getAnnotations(AnnotationName.Named), null);
        if (annotation != null)
            return Preconditions.checkNotNull(annotation.getValue(), "@Named annotation requires a value.");
        return getName();
    }

    /** The unqualified name of this object, in the source. */
    @Nonnull
    public String getSourceName() {
        return getName();
    }

    @Override
    public final Multimap<String, ? extends AnnotationModel> getAnnotations() {
        return annotations;
    }

    @Nonnull
    public Collection<? extends AnnotationModel> getAnnotations(@Nonnull String name) {
        return getAnnotations().get(name);
    }

    @Override
    public Collection<? extends AnnotationModel> getAnnotations(@Nonnull AnnotationName name) {
        return getAnnotations(name.name());
    }

    @CheckForNull
    public AnnotationModel getAnnotation(@Nonnull String name) {
        return Iterables.getFirst(getAnnotations(name), null);
    }

    @CheckForNull
    public AnnotationModel getAnnotation(@Nonnull AnnotationName name) {
        return Iterables.getFirst(getAnnotations(name), null);
    }

    public boolean hasAnnotation(@Nonnull String name) {
        return getAnnotations().containsKey(name);
    }

    @Override
    public boolean hasAnnotation(@Nonnull AnnotationName name) {
        return hasAnnotation(name.name());
    }

    public static class HasAnnotation implements Predicate<AbstractNamedModel> {

        private final String annotationName;

        public HasAnnotation(@Nonnull String annotationName) {
            this.annotationName = annotationName;
        }

        public HasAnnotation(@Nonnull AnnotationName annotationName) {
            this(annotationName.name());
        }

        @Override
        public boolean apply(AbstractNamedModel input) {
            return input.hasAnnotation(annotationName);
        }
    }

    @Nonnull
    public TIdentifier toNameToken(Token location) {
        return new TIdentifier(getSourceName(), location);
    }

    @Nonnull
    public TIdentifier toNameToken() {
        return toNameToken(getLocation());
    }

    @Override
    public String toString() {
        // if (this instanceof Indexed) return getName() + "(" + ((Indexed) this).getIndex() + ")";
        return getName();
    }
}
