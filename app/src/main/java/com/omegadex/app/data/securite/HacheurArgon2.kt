package com.omegadex.app.data.securite

import com.omegadex.app.domain.compte.HacheurMotDePasse
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/**
 * Argon2id (recommandation OWASP : 19 Mio de mémoire, 2 passes, 1 fil).
 * L'empreinte est écrite au format `argon2id$v=19$m=19456,t=2,p=1$<hash Base64>` : les paramètres
 * voyagent avec elle, ce qui permettra de les renforcer plus tard sans casser les comptes existants.
 */
class HacheurArgon2 @Inject constructor() : HacheurMotDePasse {
    private val aleatoire = SecureRandom()

    override fun nouveauSel(): String = encoder(ByteArray(TAILLE_SEL).also(aleatoire::nextBytes))

    override fun hacher(motDePasse: CharArray, sel: String): String =
        hacher(motDePasse, sel, MEMOIRE_KIO, ITERATIONS, PARALLELISME)

    override fun verifier(motDePasse: CharArray, sel: String, empreinte: String): Boolean {
        val parametres = PARAMETRES.matchEntire(empreinte) ?: return false
        val (memoire, iterations, parallelisme) = parametres.destructured
        val recalculee = hacher(motDePasse, sel, memoire.toInt(), iterations.toInt(), parallelisme.toInt())
        return MessageDigest.isEqual(recalculee.toByteArray(), empreinte.toByteArray())
    }

    private fun hacher(motDePasse: CharArray, sel: String, memoire: Int, iterations: Int, parallelisme: Int): String {
        val generateur = Argon2BytesGenerator()
        generateur.init(
            Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(Base64.getDecoder().decode(sel))
                .withMemoryAsKB(memoire)
                .withIterations(iterations)
                .withParallelism(parallelisme)
                .build(),
        )
        val sortie = ByteArray(TAILLE_HASH)
        generateur.generateBytes(motDePasse, sortie)
        return "argon2id\$v=19\$m=$memoire,t=$iterations,p=$parallelisme\$${encoder(sortie)}"
    }

    private fun encoder(octets: ByteArray) = Base64.getEncoder().withoutPadding().encodeToString(octets)

    private companion object {
        const val TAILLE_SEL = 16
        const val TAILLE_HASH = 32
        const val MEMOIRE_KIO = 19_456
        const val ITERATIONS = 2
        const val PARALLELISME = 1
        val PARAMETRES = Regex("argon2id\\\$v=19\\\$m=(\\d+),t=(\\d+),p=(\\d+)\\\$[A-Za-z0-9+/]+")
    }
}
