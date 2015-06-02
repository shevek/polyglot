/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.test.calculator;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class CalculatorInterpreterTest {

    private static final Logger LOG = LoggerFactory.getLogger(CalculatorInterpreterTest.class);

    public void testCalculator(String text) throws Exception {
        LOG.info(text + " = " + CalculatorInterpreter.evaluate(text));
    }

    @Test
    public void testCalculator() throws Exception {
        testCalculator("1 + 45");
        testCalculator("1 + 2 * 3");
    }

}
