"""Identifiants des types : clés françaises sans accent, partagées avec l'app (CouleursTypes)."""

PAR_IDENTIFIANT_POKEAPI = {
    "normal": "normal",
    "fire": "feu",
    "water": "eau",
    "grass": "plante",
    "electric": "electrik",
    "ice": "glace",
    "fighting": "combat",
    "poison": "poison",
    "ground": "sol",
    "flying": "vol",
    "psychic": "psy",
    "bug": "insecte",
    "rock": "roche",
    "ghost": "spectre",
    "dragon": "dragon",
    "dark": "tenebres",
    "steel": "acier",
    "fairy": "fee",
}


def par_id() -> dict[str, str]:
    from source_pokeapi import lire

    return {r["id"]: PAR_IDENTIFIANT_POKEAPI[r["identifier"]] for r in lire("types") if r["identifier"] in PAR_IDENTIFIANT_POKEAPI}
