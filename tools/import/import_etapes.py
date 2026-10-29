"""T04.4 — Les 13 étapes, leurs quêtes et le plafond de niveau calculé à partir des équipes des champions.

- donnees/etapes_rosa.json : étapes et quêtes principales (progression de Rubis Oméga) ;
- Poképédia : équipe du champion de chaque étape (modèle {{Tableau Dresseur}} avec jeu=ROSA),
  le plafond est le niveau de son Pokémon le plus fort ;
- quêtes annexes générées : capturer chaque espèce sauvage la première fois qu'elle devient disponible
  dans Rubis Oméga ; une exclusivité de Saphir Alpha devient une quête « obtenir par échange », placée à
  l'étape où elle apparaîtrait dans Saphir Alpha ;
- quêtes annexes d'objets (donnees/objets_rosa.json) : récupérer chaque objet utile (CT, CS, objets clés,
  objets d'évolution, objets tenus, Méga-Gemmes) à l'étape où il devient réellement accessible : lieu
  débloqué, CS utilisable (badge obtenu) et condition d'histoire remplie.
"""

from __future__ import annotations

import json
import pathlib
import re
import sqlite3

import import_lieux
import source_pokepedia

DOSSIER = pathlib.Path(__file__).resolve().parent
METHODES_CAPTURE = ("HERBE", "HORDE", "SURF", "PLONGEE", "PECHE", "NAVI_DEX", "ROCHE_FRACASSEE", "FIXE")
# Une condition de rencontre peut repousser l'étape où la capture devient possible.
CONDITIONS_ETAPE = [("Groudon", 8), ("Pokédex National", 10), ("Ligue", 10), ("Delta", 11)]
# Étape à partir de laquelle chaque méthode est utilisable (CS ou canne obtenue à l'étape précédente).
METHODES_ETAPE = {"SURF": 6, "PLONGEE": 8, "ROCHE_FRACASSEE": 3}
CANNES_ETAPE = [("Méga Canne", 7), ("Super Canne", 3), ("Canne", 2)]
# Étape à partir de laquelle chaque capacité de terrain ouvre le chemin : CS en poche et badge qui l'autorise.
CS_ETAPE = {
    "COUPE": 2,
    "ECLATE_ROC": 3,
    "FORCE": 5,
    "SURF": 6,
    "VOL": 7,
    "VELO_CROSS": 3,
    "VELO_COURSE": 3,
    "CHERCHE_OBJET": 0,
    "PLONGEE": 8,
    "CASCADE": 10,
}
# Conditions d'histoire qui repoussent un objet, repérées dans le champ « condition » (mot-clé, étape minimale).
CONDITIONS_OBJET = [
    ("Pokédex National complété", 12),
    ("Pokédex de Hoenn complété", 10),
    ("Delta", 11),
    ("Ligue", 10),
    ("Pokédex National", 10),
    ("Groudon", 8),
    ("Marc", 10),
    ("Lévy", 8),
    ("Alizée", 7),
    ("Norman", 6),
    ("Adriane", 5),
    ("Voltère", 4),
    ("Bastien", 3),
    ("Roxanne", 2),
    # Récompense remise en battant le champion : à l'étape de son arène.
    ("arène de Clémenti-Ville", 5),
    # Badge désigné par la ville de son arène.
    ("badge d'Atalanopolis", 10),
    ("badge d'Algatia", 8),
    ("badge de Cimetronelle", 7),
    ("badge de Clémenti", 6),
    ("badge de Vermilava", 5),
    ("badge de Lavandia", 4),
    ("badge de Myokara", 3),
    ("badge de Mérouville", 2),
]
NOMS_CS = {
    "COUPE": "Coupe",
    "ECLATE_ROC": "Éclate-Roc",
    "FORCE": "Force",
    "SURF": "Surf",
    "VOL": "Vol",
    "VELO_CROSS": "Vélo Cross",
    "VELO_COURSE": "Vélo Course",
    "CHERCHE_OBJET": "Cherche-Objet",
    "PLONGEE": "Plongée",
    "CASCADE": "Cascade",
}
VERBES_OBJET = {"ACHAT": "Acheter", "RECOMPENSE": "Gagner", "CADEAU": "Recevoir", "SOL": "Récupérer"}


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

    captures, echanges = generer_quetes_annexes(con, numero + 1)
    objets = generer_quetes_objets(con)
    return {
        "etapes": len(etapes),
        "quetes principales": numero,
        "quetes de capture": captures,
        "quetes d'echange (Saphir Alpha)": echanges,
        "quetes d'objets": objets,
    }


def generer_quetes_annexes(con: sqlite3.Connection, premier_numero: int) -> tuple[int, int]:
    """Quêtes annexes, numérotées à partir de [premier_numero] : (nombre de captures, nombre d'échanges).

    - « Capturer X (lieu) » : la première occasion de capturer chaque espèce sauvage dans Rubis Oméga ;
    - « Obtenir X par échange » : une espèce sauvage seulement dans Saphir Alpha (et que Rubis Oméga
      n'obtient pas non plus par évolution ou reproduction), à l'étape de sa première apparition.
    """
    capture: dict[int, tuple[int, int, str]] = {}
    autre_version: dict[int, int] = {}
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
        if not import_lieux.dans_rubis_omega(condition):
            autre_version[espece] = min(etape, autre_version.get(espece, etape))
        elif espece not in capture or (etape, lieu_id) < capture[espece][:2]:
            capture[espece] = (etape, lieu_id, lieu_nom)
    echange_requis = {e for (e,) in con.execute("SELECT especeId FROM rencontre WHERE methode = 'ECHANGE_REQUIS'")}
    noms = dict(con.execute("SELECT id, nom FROM espece"))

    # (étape, rang dans l'étape : captures puis échanges, lieu, nom) -> quête
    quetes: list[tuple[int, int, str, str, int | None, str]] = [
        (etape, 0, lieu_nom, noms[e], lieu_id, f"Capturer {noms[e]} ({lieu_nom})")
        for e, (etape, lieu_id, lieu_nom) in capture.items()
    ]
    quetes += [
        (etape, 1, "", noms[e], None, f"Obtenir {noms[e]} par échange (exclusif à Saphir Alpha)")
        for e, etape in autre_version.items()
        if e not in capture and e in echange_requis
    ]
    numero = premier_numero
    par_etape: dict[int, int] = {}
    for etape, rang, _, _, lieu_id, description in sorted(quetes, key=lambda q: q[:4]):
        par_etape[etape] = par_etape.get(etape, 0) + 1
        con.execute(
            "INSERT INTO quete VALUES (?,?,?,?,?,?,?)",
            (numero, etape, lieu_id, "ANNEXE", par_etape[etape], description, None),
        )
        numero += 1
    echanges = sum(1 for q in quetes if q[1] == 1)
    return len(quetes) - echanges, echanges


def etape_objet(etape_lieu: int, cs: str | None, condition: str | None) -> int:
    """Étape où l'objet devient récupérable : lieu débloqué, capacité de terrain utilisable, histoire avancée."""
    etape = max(etape_lieu, CS_ETAPE.get(cs, 0) if cs else 0)
    for mot, minimum in CONDITIONS_OBJET:
        if condition and mot in condition:
            etape = max(etape, minimum)
    return etape


def description_objet(objet: dict) -> str:
    """« Récupérer CT39 Tomberoche (Mérouville) », « Acheter … », avec la capacité de terrain requise."""
    verbe = VERBES_OBJET.get(objet.get("methode") or "SOL", "Récupérer")
    texte = f"{verbe} {objet['nom']} ({objet.get('lieu') or objet['lieuLibre']})"
    if objet.get("cs"):
        texte += f" · {NOMS_CS[objet['cs']]}"
    return texte


def generer_quetes_objets(con: sqlite3.Connection, fichier: pathlib.Path | None = None) -> int:
    """Quêtes annexes « Récupérer X (lieu) » des objets utiles, après les autres quêtes de chaque étape.

    Les quêtes d'objets déjà présentes sont d'abord retirées : la fonction peut être rejouée sur une base existante.
    """
    fichier = fichier or DOSSIER / "donnees" / "objets_rosa.json"
    objets = json.loads(fichier.read_text(encoding="utf-8"))["objets"]
    con.execute("DELETE FROM quete WHERE type = 'ANNEXE' AND recompense IS NOT NULL")
    lieux = {nom: (lid, etape) for lid, nom, etape in con.execute("SELECT id, nom, etapeDeblocageId FROM lieu")}
    # Les récompenses d'arène (« Badge Roche, CT39 Tomberoche ») sont déjà dans les quêtes principales.
    deja_donnes = {
        nom.strip()
        for (recompense,) in con.execute("SELECT recompense FROM quete WHERE type = 'PRINCIPALE' AND recompense IS NOT NULL")
        for nom in recompense.split(",")
    }
    quetes = []
    for rang, objet in enumerate(objets):
        if objet["nom"] in deja_donnes:
            continue
        # Exception fixée à la main : « etape » impose l'étape ; « lieuLibre » nomme un lieu absent de la base
        # (lieu mirage), la quête n'est alors rattachée à aucun lieu.
        if not objet.get("lieu"):
            if objet.get("lieuLibre") and objet.get("etape") is not None:
                quetes.append((objet["etape"], rang, None, description_objet(objet), objet["nom"]))
            continue
        if objet["lieu"] not in lieux:
            raise ValueError(f"Objet « {objet['nom']} » : lieu inconnu « {objet['lieu']} »")
        if objet.get("cs") and objet["cs"] not in CS_ETAPE:
            raise ValueError(f"Objet « {objet['nom']} » : capacité de terrain inconnue « {objet['cs']} »")
        lieu_id, etape_lieu = lieux[objet["lieu"]]
        etape = objet.get("etape")
        if etape is None:
            etape = etape_objet(etape_lieu, objet.get("cs"), objet.get("condition"))
        quetes.append((etape, rang, lieu_id, description_objet(objet), objet["nom"]))
    numero = con.execute("SELECT COALESCE(MAX(id), 0) FROM quete").fetchone()[0] + 1
    ordres = dict(con.execute("SELECT etapeId, MAX(ordre) FROM quete WHERE type = 'ANNEXE' GROUP BY etapeId"))
    for etape, _, lieu_id, description, nom in sorted(quetes, key=lambda q: q[:2]):
        ordres[etape] = ordres.get(etape, 0) + 1
        con.execute(
            "INSERT INTO quete VALUES (?,?,?,?,?,?,?)",
            (numero, etape, lieu_id, "ANNEXE", ordres[etape], description, nom),
        )
        numero += 1
    return len(quetes)


if __name__ == "__main__":
    # Rejoue seulement les quêtes d'objets sur la base embarquée (sans réseau) :
    #     python3 tools/import/import_etapes.py --objets
    import sys

    if sys.argv[1:] != ["--objets"]:
        raise SystemExit("Usage : python3 tools/import/import_etapes.py --objets")
    base = DOSSIER.parents[1] / "app" / "src" / "main" / "assets" / "database" / "omegadex_jeu.db"
    connexion = sqlite3.connect(base)
    total = generer_quetes_objets(connexion)
    connexion.commit()
    connexion.execute("VACUUM")
    connexion.close()
    print(f"{total} quêtes d'objets écrites dans {base}")
