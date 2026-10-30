package com.omegadex.app.testutil

import com.omegadex.app.domain.compte.HacheurMotDePasse
import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.Identifiants
import com.omegadex.app.domain.model.Run
import com.omegadex.app.domain.model.Utilisateur
import com.omegadex.app.domain.repository.CompteRepository
import com.omegadex.app.domain.repository.EtatSession
import com.omegadex.app.domain.repository.RunActiveRepository
import com.omegadex.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Comptes en mémoire pour les tests de la couche domaine. */
class FauxComptes : CompteRepository {
    private data class Ligne(val id: Long, val identifiant: String, val hash: String, val sel: String)

    private val lignes = MutableStateFlow(listOf<Ligne>())

    override fun utilisateurs() = lignes.map { l -> l.map { Utilisateur(it.id, it.identifiant, 0) } }

    override suspend fun identifiants(identifiant: String) = lignes.value.firstOrNull {
        it.identifiant.equals(identifiant.trim(), ignoreCase = true)
    }?.let { Identifiants(it.id, it.hash, it.sel) }

    override suspend fun creerUtilisateur(identifiant: String, hashMotDePasse: String, sel: String): Long? {
        if (identifiants(identifiant) != null) return null
        val id = lignes.value.size + 1L
        lignes.value = lignes.value + Ligne(id, identifiant, hashMotDePasse, sel)
        return id
    }

    override suspend fun supprimerUtilisateur(utilisateurId: Long) {
        lignes.value = lignes.value.filterNot { it.id == utilisateurId }
    }

    override fun runs(utilisateurId: Long) = flowOf(emptyList<Run>())

    override fun run(utilisateurId: Long, runId: Long) = flowOf<Run?>(null)

    override suspend fun supprimerRun(utilisateurId: Long, runId: Long) = Unit

    override suspend fun creerRun(
        utilisateurId: Long,
        nom: String,
        starterId: Int,
        difficulte: Difficulte,
        regles: List<String>,
    ) = 0L
}

class FausseSession : SessionRepository {
    private val id = MutableStateFlow<Long?>(null)
    override val utilisateurConnecte: StateFlow<Long?> = id
    override val etat: StateFlow<EtatSession> = MutableStateFlow(EtatSession.Deconnecte)

    override suspend fun ouvrir(utilisateurId: Long) {
        id.value = utilisateurId
        (etat as MutableStateFlow).value = EtatSession.Connecte(utilisateurId)
    }

    override suspend fun fermer() {
        id.value = null
        (etat as MutableStateFlow).value = EtatSession.Deconnecte
    }
}

/** Hachage rapide et déterministe, pour tester la logique sans le coût d'Argon2. */
class FauxHacheur : HacheurMotDePasse {
    private var compteur = 0

    override fun nouveauSel() = "sel${++compteur}"

    override fun hacher(motDePasse: CharArray, sel: String) = "h:$sel:" + String(motDePasse).reversed()

    override fun verifier(motDePasse: CharArray, sel: String, empreinte: String) = hacher(motDePasse, sel) == empreinte
}

/** Run active en mémoire, avec le même repli que l'implémentation : dernière choisie, sinon la plus récente. */
class FausseRunActive(private val comptes: CompteRepository) : RunActiveRepository {
    private val choisies = MutableStateFlow(mapOf<Long, Long>())

    override fun runActive(utilisateurId: Long) = combine(choisies, comptes.runs(utilisateurId)) { choix, runs ->
        runs.firstOrNull { it.id == choix[utilisateurId] }?.id ?: runs.firstOrNull()?.id
    }

    override suspend fun choisir(utilisateurId: Long, runId: Long) {
        choisies.value = choisies.value + (utilisateurId to runId)
    }
}
