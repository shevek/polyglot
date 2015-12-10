/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.PolyglotExecutor;
import org.apache.velocity.VelocityContext;

/**
 *
 * @author shevek
 */
public class HtmlOutputWriter extends AbstractOutputWriter {

    private final HtmlHelper helper;

    public HtmlOutputWriter(File destinationDir, Set<? extends Option> options, Map<? extends String, ? extends File> templates, OutputData data) {
        super(OutputLanguage.html, destinationDir, options, templates, data);
        this.helper = new HtmlHelper();
    }

    @Override
    protected void initContext(@Nonnull VelocityContext context) {
        super.initContext(context);
        context.put("helper", helper);
    }

    @Override
    public void run(PolyglotExecutor executor) throws InterruptedException, ExecutionException, IOException {
        process(executor, "grammar.vm", "grammar.html", ImmutableMap.<String, Object>of());
        processTemplates(executor);
    }

}
