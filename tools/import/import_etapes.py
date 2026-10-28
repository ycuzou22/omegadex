"""T04.4 — Les 13 étapes, leurs quêtes et le plafond de niveau calculé à partir des équipes des champions.

- donnees/etapes_rosa.json : étapes et quêtes principales (progression de Rubis Oméga) ;
- Poképédia : équipe du champion de chaque étape (modèle {{Tableau Dresseur}} avec jeu=ROSA),
  le plafond est le niveau de son Pokémon le plus fort ;
- quêtes annexes générées : capturer chaque espèce sauvage la première fois qu'elle devient disponible.
"""

from __future__ import annotations

import json
import pathlib
import re
import sqlite3

import source_pokepedia

DOSSIER = pathlib.Path(__file__).resolve().parent
METHODES_CAPTURE = ("HERBE", "HORDE", "SURF", "PLONGEE", "PECHE", "NAVI_DEX", "ROCHE_FRACASSEE", "FIXE")
# Une condition de rencontre peut repousser l'étape où la capture devient possible.
CONDITIONS_ETAPE = [("Groudon", 8), ("Pokédex National", 10), ("Ligue", 10), ("Delta", 11)]
# Étape à partir de laquelle chaque méthode est utilisable (CS ou canne obtenue à l'étape précédente).
METHODES_ETAPE = {"SURF": 6, "PLONGEE": 8, "ROCHE_FRACASSEE": 3}
CANNES_ETAPE = [("Méga Canne", 7), ("Super Canne", 3), ("Canne", 2)]


def combats_rosa(wiki: str, section: str | None = None) -> list[list[int]]:
    """Niveaux des Pokémon de chaque {{Tableau Dresseur}} de Rubis Oméga, dans l'ordre de la page.

    Avec [section], ne garde que les combats placés sous un titre qui contient ce texte (« Caverne Fondmer »).
    """
    combats = []
    for m in re.finditer(r"\{\{Tableau Dresseur(.*?)\n\}\}", wiki, re.S):
        bloc = m.group(1)
        if not re.search(r"\|\s*jeu\s*=\s*ROSA\b", bloc):
            continue
        if section:
            titres = re.findall(r"^=+([^=\n]+)=+\s*$", wiki[: m.start()], re.M)
            if not titres or section not in titres[-1]:
                continue
        niveaux = [int(n) for n in re.findall(r"\|\s*p\d+-niveau\s*=\s*(\d+)", bloc)]
        if niveaux:
            combats.append(niveaux)
    return combats


def plafond(page: str, selection: str, section: str | None = None) -> tuple[int, int]:
    """Plafond (niveau max) et nombre de combats retenus pour le dresseur de la page."""
    combats = combats_rosa(source_pokepedia.wikitexte(page), section)
    if not combats:
        raise ValueError(f"Aucune équipe de Rubis Oméga sur la page Poképédia « {page} »")
    return (max(combats[0]) if selection == "premier" else max(max(c) for c in combats)), len(combats)


def etape_de_capture(etape_lieu: int, methode: str, condition: str | None) -> int:
    etape = max(etape_lieu, METHODES_ETAPE.get(methode, 0))
    if methode == "PECHE":
        etape = max(etape, next((e for canne, e in CANNES_ETAPE if condition and canne in condition), 2))
    for mot, minimum in CONDITIONS_ETAPE:
        if condition and mot in condition:
            etape = max(etape, minimum)
    return etape


def importer(con: sqlite3.Connection) -> dict[str, int]:
    etapes = json.loads((DOSSIER / "donnees" / "etapes_rosa.json").read_text(encoding="utf-8"))["etapes"]
    lieux = {nom: lid for lid, nom in con.execute("SELECT id, nom FROM lieu")}
    numero = 0
    for e in etapes:
        niveau, _ = plafond(e["page"], e["selection"], e.get("section"))
        con.execute(
            "INSERT INTO etape VALUES (?,?,?,?,?,?)",
            (e["id"], e["titre"], e["adversaire"], e["typeDominant"], niveau, e["lieuPrincipal"]),
        )
        for ordre, q in enumerate(e["quetes"], 1):
            if q["lieu"] not in lieux:
                raise ValueError(f"Étape {e['id']} : lieu inconnu « {q['lieu']} »")
            numero += 1
            con.execute(
                "INSERT INTO quete VALUES (?,?,?,?,?,?,?)",
                (numero, e["id"], lieux[q["lieu"]], "PRINCIPALE", ordre, q["description"], q["recompense"]),
            )

    # Quêtes annexes : la première occasion de capturer chaque espèce sauvage.
    premieres: dict[int, tuple[int, int, str]] = {}
    lignes = con.execute(
        f"""
        SELECT r.especeId, l.id, l.nom, l.etapeDeblocageId, r.methode, r.condition
        FROM rencontre r JOIN lieu l ON l.id = r.lieuId
        WHERE r.methode IN ({",".join("?" * len(METHODES_CAPTURE))})
        """,
        METHODES_CAPTURE,
    ).fetchall()
    for espece, lieu_id, lieu_nom, etape_lieu, methode, condition in lignes:
        etape = etape_de_capture(etape_lieu, methode, condition)
        if espece not in premieres or (etape, lieu_id) < premieres[espece][:2]:
            premieres[espece] = (etape, lieu_id, lieu_nom)
    noms = dict(con.execute("SELECT id, nom FROM espece"))
    annexes = 0
    par_etape: dict[int, int] = {}
    for espece, (etape, lieu_id, lieu_nom) in sorted(premieres.items(), key=lambda x: (x[1][0], x[1][2], noms[x[0]])):
        numero += 1
        annexes += 1
        par_etape[etape] = par_etape.get(etape, 0) + 1
        con.execute(
            "INSERT INTO quete VALUES (?,?,?,?,?,?,?)",
            (numero, etape, lieu_id, "ANNEXE", par_etape[etape], f"Capturer {noms[espece]} ({lieu_nom})", None),
        )
    return {"etapes": len(etapes), "quetes principales": numero - annexes, "quetes de capture": annexes}
