/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import com.google.common.base.CaseFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.AbstractNamedModel;
import org.anarres.polyglot.model.AnnotationModel;
import org.anarres.polyglot.model.AnnotationName;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;

/**
 * <ul>
 * <li>A CST root must transform to at most one AST production.
 * </ul>
 *
 * @author shevek
 */
public class StartChecker implements Runnable {

    @Nonnull
    public static String getMachineName(@Nonnull CstProductionModel cstProductionRoot) {
        AnnotationModel startAnnotation = cstProductionRoot.getAnnotation(AnnotationName.ParserStart);
        return startAnnotation == null ? "" : getMachineName(startAnnotation);
    }

    @Nonnull
    public static String getMachineName(@Nonnull AnnotationModel annotation) {
        String value = annotation.getValue();
        if (value == null)
            return "";
        // Deprecated code path.
        if (value.indexOf('_') != -1)
            return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, value);
        return value;
    }

    private final ErrorHandler errors;
    private final GrammarModel grammar;

    public StartChecker(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    private void checkMachineNames(@Nonnull Set<? extends String> machineNames, @Nonnull AbstractNamedModel model, @Nonnull AnnotationName name) {
        for (AnnotationModel annotation : model.getAnnotations(name)) {
            String machineName = getMachineName(annotation);
            if (!machineNames.contains(machineName))
                errors.addError(model.getLocation(), "Production '" + model.getName() + "' annotated @" + name.name() + " references an unknown parser-machine '" + machineName + "'.");
        }
    }

    private void checkMachineNames(@Nonnull Set<? extends String> machineNames, @Nonnull AbstractNamedModel model) {
        checkMachineNames(machineNames, model, AnnotationName.ParserInclude);
        checkMachineNames(machineNames, model, AnnotationName.ParserExclude);
        checkMachineNames(machineNames, model, AnnotationName.ParserIgnore);
    }

    @Override
    public void run() {
        Set<String> machineNames = new HashSet<>();

        for (CstProductionModel cstProduction : grammar.getCstProductionRoots()) {
            // TODO: We could relax this if we allowed Start to have N fields by iterating this transform.
            if (cstProduction.transformPrototypes.size() != 1) {
                errors.addError(cstProduction.getLocation(), "Production '" + cstProduction.getName() + "' annotated @" + AnnotationName.ParserStart.name() + " must transform into exactly one AST production.");
            }

            String machineName = "";
            Collection<? extends AnnotationModel> annotations = cstProduction.getAnnotations(AnnotationName.ParserStart);
            if (!annotations.isEmpty()) {
                AnnotationModel annotation = annotations.iterator().next();
                if (annotations.size() > 1)
                    errors.addError(annotation.getLocation(), "At most one @" + AnnotationName.ParserStart.name() + " annotation is permitted on CST production '" + cstProduction.getName() + "'.");
                machineName = getMachineName(annotation);
            }

            if (!machineNames.add(machineName))
                errors.addError(cstProduction.getLocation(), "Duplicate parser name '" + machineName + "' on CST production '" + cstProduction.getName() + "'.");
        }

        // for (TokenModel token : grammar.tokens.values()) {
        // checkMachineNames(machineNames, token);
        // }
        for (CstProductionModel cstProduction : grammar.cstProductions.values()) {
            checkMachineNames(machineNames, cstProduction);
            for (CstAlternativeModel cstAlternative : cstProduction.alternatives.values()) {
                checkMachineNames(machineNames, cstAlternative);
            }
        }
    }

}
