/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.gradle;

import groovy.lang.Closure;
import javax.annotation.Nonnull;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.util.ConfigureUtil;

/**
 *
 * @author shevek
 */
public class PolyglotSourceSet {

    private final SourceDirectorySet polyglot;

    public PolyglotSourceSet(@Nonnull String displayName, @Nonnull SourceDirectorySetFactory sourceDirectorySetFactory) {
        // polyglot = new DefaultSourceDirectorySet(String.format("Polyglot %s source", displayName), fileResolver);
        polyglot = sourceDirectorySetFactory.create(displayName + ".polyglot", String.format("%s Polyglot source", displayName));
        polyglot.getFilter().include("**/*.polyglot", "**/*.sablecc");
    }

    @Nonnull
    public SourceDirectorySet getPolyglot() {
        return polyglot;
    }

    @Nonnull
    public PolyglotSourceSet polyglot(Closure<?> configureClosure) {
        ConfigureUtil.configure(configureClosure, getPolyglot());
        return this;
    }
}
