/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.AAlternateMatcher;
import org.anarres.polyglot.node.ACstAlternative;
import org.anarres.polyglot.node.AAstSection;
import org.anarres.polyglot.node.AAstAlternative;
import org.anarres.polyglot.node.AAstProduction;
import org.anarres.polyglot.node.ACharChar;
import org.anarres.polyglot.node.AConcatMatcher;
import org.anarres.polyglot.node.ADecChar;
import org.anarres.polyglot.node.ADifferenceMatcher;
import org.anarres.polyglot.node.AHelper;
import org.anarres.polyglot.node.AHelperMatcher;
import org.anarres.polyglot.node.AHelpersSection;
import org.anarres.polyglot.node.AHexChar;
import org.anarres.polyglot.node.AIgnoredTokensSection;
import org.anarres.polyglot.node.AIntervalMatcher;
import org.anarres.polyglot.node.AListExpression;
import org.anarres.polyglot.node.ANewExpression;
import org.anarres.polyglot.node.ANullExpression;
import org.anarres.polyglot.node.APackage;
import org.anarres.polyglot.node.APlusMatcher;
import org.anarres.polyglot.node.APlusUnOp;
import org.anarres.polyglot.node.ACstProduction;
import org.anarres.polyglot.node.AElement;
import org.anarres.polyglot.node.AProductionSpecifier;
import org.anarres.polyglot.node.AProductionsSection;
import org.anarres.polyglot.node.AQuestionMatcher;
import org.anarres.polyglot.node.AQuestionUnOp;
import org.anarres.polyglot.node.AReferenceExpression;
import org.anarres.polyglot.node.AStarMatcher;
import org.anarres.polyglot.node.AStarUnOp;
import org.anarres.polyglot.node.AStatesSection;
import org.anarres.polyglot.node.AStringMatcher;
import org.anarres.polyglot.node.AToken;
import org.anarres.polyglot.node.ATokenSpecifier;
import org.anarres.polyglot.node.ATokenState;
import org.anarres.polyglot.node.ATokensSection;
import org.anarres.polyglot.node.AUnionMatcher;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.PMatcher;
import org.anarres.polyglot.node.PSpecifier;
import org.anarres.polyglot.node.PUnOp;
import org.anarres.polyglot.node.TIdentifier;

/**
 *
 * @author shevek
 */
public class GrammarWriterVisitor extends DepthFirstAdapter {

    private final StringWriter out = new StringWriter();

    private static boolean isEmpty(@CheckForNull Collection<?> c) {
        return c == null || c.isEmpty();
    }

    @Override
    public void caseTIdentifier(TIdentifier node) {
        out.append(node.getText());
    }

    private void caseAList(List<? extends Node> nodes, String sep) {
        boolean b = false;
        for (Node node : nodes) {
            if (node == null)
                continue;
            if (b)
                out.append(sep);
            else
                b = true;
            node.apply(this);
        }
    }

    private void caseAMatcherList(List<? extends PMatcher> matchers, String sep) {
        out.append("(");
        caseAList(matchers, sep);
        out.append(")");
    }

    @Override
    public void caseAAlternateMatcher(AAlternateMatcher node) {
        caseAMatcherList(node.getMatchers(), " | ");
    }

    @Override
    public void caseAConcatMatcher(AConcatMatcher node) {
        caseAMatcherList(node.getMatchers(), " ");
    }

    private void caseAMatcherSuffix(@Nonnull PMatcher matcher, @Nonnull String suffix) {
        matcher.apply(this);
        out.append(suffix);
    }

    @Override
    public void caseAStarMatcher(AStarMatcher node) {
        caseAMatcherSuffix(node.getMatcher(), "*");
    }

    @Override
    public void caseAQuestionMatcher(AQuestionMatcher node) {
        caseAMatcherSuffix(node.getMatcher(), "*");
    }

    @Override
    public void caseAPlusMatcher(APlusMatcher node) {
        caseAMatcherSuffix(node.getMatcher(), "+");
    }

    @Override
    public void caseAStringMatcher(AStringMatcher node) {
        out.append(node.getString().getText());
    }

    @Override
    public void caseAHelperMatcher(AHelperMatcher node) {
        out.append(node.getHelperName().getText());
    }

    private void caseAMatcherCharset(Node left, Node right, String op) {
        out.append("[");
        left.apply(this);
        out.append(op);
        right.apply(this);
        out.append("]");

    }

    @Override
    public void caseAUnionMatcher(AUnionMatcher node) {
        caseAMatcherCharset(node.getLeft(), node.getRight(), " + ");
    }

    @Override
    public void caseADifferenceMatcher(ADifferenceMatcher node) {
        caseAMatcherCharset(node.getLeft(), node.getRight(), " - ");
    }

    @Override
    public void caseAIntervalMatcher(AIntervalMatcher node) {
        caseAMatcherCharset(node.getLeft(), node.getRight(), " .. ");
    }

    @Override
    public void caseACharChar(ACharChar node) {
        out.append(node.getToken().getText());
    }

    @Override
    public void caseADecChar(ADecChar node) {
        out.append(node.getToken().getText());
    }

    @Override
    public void caseAHexChar(AHexChar node) {
        out.append(node.getToken().getText());
    }

    @Override
    public void caseAStarUnOp(AStarUnOp node) {
        out.append("*");
    }

    @Override
    public void caseAPlusUnOp(APlusUnOp node) {
        out.append("+");
    }

    @Override
    public void caseAQuestionUnOp(AQuestionUnOp node) {
        out.append("?");
    }

    @Override
    public void caseAPackage(APackage node) {
        out.write("Package ");
        caseAList(node.getName(), ".");
        out.write(";\n");
    }

    @Override
    public void inAHelpersSection(AHelpersSection node) {
        out.write("\nHelpers\n");
    }

    @Override
    public void caseAHelper(AHelper node) {
        out.append("\t").append(node.getName().getText()).append(" = ");
        node.getMatcher().apply(this);
        out.append(";\n");
    }

    @Override
    public void caseAStatesSection(AStatesSection node) {
        out.write("\nStates\n\t");
        caseAList(node.getNames(), ", ");
        out.write(";");
    }

    @Override
    public void inATokensSection(ATokensSection node) {
        out.append("\nTokens\n");
    }

    @Override
    public void caseATokenState(ATokenState node) {
        out.append(node.getState().getText());
        TIdentifier transition = node.getTransition();
        if (transition != null)
            out.append("->").append(transition.getText());
    }

    @Override
    public void caseAToken(AToken node) {
        out.append("\t");
        if (!isEmpty(node.getTokenStates())) {
            out.append("{");
            caseAList(node.getTokenStates(), ",");
            out.append("} ");
        }
        out.append(node.getName().getText()).append(" = ");
        node.getMatcher().apply(this);
        out.append(";\n");
    }

    @Override
    public void caseAIgnoredTokensSection(AIgnoredTokensSection node) {
        out.append("Ignored Tokens\n\t");
        caseAList(node.getNames(), ", ");
        out.append(";");
    }

    @Override
    public void inAProductionsSection(AProductionsSection node) {
        out.append("\nProductions\n");
    }

    @Override
    public void caseACstProduction(ACstProduction node) {
        out.append("\t").append(node.getName().getText());
        if (node.getTransformSentinel() != null) {
            out.append(" { -> ");
            caseAList(node.getTransform(), " ");
            out.append(" }");
        }
        out.append(" =\n");
        caseAList(node.getAlternatives(), " |\n");
        out.append(" ;\n");
    }

    @Override
    public void caseACstAlternative(ACstAlternative node) {
        out.append("\t\t");
        TIdentifier name = node.getName();
        if (name != null) {
            out.append("{").append(name.getText()).append("} ");
        }
        caseAList(node.getElements(), " ");
        if (node.getTransformSentinel() != null) {
            out.append("\n\t\t\t{ -> ");
            caseAList(node.getTransform(), " ");
            out.append(" }");
        }
    }

    @Override
    public void caseANewExpression(ANewExpression node) {
        out.append("New ");
        out.append(node.getProductionName().getText());
        TIdentifier alternativeName = node.getAlternativeName();
        if (alternativeName != null)
            out.append(".").append(alternativeName.getText());
        out.append("(");
        caseAList(node.getArguments(), ", ");
        out.append(")");
    }

    @Override
    public void caseAListExpression(AListExpression node) {
        out.append("[");
        caseAList(node.getItems(), ", ");
        out.append("]");
    }

    @Override
    public void caseAReferenceExpression(AReferenceExpression node) {
        out.append(node.getElementName().getText());
        TIdentifier transformName = node.getTransformName();
        if (transformName != null)
            out.append(".").append(transformName.getText());
    }

    @Override
    public void caseATokenSpecifier(ATokenSpecifier node) {
        out.append("T.");
    }

    @Override
    public void caseAProductionSpecifier(AProductionSpecifier node) {
        out.append("P.");
    }

    @Override
    public void caseANullExpression(ANullExpression node) {
        out.append("Null");
    }

    @Override
    public void inAAstSection(AAstSection node) {
        out.write("\nAbstract Syntax Tree\n");
    }

    @Override
    public void caseAAstProduction(AAstProduction node) {
        out.append("\t").append(node.getName().getText()).append(" =\n");
        caseAList(node.getAlternatives(), " |\n");
        out.append(" ;\n");
    }

    @Override
    public void caseAAstAlternative(AAstAlternative node) {
        out.append("\t\t");
        TIdentifier name = node.getName();
        if (name != null) {
            out.append("{").append(name.getText()).append("} ");
        }
        caseAList(node.getElements(), " ");
    }

    @Override
    public void caseAElement(AElement node) {
        TIdentifier name = node.getName();
        if (name != null)
            out.append("[").append(name.getText()).append("]:");
        PSpecifier specifier = node.getSpecifier();
        if (specifier != null)
            specifier.apply(this);
        out.append(node.getSymbolName().getText());
        PUnOp unOp = node.getUnOp();
        if (unOp != null)
            unOp.apply(this);
    }

    @Override
    public String toString() {
        return out.toString();
    }

}
