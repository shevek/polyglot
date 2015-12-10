package org.anarres.polyglot.analysis;

import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstElementModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstTransformExpressionModel;
import org.anarres.polyglot.model.CstTransformPrototypeModel;
import org.anarres.polyglot.model.ExternalModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.HelperModel;
import org.anarres.polyglot.model.PackageModel;
import org.anarres.polyglot.model.Specifier;
import org.anarres.polyglot.model.StateModel;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.model.UnaryOperator;
import org.anarres.polyglot.node.AAstAlternative;
import org.anarres.polyglot.node.AAstProduction;
import org.anarres.polyglot.node.ACstAlternative;
import org.anarres.polyglot.node.ACstProduction;
import org.anarres.polyglot.node.AElement;
import org.anarres.polyglot.node.AExternal;
import org.anarres.polyglot.node.AGrammar;
import org.anarres.polyglot.node.AHelper;
import org.anarres.polyglot.node.AIgnoredTokensSection;
import org.anarres.polyglot.node.AListExpression;
import org.anarres.polyglot.node.ANewExpression;
import org.anarres.polyglot.node.ANullExpression;
import org.anarres.polyglot.node.APackage;
import org.anarres.polyglot.node.AReferenceExpression;
import org.anarres.polyglot.node.AStatesSection;
import org.anarres.polyglot.node.AToken;
import org.anarres.polyglot.node.ATokenState;
import org.anarres.polyglot.node.EOF;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.PAstAlternative;
import org.anarres.polyglot.node.PCstAlternative;
import org.anarres.polyglot.node.PElement;
import org.anarres.polyglot.node.PExpression;
import org.anarres.polyglot.node.PTokenState;
import org.anarres.polyglot.node.TIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class ModelBuilderVisitor extends DepthFirstAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ModelBuilderVisitor.class);

    private static class AstSynthesizerAdapter extends AnalysisAdapter {
    }
    private final ErrorHandler errors;
    private final GrammarModel grammar;
    private TokenModel token;
    private CstProductionModel cstProduction;
    private CstAlternativeModel cstAlternative;
    private AstProductionModel astProduction;
    private AstAlternativeModel astAlternative;
    // public final Map<String, AlternativeModel> productionAlternatives = new HashMap<>();
    /** This is either a CST alternative directly, or a CstTransformExpressionModel.{List,New}. */
    private final Deque<CstTransformExpressionModel.Container> cstTransformContainer = new ArrayDeque<>();

    public ModelBuilderVisitor(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    @Override
    public void defaultCase(Node node) {
        throw new UnsupportedOperationException("Unexpected node " + node.getClass().getSimpleName());
    }

    @Override
    public void caseEOF(EOF node) {
    }

    // /** The name of a given node, if it has one. */
    // public final Map<Node, String> names = new HashMap<Node, String>();
    @Override
    public void caseAPackage(APackage node) {
        grammar.setPackage(PackageModel.forNode(node));
    }

    @Override
    public void caseAExternal(AExternal node) {
        ExternalModel external = ExternalModel.forNode(node);
        if (!grammar.addExternal(external))
            errors.addError(node.getName(), "Duplicate external name '" + external.getName() + "'.");
    }

    @Override
    public void caseAHelper(AHelper node) {
        HelperModel helper = HelperModel.forNode(node);
        if (!grammar.addHelper(helper))
            errors.addError(node.getName(), "Duplicate helper name '" + helper.getName() + "'.");
    }

    /*
     // Always with a helper or a token.
     @Override
     public void caseAHelperMatcher(AHelperMatcher node) {
     String name = node.getHelperName().getText();
     Object helper = grammar.getHelper(name);
     // Whether we are in a helper or a token, these must be forward-declared.
     }
     */
    @Override
    public void caseAStatesSection(AStatesSection node) {
        for (TIdentifier identifier : node.getNames()) {
            String name = StateModel.name(identifier);
            StateModel model = new StateModel(grammar.stateIndex++, identifier);
            Object prev = grammar.states.put(name, model);
            if (prev != null)
                errors.addError(identifier, "Duplicate state name '" + token.getName() + "'.");
        }
    }

    @Override
    public void outAGrammar(AGrammar node) {
        // We have to do this here, as we might not even have a states section.
        if (grammar.states.isEmpty()) {
            TIdentifier identifier = new TIdentifier("DEFAULT");
            String name = StateModel.name(identifier);
            StateModel model = new StateModel(grammar.stateIndex++, identifier);
            grammar.states.put(name, model);
        }
    }

    @Override
    public void caseAToken(AToken node) {
        token = new TokenModel(grammar.tokenIndex++, node.getName(), node.getMatcher(), node.getAnnotations());
        token.setJavadocComment(node.getJavadocComment());
        Object prev = grammar.tokens.put(token.getName(), token);
        if (prev != null)
            errors.addError(node.getName(), "Duplicate token name '" + token.getName() + "'.");

        for (PTokenState tokenState : node.getTokenStates())
            tokenState.apply(this);

        setOut(node, token);
        token = null;
    }

    @Override
    public void caseATokenState(ATokenState node) {
        String matchName = StateModel.name(node.getState());
        StateModel matchState = grammar.states.get(matchName);
        if (matchState == null) {
            errors.addError(node.getState(), "No such state " + node.getState().getText());
            return;
        }
        StateModel transitionState;
        // CheckForNull
        TIdentifier transitionToken = node.getTransition();
        if (transitionToken != null) {
            String transitionName = StateModel.name(transitionToken);
            transitionState = grammar.states.get(transitionName);
            if (transitionState == null) {
                errors.addError(node.getTransition(), "No such state " + node.getState().getText());
                return;
            }
        } else {
            transitionState = matchState;
        }
        Object prev = token.transitions.put(matchState, transitionState);
        if (prev != null)
            errors.addError(node.getState(), "Duplicate transition " + matchName + " on token " + token.getName());
    }

    @Override
    public void caseAIgnoredTokensSection(AIgnoredTokensSection node) {
        for (TIdentifier identifier : node.getNames()) {
            String name = identifier.getText();
            TokenModel token = grammar.getToken(name);
            if (token == null)
                errors.addError(identifier, "Cannot ignore nonexistent token '" + name + "'.");
            else
                token.ignored = true;
        }
    }

    @Override
    public void caseACstProduction(ACstProduction node) {
        cstProduction = CstProductionModel.forNode(grammar.cstProductionIndex++, node);
        if (grammar.cstProductionRoot == null)
            grammar.cstProductionRoot = cstProduction;
        Object prev = grammar.cstProductions.put(cstProduction.getName(), cstProduction);
        if (prev != null)
            errors.addError(cstProduction.getLocation(), "Duplicate name in CST production '" + cstProduction.getName() + "'.");

        // If a node is untransformed, attach a simple default transform.
        if (node.getTransformSentinel() == null) {
            // node.setTransformSentinel(new TTokArrow(node.getName()));
            // AAstElement element = new AAstElement(null, new AProductionSpecifier(), node.getName().clone(), null);
            // node.setTransform(Arrays.asList(element));
            cstProduction.transformPrototypes.add(new CstTransformPrototypeModel(node.getName(), Specifier.PRODUCTION, node.getName(), UnaryOperator.NONE));
        } else {
            for (PElement transform : node.getTransform()) {
                transform.apply(this);
            }
        }

        for (PCstAlternative alternative : node.getAlternatives()) {
            alternative.apply(this);
        }

        setOut(node, cstProduction);
        cstProduction = null;
    }

    @Override
    public void caseACstAlternative(ACstAlternative node) {
        cstAlternative = CstAlternativeModel.forNode(grammar.cstAlternativeIndex++, cstProduction, node);
        Object prev = cstProduction.alternatives.put(cstAlternative.getName(), cstAlternative);
        if (prev != null)
            errors.addError(cstAlternative.getLocation(), "Duplicate name in CST alternative '" + cstAlternative.getName() + "'.");

        for (PElement element : node.getElements()) {
            element.apply(this);
        }

        if (node.getTransformSentinel() == null) {
            switch (cstProduction.transformPrototypes.size()) {
                case 0:
                    // No transform.
                    break;
                case 1:
                    CstTransformExpressionModel.New expression = new CstTransformExpressionModel.New(cstProduction.toNameToken(), node.getName());
                    for (CstElementModel element : cstAlternative.getElements()) {
                        CstTransformExpressionModel.Reference reference = new CstTransformExpressionModel.Reference(element.toNameToken(), null);
                        reference.element = element;    // Should be overwritten by ReferenceLinker.
                        CstTransformExpressionModel argument;
                        if (element.isList())
                            argument = GrammarNormalizer.newListExpression(reference);
                        else
                            argument = reference;
                        expression.addTransformExpression(argument);
                    }
                    cstAlternative.addTransformExpression(expression);
                    break;
                default:
                    errors.addError(cstAlternative.getLocation(), "Cannot synthesize multiple default transform expressions for alternative '" + cstAlternative.getName() + "'.");
                    break;
            }
        } else {
            cstTransformContainer.push(cstAlternative);
            for (PExpression transform : node.getTransform()) {
                transform.apply(this);
            }
            Preconditions.checkState(cstTransformContainer.pop() == cstAlternative);
        }

        setOut(node, cstAlternative);
        cstAlternative = null;
    }

    @Override
    public void caseANullExpression(ANullExpression node) {
        CstTransformExpressionModel.Null e = new CstTransformExpressionModel.Null(node.getLocation());
        setOut(node, e);
        cstTransformContainer.peek().addTransformExpression(e);
    }

    @Override
    public void caseAReferenceExpression(AReferenceExpression node) {
        CstTransformExpressionModel.Reference e = new CstTransformExpressionModel.Reference(node.getElementName(), node.getTransformName());
        setOut(node, e);
        cstTransformContainer.peek().addTransformExpression(e);
    }

    @Override
    public void caseAListExpression(AListExpression node) {
        CstTransformExpressionModel.List e = new CstTransformExpressionModel.List(node.getLocation());
        setOut(node, e);
        cstTransformContainer.peek().addTransformExpression(e);
        cstTransformContainer.push(e);
        for (PExpression n : node.getItems()) {
            n.apply(this);
        }
        cstTransformContainer.pop();
    }

    @Override
    public void caseANewExpression(ANewExpression node) {
        CstTransformExpressionModel.New e = new CstTransformExpressionModel.New(node.getProductionName(), node.getAlternativeName());
        setOut(node, e);
        cstTransformContainer.peek().addTransformExpression(e);
        cstTransformContainer.push(e);
        for (PExpression n : node.getArguments()) {
            n.apply(this);
        }
        cstTransformContainer.pop();
    }

    @Override
    public void caseAAstProduction(AAstProduction node) {
        astProduction = AstProductionModel.forNode(node);
        if (grammar.astProductionRoot == null)
            grammar.astProductionRoot = astProduction;
        // LOG.info("AstProductionModel " + astProduction);
        Object prev = grammar.astProductions.put(astProduction.getName(), astProduction);
        if (prev != null)
            errors.addError(astProduction.getLocation(), "Duplicate name in AST production '" + astProduction.getName() + "'");

        for (PAstAlternative alternative : node.getAlternatives()) {
            alternative.apply(this);
        }
        setOut(node, astProduction);
        astProduction = null;
    }

    @Override
    public void caseAAstAlternative(AAstAlternative node) {
        astAlternative = AstAlternativeModel.forNode(astProduction, node);
        Object prev = astProduction.alternatives.put(astAlternative.getName(), astAlternative);
        if (prev != null)
            errors.addError(astAlternative.getLocation(), "Duplicate name in AST alternative '" + astAlternative.getName() + "'.");

        for (PElement element : node.getElements()) {
            element.apply(this);
        }
        setOut(node, astAlternative);
        astAlternative = null;
    }

    @Override
    public void caseAElement(AElement node) {
        String name = AstElementModel.name(node).getText();
        // TODO: Check name is not a Java reserved word.
        // TODO: Add this to the generic alternative, not just the CST one.
        // We might be a CstProductionTransform. :-( We should probably do separation by type in the grammar.
        if (astAlternative != null) {
            AstElementModel element = AstElementModel.forNode(node);
            for (AstElementModel e : astAlternative.elements)
                if (name.equals(e.getName()))
                    errors.addError(node.getSymbolName(), "Duplicate element name '" + name + "' in AST alternative '" + astAlternative.getName() + "'.");
            astAlternative.elements.add(element);
            setOut(node, element);
        } else if (cstAlternative != null) {
            CstElementModel element = CstElementModel.forNode(node);
            for (CstElementModel e : cstAlternative.elements)
                if (name.equals(e.getName()))
                    errors.addError(node.getSymbolName(), "Duplicate element name '" + name + "' in CST alternative '" + cstAlternative.getName() + "'.");
            cstAlternative.elements.add(element);
            setOut(node, element);
        } else if (cstProduction != null) {
            CstTransformPrototypeModel transform = CstTransformPrototypeModel.forNode(node);
            for (CstTransformPrototypeModel e : cstProduction.transformPrototypes)
                if (name.equals(e.getName()))
                    errors.addError(node.getSymbolName(), "Duplicate element name '" + name + "' in CST to AST transform of '" + cstProduction.getName() + "'.");
            cstProduction.transformPrototypes.add(transform);
            setOut(node, transform);
        }
    }
}
