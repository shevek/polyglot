/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import java.util.Comparator;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.TIdentifier;
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

    protected final String name;

    public AbstractNamedModel(@Nonnull Token location, @Nonnull String name) {
        super(location);
        this.name = name;
    }

    public AbstractNamedModel(TIdentifier name) {
        super(name);
        this.name = name.getText();
    }

    /** The qualified name of this object, in the object model. */
    @Nonnull
    public String getName() {
        return name;
    }

    /** The unqualified name of this object, in the source. */
    @Nonnull
    public String getSourceName() {
        return getName();
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
