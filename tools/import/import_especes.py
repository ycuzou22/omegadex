"""T04.1 — Espèces, talents et évolutions (source : PokéAPI, état de la 6e génération)."""

from __future__ import annotations

import sqlite3
from collections import defaultdict

import types_pokemon
from source_pokeapi import (
    DERNIERE_ESPECE_GEN6,
    GENERATION_ROSA,
    POKEDEX_HOENN_ROSA,
    VERSION_GROUP_ROSA,
    lire,
    noms_fr,
    texte_fr,
    vide,
)

ORDRE_STATS = ["1", "2", "3", "4", "5", "6"]  # PV, Attaque, Défense, Atq. Spé., Déf. Spé., Vitesse


def _valeurs_passees(table: str, cle: str = "slot") -> dict[str, dict[str, dict[str, str]]]:
    """Valeurs d'avant un changement de génération : pour chaque Pokémon, slot -> ligne valable en 6G.

    Une ligne (pokemon, génération g, slot) donne la valeur portée jusqu'à la génération g incluse.
    Pour Rubis Oméga, on garde la plus petite génération g >= 6.
    """
    choix: dict[str, dict[str, tuple[int, dict[str, str]]]] = defaultdict(dict)
    for r in lire(table):
        g = int(r["generation_id"])
        if g < GENERATION_ROSA:
            continue
        actuel = choix[r["pokemon_id"]].get(r[cle])
        if actuel is None or g < actuel[0]:
            choix[r["pokemon_id"]][r[cle]] = (g, r)
    return {p: {s: r for s, (_, r) in slots.items()} for p, slots in choix.items()}


def _types(pokemon_id: str, actuels: dict, passes: dict, ids_types: dict) -> list[str]:
    slots = {r["slot"]: r for r in actuels.get(pokemon_id, [])}
    slots.update(passes.get(pokemon_id, {}))
    return [ids_types[r["type_id"]] for s, r in sorted(slots.items()) if not vide(r["type_id"])]


def _ev(stats: list[dict[str, str]], noms_stats: dict[str, str]) -> str:
    return ", ".join(f"{r['effort']} {noms_stats[r['stat_id']]}" for r in stats if r["effort"] not in ("", "0"))


def importer(con: sqlite3.Connection) -> dict[str, int]:
    especes = {r["id"]: r for r in lire("pokemon_species") if int(r["id"]) <= DERNIERE_ESPECE_GEN6}
    pokemon = {r["species_id"]: r for r in lire("pokemon") if r["is_default"] == "1" and r["species_id"] in especes}
    noms = {r["pokemon_species_id"]: r for r in lire("pokemon_species_names") if r["local_language_id"] == "5"}
    hoenn = {r["species_id"]: int(r["pokedex_number"]) for r in lire("pokemon_dex_numbers") if r["pokedex_id"] == POKEDEX_HOENN_ROSA}
    ids_types = types_pokemon.par_id()
    types_actuels: dict[str, list] = defaultdict(list)
    for r in lire("pokemon_types"):
        types_actuels[r["pokemon_id"]].append(r)
    types_passes = _valeurs_passees("pokemon_types_past")
    stats: dict[str, dict[str, dict]] = defaultdict(dict)
    for r in lire("pokemon_stats"):
        if r["stat_id"] in ORDRE_STATS:
            stats[r["pokemon_id"]][r["stat_id"]] = r
    for pid, valeurs in _valeurs_passees("pokemon_stats_past", cle="stat_id").items():
        for stat_id, r in valeurs.items():
            if stat_id in ORDRE_STATS:
                stats[pid][stat_id] = {**stats[pid][stat_id], "base_stat": r["base_stat"], "effort": r["effort"]}
    noms_stats = noms_fr("stat_names", "stat_id")
    oeufs: dict[str, list[str]] = defaultdict(list)
    noms_oeufs = noms_fr("egg_group_prose", "egg_group_id")
    for r in lire("pokemon_egg_groups"):
        oeufs[r["species_id"]].append(noms_oeufs[r["egg_group_id"]])
    megas = {
        r["pokemon_id"]
        for r in lire("pokemon_forms")
        if r["is_mega"] == "1" and int(r["introduced_in_version_group_id"]) <= int(VERSION_GROUP_ROSA)
    }
    especes_mega = {p["species_id"] for p in lire("pokemon") if p["id"] in megas}

    for sid, espece in especes.items():
        p = pokemon[sid]
        st = sorted(stats[p["id"]].values(), key=lambda r: int(r["stat_id"]))
        t = _types(p["id"], types_actuels, types_passes, ids_types)
        genre = int(espece["gender_rate"])
        con.execute(
            "INSERT INTO espece VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            (
                int(sid),
                hoenn.get(sid),
                noms[sid]["name"],
                t[0],
                t[1] if len(t) > 1 else None,
                noms[sid]["genus"],
                int(p["height"]) / 10,
                int(p["weight"]) / 10,
                *[int(r["base_stat"]) for r in st],
                _ev(st, noms_stats),
                int(espece["capture_rate"]),
                None if genre < 0 else (8 - genre) / 8,
                ",".join(oeufs[sid]),
                sid in especes_mega,
            ),
        )

    nb_talents = _importer_talents(con, especes, pokemon)
    nb_evolutions = _importer_evolutions(con, especes)
    return {"especes": len(especes), "hoenn": sum(1 for s in especes if s in hoenn), "talents": nb_talents, "evolutions": nb_evolutions}


def _importer_talents(con: sqlite3.Connection, especes: dict, pokemon: dict) -> int:
    gen6 = {r["id"] for r in lire("abilities") if int(r["generation_id"]) <= GENERATION_ROSA and r["is_main_series"] == "1"}
    noms = noms_fr("ability_names", "ability_id")
    descriptions = texte_fr("ability_flavor_text", "ability_id")
    actuels: dict[str, dict[str, dict]] = defaultdict(dict)
    for r in lire("pokemon_abilities"):
        actuels[r["pokemon_id"]][r["slot"]] = r
    passes = _valeurs_passees("pokemon_abilities_past")
    utilises: set[str] = set()
    liens: set[tuple[int, int, bool]] = set()
    for sid in especes:
        pid = pokemon[sid]["id"]
        slots = dict(actuels.get(pid, {}))
        slots.update(passes.get(pid, {}))
        for r in slots.values():
            if vide(r["ability_id"]) or r["ability_id"] not in gen6:
                continue
            utilises.add(r["ability_id"])
            liens.add((int(sid), int(r["ability_id"]), r["is_hidden"] == "1"))
    for aid in sorted(utilises, key=int):
        con.execute("INSERT INTO talent VALUES (?,?,?)", (int(aid), noms[aid], descriptions.get(aid, "")))
    # Un même talent ne peut figurer qu'une fois par espèce (clé espèce + talent) : le slot normal l'emporte.
    par_cle: dict[tuple[int, int], bool] = {}
    for espece, talent, cache in sorted(liens):
        par_cle[(espece, talent)] = par_cle.get((espece, talent), True) and cache
    con.executemany("INSERT INTO espece_talent VALUES (?,?,?)", [(e, t, c) for (e, t), c in par_cle.items()])
    return len(utilises)


PIERRES = {
    "fire-stone", "water-stone", "thunder-stone", "leaf-stone", "moon-stone",
    "sun-stone", "shiny-stone", "dusk-stone", "dawn-stone", "oval-stone",
}  # fmt: skip


def _condition(
    r: dict, items: dict, items_fr: dict, lieux_fr: dict, attaques_fr: dict, declencheurs: dict, noms_types: dict, noms_especes: dict
) -> tuple[str, str | None]:
    declencheur = declencheurs[r["evolution_trigger_id"]]
    details = []
    if not vide(r["time_of_day"]):
        details.append({"day": "de jour", "night": "de nuit"}.get(r["time_of_day"], r["time_of_day"]))
    if declencheur == "trade":
        if not vide(r["held_item_id"]):
            return "ECHANGE", f"en tenant {items_fr[r['held_item_id']]}"
        return "ECHANGE", None
    if declencheur == "use-item":
        objet = items[r["trigger_item_id"]]
        return ("PIERRE" if objet in PIERRES else "OBJET"), items_fr[r["trigger_item_id"]]
    if not vide(r["minimum_level"]):
        valeur = r["minimum_level"]
        if not vide(r["relative_physical_stats"]):
            valeur += {"1": " (Attaque > Défense)", "-1": " (Attaque < Défense)", "0": " (Attaque = Défense)"}[r["relative_physical_stats"]]
        if not vide(r["gender_id"]):
            valeur += " (femelle)" if r["gender_id"] == "1" else " (mâle)"
        if details:
            valeur += " " + " ".join(details)
        if not vide(r["party_type_id"]):
            valeur += f" avec un Pokémon {noms_types[r['party_type_id']]} dans l'équipe"
        if not vide(r["needs_overworld_rain"]) and r["needs_overworld_rain"] == "1":
            valeur += " sous la pluie"
        if r.get("turn_upside_down") == "1":
            valeur += " console retournée"
        return "NIVEAU", valeur
    if not vide(r["minimum_happiness"]):
        return "BONHEUR", " ".join(details) or None
    if not vide(r["location_id"]):
        return "LIEU", lieux_fr.get(r["location_id"])
    if not vide(r["held_item_id"]):
        return "OBJET", f"{items_fr[r['held_item_id']]} tenu" + (f" {details[0]}" if details else "")
    if not vide(r["known_move_id"]):
        return "AUTRE", f"connaître {attaques_fr[r['known_move_id']]}"
    if not vide(r["known_move_type_id"]):
        texte = f"connaître une attaque de type {noms_types[r['known_move_type_id']]}"
        if not vide(r["minimum_affection"]):
            texte += f" et {r['minimum_affection']} cœurs d'affection (Poké Récré)"
        return "AUTRE", texte
    if not vide(r["party_species_id"]):
        return "AUTRE", f"avec {noms_especes[r['party_species_id']]} dans l'équipe"
    if not vide(r["minimum_beauty"]):
        return "AUTRE", "Beauté maximale"
    if declencheur == "shed":
        return "AUTRE", "place libre et Poké Ball dans le sac"
    return "AUTRE", None


def _importer_evolutions(con: sqlite3.Connection, especes: dict) -> int:
    items = {r["id"]: r["identifier"] for r in lire("items")}
    items_fr = noms_fr("item_names", "item_id")
    lieux = {r["id"]: r["region_id"] for r in lire("locations")}
    lieux_fr = noms_fr("location_names", "location_id")
    attaques_fr = noms_fr("move_names", "move_id")
    declencheurs = {r["id"]: r["identifier"] for r in lire("evolution_triggers")}
    noms_types = noms_fr("type_names", "type_id")
    noms_especes = noms_fr("pokemon_species_names", "pokemon_species_id")
    ordre_vg = {r["id"]: int(r["order"]) for r in lire("version_groups")}
    rosa = ordre_vg[VERSION_GROUP_ROSA]
    lignes = []
    for r in lire("pokemon_evolution"):
        cible = r["evolved_species_id"]
        if cible not in especes or vide(especes[cible]["evolves_from_species_id"]):
            continue
        if not vide(r["version_group_id"]) and ordre_vg[r["version_group_id"]] > rosa:
            continue  # méthode apparue après Rubis Oméga
        if not vide(r["location_id"]) and lieux.get(r["location_id"]) != "3":
            continue  # évolution liée à un lieu hors de Hoenn
        if not vide(r["region_id"]) and r["region_id"] != "3":
            continue
        if not vide(r["trigger_item_id"]) and items[r["trigger_item_id"]] in {"thunder-stone", "ice-stone"} and cible in {"462", "476", "470", "471"}:
            continue  # en 6G, Magnézone, Tarinorme, Phyllali et Givrali évoluent par lieu, pas par pierre
        lignes.append((especes[cible]["evolves_from_species_id"], cible, *_condition(r, items, items_fr, lieux_fr, attaques_fr, declencheurs, noms_types, noms_especes)))
    vues = set()
    numero = 0
    for source, cible, condition, valeur in sorted(lignes, key=lambda x: (int(x[1]), x[2], x[3] or "")):
        if (source, cible, condition, valeur) in vues:
            continue
        vues.add((source, cible, condition, valeur))
        numero += 1
        con.execute("INSERT INTO evolution VALUES (?,?,?,?,?)", (numero, int(source), int(cible), condition, valeur))
    return numero
