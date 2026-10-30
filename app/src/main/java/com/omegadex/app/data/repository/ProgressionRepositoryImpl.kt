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
import com.omegadex.app.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Levée quand on tente de modifier une run qui n'appartient pas au joueur connecté. */
class AccesRunRefuse(runId: Long) : SecurityException("La run $runId n'appartient pas au joueur connecté")

/**
 * Progression d'une run. Chaque lecture et chaque écriture est filtrée par le joueur connecté :
 * une run d'un autre compte se lit comme vide et ne peut pas être modifiée.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressionRepositoryImpl @Inject constructor(
    private val base: ProgressionDatabase,
    private val horloge: Horloge,
    private val session: SessionRepository,
) : ProgressionRepository {
    private val comptes = base.compteDao()
    private val aventure = base.aventureDao()
    private val pokedex = base.pokedexDao()
    private val equipe = base.equipeDao()
    private val defis = base.defiDao()

    override fun etapes(runId: Long) = duJoueur(runId, emptyList()) {
        aventure.etapes(runId)
    }.map { liste -> liste.map { it.versDomaine() } }

    override suspend fun deverrouillerEtape(runId: Long, etapeId: Int) = base.withTransaction {
        verifierProprietaire(runId)
        val existante = aventure.etapes(runId).first().firstOrNull { it.etapeId == etapeId }
        if (existante?.deverrouillee != true) {
            aventure.enregistrerEtape(ProgressionEtapeEntity(runId, etapeId, true, horloge.maintenant()))
        }
    }

    override suspend fun validerEtape(runId: Long, etapeId: Int) = base.withTransaction {
        verifierProprietaire(runId)
        val existante = aventure.etapes(runId).first().firstOrNull { it.etapeId == etapeId }
        val maintenant = horloge.maintenant()
        aventure.enregistrerEtape(
            ProgressionEtapeEntity(runId, etapeId, true, existante?.dateDeverrouillage ?: maintenant, maintenant),
        )
    }

    override fun quetesCochees(runId: Long) = duJoueur(runId, emptyList()) {
        aventure.quetesCochees(runId)
    }.map { it.toSet() }

    override suspend fun cocherQuete(runId: Long, queteId: Int, cochee: Boolean) {
        verifierProprietaire(runId)
        if (cochee) {
            aventure.cocher(
                QueteCocheeEntity(runId, queteId, horloge.maintenant()),
            )
        } else {
            aventure.decocher(runId, queteId)
        }
    }

    override fun statuts(runId: Long) = duJoueur(runId, emptyList()) { pokedex.statuts(runId) }.map { liste ->
        liste.associate { it.especeId to it.statut }
    }

    override suspend fun definirStatut(runId: Long, especeId: Int, statut: StatutEspece) = base.withTransaction {
        verifierProprietaire(runId)
        val notes = pokedex.statut(runId, especeId).first()?.notes.orEmpty()
        pokedex.enregistrer(ProgressionEspeceEntity(runId, especeId, statut, notes, horloge.maintenant()))
    }

    override fun notes(runId: Long, especeId: Int) =
        duJoueur(runId, null) { pokedex.statut(runId, especeId) }.map { it?.notes.orEmpty() }

    override suspend fun definirNotes(runId: Long, especeId: Int, notes: String) = base.withTransaction {
        verifierProprietaire(runId)
        val statut = pokedex.statut(runId, especeId).first()?.statut ?: StatutEspece.NON_VUE
        pokedex.enregistrer(ProgressionEspeceEntity(runId, especeId, statut, notes, horloge.maintenant()))
    }

    override fun equipe(runId: Long) = duJoueur(runId, emptyList()) {
        equipe.equipe(runId)
    }.map { liste -> liste.map { it.versDomaine() } }

    override suspend fun placerMembre(runId: Long, membre: MembreEquipe) {
        verifierProprietaire(runId)
        require(membre.emplacement in EMPLACEMENTS) { "Emplacement ${membre.emplacement} hors de 1 à 6" }
        equipe.placer(MembreEquipeEntity(runId, membre.emplacement, membre.especeId, membre.surnom, membre.niveau))
    }

    override suspend fun retirerMembre(runId: Long, emplacement: Int) {
        verifierProprietaire(runId)
        equipe.retirer(runId, emplacement)
    }

    override fun defis(runId: Long) = duJoueur(runId, emptyList()) {
        defis.defis(runId)
    }.map { liste -> liste.map { it.versDomaine() } }

    override suspend fun ajouterDefi(runId: Long, modeleId: Int, especeId: Int?, etapeId: Int?): Long {
        verifierProprietaire(runId)
        return defis.ajouter(
            DefiEntity(
                runId = runId,
                modeleId = modeleId,
                especeId = especeId,
                etapeId = etapeId,
                etat = EtatDefi.ACTIF,
                dateDebut = horloge.maintenant(),
            ),
        )
    }

    override suspend fun changerEtatDefi(runId: Long, defiId: Long, etat: EtatDefi) {
        verifierProprietaire(runId)
        defis.changerEtat(runId, defiId, etat, if (etat == EtatDefi.ACTIF) null else horloge.maintenant())
    }

    /**
     * Lit [lecture] seulement si la run appartient au joueur connecté ; sinon [vide].
     * Suit les changements de joueur.
     */
    private fun <T> duJoueur(runId: Long, vide: T, lecture: () -> Flow<T>): Flow<T> =
        session.utilisateurConnecte.flatMapLatest { utilisateurId ->
            if (utilisateurId == null) {
                flowOf(vide)
            } else {
                comptes.runAppartient(utilisateurId, runId).flatMapLatest { ok -> if (ok) lecture() else flowOf(vide) }
            }
        }

    private suspend fun verifierProprietaire(runId: Long) {
        val utilisateurId = session.utilisateurConnecte.value ?: throw AccesRunRefuse(runId)
        if (!comptes.runAppartient(utilisateurId, runId).first()) throw AccesRunRefuse(runId)
    }

    private companion object {
        val EMPLACEMENTS = 1..6
    }
}
