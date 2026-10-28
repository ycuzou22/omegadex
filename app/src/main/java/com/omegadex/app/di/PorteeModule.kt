package com.omegadex.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Portée de coroutines qui vit autant que l'application (états partagés comme la session). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PorteeApplication

@Module
@InstallIn(SingletonComponent::class)
object PorteeModule {
    @Provides
    @Singleton
    @PorteeApplication
    fun porteeApplication(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
