package com.quickfind.core.data.di

import javax.inject.Qualifier

/** A process-wide scope for work that must outlive a screen (writes, index refresh). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
