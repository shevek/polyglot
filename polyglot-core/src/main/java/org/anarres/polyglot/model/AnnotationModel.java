/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.MoreObjects;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.AAnnotation;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.TString;

/**
 *
 * @author shevek
 */
public class AnnotationModel extends AbstractModel {

    private final TIdentifier name;
    private final String value;

    public AnnotationModel(@Nonnull TIdentifier name, @CheckForNull String value) {
        super(name);
        this.name = name;
        this.value = value;
    }

    @Nonnull
    public String getName() {
        return name.getText();
    }

    @CheckForNull
    public String getValue() {
        return value;
    }

    @Override
    public AAnnotation toNode() {
        return new AAnnotation(name, new TString("'" + value + "'"));
    }

    @Override
    public String toString() {
        return "@" + getName() + "(" + MoreObjects.firstNonNull(getValue(), "") + ")";
    }
}
