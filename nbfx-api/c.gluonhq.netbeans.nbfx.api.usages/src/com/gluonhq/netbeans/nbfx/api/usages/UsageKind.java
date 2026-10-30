package com.gluonhq.netbeans.nbfx.api.usages;

/**
 * What an occurrence is, independent of the language that found it. The Usages view uses this to
 * choose the result marker and to group the filter buttons; {@link #OTHER} is the fallback for
 * languages whose usages do not have these categories.
 */
public enum UsageKind {
    /** A named reference that is not a variable access: a type, method, field, ... */
    REFERENCE,
    /** A variable read. */
    READ,
    /** A variable write. */
    WRITE,
    /** A variable read and write. */
    READ_WRITE,
    /** A name in an import statement. */
    IMPORT,
    /** A name mentioned in a comment. */
    COMMENT,
    /** An occurrence with no further category. */
    OTHER
}
