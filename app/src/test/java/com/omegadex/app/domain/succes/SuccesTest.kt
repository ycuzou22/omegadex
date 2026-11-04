package com.omegadex.app.domain.succes

import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.domain.model.StatutEspece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuccesTest {
    private fun etape(id: Int, validee: Boolean) = ProgressionEtape(id, true, 0L, if (validee) 1L else null)

    private fun defi(id: Long, etat: EtatDefi) = Defi(id, 1, null, null, etat, 0L, null)

    @Test
    fun `les mesures comptent captures, maitrises, evolutions, badges, defis et equipe`() {
        val statuts = (1..12).associateWith { StatutEspece.CAPTUREE } +
            mapOf(20 to StatutEspece.MAITRISEE, 21 to StatutEspece.EVOLUEE, 22 to StatutEspece.VUE)
        val mesures = MesuresSucces.de(
            statuts,
            listOf(etape(0, true), etape(1, true), etape(2, true), etape(3, false)),
            listOf(defi(1, EtatDefi.REUSSI), defi(2, EtatDefi.RATE), defi(3, EtatDefi.ACTIF)),
            (1..6).map { MembreEquipe(it, 250 + it, null, 10) },
        )

        assertEquals(MesuresSucces(14, 1, 1, 2, setOf(0, 1, 2), 1, 6), mesures)
        assertEquals(
            setOf(Succes.CAPTURES_10, Succes.MAITRISES_1, Succes.BADGES_1, Succes.DEFIS_1, Succes.EQUIPE_COMPLETE),
            mesures.atteints(),
        )
    }

    @Test
    fun `chaque palier est atteint a son seuil exact`() {
        assertFalse(MesuresSucces(captures = 9).atteint(Succes.CAPTURES_10))
        assertTrue(MesuresSucces(captures = 10).atteint(Succes.CAPTURES_10))
        val maitrises = Succes.entries.filter { it.famille == FamilleSucces.MAITRISES }
        assertTrue(MesuresSucces(maitrises = 50).atteints().containsAll(maitrises))
        assertEquals(setOf(Succes.BADGES_1, Succes.BADGES_4, Succes.BADGES_8), MesuresSucces(badges = 8).atteints())
    }

    @Test
    fun `la Ligue et la fin de l'aventure dependent des etapes validees`() {
        val avantLigue = MesuresSucces(etapesValidees = (0..9).toSet())
        assertFalse(avantLigue.atteint(Succes.LIGUE))
        assertEquals(9, avantLigue.valeur(Succes.LIGUE))
        assertTrue(MesuresSucces(etapesValidees = (0..10).toSet()).atteint(Succes.LIGUE))
        assertFalse(MesuresSucces(etapesValidees = (0..11).toSet()).atteint(Succes.AVENTURE_TERMINEE))
        assertTrue(MesuresSucces(etapesValidees = (0..12).toSet()).atteint(Succes.AVENTURE_TERMINEE))
    }
}
