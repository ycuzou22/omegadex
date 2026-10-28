package com.omegadex.app.data.sauvegarde

import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.domain.sauvegarde.DefiSauvegarde
import com.omegadex.app.domain.sauvegarde.EspeceSuivie
import com.omegadex.app.domain.sauvegarde.QueteCochee
import com.omegadex.app.domain.sauvegarde.RunSauvegardee
import com.omegadex.app.domain.sauvegarde.Sauvegarde
import org.json.JSONArray
import org.json.JSONObject

/**
 * Format JSON des sauvegardes Omégadex. Les clés sont écrites à la main (et non dérivées des noms
 * de classes) pour que renommer une propriété Kotlin ne casse pas les fichiers déjà exportés.
 */
object SauvegardeJson {
    /** Marque les fichiers Omégadex, pour refuser à l'import un JSON qui n'en est pas un. */
    const val FORMAT = "omegadex-sauvegarde"
    private const val INDENTATION = 2

    fun encoder(sauvegarde: Sauvegarde): String = JSONObject()
        .put(Cles.FORMAT, FORMAT)
        .put(Cles.VERSION, sauvegarde.version)
        .put(Cles.DATE_EXPORT, sauvegarde.dateExport)
        .put(Cles.IDENTIFIANT, sauvegarde.identifiant)
        .put(Cles.RUNS, sauvegarde.runs.versJson { it.versJson() })
        .toString(INDENTATION)

    private fun RunSauvegardee.versJson() = JSONObject()
        .put(Cles.NOM, nom)
        .put(Cles.STARTER_ID, starterId)
        .put(Cles.DIFFICULTE, difficulte.name)
        .put(Cles.REGLES, JSONArray(regles))
        .put(Cles.DATE_DEBUT, dateDebut)
        .put(Cles.DATE_FIN, dateFin.ouNull())
        .put(Cles.ETAPES, etapes.versJson { it.versJson() })
        .put(Cles.QUETES_COCHEES, quetesCochees.versJson { it.versJson() })
        .put(Cles.ESPECES, especes.versJson { it.versJson() })
        .put(Cles.EQUIPE, equipe.versJson { it.versJson() })
        .put(Cles.DEFIS, defis.versJson { it.versJson() })

    private fun ProgressionEtape.versJson() = JSONObject()
        .put(Cles.ETAPE_ID, etapeId)
        .put(Cles.DEVERROUILLEE, deverrouillee)
        .put(Cles.DATE_DEVERROUILLAGE, dateDeverrouillage.ouNull())
        .put(Cles.DATE_VALIDATION, dateValidation.ouNull())

    private fun QueteCochee.versJson() = JSONObject()
        .put(Cles.QUETE_ID, queteId)
        .put(Cles.DATE_COCHAGE, dateCochage)

    private fun EspeceSuivie.versJson() = JSONObject()
        .put(Cles.ESPECE_ID, especeId)
        .put(Cles.STATUT, statut.name)
        .put(Cles.NOTES, notes)
        .put(Cles.DATE_MAJ, dateMaj)

    private fun MembreEquipe.versJson() = JSONObject()
        .put(Cles.EMPLACEMENT, emplacement)
        .put(Cles.ESPECE_ID, especeId)
        .put(Cles.SURNOM, surnom.ouNull())
        .put(Cles.NIVEAU, niveau)

    private fun DefiSauvegarde.versJson() = JSONObject()
        .put(Cles.MODELE_ID, modeleId)
        .put(Cles.ESPECE_ID, especeId.ouNull())
        .put(Cles.ETAPE_ID, etapeId.ouNull())
        .put(Cles.ETAT, etat.name)
        .put(Cles.DATE_DEBUT, dateDebut)
        .put(Cles.DATE_FIN, dateFin.ouNull())

    private fun <T> List<T>.versJson(element: (T) -> JSONObject) = JSONArray().also { tableau ->
        forEach { tableau.put(element(it)) }
    }

    /** `put(cle, null)` retirerait la clé : on écrit un null JSON explicite. */
    private fun Any?.ouNull(): Any = this ?: JSONObject.NULL

    /** Clés du fichier, partagées par l'export et l'import. */
    object Cles {
        const val FORMAT = "format"
        const val VERSION = "version"
        const val DATE_EXPORT = "dateExport"
        const val IDENTIFIANT = "identifiant"
        const val RUNS = "runs"
        const val NOM = "nom"
        const val STARTER_ID = "starterId"
        const val DIFFICULTE = "difficulte"
        const val REGLES = "regles"
        const val DATE_DEBUT = "dateDebut"
        const val DATE_FIN = "dateFin"
        const val ETAPES = "etapes"
        const val QUETES_COCHEES = "quetesCochees"
        const val ESPECES = "especes"
        const val EQUIPE = "equipe"
        const val DEFIS = "defis"
        const val ETAPE_ID = "etapeId"
        const val DEVERROUILLEE = "deverrouillee"
        const val DATE_DEVERROUILLAGE = "dateDeverrouillage"
        const val DATE_VALIDATION = "dateValidation"
        const val QUETE_ID = "queteId"
        const val DATE_COCHAGE = "dateCochage"
        const val ESPECE_ID = "especeId"
        const val STATUT = "statut"
        const val NOTES = "notes"
        const val DATE_MAJ = "dateMaj"
        const val EMPLACEMENT = "emplacement"
        const val SURNOM = "surnom"
        const val NIVEAU = "niveau"
        const val MODELE_ID = "modeleId"
        const val ETAT = "etat"
    }
}
