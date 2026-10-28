package com.omegadex.app.domain.sauvegarde

import com.omegadex.app.domain.repository.FichiersSauvegarde
import com.omegadex.app.domain.repository.SauvegardeRepository
import com.omegadex.app.domain.repository.SessionRepository
import java.io.IOException
import javax.inject.Inject

/** Levée quand un fichier n'est pas une sauvegarde Omégadex lisible ; [message] dit pourquoi. */
class SauvegardeInvalide(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Résultat d'un import, pour le message affiché au joueur. */
sealed interface ResultatImport {
    /** [runsIgnorees] : runs déjà présentes dans le compte (même nom et même date de début). */
    data class Reussi(val runsImportees: Int, val runsIgnorees: Int) : ResultatImport

    data object NonConnecte : ResultatImport

    /** Le fichier n'a pas pu être lu (accès retiré, fichier supprimé…). */
    data object EchecLecture : ResultatImport

    /** Le fichier n'est pas une sauvegarde valide : rien n'a été importé. */
    data class FichierInvalide(val raison: String) : ResultatImport
}

/**
 * Importe une sauvegarde dans le compte connecté. L'import ajoute des runs sans jamais toucher
 * aux runs existantes ; une run déjà présente n'est pas importée une seconde fois.
 */
class ImporterSauvegardeUseCase @Inject constructor(
    private val session: SessionRepository,
    private val sauvegardes: SauvegardeRepository,
    private val fichiers: FichiersSauvegarde,
) {
    suspend operator fun invoke(source: String): ResultatImport {
        val utilisateurId = session.utilisateurConnecte.value ?: return ResultatImport.NonConnecte
        val sauvegarde = try {
            fichiers.lire(source)
        } catch (e: SauvegardeInvalide) {
            return ResultatImport.FichierInvalide(e.message.orEmpty())
        } catch (_: IOException) {
            return ResultatImport.EchecLecture
        }
        return importer(utilisateurId, sauvegarde)
    }

    /** Contrôle puis ajoute au compte les runs de [sauvegarde] qu'il ne contient pas encore. */
    suspend fun importer(utilisateurId: Long, sauvegarde: Sauvegarde): ResultatImport {
        try {
            ControleSauvegarde.verifier(sauvegarde)
        } catch (e: SauvegardeInvalide) {
            return ResultatImport.FichierInvalide(e.message.orEmpty())
        }
        val existantes = sauvegardes.runs(utilisateurId).map { it.cle() }.toSet()
        val nouvelles = sauvegarde.runs.filterNot { it.cle() in existantes }
        sauvegardes.restaurer(utilisateurId, nouvelles)
        return ResultatImport.Reussi(nouvelles.size, sauvegarde.runs.size - nouvelles.size)
    }

    private fun RunSauvegardee.cle() = nom to dateDebut
}
