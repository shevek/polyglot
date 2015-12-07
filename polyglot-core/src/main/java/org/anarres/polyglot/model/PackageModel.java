/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Joiner;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.APackage;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.TKwPackage;
import org.anarres.polyglot.node.Token;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
public class PackageModel extends AbstractModel {

    @Nonnull
    public static PackageModel forNode(@Nonnull APackage node) {
        List<String> packageNameParts = new ArrayList<>();
        for (TIdentifier part : node.getName())
            packageNameParts.add(part.getText());
        PackageModel model = new PackageModel(node.getLocation(), packageNameParts);
        model.setJavadocComment(node.getJavadocComment());
        return model;
    }

    private final List<? extends String> packageNameParts;

    public PackageModel(@Nonnull Token location, @Nonnull List<? extends String> packageNameParts) {
        super(location);
        this.packageNameParts = packageNameParts;
    }

    @Nonnull
    public List<? extends String> getPackageNameParts() {
        return packageNameParts;
    }

    @Nonnull
    @TemplateProperty
    public String getPackageName() {
        return Joiner.on('.').join(getPackageNameParts());
    }

    @Nonnull
    public String getPackagePath() {
        return Joiner.on(File.separator).join(getPackageNameParts());
    }

    @Override
    public APackage toNode() {
        List<TIdentifier> name = new ArrayList<>();
        for (String part : getPackageNameParts())
            name.add(new TIdentifier(part));
        return new APackage(newJavadocCommentToken(), name, new TKwPackage(getLocation()));
    }
}
