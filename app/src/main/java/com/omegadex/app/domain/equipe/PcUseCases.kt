package com.omegadex.app.domain.equipe

import com.omegadex.app.domain.model.Nature
import com.omegadex.app.domain.model.PlacePokemon
import com.omegadex.app.domain.repository.ProgressionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Places d'une boîte du PC. */
const val TAILLE_BOITE = 30

/** Nombre de boîtes du PC. */
const val NOMBRE_BOITES = 8

private const val TAILLE_EQUIPE = 6

/** Résultat d'un déplacement entre l'équipe et le PC. */
sealed interface ResultatPc {
    data object Fait : ResultatPc

    /** L'équipe garde toujours au moins un Pokémon. */
    data object DernierMembre : ResultatPc

    data object BoitePleine : ResultatPc

    data object EquipePleine : ResultatPc

    /** Le Pokémon a déjà quitté l'emplacement ou la boîte (double toucher, autre écran). */
    data object Introuvable : ResultatPc
}

/** Range le membre de l'[emplacement] dans une boîte du PC qui a encore de la place. */
class DeposerAuPcUseCase @Inject constructor(private val progression: ProgressionRepository) {
    suspend operator fun invoke(runId: Long, emplacement: Int, boite: Int): ResultatPc {
        require(boite in 1..NOMBRE_BOITES) { "Boîte $boite hors de 1 à $NOMBRE_BOITES" }
        val equipe = progression.equipe(runId).first()
        if (equipe.none { it.emplacement == emplacement }) return ResultatPc.Introuvable
        if (equipe.size <= 1) return ResultatPc.DernierMembre
        if (progression.pc(runId).first().count { it.boite == boite } >= TAILLE_BOITE) return ResultatPc.BoitePleine
        progression.deposerAuPc(runId, emplacement, boite)
        return ResultatPc.Fait
    }
}

/** Sort un Pokémon du PC vers le premier emplacement libre de l'équipe. */
class RetirerDuPcUseCase @Inject constructor(private val progression: ProgressionRepository) {
    suspend operator fun invoke(runId: Long, pcId: Long): ResultatPc {
        if (progression.pc(runId).first().none { it.id == pcId }) return ResultatPc.Introuvable
        val occupes = progression.equipe(runId).first().map { it.emplacement }.toSet()
        val libre = (1..TAILLE_EQUIPE).firstOrNull { it !in occupes } ?: return ResultatPc.EquipePleine
        progression.retirerDuPc(runId, pcId, libre)
        return ResultatPc.Fait
    }
}

/** Renseigne la nature et le talent d'un Pokémon de l'équipe ou du PC ; un talent vide est effacé. */
class DefinirDetailsUseCase @Inject constructor(private val progression: ProgressionRepository) {
    suspend operator fun invoke(runId: Long, place: PlacePokemon, nature: Nature?, talent: String?) {
        progression.definirDetails(runId, place, nature, talent?.trim()?.takeIf { it.isNotEmpty() })
    }
}
