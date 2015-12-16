/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.AbstractNamedModel;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstTransformPrototypeModel;
import org.anarres.polyglot.model.ExternalModel;
import org.anarres.polyglot.model.TokenModel;

/**
 *
 * @author shevek
 */
public class HtmlHelper {

    @Nonnull
    public String a(@Nonnull AbstractNamedModel m) {
        Preconditions.checkNotNull(m, "AbstractNamedModel was null.");
        if (m instanceof ExternalModel)
            return "E-" + m.getName();
        if (m instanceof TokenModel)
            return "T-" + m.getName();
        if (m instanceof CstProductionModel)
            return "CP-" + m.getName();
        if (m instanceof CstAlternativeModel)
            return "CA-" + m.getName();
        if (m instanceof AstProductionModel)
            return "AP-" + m.getName();
        if (m instanceof AstAlternativeModel)
            return "AA-" + m.getName();
        if (m instanceof CstTransformPrototypeModel)
            return "CT-" + m.getName();
        throw new IllegalArgumentException("Unknown model " + m.getClass().getSimpleName());
    }

}
