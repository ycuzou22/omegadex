#!/usr/bin/env python3
"""Régénère la base jeu de Rubis Oméga de zéro, en une commande.

    python3 tools/import/importer.py --version-donnees 2

Étapes : base vide au schéma Room (creer_base_jeu.py), puis chaque module d'import dans l'ordre.
Chaque module renvoie ses compteurs, affichés à la fin.
"""

from __future__ import annotations

import argparse
import pathlib
import sqlite3
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

import creer_base_jeu  # noqa: E402
import import_attaques  # noqa: E402
import import_especes  # noqa: E402
import import_etapes  # noqa: E402
import import_lieux  # noqa: E402
import source_pokeapi  # noqa: E402

MODULES = [
    ("Espèces, talents et évolutions", import_especes.importer),
    ("Attaques et apprentissages", import_attaques.importer),
    ("Lieux et rencontres", import_lieux.importer),
    ("Étapes, quêtes et plafonds", import_etapes.importer),
]
SOURCES = f"PokéAPI ({source_pokeapi.DEPOT}, commit {source_pokeapi.COMMIT[:7]}) ; Poképédia (CC BY-NC-SA 3.0)"


def main() -> None:
    args = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    args.add_argument("--version-donnees", type=int, required=True)
    args.add_argument("--sortie", type=pathlib.Path, default=creer_base_jeu.SORTIE)
    a = args.parse_args()

    creer_base_jeu.creer(a.sortie, a.version_donnees, SOURCES)
    con = sqlite3.connect(a.sortie)
    for nom, module in MODULES:
        compteurs = module(con)
        print(f"- {nom} : " + ", ".join(f"{k} {v}" for k, v in compteurs.items()))
    con.commit()
    probleme = con.execute("PRAGMA foreign_key_check").fetchall()
    sans_etapes = con.execute("SELECT COUNT(*) FROM etape").fetchone()[0] == 0
    if sans_etapes:
        probleme = [p for p in probleme if p[2] != "etape"]  # les étapes ne sont pas encore importées
    if probleme:
        raise SystemExit(f"Clés étrangères invalides : {probleme[:10]}")
    con.execute("VACUUM")
    con.close()


if __name__ == "__main__":
    main()
