/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.AExternal;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.TIdentifier;

/**
 *
 * @author shevek
 */
public class ExternalModel extends AbstractNamedJavaModel /* implements AstProductionSymbol */ {

    @Nonnull
    public static ExternalModel forNode(@Nonnull AExternal node) {
        List<String> typeParts = new ArrayList<>();
        for (TIdentifier part : node.getExternalType())
            typeParts.add(part.getText());
        return new ExternalModel(node.getName(), typeParts);
    }

    private final List<? extends String> externalTypeParts;

    public ExternalModel(@Nonnull TIdentifier name, @Nonnull List<? extends String> externalTypeParts) {
        super(name);
        this.externalTypeParts = externalTypeParts;
    }

    @Override
    public String getJavaTypeName() {
        return Joiner.on('.').join(externalTypeParts);
    }

    @Override
    public Node toNode() {
        List<TIdentifier> externalType = new ArrayList<>();
        for (String part : externalTypeParts)
            externalType.add(new TIdentifier(part, getLocation()));
        return new AExternal(toNameToken(), externalType);
    }
}
