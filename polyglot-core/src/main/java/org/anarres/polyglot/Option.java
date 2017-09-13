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

    /** Enables SLR parser generation. */
    SLR,
    /** Enables LR(1) parser generation. */
    LR1,
    /** Enables the inliner to solve simple conflicts. */
    INLINE_ON_CONFLICT,
    /** Enables inline table (base64/gzip) table generation. */
    CG_INLINE_TABLES,
    /** Enables API documentation generation. */
    CG_APIDOC,
    /** Enables additional comment generation. */
    CG_COMMENT,
    /** Enables jsr305 annotation generation on public APIs. */
    CG_JSR305,
    /** Enables jsr305 annotation generation on internal APIs. */
    CG_JSR305_INTERNAL,
    /** Makes list references mutable, instead of keeping a final list and changing the contents. */
    CG_LISTREFS_MUTABLE,
    /** Enables findbugs annotation generation. */
    CG_FINDBUGS,
    /** Enables compact code generation. */
    CG_COMPACT,
    /** Enables usage of common runtime interfaces. */
    CG_RUNTIME,
    /** Enables parent pointers in the generated code. */
    CG_PARENT,
    /** Enables "large-mode", typically automatic, but can be forced. */
    CG_LARGE,
    /** Forces linear-search in the lexer. */
    CG_LEXER_LINEARSEARCH,
    /** Forces binary-search in the lexer. */
    CG_LEXER_BINARYSEARCH,
    /** Enables threaded execution, which can generate simpler error messages. */
    PARALLEL,
    /** Log additional debugging information. */
    VERBOSE,
    /** Allow token masking, that is, tokens which can never match. */
    ALLOWMASKEDTOKENS,
    /** Perform advanced diagnosis (not yet reliable). */
    DIAGNOSIS,
}
