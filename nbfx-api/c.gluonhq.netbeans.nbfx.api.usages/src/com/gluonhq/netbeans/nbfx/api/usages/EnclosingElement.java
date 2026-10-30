package com.gluonhq.netbeans.nbfx.api.usages;

import com.gluonhq.netbeans.nbfx.api.elements.SourceElementKind;
import com.gluonhq.netbeans.nbfx.api.elements.SourceTypeKind;

/**
 * One declaration enclosing a usage, outermost first in {@link UsageContext#enclosing()}: a type,
 * a method or constructor, a field or an initializer.
 *
 * @param kind       the declaration's kind
 * @param typeKind   the type's sub-kind for {@link SourceElementKind#TYPE} (and constructors), else
 *                   {@link SourceTypeKind#OTHER}
 * @param name       the display name: the simple type name, {@code method(ParamType, ...)} for
 *                   executables, the field name
 * @param modifiers  the declaration's modifiers as {@link java.lang.reflect.Modifier} bits, for its icon
 */
public record EnclosingElement(SourceElementKind kind, SourceTypeKind typeKind, String name, int modifiers) {
}
