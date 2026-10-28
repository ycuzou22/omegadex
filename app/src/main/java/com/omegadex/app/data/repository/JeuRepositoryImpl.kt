package com.omegadex.app.data.repository

import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.mapper.versDomaine
import com.omegadex.app.domain.repository.JeuRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class JeuRepositoryImpl @Inject constructor(base: JeuDatabase) : JeuRepository {
    private val etapes = base.etapeDao()
    private val especes = base.especeDao()
    private val attaques = base.attaqueDao()
    private val rencontres = base.rencontreDao()

    override fun etapes() = etapes.etapes().map { liste -> liste.map { it.versDomaine() } }

    override fun etape(id: Int) = etapes.etape(id).map { it?.versDomaine() }

    override fun quetes(etapeId: Int) = etapes.quetes(etapeId).map { liste -> liste.map { it.versDomaine() } }

    override fun lieuxDebloques(etapeId: Int) = etapes.lieuxDebloques(etapeId).map { liste ->
        liste.map { it.versDomaine() }
    }

    override fun pokedexHoenn() = especes.pokedexHoenn().map { liste -> liste.map { it.versDomaine() } }

    override fun pokedexNational() = especes.pokedexNational().map { liste -> liste.map { it.versDomaine() } }

    override fun espece(id: Int) = especes.espece(id).map { it?.versDomaine() }

    override fun talents(especeId: Int) = especes.talents(especeId).map { liste -> liste.map { it.versDomaine() } }

    override fun famille(especeId: Int) = especes.famille(especeId).map { liste -> liste.map { it.versDomaine() } }

    override fun attaquesApprises(especeId: Int) =
        attaques.attaquesApprises(especeId).map { liste -> liste.map { it.versDomaine() } }

    override fun lieuxDeLEspece(especeId: Int) =
        rencontres.lieuxDeLEspece(especeId).map { liste -> liste.map { it.versDomaine() } }

    override fun especesDuLieu(lieuId: Int) = rencontres.especesDuLieu(lieuId).map { liste ->
        liste.map { it.versDomaine() }
    }

    override fun especesAccessibles(etapeId: Int) = rencontres.especesAccessibles(etapeId).map { it.toSet() }
}
