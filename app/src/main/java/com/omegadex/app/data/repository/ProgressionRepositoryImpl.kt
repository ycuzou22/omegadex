package com.omegadex.app.data.repository

import androidx.room.withTransaction
import com.omegadex.app.core.Horloge
import com.omegadex.app.data.mapper.versDomaine
import com.omegadex.app.data.progression.ProgressionDatabase
import com.omegadex.app.data.progression.entity.DefiEntity
import com.omegadex.app.data.progression.entity.MembreEquipeEntity
import com.omegadex.app.data.progression.entity.ProgressionEspeceEntity
import com.omegadex.app.data.progression.entity.ProgressionEtapeEntity
import com.omegadex.app.data.progression.entity.QueteCocheeEntity
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.repository.ProgressionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ProgressionRepositoryImpl @Inject constructor(
    private val base: ProgressionDatabase,
    private val horloge: Horloge,
) : ProgressionRepository {
    private val aventure = base.aventureDao()
    private val pokedex = base.pokedexDao()
    private val equipe = base.equipeDao()
    private val defis = base.defiDao()

    override fun etapes(runId: Long) = aventure.etapes(runId).map { liste -> liste.map { it.versDomaine() } }

    override suspend fun deverrouillerEtape(runId: Long, etapeId: Int) = base.withTransaction {
        val existante = aventure.etapes(runId).first().firstOrNull { it.etapeId == etapeId }
        if (existante?.deverrouillee != true) {
            aventure.enregistrerEtape(ProgressionEtapeEntity(runId, etapeId, true, horloge.maintenant()))
        }
    }

    override suspend fun validerEtape(runId: Long, etapeId: Int) = base.withTransaction {
        val existante = aventure.etapes(runId).first().firstOrNull { it.etapeId == etapeId }
        val maintenant = horloge.maintenant()
        aventure.enregistrerEtape(
            ProgressionEtapeEntity(runId, etapeId, true, existante?.dateDeverrouillage ?: maintenant, maintenant),
        )
    }

    override fun quetesCochees(runId: Long) = aventure.quetesCochees(runId).map { it.toSet() }

    override suspend fun cocherQuete(runId: Long, queteId: Int, cochee: Boolean) {
        if (cochee) {
            aventure.cocher(
                QueteCocheeEntity(runId, queteId, horloge.maintenant()),
            )
        } else {
            aventure.decocher(runId, queteId)
        }
    }

    override fun statuts(runId: Long) = pokedex.statuts(runId).map { liste ->
        liste.associate { it.especeId to it.statut }
    }

    override suspend fun definirStatut(runId: Long, especeId: Int, statut: StatutEspece) = base.withTransaction {
        val notes = pokedex.statut(runId, especeId).first()?.notes.orEmpty()
        pokedex.enregistrer(ProgressionEspeceEntity(runId, especeId, statut, notes, horloge.maintenant()))
    }

    override fun equipe(runId: Long) = equipe.equipe(runId).map { liste -> liste.map { it.versDomaine() } }

    override suspend fun placerMembre(runId: Long, membre: MembreEquipe) {
        require(membre.emplacement in EMPLACEMENTS) { "Emplacement ${membre.emplacement} hors de 1 à 6" }
        equipe.placer(MembreEquipeEntity(runId, membre.emplacement, membre.especeId, membre.surnom, membre.niveau))
    }

    override suspend fun retirerMembre(runId: Long, emplacement: Int) = equipe.retirer(runId, emplacement)

    override fun defis(runId: Long) = defis.defis(runId).map { liste -> liste.map { it.versDomaine() } }

    override suspend fun ajouterDefi(runId: Long, modeleId: Int, especeId: Int?, etapeId: Int?) = defis.ajouter(
        DefiEntity(
            runId = runId,
            modeleId = modeleId,
            especeId = especeId,
            etapeId = etapeId,
            etat = EtatDefi.ACTIF,
            dateDebut = horloge.maintenant(),
        ),
    )

    override suspend fun changerEtatDefi(runId: Long, defiId: Long, etat: EtatDefi) =
        defis.changerEtat(runId, defiId, etat, if (etat == EtatDefi.ACTIF) null else horloge.maintenant())

    private companion object {
        val EMPLACEMENTS = 1..6
    }
}
