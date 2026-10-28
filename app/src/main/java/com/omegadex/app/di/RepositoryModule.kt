package com.omegadex.app.di

import com.omegadex.app.core.Horloge
import com.omegadex.app.core.HorlogeSysteme
import com.omegadex.app.data.repository.CompteRepositoryImpl
import com.omegadex.app.data.repository.InfosAppRepositoryImpl
import com.omegadex.app.data.repository.JeuRepositoryImpl
import com.omegadex.app.data.repository.ProgressionRepositoryImpl
import com.omegadex.app.data.repository.SessionDataStore
import com.omegadex.app.data.securite.CleBaseKeystore
import com.omegadex.app.data.securite.FournisseurCleBase
import com.omegadex.app.data.securite.HacheurArgon2
import com.omegadex.app.domain.compte.HacheurMotDePasse
import com.omegadex.app.domain.repository.CompteRepository
import com.omegadex.app.domain.repository.InfosAppRepository
import com.omegadex.app.domain.repository.JeuRepository
import com.omegadex.app.domain.repository.ProgressionRepository
import com.omegadex.app.domain.repository.SessionRepository
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

    @Binds
    @Singleton
    abstract fun bindJeuRepository(impl: JeuRepositoryImpl): JeuRepository

    @Binds
    @Singleton
    abstract fun bindCompteRepository(impl: CompteRepositoryImpl): CompteRepository

    @Binds
    @Singleton
    abstract fun bindProgressionRepository(impl: ProgressionRepositoryImpl): ProgressionRepository

    @Binds
    abstract fun bindHorloge(impl: HorlogeSysteme): Horloge

    @Binds
    abstract fun bindHacheur(impl: HacheurArgon2): HacheurMotDePasse

    @Binds
    abstract fun bindSession(impl: SessionDataStore): SessionRepository

    @Binds
    @Singleton
    abstract fun bindCleBase(impl: CleBaseKeystore): FournisseurCleBase
}
