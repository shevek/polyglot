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
import org.anarres.polyglot.node.APlusUnOp;
import org.anarres.polyglot.node.AQuestionUnOp;
import org.anarres.polyglot.node.AStarUnOp;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.PUnOp;

/**
 *
 * @author shevek
 */
public enum UnaryOperator {

    NONE("") {
                @Override
                public PUnOp newUnOp() {
                    return null;
                }
            },
    STAR("*") {
                @Override
                public PUnOp newUnOp() {
                    return new AStarUnOp();
                }

                @Override
                public boolean isList() {
                    return true;
                }

                @Override
                public boolean isNullable() {
                    return true;
                }
            },
    PLUS("+") {
                @Override
                public PUnOp newUnOp() {
                    return new APlusUnOp();
                }

                @Override
                public boolean isList() {
                    return true;
                }
            },
    QUESTION("?") {
                @Override
                public PUnOp newUnOp() {
                    return new AQuestionUnOp();
                }

                @Override
                public boolean isNullable() {
                    return true;
                }
            };
    private final String text;

    private UnaryOperator(@Nonnull String text) {
        this.text = text;
    }

    @Nonnull
    public String getText() {
        return text;
    }

    @CheckForNull
    public abstract PUnOp newUnOp();

    public boolean isNullable() {
        return false;
    }

    public boolean isList() {
        return false;
    }

    private static class UnaryOperatorVisitor extends AnalysisAdapter {

        private UnaryOperator operator = null;

        @Override
        public void defaultCase(Node node) {
            throw new UnsupportedOperationException("Unexpected node " + node.getClass().getSimpleName());
        }

        @Override
        public void caseAStarUnOp(AStarUnOp node) {
            operator = UnaryOperator.STAR;
        }

        @Override
        public void caseAPlusUnOp(APlusUnOp node) {
            operator = UnaryOperator.PLUS;
        }

        @Override
        public void caseAQuestionUnOp(AQuestionUnOp node) {
            operator = UnaryOperator.QUESTION;
        }
    }

    @Nonnull
    public static UnaryOperator toUnaryOperator(@CheckForNull PUnOp node) {
        if (node == null)
            return NONE;
        UnaryOperatorVisitor visitor = new UnaryOperatorVisitor();
        node.apply(visitor);
        return Preconditions.checkNotNull(visitor.operator, "Got no UnaryOperator from " + node.getClass());
    }

    @CheckForNull
    @Deprecated // Not used.
    public static UnaryOperator merge(@Nonnull UnaryOperator a, @Nonnull UnaryOperator b) {
        // List changes are not nullable.
        if (a.isList() != b.isList())
            return null;
        // Return the nullable one, if present.
        if (a.isNullable())
            return a;
        // Either b is nullable, or neither is.
        return b;
    }
}
