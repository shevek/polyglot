/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.analysis.NFABuilderVisitor;
import org.anarres.polyglot.node.AAnnotation;
import org.anarres.polyglot.node.PAnnotation;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.Token;

/**
 *
 * @author shevek
 */
public abstract class AbstractModel implements Model {

    /** Computes a location token for an alternative which might not have its own name. */
    @Nonnull
    protected static Token location(@Nonnull AbstractModel parent, @CheckForNull TIdentifier name) {
        if (name != null)
            return name;
        return parent.getLocation();
    }

    @Nonnull
    protected static Multimap<String, AnnotationModel> annotations(@CheckForNull Iterable<? extends PAnnotation> nodes) {
        Multimap<String, AnnotationModel> out = HashMultimap.create();
        if (nodes != null) {
            for (PAnnotation node : nodes) {
                AAnnotation a = (AAnnotation) node;
                AnnotationModel m = new AnnotationModel(a.getName(), NFABuilderVisitor.parse(a.getValue()));
                out.put(m.getName(), m);
            }
        }
        return out;
    }

    @Nonnull
    public List<AAnnotation> toAnnotations(@Nonnull Multimap<String, AnnotationModel> annotations) {
        List<AAnnotation> out = new ArrayList<>();
        for (AnnotationModel a : annotations.values())
            out.add(a.toNode());
        return out;
    }

    private final Token location;

    public AbstractModel(@Nonnull Token location) {
        this.location = Preconditions.checkNotNull(location, "Location was null.");
    }

    @Override
    public Token getLocation() {
        return location;
    }
}
