package com.omegadex.app.domain.sauvegarde

import com.omegadex.app.core.Horloge
import com.omegadex.app.domain.repository.CompteRepository
import com.omegadex.app.domain.repository.FichiersSauvegarde
import com.omegadex.app.domain.repository.SauvegardeRepository
import com.omegadex.app.domain.repository.SessionRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Résultat d'un export, pour le message affiché au joueur. */
sealed interface ResultatExport {
    data class Reussi(val nombreRuns: Int) : ResultatExport

    /** Aucun joueur connecté : on n'exporte que la progression du compte ouvert. */
    data object NonConnecte : ResultatExport

    /** Le fichier choisi n'a pas pu être écrit (stockage plein, accès retiré…). */
    data object EchecEcriture : ResultatExport
}

/** Exporte la progression du joueur connecté dans un fichier JSON choisi avec le sélecteur Android. */
class ExporterProgressionUseCase @Inject constructor(
    private val session: SessionRepository,
    private val comptes: CompteRepository,
    private val sauvegardes: SauvegardeRepository,
    private val fichiers: FichiersSauvegarde,
    private val horloge: Horloge,
) {
    suspend operator fun invoke(destination: String): ResultatExport {
        val sauvegarde = construire() ?: return ResultatExport.NonConnecte
        return try {
            fichiers.ecrire(destination, sauvegarde)
            ResultatExport.Reussi(sauvegarde.runs.size)
        } catch (_: IOException) {
            ResultatExport.EchecEcriture
        }
    }

    /** Sauvegarde du joueur connecté, ou null si personne n'est connecté. */
    suspend fun construire(): Sauvegarde? {
        val utilisateurId = session.utilisateurConnecte.value ?: return null
        val compte = comptes.utilisateurs().first().firstOrNull { it.id == utilisateurId } ?: return null
        return Sauvegarde(
            version = Sauvegarde.VERSION_FORMAT,
            dateExport = horloge.maintenant(),
            identifiant = compte.identifiant,
            runs = sauvegardes.runs(utilisateurId),
        )
    }
}
