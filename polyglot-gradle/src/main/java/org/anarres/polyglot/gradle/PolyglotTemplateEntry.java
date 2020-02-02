/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.gradle;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import java.io.File;
import java.io.Serializable;
import org.anarres.polyglot.output.OutputLanguage;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

/**
 * A helper class for Gradle build-cache key calculation.
 *
 * @author shevek
 */
/* pp */ class PolyglotTemplateEntry implements Comparable<PolyglotTemplateEntry>, Serializable {

    private static final long serialVersionUID = 1L;
    @Internal
    private final Project project;
    private final String templateGlob;
    private final Table.Cell<OutputLanguage, String, Object> cell;

    public PolyglotTemplateEntry(Project project, String templateGlob, Table.Cell<OutputLanguage, String, Object> cell) {
        this.project = project;
        this.templateGlob = templateGlob;
        this.cell = cell;
    }

    @Input
    public String getTemplateGlob() {
        return templateGlob;
    }

    @Input
    public OutputLanguage getOutputLanguage() {
        return cell.getRowKey();
    }

    @Input
    public String getTemplateTarget() {
        return cell.getColumnKey();
    }

    @Input
    public Object getTemplateSource() {
        return cell.getValue();
    }

    @InputFile
    @PathSensitive(value = PathSensitivity.NONE)
    public File getTemplateSourceFile() {
        return project.file(cell.getValue());
    }

    @Override
    public int compareTo(PolyglotTemplateEntry o) {
        return ComparisonChain.start()
                .compare(getTemplateGlob(), o.getTemplateGlob())
                .compare(getOutputLanguage(), o.getOutputLanguage())
                .compare(getTemplateTarget(), o.getTemplateTarget())
                .compare(getTemplateSource(), o.getTemplateSource(), Ordering.natural().onResultOf(String::valueOf))
                .result();
    }

    @Override
    public String toString() {
        return getTemplateGlob() + " : " + cell;
    }

}
