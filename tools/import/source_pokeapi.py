"""Accès aux données de PokéAPI (https://github.com/PokeAPI/pokeapi), lues dans ses fichiers CSV.

Les CSV sont récupérés par git (sparse checkout du dossier data/v2/csv) à un commit figé, puis
mis en cache dans tools/import/.cache : l'import est reproductible et fonctionne hors ligne ensuite.
Licence : code PokéAPI sous BSD-3-Clause ; données issues de veekun/pokedex, citées dans docs/sources-donnees.md.
"""

from __future__ import annotations

import csv
import functools
import pathlib
import subprocess

DEPOT = "https://github.com/PokeAPI/pokeapi"
COMMIT = "2bf639e5572812a67d2b94903fdcb9d095f5aeb2"
CACHE = pathlib.Path(__file__).resolve().parent / ".cache" / "pokeapi"
CSV = CACHE / "data" / "v2" / "csv"

FRANCAIS = "5"
VERSION_GROUP_ROSA = "16"  # omega-ruby-alpha-sapphire
GENERATION_ROSA = 6
POKEDEX_HOENN_ROSA = "15"  # updated-hoenn, 211 espèces
DERNIERE_ESPECE_GEN6 = 721


def _git(*args: str, cwd: pathlib.Path | None = None) -> None:
    subprocess.run(["git", *args], cwd=cwd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def preparer() -> None:
    """Télécharge (une seule fois) les CSV de PokéAPI au commit figé."""
    if (CSV / "pokemon.csv").exists():
        return
    CACHE.parent.mkdir(parents=True, exist_ok=True)
    _git("clone", "--filter=blob:none", "--sparse", "--no-checkout", DEPOT, str(CACHE))
    _git("sparse-checkout", "set", "data/v2/csv", cwd=CACHE)
    _git("checkout", COMMIT, cwd=CACHE)


@functools.cache
def lire(nom: str) -> list[dict[str, str]]:
    preparer()
    with open(CSV / f"{nom}.csv", encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def noms_fr(table: str, cle: str, colonne: str = "name") -> dict[str, str]:
    """Noms français d'une table *_names (ou *_prose), indexés par identifiant."""
    return {r[cle]: r[colonne] for r in lire(table) if r["local_language_id"] == FRANCAIS}


def texte_fr(table: str, cle: str) -> dict[str, str]:
    """Texte d'un *_flavor_text en français, de Rubis Oméga en priorité, sinon le plus récent."""
    textes: dict[str, tuple[int, str]] = {}
    for r in lire(table):
        if r["language_id"] != FRANCAIS:
            continue
        rang = 10_000 if r["version_group_id"] == VERSION_GROUP_ROSA else int(r["version_group_id"])
        if r[cle] not in textes or rang > textes[r[cle]][0]:
            textes[r[cle]] = (rang, " ".join(r["flavor_text"].split()))
    return {k: v for k, (_, v) in textes.items()}


def vide(valeur: str) -> bool:
    return valeur == ""
