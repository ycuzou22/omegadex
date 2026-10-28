#!/usr/bin/env python3
"""Crée la base jeu vide au schéma exact attendu par Room, prête à être remplie par l'import.

Le schéma est lu dans le fichier exporté par Room (app/schemas/…/JeuDatabase/<version>.json) :
tables, index et version (PRAGMA user_version) sont identiques à ceux que Room vérifie à l'ouverture.
La table room_master_table n'est pas créée : Room valide alors la base colonne par colonne
lors de la première copie, puis enregistre lui-même son empreinte.

Usage : python3 tools/import/creer_base_jeu.py --version-donnees 1 [--sortie chemin.db]
"""

import argparse
import datetime
import json
import pathlib
import sqlite3

RACINE = pathlib.Path(__file__).resolve().parents[2]
SCHEMAS = RACINE / "app/schemas/com.omegadex.app.data.jeu.JeuDatabase"
SORTIE = RACINE / "app/src/main/assets/database/omegadex_jeu.db"
JETON_TABLE = "${TABLE_NAME}"


def dernier_schema() -> tuple[int, dict]:
    versions = sorted(int(f.stem) for f in SCHEMAS.glob("*.json"))
    if not versions:
        raise SystemExit(f"Aucun schéma dans {SCHEMAS} : compiler l'application d'abord.")
    version = versions[-1]
    return version, json.loads((SCHEMAS / f"{version}.json").read_text(encoding="utf-8"))["database"]


def creer(sortie: pathlib.Path, version_donnees: int, sources: str) -> None:
    version, base = dernier_schema()
    sortie.parent.mkdir(parents=True, exist_ok=True)
    sortie.unlink(missing_ok=True)
    con = sqlite3.connect(sortie)
    for entite in base["entities"]:
        table = entite["tableName"]
        con.execute(entite["createSql"].replace(JETON_TABLE, table))
        for index in entite.get("indices", []):
            con.execute(index["createSql"].replace(JETON_TABLE, table))
    date = datetime.date.today().isoformat()
    con.execute(
        "INSERT INTO version_donnees (id, version, dateGeneration, sources) VALUES (1, ?, ?, ?)",
        (version_donnees, date, sources),
    )
    con.execute(f"PRAGMA user_version = {version}")
    con.commit()
    con.execute("VACUUM")
    con.close()
    print(f"Base jeu créée : {sortie} (schéma v{version}, données v{version_donnees})")


if __name__ == "__main__":
    args = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    args.add_argument("--version-donnees", type=int, required=True)
    args.add_argument("--sources", default="")
    args.add_argument("--sortie", type=pathlib.Path, default=SORTIE)
    a = args.parse_args()
    creer(a.sortie, a.version_donnees, a.sources)
