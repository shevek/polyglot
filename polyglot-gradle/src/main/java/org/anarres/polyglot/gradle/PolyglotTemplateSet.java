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
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.specs.Spec;

/**
 *
 * @author shevek
 */
public class PolyglotTemplateSet implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final Table<OutputLanguage, String, File> templates = HashBasedTable.create();

    public PolyglotTemplateSet(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public Table<OutputLanguage, String, File> getTemplates() {
        return templates;
    }

    public void template(String language, String dstPath, File srcFile) {
        templates.put(OutputLanguage.valueOf(language), dstPath, srcFile);
    }

    public void java(String dstPath, File srcFile) {
        templates.put(OutputLanguage.java, dstPath, srcFile);
    }

    public void html(String dstPath, File srcFile) {
        templates.put(OutputLanguage.html, dstPath, srcFile);
    }

    public void graphviz(String dstPath, File srcFile) {
        templates.put(OutputLanguage.graphviz, dstPath, srcFile);
    }

    @Nonnull
    public Spec<? super FileTreeElement> toSpec() {
        return new Spec<FileTreeElement>() {
            @Override
            public boolean isSatisfiedBy(FileTreeElement t) {
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
