/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Preconditions;
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
import org.anarres.polyglot.analysis.NFABuilderVisitor;
import org.anarres.polyglot.node.AAnnotation;
import org.anarres.polyglot.node.PAnnotation;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.TString;
import org.anarres.polyglot.node.Token;

/**
 *
 * @author shevek
 */
public abstract class AbstractNamedModel extends AbstractModel {

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
    protected static Multimap<String, ? extends AnnotationModel> annotations(@CheckForNull Iterable<? extends PAnnotation> nodes) {
        if (nodes == null)
            return ImmutableMultimap.of();
        Multimap<String, AnnotationModel> out = HashMultimap.create();
        if (nodes != null) {
            for (PAnnotation node : nodes) {
                AAnnotation a = (AAnnotation) node;
                TString value = a.getValue();
                AnnotationModel m = new AnnotationModel(a.getName(), value == null ? null : NFABuilderVisitor.parse(a.getValue()));
                // LOG.info("Annotation: " + m);
                out.put(m.getName(), m);
            }
        }
        // LOG.info("Annotations: " + out);
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

    @Nonnull
    public final Multimap<String, ? extends AnnotationModel> getAnnotations() {
        return annotations;
    }

    @Nonnull
    public Collection<? extends AnnotationModel> getAnnotations(@Nonnull String name) {
        return annotations.get(name);
    }

    @Nonnull
    public Collection<? extends AnnotationModel> getAnnotations(@Nonnull AnnotationName name) {
        return getAnnotations(name.name());
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
