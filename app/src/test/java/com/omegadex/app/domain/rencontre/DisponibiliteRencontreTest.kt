package com.omegadex.app.domain.rencontre

import com.omegadex.app.domain.model.Lieu
import com.omegadex.app.domain.model.MethodeRencontre
import com.omegadex.app.domain.model.Rencontre
import com.omegadex.app.domain.model.TypeLieu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisponibiliteRencontreTest {
    private val route104 = Lieu(5, "Route 104", TypeLieu.ROUTE, 1)
    private val route119 = Lieu(40, "Route 119", TypeLieu.ROUTE, 6)
    private val horsHoenn = Lieu(78, "Hors de Hoenn (échange ou transfert)", TypeLieu.HORS_HOENN, 10)

    private fun r(lieu: Lieu, espece: Int, methode: MethodeRencontre, condition: String? = null) =
        Rencontre(lieu, espece, methode, 5, 7, 40, condition)

    @Test
    fun `herbes, hordes et Navi-Dex suivent seulement le lieu`() {
        listOf(MethodeRencontre.HERBE, MethodeRencontre.HORDE, MethodeRencontre.NAVI_DEX).forEach {
            assertEquals(Disponibilite(1, null, null), DisponibiliteRencontre.de(r(route104, 263, it, "zone sud")))
        }
    }

    @Test
    fun `cannes, Surf, Plongee et Eclate-Roc attendent leur etape`() {
        fun etape(methode: MethodeRencontre, condition: String? = null) =
            DisponibiliteRencontre.de(r(route104, 129, methode, condition)).let { it.etape to it.debloqueur }
        assertEquals(2 to Debloqueur.CANNE, etape(MethodeRencontre.PECHE, "zone sud; Canne"))
        assertEquals(6 to Debloqueur.SUPER_CANNE, etape(MethodeRencontre.PECHE, "zone nord; Super Canne"))
        assertEquals(7 to Debloqueur.MEGA_CANNE, etape(MethodeRencontre.PECHE, "Méga Canne"))
        assertEquals(6 to Debloqueur.SURF, etape(MethodeRencontre.SURF))
        assertEquals(8 to Debloqueur.PLONGEE, etape(MethodeRencontre.PLONGEE))
        assertEquals(4 to Debloqueur.ECLATE_ROC, etape(MethodeRencontre.ROCHE_FRACASSEE))
        assertEquals(8 to Debloqueur.PLONGEE, etape(MethodeRencontre.HERBE, "CS07 Plongée requise"))
    }

    @Test
    fun `un lieu atteint apres la methode n'a pas de debloqueur`() {
        val surf = DisponibiliteRencontre.de(r(route119, 72, MethodeRencontre.SURF))
        assertEquals(6, surf.etape)
        assertNull(surf.debloqueur)
        val megaCanne = DisponibiliteRencontre.de(r(route119, 129, MethodeRencontre.PECHE, "Méga Canne"))
        assertEquals(7 to Debloqueur.MEGA_CANNE, megaCanne.etape to megaCanne.debloqueur)
    }

    @Test
    fun `apres Groudon ou Kyogre, la condition la plus tardive l'emporte`() {
        val naviDex = r(route104, 16, MethodeRencontre.NAVI_DEX, "zone nord; après avoir vaincu Groudon ou Kyogre")
        val dispo = DisponibiliteRencontre.de(naviDex)
        assertEquals(9 to Debloqueur.APRES_GROUDON, dispo.etape to dispo.debloqueur)
        val capture = r(route104, 16, MethodeRencontre.HERBE, "sous-sol; Après avoir capturé Groudon ou Kyogre")
        assertEquals(9, DisponibiliteRencontre.de(capture).etape)
        assertFalse(DisponibiliteRencontre.possibleA(naviDex, 8))
        assertTrue(DisponibiliteRencontre.possibleA(naviDex, 9))
    }

    @Test
    fun `echanges Saphir Alpha a leur lieu, especes hors de Hoenn avec le Pokedex National`() {
        val seviper = DisponibiliteRencontre.de(r(route104, 336, MethodeRencontre.HERBE, "Saphir Alpha uniquement"))
        assertEquals(Disponibilite(1, null, Echange.SAPHIR_ALPHA), seviper)
        val bulbizarre = DisponibiliteRencontre.de(r(horsHoenn, 1, MethodeRencontre.ECHANGE_REQUIS))
        assertEquals(Disponibilite(11, Debloqueur.POKEDEX_NATIONAL, Echange.HORS_HOENN), bulbizarre)
    }

    @Test
    fun `les rencontres d'une etape sont groupees par lieu et par ce qui les debloque`() {
        val rencontres = listOf(
            r(route104, 263, MethodeRencontre.HERBE),
            r(route104, 279, MethodeRencontre.SURF, "zone sud"),
            r(route104, 279, MethodeRencontre.SURF, "zone nord"),
            r(route104, 129, MethodeRencontre.PECHE, "Super Canne"),
            r(route104, 129, MethodeRencontre.PECHE, "Méga Canne"),
            r(route119, 357, MethodeRencontre.HERBE, "longues herbes"),
            r(route119, 336, MethodeRencontre.HERBE, "Saphir Alpha uniquement"),
        )
        val groupes = RencontresDeLEtapeUseCase.grouper(rencontres, 6)
        assertEquals(
            listOf("Route 119" to null, "Route 104" to Debloqueur.SURF, "Route 104" to Debloqueur.SUPER_CANNE),
            groupes.map { it.lieu.nom to it.debloqueur },
        )
        assertEquals(
            listOf(357 to null, 336 to Echange.SAPHIR_ALPHA),
            groupes[0].rencontres.map { it.rencontre.especeId to it.echange },
        )
        assertEquals(listOf(279), groupes[1].rencontres.map { it.rencontre.especeId })
        val etape1 = RencontresDeLEtapeUseCase.grouper(rencontres, 1).single()
        assertEquals(263, etape1.rencontres.single().rencontre.especeId)
    }
}
