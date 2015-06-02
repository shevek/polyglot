/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.anarres.cpp.LexerException;
import org.anarres.cpp.StringLexerSource;
import org.anarres.cpp.Token;
import org.anarres.polyglot.node.ACharChar;
import org.anarres.polyglot.node.ADecChar;
import org.anarres.polyglot.node.AHexChar;
import org.anarres.polyglot.node.TString;

/**
 *
 * @author shevek
 */
public class MatcherParserVisitor extends DepthFirstAdapter {

    private static final boolean LEX = false;

    @Nonnull
    private <T> T parse(Class<T> type, @Nonnull String text) {
        try {
            StringLexerSource source = new StringLexerSource(text);
            Token token = source.token();
            Object value = token.getValue();
            if (!type.isInstance(value))
                throw new IllegalArgumentException("Not a " + type + ": " + value);
            return type.cast(value);
        } catch (IOException | LexerException e) {
            throw new IllegalStateException("Failed to lex " + text, e);
        }
    }

    @Nonnull
    public static String parse(@Nonnull TString in) {
        String text = in.getText();
        text = text.substring(1, text.length() - 1);
        return text;
    }

    @Override
    public void outACharChar(ACharChar node) {
        if (LEX) {
            String value = parse(String.class, node.getToken().getText());
            setOut(node, value);
        } else {
            setOut(node, Character.valueOf(node.getToken().getText().charAt(1)));
        }
    }

    @Override
    public void outADecChar(ADecChar node) {
        setOut(node, Character.valueOf((char) Integer.parseInt(node.getToken().getText())));
    }

    @Override
    public void outAHexChar(AHexChar node) {
        setOut(node, Character.valueOf((char) Integer.parseInt(node.getToken().getText().substring(2), 16)));
    }

}
