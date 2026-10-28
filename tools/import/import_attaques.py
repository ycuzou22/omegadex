"""T04.2 — Attaques et apprentissages, version Rubis Oméga / Saphir Alpha (source : PokéAPI)."""

from __future__ import annotations

import sqlite3

import types_pokemon
from source_pokeapi import DERNIERE_ESPECE_GEN6, GENERATION_ROSA, VERSION_GROUP_ROSA, lire, noms_fr, texte_fr, vide

CATEGORIES = {"1": "STATUT", "2": "PHYSIQUE", "3": "SPECIALE"}
METHODES = {"level-up": "NIVEAU", "egg": "REPRODUCTION", "tutor": "DONNEUR", "machine": "CT_CS"}
NOMBRE_CT = 100  # au-delà, ce sont des CS


def _valeurs_rosa() -> dict[str, dict[str, str]]:
    """Attaques de la 6G avec les valeurs de Rubis Oméga (avant les changements des jeux suivants)."""
    ordre_vg = {r["id"]: int(r["order"]) for r in lire("version_groups")}
    rosa = ordre_vg[VERSION_GROUP_ROSA]
    attaques = {r["id"]: dict(r) for r in lire("moves") if int(r["generation_id"]) <= GENERATION_ROSA}
    changements: dict[str, tuple[int, dict]] = {}
    for r in lire("move_changelog"):
        ordre = ordre_vg[r["changed_in_version_group_id"]]
        if r["move_id"] in attaques and ordre > rosa and (r["move_id"] not in changements or ordre < changements[r["move_id"]][0]):
            changements[r["move_id"]] = (ordre, r)
    for mid, (_, r) in changements.items():
        for champ in ("type_id", "power", "pp", "accuracy"):
            if not vide(r[champ]):
                attaques[mid][champ] = r[champ]
    return attaques


def importer(con: sqlite3.Connection) -> dict[str, int]:
    attaques = _valeurs_rosa()
    noms = noms_fr("move_names", "move_id")
    descriptions = texte_fr("move_flavor_text", "move_id")
    ids_types = types_pokemon.par_id()
    for mid, a in sorted(attaques.items(), key=lambda x: int(x[0])):
        if a["type_id"] not in ids_types:
            continue  # type « ??? » (Malédiction en 4G) ou Ombre de Colosseum
        con.execute(
            "INSERT INTO attaque VALUES (?,?,?,?,?,?,?,?)",
            (
                int(mid),
                noms[mid],
                ids_types[a["type_id"]],
                CATEGORIES[a["damage_class_id"]],
                None if vide(a["power"]) else int(a["power"]),
                None if vide(a["accuracy"]) else int(a["accuracy"]),
                int(a["pp"]),
                descriptions.get(mid, ""),
            ),
        )
    presentes = {r[0] for r in con.execute("SELECT id FROM attaque")}

    pokemon = {r["id"]: int(r["species_id"]) for r in lire("pokemon") if r["is_default"] == "1" and int(r["species_id"]) <= DERNIERE_ESPECE_GEN6}
    methodes = {r["id"]: METHODES.get(r["identifier"]) for r in lire("pokemon_move_methods")}
    machines = {r["move_id"]: int(r["machine_number"]) for r in lire("machines") if r["version_group_id"] == VERSION_GROUP_ROSA}
    lignes = set()
    for r in lire("pokemon_moves"):
        if r["version_group_id"] != VERSION_GROUP_ROSA or r["pokemon_id"] not in pokemon:
            continue
        methode = methodes.get(r["pokemon_move_method_id"])
        if methode is None or int(r["move_id"]) not in presentes:
            continue
        niveau = int(r["level"] or 0) if methode == "NIVEAU" else 0
        numero = None
        if methode == "CT_CS" and r["move_id"] in machines:
            n = machines[r["move_id"]]
            numero = f"CT{n:02d}" if n <= NOMBRE_CT else f"CS{n - NOMBRE_CT:02d}"
        lignes.add((pokemon[r["pokemon_id"]], int(r["move_id"]), methode, niveau, numero))
    con.executemany("INSERT OR IGNORE INTO attaque_apprise VALUES (?,?,?,?,?)", sorted(lignes, key=lambda x: (x[0], x[2], x[3], x[1])))
    especes_sans = con.execute("SELECT COUNT(*) FROM espece WHERE id NOT IN (SELECT especeId FROM attaque_apprise)").fetchone()[0]
    return {"attaques": len(presentes), "apprentissages": len(lignes), "especes sans attaque": especes_sans}
