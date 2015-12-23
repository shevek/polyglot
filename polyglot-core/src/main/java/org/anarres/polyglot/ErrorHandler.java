/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.io.CharSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.Token;

/**
 * The error handler.
 *
 * Consider particularly {@link #toString(com.google.common.io.CharSource)} for verbose error messages.
 *
 * @author shevek
 */
public class ErrorHandler {

    public static class Error {

        private static final int CONTEXT_LENGTH = 20;

        private final Token location;
        private final String message;

        public Error(@CheckForNull Token location, @Nonnull String message) {
            this.location = location;
            this.message = message;
        }

        @VisibleForTesting
        @Nonnull
        /* pp */ String toDescription(@Nonnull Token location, @Nonnull String source) {
            if (source == null)
                return "<no-text>";
            if (location == null)
                return "<no-location>";
            int offset = location.getOffset();
            if (offset < 0 || offset > source.length())
                return "<invalid-offset>";

            String prefix = CharMatcher.WHITESPACE.collapseFrom(source.substring(0, offset), ' ');
            int start = Math.max(0, prefix.length() - CONTEXT_LENGTH);
            prefix = prefix.substring(start);

            String suffix = CharMatcher.WHITESPACE.collapseFrom(source.substring(offset), ' ');
            int end = Math.min(suffix.length(), CONTEXT_LENGTH);
            suffix = suffix.substring(0, end);

            String text = prefix + "<HERE>" + suffix;
            return text;
        }

        @Nonnull
        public void toStringBuilder(@Nonnull StringBuilder buf, @CheckForNull String source) {
            if (location == null) {
                buf.append(message);
            } else {
                buf.append(location.getLine()).append(':').append(location.getColumn()).append(": ").append(message);
                if (source != null)
                    buf.append('\n').append(toDescription(location, source));
            }
        }

        @Override
        public String toString() {
            if (location == null)
                return message;
            StringBuilder buf = new StringBuilder();
            toStringBuilder(buf, null);
            return buf.toString();
        }
    }

    private final List<Error> errors = new ArrayList<>();

    @Nonnull
    public static String toLocationString(@CheckForNull Token location) {
        if (location == null)
            return "unknown location";
        return location.getLine() + ":" + location.getColumn();
    }

    @Nonnull
    public List<? extends Error> getErrors() {
        return errors;
    }

    public void addError(@CheckForNull Token location, @Nonnull String message) {
        errors.add(new Error(location, message));
    }

    public boolean isFatal() {
        return !errors.isEmpty();
    }

    @Nonnull
    public String toString(@CheckForNull String source) {
        if (errors.isEmpty())
            return "No errors.";
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            Error error = errors.get(i);
            if (i > 100) {
                buf.append(errors.size() - i).append(" more...");
                break;
            }
            error.toStringBuilder(buf, source);
            buf.append('\n');
        }
        buf.append(errors.size()).append(" errors total.");
        return buf.toString();
    }

    @Nonnull
    public String toString(@Nonnull CharSource source) throws IOException {
        return toString(source.read());
    }

    @Override
    public String toString() {
        return toString((String) null);
    }
}
