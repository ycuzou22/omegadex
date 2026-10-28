package com.omegadex.app.domain.repository

import com.omegadex.app.domain.sauvegarde.RunSauvegardee
import com.omegadex.app.domain.sauvegarde.Sauvegarde

/** Lecture de la progression complète d'un compte, pour l'export. */
interface SauvegardeRepository {
    /** Toutes les runs du compte et leur progression, des plus anciennes aux plus récentes. */
    suspend fun runs(utilisateurId: Long): List<RunSauvegardee>
}

/** Écriture d'une sauvegarde dans un fichier choisi par le joueur. */
interface FichiersSauvegarde {
    /**
     * Écrit [sauvegarde] à [destination] (adresse renvoyée par le sélecteur de fichiers Android).
     * Lève une [java.io.IOException] si le fichier ne peut pas être écrit.
     */
    suspend fun ecrire(destination: String, sauvegarde: Sauvegarde)
}
