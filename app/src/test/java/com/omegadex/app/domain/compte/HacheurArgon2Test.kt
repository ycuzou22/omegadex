package com.omegadex.app.domain.compte

import com.omegadex.app.data.securite.HacheurArgon2
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HacheurArgon2Test {
    private val hacheur = HacheurArgon2()

    @Test
    fun `l'empreinte verifie le bon mot de passe uniquement`() {
        val sel = hacheur.nouveauSel()
        val empreinte = hacheur.hacher("rubis-omega".toCharArray(), sel)
        assertTrue(empreinte.startsWith("argon2id\$v=19\$m=19456,t=2,p=1\$"))
        assertTrue(hacheur.verifier("rubis-omega".toCharArray(), sel, empreinte))
        assertFalse(hacheur.verifier("rubis-omegA".toCharArray(), sel, empreinte))
        assertFalse(hacheur.verifier("rubis-omega".toCharArray(), sel, "empreinte-trafiquée"))
    }
}
