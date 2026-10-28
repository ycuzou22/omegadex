package com.omegadex.app.di

import com.omegadex.app.data.repository.InfosAppRepositoryImpl
import com.omegadex.app.domain.repository.InfosAppRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Associe chaque interface de repository (domain) à son implémentation (data). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindInfosAppRepository(impl: InfosAppRepositoryImpl): InfosAppRepository
}
