/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.Token;

/**
 *
 * @author shevek
 */
public class ErrorHandler {

    public static class Error {

        private final Token location;
        private final String message;

        public Error(@CheckForNull Token location, @Nonnull String message) {
            this.location = location;
            this.message = message;
        }

        @Override
        public String toString() {
            if (location == null)
                return message;
            return toLocationString(location) + ": " + message;
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

    @Override
    public String toString() {
        if (errors.isEmpty())
            return "No errors.";
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            Error error = errors.get(i);
            if (i > 100) {
                buf.append(errors.size() - i).append(" more...");
                break;
            }
            buf.append(error).append('\n');
        }
        buf.append(errors.size()).append(" errors total.");
        return buf.toString();
    }

}
