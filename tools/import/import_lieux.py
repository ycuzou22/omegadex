"""T04.3 — Lieux (zones annexes et lieux mirages compris) et rencontres de Rubis Oméga.

Sources :
- donnees/lieux_rosa.json : liste des lieux, leur type et leur étape de déblocage ;
- Poképédia, module « Localisations/Données/Pokémon Rubis Oméga et Saphir Alpha » : pour chaque lieu
  (et chaque zone), les espèces, la méthode, les niveaux et le taux d'apparition.
Une espèce qu'aucun lieu de Hoenn ne propose est rattachée au lieu « Hors de Hoenn » (échange requis).
"""

from __future__ import annotations

import json
import pathlib
import re
import sqlite3

import lua_table
import source_pokepedia

DOSSIER = pathlib.Path(__file__).resolve().parent
MIRAGES = ("Forêt Mirage", "Grotte Mirage", "Mont Mirage", "Île Mirage")
ETAPE_MIRAGES = 12
NOM_HORS_HOENN = "Hors de Hoenn (échange ou transfert)"
ETAPE_POKEDEX_NATIONAL = 10

# Méthode Poképédia (début du libellé « milieu ») → MethodeRencontre de l'app
METHODES = {
    "Hautes herbes": "HERBE",
    "Longues herbes": "HERBE",
    "En marchant": "HERBE",
    "Sable": "HERBE",
    "Vols d'oiseaux": "HERBE",
    "Hordes": "HORDE",
    "Sur l'eau": "SURF",
    "Algues": "PLONGEE",
    "Pêche à la Canne": "PECHE",
    "Pêche à la Super Canne": "PECHE",
    "Pêche à la Méga Canne": "PECHE",
    "Navi-Dex": "NAVI_DEX",
    "Éclate-Roc": "ROCHE_FRACASSEE",
    "Donné": "CADEAU",
    "Dans une Poké Ball": "CADEAU",
    "Fossiles": "CADEAU",
    "Échange": "ECHANGE_EN_JEU",
    "Rencontre": "FIXE",
    "Spécial": "FIXE",
    "Fausses Balls au sol": "FIXE",
}
PRECISIONS_METHODE = {"Pêche à la Canne": "Canne", "Pêche à la Super Canne": "Super Canne", "Pêche à la Méga Canne": "Méga Canne",
                      "Longues herbes": "longues herbes", "Sable": "sable", "Vols d'oiseaux": "vols d'oiseaux",
                      "Fossiles": "fossile ranimé", "Fausses Balls au sol": "fausse Poké Ball au sol"}  # fmt: skip
VERSIONS = {"RuO": "Rubis Oméga uniquement", "SaA": "Saphir Alpha uniquement"}


def texte_brut(wiki: str) -> str:
    """Retire la mise en forme wiki : [[lien|texte]] → texte, <br> → espace, parenthèses superflues."""
    wiki = re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r"\1", wiki)
    wiki = re.sub(r"<br\s*/?>", " ", wiki)
    wiki = " ".join(wiki.split())
    return wiki.strip(" ,")


def methode_et_condition(milieu: str) -> tuple[str, str | None]:
    tete, _, reste = milieu.partition(",")
    tete = tete.strip()
    if tete not in METHODES:
        raise ValueError(f"Méthode de rencontre inconnue : « {milieu} »")
    precisions = [PRECISIONS_METHODE[tete]] if tete in PRECISIONS_METHODE else []
    reste = texte_brut(reste)
    if reste:
        precisions.append(reste.removeprefix("(").removesuffix(")"))
    return METHODES[tete], "; ".join(precisions) or None


def niveaux(texte: str) -> tuple[int | None, int | None]:
    nombres = [int(n) for n in re.findall(r"\d+", texte)]
    return (min(nombres), max(nombres)) if nombres else (None, None)


def taux_et_version(texte: str) -> tuple[int | None, str | None]:
    morceaux = [m.strip() for m in texte.split(",")]
    version = next((VERSIONS[m] for m in morceaux if m in VERSIONS), None)
    nombre = next((int(float(m.replace("%", "").strip())) for m in morceaux if re.fullmatch(r"[\d.]+ ?%?", m)), None)
    return nombre, version


def importer(con: sqlite3.Connection) -> dict[str, int]:
    config = json.loads((DOSSIER / "donnees" / "lieux_rosa.json").read_text(encoding="utf-8"))["lieux"]
    module = lua_table.lire(source_pokepedia.wikitexte(source_pokepedia.MODULE_LOCALISATIONS))
    especes = {nom: eid for eid, nom in con.execute("SELECT id, nom FROM espece")}

    lieux: dict[str, dict] = {}
    for l in config:
        lieux[l.get("cleModule", l["nom"])] = l
    prochain_id = max(l["id"] for l in config) + 1
    rencontres: list[tuple] = []
    inconnues: set[str] = set()

    for cle, par_espece in module.items():
        base, _, zone = cle.partition("@")
        if base in MIRAGES:
            nom = f"{base} ({texte_brut(zone)})"
            if nom not in lieux:
                lieux[nom] = {"id": prochain_id, "nom": nom, "type": "LIEU_MIRAGE", "etape": ETAPE_MIRAGES}
                prochain_id += 1
            lieu = lieux[nom]
        elif base in lieux:
            lieu = lieux[base]
        else:
            raise ValueError(f"Lieu du module absent de lieux_rosa.json : « {base} »")
        for nom_brut, lignes in par_espece.items():
            nom_espece, forme = re.fullmatch(r"(.+?)(?: forme\((.+)\))?", nom_brut).groups()
            if nom_espece not in especes:
                inconnues.add(nom_espece)
                continue
            for ligne in lignes:
                methode, condition = methode_et_condition(ligne["milieu"])
                taux, version = taux_et_version(ligne.get("taux", ""))
                details = [c for c in (f"forme {forme}" if forme else None, condition, version) if c]
                if ligne.get("emplacement"):
                    details.append(texte_brut(ligne["emplacement"]))
                if zone and base not in MIRAGES:
                    details.insert(0, texte_brut(zone))
                rencontres.append((lieu["id"], especes[nom_espece], methode, *niveaux(ligne.get("niveau", "")), taux, "; ".join(details) or None))
    if inconnues:
        raise ValueError(f"Espèces du module introuvables dans la base : {sorted(inconnues)}")

    for l in sorted(lieux.values(), key=lambda x: x["id"]):
        con.execute("INSERT INTO lieu VALUES (?,?,?,?)", (l["id"], l["nom"], l["type"], l["etape"]))
    id_hors_hoenn = prochain_id
    con.execute("INSERT INTO lieu VALUES (?,?,?,?)", (id_hors_hoenn, NOM_HORS_HOENN, "HORS_HOENN", ETAPE_POKEDEX_NATIONAL))

    uniques = sorted(set(rencontres), key=lambda r: (r[0], r[1], r[2], r[3] or 0))
    con.executemany(
        "INSERT INTO rencontre (lieuId, especeId, methode, niveauMin, niveauMax, taux, condition) VALUES (?,?,?,?,?,?,?)",
        uniques,
    )
    presentes = {r[1] for r in uniques}
    obtenables = _famille_complete(con, presentes)
    absentes = [eid for eid in especes.values() if eid not in obtenables]
    con.executemany(
        "INSERT INTO rencontre (lieuId, especeId, methode, niveauMin, niveauMax, taux, condition) VALUES (?,?,?,?,?,?,?)",
        [(id_hors_hoenn, eid, "ECHANGE_REQUIS", None, None, None, None) for eid in sorted(absentes)],
    )
    return {
        "lieux": len(lieux) + 1,
        "rencontres": len(uniques),
        "especes trouvables": len(presentes),
        "obtenables par evolution ou reproduction": len(obtenables) - len(presentes),
        "echange requis": len(absentes),
    }


def _famille_complete(con: sqlite3.Connection, trouvables: set[int]) -> set[int]:
    """Espèces obtenables dans le jeu : trouvables, plus leurs évolutions et pré-évolutions (reproduction)."""
    voisins: dict[int, set[int]] = {}
    for source, cible in con.execute("SELECT especeSourceId, especeCibleId FROM evolution"):
        voisins.setdefault(source, set()).add(cible)
        voisins.setdefault(cible, set()).add(source)
    obtenables, pile = set(trouvables), list(trouvables)
    while pile:
        for v in voisins.get(pile.pop(), ()):
            if v not in obtenables:
                obtenables.add(v)
                pile.append(v)
    return obtenables
