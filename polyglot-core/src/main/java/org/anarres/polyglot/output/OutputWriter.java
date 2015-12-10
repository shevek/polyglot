/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.anarres.polyglot.PolyglotExecutor;

/**
 *
 * @author shevek
 */
public interface OutputWriter {

    public void run(@Nonnull PolyglotExecutor executor) throws InterruptedException, ExecutionException, IOException;
}
