package com.omegadex.app.data.sauvegarde

import com.omegadex.app.domain.model.Difficulte
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.Nature
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.domain.model.StatutEspece
import com.omegadex.app.domain.sauvegarde.DefiSauvegarde
import com.omegadex.app.domain.sauvegarde.EspeceSuivie
import com.omegadex.app.domain.sauvegarde.PokemonPcSauvegarde
import com.omegadex.app.domain.sauvegarde.QueteCochee
import com.omegadex.app.domain.sauvegarde.RunSauvegardee
import com.omegadex.app.domain.sauvegarde.Sauvegarde
import com.omegadex.app.domain.sauvegarde.SauvegardeInvalide
import org.json.JSONArray
import org.json.JSONException
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
        .put(Cles.PC, pc.versJson { it.versJson() })

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
        .put(Cles.NATURE, nature?.name.ouNull())
        .put(Cles.TALENT, talent.ouNull())

    private fun PokemonPcSauvegarde.versJson() = JSONObject()
        .put(Cles.BOITE, boite)
        .put(Cles.ESPECE_ID, especeId)
        .put(Cles.SURNOM, surnom.ouNull())
        .put(Cles.NIVEAU, niveau)
        .put(Cles.NATURE, nature?.name.ouNull())
        .put(Cles.TALENT, talent.ouNull())

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

    /**
     * Relit un fichier exporté. Lève [SauvegardeInvalide] si ce n'est pas du JSON, pas une sauvegarde
     * Omégadex, une version plus récente que l'application, ou si un champ manque ou est mal typé.
     * La cohérence des valeurs est contrôlée ensuite par le domaine (ControleSauvegarde).
     */
    fun decoder(texte: String): Sauvegarde {
        val racine = lireJson(texte)
        return lireSauvegarde(racine, verifierEntete(racine))
    }

    private fun lireJson(texte: String) = try {
        JSONObject(texte)
    } catch (e: JSONException) {
        throw SauvegardeInvalide("Ce fichier n'est pas un fichier JSON", e)
    }

    /** Vérifie la marque du format et retourne la version du fichier. */
    private fun verifierEntete(racine: JSONObject): Int {
        if (racine.optString(Cles.FORMAT) != FORMAT) {
            throw SauvegardeInvalide("Ce fichier n'est pas une sauvegarde Omégadex")
        }
        val version = racine.optInt(Cles.VERSION, 0)
        if (version !in 1..Sauvegarde.VERSION_FORMAT) {
            throw SauvegardeInvalide(
                "Version $version non prise en charge : mettez à jour l'application pour importer ce fichier",
            )
        }
        return version
    }

    private fun lireSauvegarde(racine: JSONObject, version: Int): Sauvegarde = try {
        Sauvegarde(
            version = version,
            dateExport = racine.getLong(Cles.DATE_EXPORT),
            identifiant = racine.getString(Cles.IDENTIFIANT),
            runs = racine.getJSONArray(Cles.RUNS).objets().map { it.versRun() },
        )
    } catch (e: JSONException) {
        throw SauvegardeInvalide("Sauvegarde incomplète ou abîmée : ${e.message}", e)
    } catch (e: IllegalArgumentException) {
        throw SauvegardeInvalide("Valeur inconnue dans la sauvegarde : ${e.message}", e)
    }

    private fun JSONObject.versRun() = RunSauvegardee(
        nom = getString(Cles.NOM),
        starterId = getInt(Cles.STARTER_ID),
        difficulte = Difficulte.valueOf(getString(Cles.DIFFICULTE)),
        regles = getJSONArray(Cles.REGLES).let { tableau -> List(tableau.length()) { tableau.getString(it) } },
        dateDebut = getLong(Cles.DATE_DEBUT),
        dateFin = longOuNull(Cles.DATE_FIN),
        etapes = getJSONArray(Cles.ETAPES).objets().map {
            ProgressionEtape(
                etapeId = it.getInt(Cles.ETAPE_ID),
                deverrouillee = it.getBoolean(Cles.DEVERROUILLEE),
                dateDeverrouillage = it.longOuNull(Cles.DATE_DEVERROUILLAGE),
                dateValidation = it.longOuNull(Cles.DATE_VALIDATION),
            )
        },
        quetesCochees = getJSONArray(Cles.QUETES_COCHEES).objets().map {
            QueteCochee(it.getInt(Cles.QUETE_ID), it.getLong(Cles.DATE_COCHAGE))
        },
        especes = getJSONArray(Cles.ESPECES).objets().map {
            EspeceSuivie(
                especeId = it.getInt(Cles.ESPECE_ID),
                statut = StatutEspece.valueOf(it.getString(Cles.STATUT)),
                notes = it.getString(Cles.NOTES),
                dateMaj = it.getLong(Cles.DATE_MAJ),
            )
        },
        equipe = getJSONArray(Cles.EQUIPE).objets().map {
            MembreEquipe(
                emplacement = it.getInt(Cles.EMPLACEMENT),
                especeId = it.getInt(Cles.ESPECE_ID),
                surnom = it.stringOuNull(Cles.SURNOM),
                niveau = it.getInt(Cles.NIVEAU),
                nature = it.natureFacultative(),
                talent = it.stringFacultatif(Cles.TALENT),
            )
        },
        // Absent des fichiers version 1.
        pc = optJSONArray(Cles.PC)?.objets().orEmpty().map {
            PokemonPcSauvegarde(
                boite = it.getInt(Cles.BOITE),
                especeId = it.getInt(Cles.ESPECE_ID),
                surnom = it.stringOuNull(Cles.SURNOM),
                niveau = it.getInt(Cles.NIVEAU),
                nature = it.natureFacultative(),
                talent = it.stringFacultatif(Cles.TALENT),
            )
        },
        defis = getJSONArray(Cles.DEFIS).objets().map {
            DefiSauvegarde(
                modeleId = it.getInt(Cles.MODELE_ID),
                especeId = it.intOuNull(Cles.ESPECE_ID),
                etapeId = it.intOuNull(Cles.ETAPE_ID),
                etat = EtatDefi.valueOf(it.getString(Cles.ETAT)),
                dateDebut = it.getLong(Cles.DATE_DEBUT),
                dateFin = it.longOuNull(Cles.DATE_FIN),
            )
        },
    )

    private fun JSONArray.objets(): List<JSONObject> = List(length()) { getJSONObject(it) }

    /** Une clé absente est une erreur ; seule une valeur `null` explicite donne null. */
    private fun JSONObject.exigerCle(cle: String) {
        if (!has(cle)) throw JSONException("champ « $cle » manquant")
    }

    private fun JSONObject.longOuNull(cle: String): Long? = also { it.exigerCle(cle) }.let {
        if (it.isNull(cle)) null else it.getLong(cle)
    }

    private fun JSONObject.intOuNull(cle: String): Int? = also { it.exigerCle(cle) }.let {
        if (it.isNull(cle)) null else it.getInt(cle)
    }

    private fun JSONObject.stringOuNull(cle: String): String? = also { it.exigerCle(cle) }.let {
        if (it.isNull(cle)) null else it.getString(cle)
    }

    /** Champ ajouté en version 2 : absent d'un fichier version 1, il vaut null. */
    private fun JSONObject.stringFacultatif(cle: String): String? = if (isNull(cle)) null else getString(cle)

    private fun JSONObject.natureFacultative(): Nature? = stringFacultatif(Cles.NATURE)?.let { Nature.valueOf(it) }

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
        const val NATURE = "nature"
        const val TALENT = "talent"
        const val PC = "pc"
        const val BOITE = "boite"
    }
}
