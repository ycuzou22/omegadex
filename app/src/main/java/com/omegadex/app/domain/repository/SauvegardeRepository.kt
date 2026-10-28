package com.omegadex.app.domain.repository

import com.omegadex.app.domain.sauvegarde.RunSauvegardee
import com.omegadex.app.domain.sauvegarde.Sauvegarde

/** Lecture et restauration de la progression complète d'un compte. */
interface SauvegardeRepository {
    /** Toutes les runs du compte et leur progression, des plus anciennes aux plus récentes. */
    suspend fun runs(utilisateurId: Long): List<RunSauvegardee>

    /**
     * Ajoute [runs] au compte, en une seule transaction : soit tout est importé, soit rien.
     * Les runs existantes du compte ne sont jamais modifiées. Retourne les id des runs créées.
     */
    suspend fun restaurer(utilisateurId: Long, runs: List<RunSauvegardee>): List<Long>
}

/** Lecture et écriture d'une sauvegarde dans un fichier choisi par le joueur. */
interface FichiersSauvegarde {
    /**
     * Écrit [sauvegarde] à [destination] (adresse renvoyée par le sélecteur de fichiers Android).
     * Lève une [java.io.IOException] si le fichier ne peut pas être écrit.
     */
    suspend fun ecrire(destination: String, sauvegarde: Sauvegarde)

    /**
     * Lit la sauvegarde à [source]. Lève une [java.io.IOException] si le fichier ne peut pas être lu,
     * et une [com.omegadex.app.domain.sauvegarde.SauvegardeInvalide] s'il n'est pas une sauvegarde valide.
     */
    suspend fun lire(source: String): Sauvegarde
}
