/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

/**
 *
 * @author shevek
 */
public enum Option {

    /** Enables SLR parser generation. *//** Enables SLR parser generation. */
    SLR,
    /** Enables LR(1) parser generation. */
    LR1,
    /** Enables inline table (base64/gzip) table generation. */
    INLINE_TABLES,
    /** Enables API documentation generation. */
    CG_APIDOC,
    /** Enables additional comment generation. */
    CG_COMMENT,
    /** Enables findbugs annotation generation. */
    CG_FINDBUGS,
    /** Enables usage of common runtime interfaces. */
    CG_RUNTIME,
    /** Enables parent pointers in the generated code. */
    CG_PARENT,
    /** Enables threaded execution, which can generate simpler error messages. */
    PARALLEL,
    /** Log additional debugging information. */
    VERBOSE,
    /** Allow token masking, that is, tokens which can never match. */
    ALLOWMASKEDTOKENS,
    /** Perform advanced diagnosis (not yet reliable). */
    DIAGNOSIS,
}
