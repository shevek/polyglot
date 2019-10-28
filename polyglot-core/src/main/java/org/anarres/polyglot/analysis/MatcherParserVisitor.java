/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.anarres.cpp.LexerException;
import org.anarres.cpp.StringLexerSource;
import org.anarres.cpp.Token;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.node.AStringLiteral;
import org.anarres.polyglot.node.ADecCharLiteral;
import org.anarres.polyglot.node.AHexCharLiteral;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.TString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class MatcherParserVisitor extends DepthFirstAdapter {

    @SuppressWarnings("UnusedVariable")
    private static final Logger LOG = LoggerFactory.getLogger(MatcherParserVisitor.class);

    @Nonnull
    public static String parse(@Nonnull ErrorHandler errors, @Nonnull TString in) {
        try {
            StringLexerSource source = new StringLexerSource(in.getText());
            Token token = source.token();
            // LOG.info(in.getText() + " -> " + token);
            return (String) token.getValue();
        } catch (IOException | LexerException e) {
            errors.addError(in, "Invalid string literal: '" + in.getText() + "': " + e.getMessage());
            return "<error>";
        }
    }

    private static final Escaper ESCAPER = Escapers.builder()
            .addEscape('\t', "\\t")
            .addEscape('\r', "\\r")
            .addEscape('\n', "\\n")
            .addEscape('\'', "\\'")
            .addEscape('\\', "\\\\")
            .build();

    @Nonnull
    public static TString escape(@Nonnull String in) {
        return new TString(ESCAPER.escape(in));
    }

    private final ErrorHandler errors;

    public MatcherParserVisitor(ErrorHandler errors) {
        this.errors = errors;
    }

    @Nonnull
    public String getString(@Nonnull Node node) {
        Object o = getOut(node);
        if (o instanceof Character)
            return Character.toString((Character) o);
        if (o instanceof String)
            return (String) o;
        throw new IllegalStateException("What is " + o.getClass() + " for " + node.getClass());
    }

    @Override
    public void outAStringLiteral(AStringLiteral node) {
        String value = parse(errors, node.getToken());
        setOut(node, value);
    }

    @Override
    public void outADecCharLiteral(ADecCharLiteral node) {
        setOut(node, Character.valueOf((char) Integer.parseInt(node.getToken().getText())));
    }

    @Override
    public void outAHexCharLiteral(AHexCharLiteral node) {
        setOut(node, Character.valueOf((char) Integer.parseInt(node.getToken().getText().substring(2), 16)));
    }
}
