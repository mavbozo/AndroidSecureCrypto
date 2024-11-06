package com.mavbozo.androidsecurecrypto.internal

/**
 * Marks declarations that are internal to AndroidSecureCrypto library.
 * These APIs should not be used outside of the library as they may change
 * without notice.
 *
 * This annotation is used to clearly distinguish between public API and
 * internal implementation details.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS
)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class InternalApi