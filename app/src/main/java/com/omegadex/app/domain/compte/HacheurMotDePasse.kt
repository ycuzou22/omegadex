package com.omegadex.app.domain.compte

/**
 * Empreinte d'un mot de passe. Le mot de passe n'est jamais stocké : seuls l'empreinte
 * (qui contient l'algorithme et ses paramètres) et le sel unique du compte le sont.
 */
interface HacheurMotDePasse {
    /** Nouveau sel aléatoire, unique par compte, encodé en Base64. */
    fun nouveauSel(): String

    /** Empreinte de [motDePasse] avec [sel]. */
    fun hacher(motDePasse: CharArray, sel: String): String

    /** Compare en temps constant le mot de passe saisi à l'empreinte stockée. */
    fun verifier(motDePasse: CharArray, sel: String, empreinte: String): Boolean
}
