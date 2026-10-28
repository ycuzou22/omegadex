package com.omegadex.app.di

import com.omegadex.app.data.repository.SauvegardeRepositoryImpl
import com.omegadex.app.data.sauvegarde.FichiersSauvegardeAndroid
import com.omegadex.app.domain.repository.FichiersSauvegarde
import com.omegadex.app.domain.repository.SauvegardeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Sauvegarde locale de la progression (export et import JSON, T15). */
@Module
@InstallIn(SingletonComponent::class)
abstract class SauvegardeModule {
    @Binds
    abstract fun bindSauvegardeRepository(impl: SauvegardeRepositoryImpl): SauvegardeRepository

    @Binds
    abstract fun bindFichiersSauvegarde(impl: FichiersSauvegardeAndroid): FichiersSauvegarde
}
