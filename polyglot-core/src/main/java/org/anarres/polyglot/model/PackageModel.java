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
import org.anarres.polyglot.output.TemplateProperty;
import org.anarres.polyglot.node.APackage;
import org.anarres.polyglot.node.TIdentifier;

/**
 *
 * @author shevek
 */
public class PackageModel {

    @Nonnull
    public static PackageModel fromNode(@Nonnull APackage node) {
        List<String> packageNameParts = new ArrayList<>();
        for (TIdentifier part : node.getName())
            packageNameParts.add(part.getText());
        return new PackageModel(packageNameParts);
    }

    private final List<? extends String> packageNameParts;

    public PackageModel(@Nonnull List<? extends String> packageNameParts) {
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

    @Nonnull
    public APackage toNode() {
        List<TIdentifier> name = new ArrayList<>();
        for (String part : getPackageNameParts())
            name.add(new TIdentifier(part));
        return new APackage(name);
    }
}
