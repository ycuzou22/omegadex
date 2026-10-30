"""Étape à partir de laquelle un défi « Monotype » devient réalisable, d'après les espèces obtenables.

Un défi Monotype demande de battre l'adversaire de l'étape uniquement avec des Pokémon d'un type : il n'est
proposé que lorsqu'au moins [FAMILLES_MIN] familles d'évolution différentes de ce type s'obtiennent
(capture, évolution sous le plafond, reproduction), **sans compter les starters** : le joueur n'en a qu'un.
"""

from __future__ import annotations

import import_banque_defis as banque

FAMILLES_MIN = 2
# Racines des starters de Hoenn (Arcko, Poussifeu, Gobou) : un seul est choisi par run.
STARTERS = {252, 255, 258}
# Un défi Monotype vise l'adversaire de l'étape : jamais avant Roxanne.
ETAPE_MIN = 1


def familles_par_type(donnees: dict, regles: banque.Regles) -> dict[str, list[set[int]]]:
    """Pour chaque type, les familles obtenables à chaque étape (index = étape)."""
    obtention = banque.etapes_d_obtention(donnees, regles)
    racine = {m: r for r, membres in banque.familles(donnees).items() for m in membres}
    etapes = range(banque.DERNIERE_ETAPE + 1)
    resultat: dict[str, list[set[int]]] = {t: [set() for _ in etapes] for t in banque.NOMS_TYPES}
    for espece, etape in obtention.items():
        if racine[espece] in STARTERS:
            continue
        sp = donnees["especes"][espece]
        for t in {sp["type1"], sp["type2"]} - {None}:
            for e in range(etape, banque.DERNIERE_ETAPE + 1):
                resultat[t][e].add(racine[espece])
    return resultat


def etapes_monotype(donnees: dict, regles: banque.Regles) -> dict[str, int | None]:
    """Première étape où chaque type réunit assez de familles ; None si jamais."""
    etapes = {}
    for t, par_etape in familles_par_type(donnees, regles).items():
        possibles = [e for e, familles in enumerate(par_etape) if len(familles) >= FAMILLES_MIN]
        etapes[t] = max(ETAPE_MIN, possibles[0]) if possibles else None
    return etapes
