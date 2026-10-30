package com.omegadex.app.domain.sauvegarde

import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.domain.model.StatutEspece

/**
 * Sauvegarde de la progression d'un compte. Elle ne contient jamais le mot de passe, son empreinte
 * ni son sel, ni les identifiants internes de la base : une run restaurée reçoit de nouveaux id.
 */
data class Sauvegarde(
    /** Version du format, pour refuser ou convertir un fichier d'une autre version à l'import. */
    val version: Int,
    val dateExport: Long,
    /** Identifiant du compte exporté, à titre indicatif : l'import se fait dans le compte connecté. */
    val identifiant: String,
    val runs: List<RunSauvegardee>,
) {
    companion object {
        /** Version courante du format ; à augmenter à chaque changement de structure du fichier. */
        const val VERSION_FORMAT = 1
    }
}

/** Une run et toute sa progression. */
data class RunSauvegardee(
    val nom: String,
    val starterId: Int,
    val difficulte: Difficulte,
    val regles: List<String>,
    val dateDebut: Long,
    val dateFin: Long?,
    val etapes: List<ProgressionEtape>,
    val quetesCochees: List<QueteCochee>,
    val especes: List<EspeceSuivie>,
    val equipe: List<MembreEquipe>,
    val defis: List<DefiSauvegarde>,
)

data class QueteCochee(val queteId: Int, val dateCochage: Long)

/** Statut d'entraînement d'une espèce, avec les notes du joueur. */
data class EspeceSuivie(val especeId: Int, val statut: StatutEspece, val notes: String, val dateMaj: Long)

data class DefiSauvegarde(
    val modeleId: Int,
    val especeId: Int?,
    val etapeId: Int?,
    val etat: EtatDefi,
    val dateDebut: Long,
    val dateFin: Long?,
)
