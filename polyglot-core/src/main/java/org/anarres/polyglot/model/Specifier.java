/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Preconditions;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.analysis.AnalysisAdapter;
import org.anarres.polyglot.node.AExternalSpecifier;
import org.anarres.polyglot.node.AProductionSpecifier;
import org.anarres.polyglot.node.ATokenSpecifier;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.PSpecifier;

/**
 *
 * @author shevek
 */
public enum Specifier {

    ANY {
                @Override
                public PSpecifier newSpecifier() {
                    return null;
                }

                @Override
                public boolean isAllowed(Specifier specifier) {
                    return true;
                }
            },
    TOKEN {
                @Override
                public PSpecifier newSpecifier() {
                    return new ATokenSpecifier();
                }
            },
    PRODUCTION {
                @Override
                public PSpecifier newSpecifier() {
                    return new AProductionSpecifier();
                }
            },
    EXTERNAL {
                @Override
                public PSpecifier newSpecifier() {
                    return new AExternalSpecifier();
                }
            };

    public boolean isAllowed(@Nonnull Specifier specifier) {
        return specifier == this;
    }

    @CheckForNull
    public abstract PSpecifier newSpecifier();

    private static class SpecifierVisitor extends AnalysisAdapter {

        private Specifier specifier = null;

        @Override
        public void defaultCase(Node node) {
            throw new UnsupportedOperationException("Unexpected node " + node.getClass().getSimpleName());
        }

        @Override
        public void caseATokenSpecifier(ATokenSpecifier node) {
            this.specifier = TOKEN;
        }

        @Override
        public void caseAProductionSpecifier(AProductionSpecifier node) {
            this.specifier = PRODUCTION;
        }
    }

    @Nonnull
    public static Specifier toSpecifier(@CheckForNull PSpecifier node) {
        if (node == null)
            return ANY;
        SpecifierVisitor visitor = new SpecifierVisitor();
        node.apply(visitor);
        return Preconditions.checkNotNull(visitor.specifier, "Got no Specifier from " + node.getClass());
    }

}
