/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Preconditions;
import java.util.Comparator;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.analysis.DepthFirstAdapter;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.TJavadocComment;
import org.anarres.polyglot.node.Token;
import org.anarres.polyglot.output.TemplateProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public abstract class AbstractModel implements Model {

    @SuppressWarnings("UnusedVariable")
    private static final Logger LOG = LoggerFactory.getLogger(AbstractModel.class);

    private static class TokenFinder extends DepthFirstAdapter {

        private static class LocationComparator implements Comparator<Token> {

            private static final LocationComparator INSTANCE = new LocationComparator();

            @Override
            public int compare(Token o1, Token o2) {
                int cmp = Integer.compare(o1.getLine(), o2.getLine());
                if (cmp != 0)
                    return cmp;
                return Integer.compare(o1.getColumn(), o2.getColumn());
            }
        }

        private Token token;

        @Override
        public void defaultCase(Node node) {
            if (node instanceof Token) {
                Token t = (Token) node;
                if (this.token == null)
                    this.token = t;
                else if (LocationComparator.INSTANCE.compare(t, this.token) < 0)
                    this.token = t;
            }
        }

        @CheckForNull
        public static Token find(@CheckForNull Node node) {
            if (node instanceof Token)
                return (Token) node;
            if (node == null)
                return null;
            TokenFinder finder = new TokenFinder();
            node.apply(finder);
            return finder.token;
        }
    }

    /** Computes a location token for an alternative which might not have its own name. */
    @Nonnull
    protected static Token location(@Nonnull AbstractModel parent, @Nonnull Node... nodes) {
        for (Node node : nodes) {
            Token token = TokenFinder.find(node);
            if (token != null)
                return token;
        }
        return parent.getLocation();
    }

    private final Token location;
    private TJavadocComment javadocComment;

    public AbstractModel(@Nonnull Token location) {
        this.location = Preconditions.checkNotNull(location, "Location was null.");
    }

    @Override
    public Token getLocation() {
        return location;
    }

    @CheckForNull
    public TJavadocComment newJavadocCommentToken() {
        if (javadocComment == null)
            return null;
        return javadocComment.clone();
    }

    @CheckForNull
    @TemplateProperty
    public String getJavadocComment() {
        if (javadocComment == null)
            return null;
        return javadocComment.getText();
    }

    public void setJavadocComment(@CheckForNull TJavadocComment javadocComment) {
        this.javadocComment = javadocComment;
    }
}
