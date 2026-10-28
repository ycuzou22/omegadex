package com.omegadex.app.domain.compte

import com.omegadex.app.domain.repository.CompteRepository
import com.omegadex.app.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Erreurs d'inscription, affichées par l'écran avec un message propre à chacune. */
enum class ErreurInscription {
    IDENTIFIANT_INVALIDE,
    MOT_DE_PASSE_TROP_COURT,
    CONFIRMATION_DIFFERENTE,
    IDENTIFIANT_PRIS,
}

sealed interface ResultatInscription {
    data class Reussie(val utilisateurId: Long) : ResultatInscription

    data class Refusee(val erreurs: Set<ErreurInscription>) : ResultatInscription
}

sealed interface ResultatConnexion {
    data class Reussie(val utilisateurId: Long) : ResultatConnexion

    /** Identifiant inconnu ou mauvais mot de passe : le message ne dit pas lequel des deux. */
    data object IdentifiantsIncorrects : ResultatConnexion

    data class TropDeTentatives(val secondesRestantes: Long) : ResultatConnexion
}

/** Règles d'un identifiant : 3 à 20 caractères, lettres, chiffres, « - », « _ » ou « . ». */
private val FORMAT_IDENTIFIANT = Regex("^[\\p{L}\\p{N}._-]{3,20}$")
const val LONGUEUR_MIN_MOT_DE_PASSE = 8

class InscrireUseCase @Inject constructor(
    private val comptes: CompteRepository,
    private val hacheur: HacheurMotDePasse,
    private val session: SessionRepository,
) {
    suspend operator fun invoke(
        identifiant: String,
        motDePasse: CharArray,
        confirmation: CharArray,
    ): ResultatInscription {
        val nom = identifiant.trim()
        val erreurs = buildSet {
            if (!FORMAT_IDENTIFIANT.matches(nom)) add(ErreurInscription.IDENTIFIANT_INVALIDE)
            if (motDePasse.size < LONGUEUR_MIN_MOT_DE_PASSE) add(ErreurInscription.MOT_DE_PASSE_TROP_COURT)
            if (!motDePasse.contentEquals(confirmation)) add(ErreurInscription.CONFIRMATION_DIFFERENTE)
        }
        if (erreurs.isNotEmpty()) return ResultatInscription.Refusee(erreurs)
        if (comptes.identifiants(nom) !=
            null
        ) {
            return ResultatInscription.Refusee(setOf(ErreurInscription.IDENTIFIANT_PRIS))
        }
        val sel = hacheur.nouveauSel()
        val empreinte = withContext(Dispatchers.Default) { hacheur.hacher(motDePasse, sel) }
        val id = comptes.creerUtilisateur(nom, empreinte, sel)
            ?: return ResultatInscription.Refusee(setOf(ErreurInscription.IDENTIFIANT_PRIS))
        session.ouvrir(id)
        return ResultatInscription.Reussie(id)
    }
}

class ConnecterUseCase @Inject constructor(
    private val comptes: CompteRepository,
    private val hacheur: HacheurMotDePasse,
    private val session: SessionRepository,
    private val limiteur: LimiteurTentatives,
) {
    suspend operator fun invoke(identifiant: String, motDePasse: CharArray): ResultatConnexion {
        val attente = limiteur.attenteRestante(identifiant)
        if (attente > 0) return ResultatConnexion.TropDeTentatives(attente)
        val stockes = comptes.identifiants(identifiant)
        val valide = stockes != null &&
            withContext(Dispatchers.Default) { hacheur.verifier(motDePasse, stockes.sel, stockes.hashMotDePasse) }
        if (!valide || stockes == null) {
            limiteur.enregistrerEchec(identifiant)
            val nouvelleAttente = limiteur.attenteRestante(identifiant)
            return if (nouvelleAttente >
                0
            ) {
                ResultatConnexion.TropDeTentatives(nouvelleAttente)
            } else {
                ResultatConnexion.IdentifiantsIncorrects
            }
        }
        limiteur.enregistrerSucces(identifiant)
        session.ouvrir(stockes.utilisateurId)
        return ResultatConnexion.Reussie(stockes.utilisateurId)
    }
}
