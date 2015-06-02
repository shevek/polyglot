/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.gradle;

import java.io.PushbackReader;
import java.io.StringReader;
import org.anarres.polyglot.lexer.Lexer;
import org.anarres.polyglot.parser.Parser;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class ParserJarTest {

    private static final Logger LOG = LoggerFactory.getLogger(ParserJarTest.class);

    @Test
    public void testParserInstantiation() {
        PushbackReader reader = new PushbackReader(new StringReader("foo bar"));
        Lexer lexer = new Lexer(reader);
        Parser parser = new Parser(lexer);
        LOG.info("Parser is " + parser);
    }
}
