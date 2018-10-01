/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.PolyglotExecutor;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.HelperModel;
import org.anarres.polyglot.model.TokenModel;
import static org.anarres.polyglot.output.HtmlHelper.ListGroup.*;

/**
 *
 * @author shevek
 */
public class HtmlOutputWriter extends AbstractOutputWriter {

    public HtmlOutputWriter(ErrorHandler errors, String grammarName, File destinationDir, Set<? extends Option> options) {
        super(errors, OutputLanguage.html, grammarName, destinationDir, options);
    }

    @Override
    public void writeModel(PolyglotExecutor executor, GrammarModel grammar, Map<? extends String, ? extends File> templates) throws ExecutionException, IOException {
        HtmlHelper helper = new HtmlHelper(grammar);
        // process(executor, "grammar.vm", "grammar.html", ImmutableMap.<String, Object>of());
        ImmutableMap<String, Object> context = ImmutableMap.<String, Object>of(
                "helper", helper,
                "grammar", grammar
        );

        processResource(executor, "stylesheet.vm", "stylesheet.css", context);

        processResource(executor, "index.vm", "index.html", context);
        processResource(executor, "menu.vm", "menu.html", context);
        processResource(executor, "overview.vm", "overview.html", context);

        processResource(executor, "list.vm", "list-all.html", context, ImmutableMap.<String, Object>of("listTitle", "All Objects", "listGroups", EnumSet.of(Helpers, Tokens, Externals, CstProductions, CstAlternatives, AstProductions, AstAlternatives)));
        processResource(executor, "list.vm", "list-lexer.html", context, ImmutableMap.<String, Object>of("listTitle", "Lexer Objects", "listGroups", EnumSet.of(Helpers, Tokens)));
        processResource(executor, "list.vm", "list-parser.html", context, ImmutableMap.<String, Object>of("listTitle", "Parser Objects", "listGroups", EnumSet.of(Tokens, CstProductions, CstAlternatives)));
        processResource(executor, "list.vm", "list-model.html", context, ImmutableMap.<String, Object>of("listTitle", "Model Objects", "listGroups", EnumSet.of(Tokens, Externals, AstProductions, AstAlternatives)));
        processResource(executor, "list.vm", "list-unused.html", context, ImmutableMap.<String, Object>of("listTitle", "Unused Objects", "listGroups", EnumSet.of(Helpers, Tokens, CstProductions, AstProductions, AstAlternatives, Unused)));

        for (HelperModel model : grammar.getHelpers()) {
            processResource(executor, "helper.vm", helper.a(model) + ".html", context, ImmutableMap.<String, Object>of("model", model));
        }

        for (TokenModel model : grammar.getTokens()) {
            processResource(executor, "token.vm", helper.a(model) + ".html", context, ImmutableMap.<String, Object>of("model", model));
        }

        for (CstProductionModel production : grammar.getCstProductions()) {
            processResource(executor, "cst-production.vm", helper.a(production) + ".html", context, ImmutableMap.<String, Object>of("model", production));
            for (CstAlternativeModel alternative : production.getAlternatives()) {
                processResource(executor, "cst-alternative.vm", helper.a(alternative) + ".html", context, ImmutableMap.<String, Object>of("model", alternative));
            }
        }

        for (AstProductionModel production : grammar.getAstProductions()) {
            processResource(executor, "ast-production.vm", helper.a(production) + ".html", context, ImmutableMap.<String, Object>of("model", production));
            for (AstAlternativeModel alternative : production.getAlternatives()) {
                processResource(executor, "ast-alternative.vm", helper.a(alternative) + ".html", context, ImmutableMap.<String, Object>of("model", alternative));
            }
        }

        processTemplates(executor, templates, context);
    }

    @Override
    public void writeLexerMachine(PolyglotExecutor executor, GrammarModel grammar, EncodedStateMachine.Lexer lexerMachine) throws ExecutionException, IOException {
    }

    @Override
    public void writeParserMachine(PolyglotExecutor executor, GrammarModel grammar, EncodedStateMachine.Parser parserMachine) throws ExecutionException, IOException {
    }
}
