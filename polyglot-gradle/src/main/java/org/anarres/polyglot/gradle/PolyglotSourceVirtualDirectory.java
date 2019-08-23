/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.gradle;

import groovy.lang.Closure;
import javax.annotation.Nonnull;
import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.util.ConfigureUtil;

/**
 *
 * @author shevek
 */
public class PolyglotSourceVirtualDirectory {

    private final SourceDirectorySet polyglot;

    public PolyglotSourceVirtualDirectory(@Nonnull String displayName, @Nonnull ObjectFactory objectFactory) {
        // polyglot = new DefaultSourceDirectorySet(String.format("Polyglot %s source", displayName), fileResolver);
        polyglot = objectFactory.sourceDirectorySet(displayName + ".polyglot", String.format("%s Polyglot source", displayName));
        polyglot.getFilter().include("**/*.polyglot", "**/*.sablecc");
    }

    @Nonnull
    public SourceDirectorySet getPolyglot() {
        return polyglot;
    }

    @Nonnull
    public PolyglotSourceVirtualDirectory polyglot(Closure<?> configureClosure) {
        ConfigureUtil.configure(configureClosure, getPolyglot());
        return this;
    }

    @Nonnull
    public PolyglotSourceVirtualDirectory polyglot(Action<? super SourceDirectorySet> configureAction) {
        configureAction.execute(getPolyglot());
        return this;
    }

}
