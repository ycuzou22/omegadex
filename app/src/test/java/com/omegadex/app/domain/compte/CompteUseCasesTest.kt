package com.omegadex.app.domain.compte

import com.omegadex.app.core.Horloge
import com.omegadex.app.testutil.FausseSession
import com.omegadex.app.testutil.FauxComptes
import com.omegadex.app.testutil.FauxHacheur
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompteUseCasesTest {
    private var maintenant = 0L
    private val comptes = FauxComptes()
    private val session = FausseSession()
    private val hacheur = FauxHacheur()
    private val limiteur = LimiteurTentatives(Horloge { maintenant })
    private val inscrire = InscrireUseCase(comptes, hacheur, session)
    private val connecter = ConnecterUseCase(comptes, hacheur, session, limiteur)

    private fun mdp(texte: String) = texte.toCharArray()

    @Test
    fun `inscription valide puis connexion avec le bon mot de passe`() = runTest {
        val inscription = inscrire("Yamao", mdp("rubis-omega"), mdp("rubis-omega"))
        assertTrue(inscription is ResultatInscription.Reussie)
        session.fermer()

        assertEquals(ResultatConnexion.Reussie(1), connecter("yamao", mdp("rubis-omega")))
        assertEquals(1L, session.utilisateurConnecte.value)
    }

    @Test
    fun `connexion impossible avec un mauvais mot de passe`() = runTest {
        inscrire("Yamao", mdp("rubis-omega"), mdp("rubis-omega"))
        session.fermer()

        assertEquals(ResultatConnexion.IdentifiantsIncorrects, connecter("Yamao", mdp("saphir-alpha")))
        assertEquals(ResultatConnexion.IdentifiantsIncorrects, connecter("Inconnu", mdp("rubis-omega")))
        assertNull(session.utilisateurConnecte.value)
    }

    @Test
    fun `chaque regle d'inscription a son erreur`() = runTest {
        val refus = inscrire("yo", mdp("court"), mdp("autre"))
        assertEquals(
            ResultatInscription.Refusee(
                setOf(
                    ErreurInscription.IDENTIFIANT_INVALIDE,
                    ErreurInscription.MOT_DE_PASSE_TROP_COURT,
                    ErreurInscription.CONFIRMATION_DIFFERENTE,
                ),
            ),
            refus,
        )
        inscrire("Yamao", mdp("rubis-omega"), mdp("rubis-omega"))
        assertEquals(
            ResultatInscription.Refusee(setOf(ErreurInscription.IDENTIFIANT_PRIS)),
            inscrire("YAMAO", mdp("12345678"), mdp("12345678")),
        )
    }

    @Test
    fun `trop d'essais bloquent temporairement le compte`() = runTest {
        inscrire("Yamao", mdp("rubis-omega"), mdp("rubis-omega"))
        repeat(LimiteurTentatives.ESSAIS_LIBRES - 1) {
            assertEquals(ResultatConnexion.IdentifiantsIncorrects, connecter("Yamao", mdp("mauvais")))
        }
        assertEquals(ResultatConnexion.IdentifiantsIncorrects, connecter("Yamao", mdp("mauvais")))
        assertEquals(ResultatConnexion.TropDeTentatives(30), connecter("Yamao", mdp("mauvais")))
        // Même le bon mot de passe est refusé pendant l'attente.
        assertTrue(connecter("Yamao", mdp("rubis-omega")) is ResultatConnexion.TropDeTentatives)
        maintenant += 31_000
        assertEquals(ResultatConnexion.Reussie(1), connecter("Yamao", mdp("rubis-omega")))
    }
}
