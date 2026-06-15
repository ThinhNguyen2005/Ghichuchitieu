package com.notepay.di

import javax.inject.Qualifier

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher
