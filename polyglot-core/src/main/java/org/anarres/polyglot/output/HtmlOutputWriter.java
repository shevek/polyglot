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
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.PolyglotExecutor;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.HelperModel;
import org.anarres.polyglot.model.TokenModel;
import org.apache.velocity.VelocityContext;

/**
 *
 * @author shevek
 */
public class HtmlOutputWriter extends AbstractOutputWriter {

    private final HtmlHelper helper;

    public HtmlOutputWriter(File destinationDir, Set<? extends Option> options, Map<? extends String, ? extends File> templates, OutputData data) {
        super(OutputLanguage.html, destinationDir, options, templates, data);
        this.helper = new HtmlHelper(data);
    }

    @Override
    protected void initContext(@Nonnull VelocityContext context) {
        super.initContext(context);
        context.put("helper", helper);
    }

    @Override
    public void run(PolyglotExecutor executor) throws InterruptedException, ExecutionException, IOException {
        process(executor, "grammar.vm", "grammar.html", ImmutableMap.<String, Object>of());

        process(executor, "stylesheet.vm", "stylesheet.css", ImmutableMap.<String, Object>of());

        process(executor, "index.vm", "index.html", ImmutableMap.<String, Object>of());
        process(executor, "frame-groups.vm", "frame-groups.html", ImmutableMap.<String, Object>of());

        process(executor, "list.vm", "list-all.html", ImmutableMap.<String, Object>of("listTitle", "All Objects", "listGroups", EnumSet.allOf(HtmlHelper.ListGroup.class)));
        process(executor, "list.vm", "list-lexer.html", ImmutableMap.<String, Object>of("listTitle", "Lexer Objects", "listGroups", EnumSet.of(HtmlHelper.ListGroup.Helpers, HtmlHelper.ListGroup.Tokens)));
        process(executor, "list.vm", "list-parser.html", ImmutableMap.<String, Object>of("listTitle", "Parser Objects", "listGroups", EnumSet.of(HtmlHelper.ListGroup.Tokens, HtmlHelper.ListGroup.CstProductions)));
        process(executor, "list.vm", "list-model.html", ImmutableMap.<String, Object>of("listTitle", "Model Objects", "listGroups", EnumSet.of(HtmlHelper.ListGroup.Tokens, HtmlHelper.ListGroup.Externals, HtmlHelper.ListGroup.AstProductions)));

        for (HelperModel model : getGrammar().getHelpers()) {
            process(executor, "helper.vm", helper.a(model) + ".html", ImmutableMap.<String, Object>of("model", model));
        }

        for (TokenModel model : getGrammar().getTokens()) {
            process(executor, "token.vm", helper.a(model) + ".html", ImmutableMap.<String, Object>of("model", model));
        }

        for (CstProductionModel production : getGrammar().getCstProductions()) {
            process(executor, "cst-production.vm", helper.a(production) + ".html", ImmutableMap.<String, Object>of("model", production));
            for (CstAlternativeModel alternative : production.getAlternatives().values()) {
                process(executor, "cst-alternative.vm", helper.a(alternative) + ".html", ImmutableMap.<String, Object>of("model", alternative));
            }
        }

        for (AstProductionModel production : getGrammar().getAstProductions()) {
            process(executor, "ast-production.vm", helper.a(production) + ".html", ImmutableMap.<String, Object>of("model", production));
            for (AstAlternativeModel alternative : production.getAlternatives()) {
                process(executor, "ast-alternative.vm", helper.a(alternative) + ".html", ImmutableMap.<String, Object>of("model", alternative));
            }
        }

        processTemplates(executor);
    }

}
