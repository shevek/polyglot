/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

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

    public AnnotationModel(TIdentifier name, String value) {
        super(name);
        this.name = name;
        this.value = value;
    }

    @Nonnull
    public String getName() {
        return name.getText();
    }

    @Nonnull
    public String getValue() {
        return value;
    }

    @Override
    public AAnnotation toNode() {
        return new AAnnotation(name, new TString(value));
    }

}
