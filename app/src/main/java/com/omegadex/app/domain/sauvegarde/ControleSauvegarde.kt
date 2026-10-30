package com.omegadex.app.domain.sauvegarde

import com.omegadex.app.domain.equipe.NOMBRE_BOITES
import com.omegadex.app.domain.equipe.TAILLE_BOITE

/**
 * Contrôles de cohérence d'une sauvegarde avant import : un fichier modifié à la main ou abîmé
 * est refusé en entier plutôt que de laisser une progression incohérente dans la base.
 */
object ControleSauvegarde {
    private val EMPLACEMENTS = 1..6
    private val NIVEAUX = 1..100

    /** Lève [SauvegardeInvalide] au premier problème rencontré. */
    fun verifier(sauvegarde: Sauvegarde) {
        exiger(sauvegarde.version in 1..Sauvegarde.VERSION_FORMAT) {
            "Version ${sauvegarde.version} non prise en charge (version maximale : ${Sauvegarde.VERSION_FORMAT})"
        }
        sauvegarde.runs.forEachIndexed { index, run -> verifierRun(run, "run ${index + 1}") }
    }

    private fun verifierRun(run: RunSauvegardee, ou: String) {
        exiger(run.nom.isNotBlank()) { "$ou : nom vide" }
        exiger(run.dateFin == null || run.dateFin >= run.dateDebut) { "$ou : fin avant le début" }
        exiger(run.etapes.all { it.etapeId >= 0 }) { "$ou : étape négative" }
        exigerUniques(run.etapes.map { it.etapeId }) { "$ou : étape $it en double" }
        exigerUniques(run.quetesCochees.map { it.queteId }) { "$ou : quête $it en double" }
        exigerUniques(run.especes.map { it.especeId }) { "$ou : espèce $it en double" }
        exigerUniques(run.equipe.map { it.emplacement }) { "$ou : emplacement d'équipe $it en double" }
        run.equipe.forEach { membre ->
            exiger(membre.emplacement in EMPLACEMENTS) { "$ou : emplacement ${membre.emplacement} hors de 1 à 6" }
            exiger(membre.niveau in NIVEAUX) { "$ou : niveau ${membre.niveau} hors de 1 à 100" }
        }
        run.pc.forEach { pokemon ->
            exiger(pokemon.boite in 1..NOMBRE_BOITES) { "$ou : boîte ${pokemon.boite} hors de 1 à $NOMBRE_BOITES" }
            exiger(pokemon.niveau in NIVEAUX) { "$ou : niveau ${pokemon.niveau} hors de 1 à 100" }
        }
        run.pc.groupingBy { it.boite }.eachCount().forEach { (boite, nombre) ->
            exiger(nombre <= TAILLE_BOITE) { "$ou : boîte $boite trop pleine ($nombre Pokémon)" }
        }
        run.defis.forEach { defi ->
            exiger(defi.dateFin == null || defi.dateFin >= defi.dateDebut) { "$ou : défi terminé avant son début" }
        }
    }

    private inline fun exiger(condition: Boolean, raison: () -> String) {
        if (!condition) throw SauvegardeInvalide(raison())
    }

    private inline fun <T> exigerUniques(valeurs: List<T>, raison: (T) -> String) {
        val doublon = valeurs.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.key
        if (doublon != null) throw SauvegardeInvalide(raison(doublon))
    }
}
