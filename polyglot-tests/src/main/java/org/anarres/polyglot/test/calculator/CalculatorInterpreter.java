/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.test.calculator;

import java.io.PushbackReader;
import java.io.StringReader;
import javax.annotation.Nonnull;
import org.anarres.polyglot.test.calculator.analysis.DepthFirstAdapter;
import org.anarres.polyglot.test.calculator.lexer.Lexer;
import org.anarres.polyglot.test.calculator.node.AAddExpression;
import org.anarres.polyglot.test.calculator.node.AConstantExpression;
import org.anarres.polyglot.test.calculator.node.ADivExpression;
import org.anarres.polyglot.test.calculator.node.AMulExpression;
import org.anarres.polyglot.test.calculator.node.ARemExpression;
import org.anarres.polyglot.test.calculator.node.ASubExpression;
import org.anarres.polyglot.test.calculator.node.Node;
import org.anarres.polyglot.test.calculator.node.Start;
import org.anarres.polyglot.test.calculator.node.TIntegerConstant;
import org.anarres.polyglot.test.calculator.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class CalculatorInterpreter extends DepthFirstAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(CalculatorInterpreter.class);

    private int getInteger(@Nonnull Node node) {
        Object v = getOut(node);
        // LOG.info(node + " -> " + v);
        return (Integer) v;
    }

    @Override
    public void caseTIntegerConstant(TIntegerConstant node) {
        setOut(node, Integer.parseInt(node.getText()));
    }

    @Override
    public void outAConstantExpression(AConstantExpression node) {
        setOut(node, getInteger(node.getIntegerConstant()));
    }

    @Override
    public void outAAddExpression(AAddExpression node) {
        setOut(node, getInteger(node.getLeft()) + getInteger(node.getRight()));
    }

    @Override
    public void outASubExpression(ASubExpression node) {
        setOut(node, getInteger(node.getLeft()) - getInteger(node.getRight()));
    }

    @Override
    public void outAMulExpression(AMulExpression node) {
        setOut(node, getInteger(node.getLeft()) * getInteger(node.getRight()));
    }

    @Override
    public void outADivExpression(ADivExpression node) {
        setOut(node, getInteger(node.getLeft()) / getInteger(node.getRight()));
    }

    @Override
    public void outARemExpression(ARemExpression node) {
        setOut(node, getInteger(node.getLeft()) % getInteger(node.getRight()));
    }

    @Override
    public void outStart(Start node) {
        setOut(node, getInteger(node.getPExpression()));
    }

    public static Integer evaluate(@Nonnull String text) throws Exception {
        StringReader reader = new StringReader(text);
        PushbackReader pr = new PushbackReader(reader, 20);
        Lexer l = new Lexer(pr);
        Parser p = new Parser(l);
        Start ast = p.parse();

        CalculatorInterpreter i = new CalculatorInterpreter();
        ast.apply(i);
        return i.getInteger(ast);
    }
}
