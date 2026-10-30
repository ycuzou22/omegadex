package com.omegadex.app.domain.repository

import com.omegadex.app.domain.model.Defi
import com.omegadex.app.domain.model.EtatDefi
import com.omegadex.app.domain.model.MembreEquipe
import com.omegadex.app.domain.model.Nature
import com.omegadex.app.domain.model.PlacePokemon
import com.omegadex.app.domain.model.PokemonPc
import com.omegadex.app.domain.model.ProgressionEtape
import com.omegadex.app.domain.model.StatutEspece
import kotlinx.coroutines.flow.Flow

/** Progression d'une run : étapes, quêtes, statuts d'espèces, équipe, PC et défis. */
interface ProgressionRepository {
    fun etapes(runId: Long): Flow<List<ProgressionEtape>>
    suspend fun deverrouillerEtape(runId: Long, etapeId: Int)
    suspend fun validerEtape(runId: Long, etapeId: Int)
    fun quetesCochees(runId: Long): Flow<Set<Int>>
    suspend fun cocherQuete(runId: Long, queteId: Int, cochee: Boolean)

    /** Statut de chaque espèce rencontrée ; une espèce absente est non vue. */
    fun statuts(runId: Long): Flow<Map<Int, StatutEspece>>
    suspend fun definirStatut(runId: Long, especeId: Int, statut: StatutEspece)

    /** Notes libres du joueur sur une espèce (vides par défaut). */
    fun notes(runId: Long, especeId: Int): Flow<String>
    suspend fun definirNotes(runId: Long, especeId: Int, notes: String)

    fun equipe(runId: Long): Flow<List<MembreEquipe>>
    suspend fun placerMembre(runId: Long, membre: MembreEquipe)
    suspend fun retirerMembre(runId: Long, emplacement: Int)

    /** Pokémon rangés dans les boîtes du PC, par boîte puis par ordre d'arrivée. */
    fun pc(runId: Long): Flow<List<PokemonPc>>

    /** Range le membre de l'[emplacement] dans la [boite] du PC ; l'emplacement redevient libre. */
    suspend fun deposerAuPc(runId: Long, emplacement: Int, boite: Int)

    /** Sort le Pokémon [pcId] du PC et le place dans l'[emplacement] de l'équipe, qui doit être libre. */
    suspend fun retirerDuPc(runId: Long, pcId: Long, emplacement: Int)

    /** Nature et talent d'un Pokémon de l'équipe ou du PC ; null efface la valeur. */
    suspend fun definirDetails(runId: Long, place: PlacePokemon, nature: Nature?, talent: String?)

    fun defis(runId: Long): Flow<List<Defi>>
    suspend fun ajouterDefi(runId: Long, modeleId: Int, especeId: Int?, etapeId: Int?): Long
    suspend fun changerEtatDefi(runId: Long, defiId: Long, etat: EtatDefi)
}
