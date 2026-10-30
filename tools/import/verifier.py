"""T04.5 — Contrôle de la base jeu, étape par étape, et croisement des sources.

Produit tools/import/rapport_controle.md. Une erreur bloquante fait échouer l'import ;
un avertissement est signalé mais n'empêche pas la génération.
"""

from __future__ import annotations

import pathlib
import sqlite3
from collections import defaultdict

from source_pokeapi import lire, noms_fr

DOSSIER = pathlib.Path(__file__).resolve().parent
RAPPORT = DOSSIER / "rapport_controle.md"
NOMBRE_ETAPES = 13
ESPECES_HOENN = 211
ESPECES_NATIONAL = 721
VERSIONS_ROSA = {"25", "26"}


class Controle:
    def __init__(self) -> None:
        self.erreurs: list[str] = []
        self.avertissements: list[str] = []

    def exiger(self, condition: bool, message: str) -> None:
        if not condition:
            self.erreurs.append(message)

    def signaler(self, condition: bool, message: str) -> None:
        if not condition:
            self.avertissements.append(message)


def _globaux(con: sqlite3.Connection, c: Controle) -> list[str]:
    q = lambda sql: con.execute(sql).fetchone()[0]  # noqa: E731
    lignes = []
    especes, hoenn = q("SELECT COUNT(*) FROM espece"), q("SELECT COUNT(*) FROM espece WHERE numeroHoenn IS NOT NULL")
    c.exiger(especes == ESPECES_NATIONAL, f"{especes} espèces au lieu de {ESPECES_NATIONAL}")
    c.exiger(hoenn == ESPECES_HOENN, f"{hoenn} espèces de Hoenn au lieu de {ESPECES_HOENN}")
    c.exiger(q("SELECT COUNT(DISTINCT numeroHoenn) FROM espece WHERE numeroHoenn IS NOT NULL") == hoenn, "numéros de Hoenn en double")
    c.exiger(q("SELECT COUNT(*) FROM espece WHERE nom = '' OR nom IS NULL") == 0, "espèce sans nom français")
    sans = q("SELECT COUNT(*) FROM espece WHERE id NOT IN (SELECT especeId FROM rencontre) "
             "AND id NOT IN (SELECT especeCibleId FROM evolution) AND id NOT IN (SELECT especeSourceId FROM evolution)")  # fmt: skip
    c.exiger(sans == 0, f"{sans} espèces sans lieu, sans famille d'évolution et sans mention « échange requis »")
    c.exiger(q("SELECT COUNT(*) FROM espece WHERE id NOT IN (SELECT especeId FROM attaque_apprise)") == 0, "espèce sans attaque")
    c.exiger(q("SELECT COUNT(*) FROM espece WHERE id NOT IN (SELECT especeId FROM espece_talent)") == 0, "espèce sans talent")
    c.exiger(con.execute("PRAGMA foreign_key_check").fetchall() == [], "clés étrangères invalides")
    c.signaler(q("SELECT COUNT(*) FROM attaque WHERE description = ''") == 0, "attaques sans description française")
    echanges = q("SELECT COUNT(*) FROM rencontre WHERE methode = 'ECHANGE_REQUIS'")
    lignes += [
        f"- Espèces : {especes} (Pokédex national 6G), dont {hoenn} au Pokédex de Hoenn",
        f"- Talents : {q('SELECT COUNT(*) FROM talent')}, évolutions : {q('SELECT COUNT(*) FROM evolution')}",
        f"- Attaques : {q('SELECT COUNT(*) FROM attaque')}, apprentissages : {q('SELECT COUNT(*) FROM attaque_apprise')}",
        f"- Lieux : {q('SELECT COUNT(*) FROM lieu')}, rencontres : {q('SELECT COUNT(*) FROM rencontre')} "
        f"(dont {echanges} « échange requis »)",
    ]
    return lignes


def _etapes(con: sqlite3.Connection, c: Controle) -> list[str]:
    lignes = [
        "| Étape | Adversaire | Plafond | Lieux débloqués | Quêtes principales | Captures | Défis (base / Difficile) | Contrôle |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    etapes = con.execute("SELECT id, titre, adversaire, plafond FROM etape ORDER BY id").fetchall()
    c.exiger(len(etapes) == NOMBRE_ETAPES, f"{len(etapes)} étapes au lieu de {NOMBRE_ETAPES}")
    precedent = 0
    for eid, titre, adversaire, plafond in etapes:
        avant = len(c.erreurs), len(c.avertissements)
        lieux = con.execute("SELECT COUNT(*) FROM lieu WHERE etapeDeblocageId = ?", (eid,)).fetchone()[0]
        principales = con.execute("SELECT COUNT(*) FROM quete WHERE etapeId = ? AND type = 'PRINCIPALE'", (eid,)).fetchone()[0]
        captures = con.execute("SELECT COUNT(*) FROM quete WHERE etapeId = ? AND type = 'ANNEXE'", (eid,)).fetchone()[0]
        base, difficile = (
            con.execute("SELECT COUNT(*) FROM modele_defi WHERE etapeId = ? AND difficulteMin = ?", (eid, d)).fetchone()[0]
            for d in ("NORMAL", "DIFFICILE")
        )
        c.exiger(principales >= 1, f"étape {eid} ({titre}) sans quête principale")
        c.exiger(base >= 1 and difficile >= 1, f"étape {eid} ({titre}) sans défi de base ou sans défi Difficile")
        c.exiger(plafond and plafond > 0, f"étape {eid} ({titre}) sans plafond")
        c.signaler(plafond >= precedent, f"étape {eid} ({titre}) : plafond {plafond} inférieur à l'étape précédente ({precedent})")
        c.signaler(lieux > 0 or eid == 0, f"étape {eid} ({titre}) ne débloque aucun lieu")
        precedent = max(precedent, plafond or 0)
        statut = "erreur" if len(c.erreurs) > avant[0] else ("avertissement" if len(c.avertissements) > avant[1] else "ok")
        lignes.append(f"| {eid} · {titre} | {adversaire} | {plafond} | {lieux} | {principales} | {captures} | {base} / {difficile} | {statut} |")
    return lignes


def controler_quetes_annexes(con: sqlite3.Connection, c: Controle) -> None:
    """Aucune quête « Capturer » : le bouton « Attraper » des rencontres enregistre déjà les captures.
    Une exclusivité de Saphir Alpha ne peut être qu'une quête d'échange, sur une espèce « échange requis »."""
    for (description,) in con.execute(
        "SELECT description FROM quete WHERE type = 'ANNEXE' AND description LIKE 'Capturer %'"
    ).fetchall():
        c.exiger(False, f"quête « {description} » : les captures passent par le bouton « Attraper », pas par une quête")
    for description, espece in con.execute(
        """
        SELECT q.description, e.id FROM quete q
        JOIN espece e ON q.description LIKE 'Obtenir ' || e.nom || ' par échange%'
        WHERE q.type = 'ANNEXE'
        """
    ).fetchall():
        requis = con.execute(
            "SELECT COUNT(*) FROM rencontre WHERE especeId = ? AND methode = 'ECHANGE_REQUIS'", (espece,)
        ).fetchone()[0]
        c.exiger(requis > 0, f"quête « {description} » sur une espèce sans mention « échange requis »")


def _croiser_pokeapi(con: sqlite3.Connection, c: Controle) -> list[str]:
    """Compare les espèces par lieu avec les rencontres Rubis Oméga connues de PokéAPI (couverture partielle)."""
    zones = {r["id"]: r["location_id"] for r in lire("location_areas")}
    noms_lieux = noms_fr("location_names", "location_id")
    pokemon = {r["id"]: int(r["species_id"]) for r in lire("pokemon")}
    pokeapi: dict[str, set[int]] = defaultdict(set)
    for r in lire("encounters"):
        if r["version_id"] in VERSIONS_ROSA:
            pokeapi[noms_lieux.get(zones[r["location_area_id"]], "")].add(pokemon[r["pokemon_id"]])
    nos_lieux = defaultdict(set)
    for nom, espece in con.execute("SELECT l.nom, r.especeId FROM rencontre r JOIN lieu l ON l.id = r.lieuId"):
        nos_lieux[nom].add(espece)
    noms = dict(con.execute("SELECT id, nom FROM espece"))
    communs = [n for n in pokeapi if n in nos_lieux]
    ou = defaultdict(list)
    for nom, especes in nos_lieux.items():
        for e in especes:
            ou[e].append(nom)
    ecarts, absentes = [], 0
    for nom in sorted(communs):
        for e in sorted(pokeapi[nom] - nos_lieux[nom], key=lambda x: noms.get(x, "")):
            ailleurs = [n for n in ou.get(e, []) if n != nom]
            if not ailleurs:
                absentes += 1
            precision = f"présent dans Poképédia à {', '.join(sorted(ailleurs)[:3])}" if ailleurs else "absent de Poképédia"
            ecarts.append(f"- {nom} : PokéAPI cite {noms.get(e, e)} ({precision})")
    c.signaler(absentes == 0, f"{absentes} rencontres citées par PokéAPI absentes de Poképédia (voir le croisement)")
    return [f"{len(communs)} lieux présents dans les deux sources, {len(ecarts)} avec des écarts.", ""] + (ecarts or ["Aucun écart."])


def verifier(con: sqlite3.Connection, corrections: int) -> Controle:
    c = Controle()
    globaux = _globaux(con, c)
    etapes = _etapes(con, c)
    controler_quetes_annexes(con, c)
    croisement = _croiser_pokeapi(con, c)
    texte = [
        "# Rapport de contrôle de la base jeu",
        "",
        "Généré par `tools/import/importer.py` (ne pas modifier à la main).",
        "",
        f"**Erreurs bloquantes : {len(c.erreurs)}** · avertissements : {len(c.avertissements)} · corrections manuelles appliquées : {corrections}",
        "",
        "## Contenu",
        "",
        *globaux,
        "",
        "## Étapes",
        "",
        *etapes,
        "",
        "## Erreurs bloquantes",
        "",
        *([f"- {e}" for e in c.erreurs] or ["Aucune."]),
        "",
        "## Avertissements",
        "",
        *([f"- {a}" for a in c.avertissements] or ["Aucun."]),
        "",
        "## Croisement Poképédia / PokéAPI",
        "",
        *croisement,
        "",
    ]
    RAPPORT.write_text("\n".join(texte), encoding="utf-8")
    return c
