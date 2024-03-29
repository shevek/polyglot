/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.gradle;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.io.File;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.anarres.polyglot.output.OutputLanguage;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;

/**
 *
 * @author shevek
 */
public class PolyglotTemplateSet implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final Table<OutputLanguage, String, Object> templates = HashBasedTable.create();

    public PolyglotTemplateSet(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    @Input
    public String getName() {
        return name;
    }

    @Nonnull
    @Input
    public Table<OutputLanguage, String, Object> getTemplates() {
        return templates;
    }

    public void template(@Nonnull String language, @Nonnull String dstPath, @Nonnull Object srcFile) {
        templates.put(OutputLanguage.valueOf(language), dstPath, srcFile);
    }

    public void java(@Nonnull String dstPath, @Nonnull Object srcFile) {
        templates.put(OutputLanguage.java, dstPath, srcFile);
    }

    public void html(@Nonnull String dstPath, @Nonnull Object srcFile) {
        templates.put(OutputLanguage.html, dstPath, srcFile);
    }

    public void graphviz(@Nonnull String dstPath, @Nonnull Object srcFile) {
        templates.put(OutputLanguage.graphviz, dstPath, srcFile);
    }

    @Nonnull
    public Spec<? super File> toSpec() {
        return new Spec<File>() {
            @Override
            public boolean isSatisfiedBy(File t) {
                String glob = getName();
                String name = t.getName();
                return "*".equals(glob) || name.contains(glob);
            }
        };
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getTemplates());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (null == obj)
            return false;
        if (!getClass().equals(obj.getClass()))
            return false;
        PolyglotTemplateSet o = (PolyglotTemplateSet) obj;
        return Objects.equals(getName(), o.getName())
                && Objects.equals(getTemplates(), o.getTemplates());
    }
}
