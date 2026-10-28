"""T05 — Modèles de défis : seule saisie manuelle, dans donnees/defis.json, validée au chargement.

Le format est décrit dans docs/modeles-defis.md. Un modèle invalide arrête l'import avec un message
qui cite le modèle et le champ en cause.
"""

from __future__ import annotations

import json
import pathlib
import re
import sqlite3

import types_pokemon

FICHIER = pathlib.Path(__file__).resolve().parent / "donnees" / "defis.json"
TYPES = {"ETAPE", "ESPECE", "EVOLUTION", "ROTATION", "MONOTYPE", "NAVI_DEX", "MEGA", "CONCOURS"}
DIFFICULTES = ("NORMAL", "DIFFICILE", "HARDCORE")
ETAPES = range(0, 13)
# Marqueurs remplis par le moteur de défis à partir de la run et de l'étape.
MARQUEURS_MOTEUR = {"adversaire", "plafond", "lieu", "starter", "espece", "evolution", "niveau", "etape"}
CONDITIONS = {
    "ESPECE_CAPTUREE",  # le défi cible une espèce déjà capturée
    "ESPECE_DANS_EQUIPE",  # … présente dans l'équipe actuelle
    "EVOLUTION_SOUS_PLAFOND",  # l'évolution de l'espèce est atteignable sans dépasser le plafond
    "MEGA_DISPONIBLE",  # l'espèce a une Méga-Évolution et la Méga-Gemme est obtenable
    "TALENT_CACHE",  # l'espèce a un talent caché trouvable au Navi-Dex
    "CONCOURS_OUVERTS",  # les Concours Live sont accessibles
    "POKEDEX_NATIONAL",  # Pokédex national obtenu (après la Ligue)
}
CHAMPS = {"id": int, "type": str, "etape": (int, type(None)), "titre": str, "gabarit": str,
          "parametres": dict, "difficulteMin": str, "deblocage": dict}  # fmt: skip


class ModeleInvalide(ValueError):
    pass


def valider(modele: dict, position: int) -> None:
    ref = f"defis.json, modèle n° {position}" + (f" (id {modele['id']})" if isinstance(modele.get("id"), int) else "")

    def erreur(message: str) -> None:
        raise ModeleInvalide(f"{ref} : {message}")

    for champ, attendu in CHAMPS.items():
        if champ not in modele:
            erreur(f"champ obligatoire « {champ} » manquant")
        if not isinstance(modele[champ], attendu) or (attendu is int and isinstance(modele[champ], bool)):
            erreur(f"« {champ} » a un type invalide ({type(modele[champ]).__name__})")
    inconnus = set(modele) - set(CHAMPS)
    if inconnus:
        erreur(f"champ(s) inconnu(s) : {', '.join(sorted(inconnus))}")
    if modele["type"] not in TYPES:
        erreur(f"type « {modele['type']} » inconnu (attendu : {', '.join(sorted(TYPES))})")
    if modele["type"] == "ETAPE" and modele["etape"] not in ETAPES:
        erreur("un défi d'étape doit indiquer « etape » entre 0 et 12")
    if modele["type"] != "ETAPE" and modele["etape"] is not None:
        erreur("seuls les défis de type ETAPE sont rattachés à une étape (« etape » doit valoir null)")
    if modele["difficulteMin"] not in DIFFICULTES:
        erreur(f"« difficulteMin » doit valoir {', '.join(DIFFICULTES)}")
    if not modele["titre"].strip() or not modele["gabarit"].strip():
        erreur("« titre » et « gabarit » ne peuvent pas être vides")
    marqueurs = set(re.findall(r"\{(\w+)\}", modele["gabarit"]))
    manquants = marqueurs - MARQUEURS_MOTEUR - set(modele["parametres"])
    if manquants:
        erreur(f"marqueur(s) sans valeur dans « parametres » : {', '.join('{' + m + '}' for m in sorted(manquants))}")
    if "type" in modele["parametres"]:
        for t in str(modele["parametres"]["type"]).split(","):
            if t not in types_pokemon.PAR_IDENTIFIANT_POKEAPI.values():
                erreur(f"type Pokémon « {t} » inconnu dans « parametres »")
    deblocage = modele["deblocage"]
    if set(deblocage) - {"etapeMin", "conditions"}:
        erreur("« deblocage » n'accepte que « etapeMin » et « conditions »")
    if not isinstance(deblocage.get("etapeMin"), int) or deblocage["etapeMin"] not in ETAPES:
        erreur("« deblocage.etapeMin » doit être une étape entre 0 et 12")
    if modele["type"] == "ETAPE" and deblocage["etapeMin"] > modele["etape"]:
        erreur("un défi d'étape ne peut pas se débloquer après son étape")
    conditions = deblocage.get("conditions", [])
    if not isinstance(conditions, list) or set(conditions) - CONDITIONS:
        erreur(f"« deblocage.conditions » contient une condition inconnue (connues : {', '.join(sorted(CONDITIONS))})")


def charger(fichier: pathlib.Path = FICHIER) -> list[dict]:
    try:
        contenu = json.loads(fichier.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        raise ModeleInvalide(f"defis.json n'est pas du JSON valide (ligne {e.lineno}, colonne {e.colno}) : {e.msg}") from e
    modeles = contenu.get("modeles")
    if not isinstance(modeles, list):
        raise ModeleInvalide("defis.json doit contenir une liste « modeles »")
    vus: set[int] = set()
    for position, modele in enumerate(modeles, 1):
        if not isinstance(modele, dict):
            raise ModeleInvalide(f"defis.json, modèle n° {position} : un modèle doit être un objet JSON")
        valider(modele, position)
        if modele["id"] in vus:
            raise ModeleInvalide(f"defis.json, modèle n° {position} : id {modele['id']} déjà utilisé")
        vus.add(modele["id"])
    return modeles


def importer(con: sqlite3.Connection) -> dict[str, int]:
    modeles = charger()
    for m in modeles:
        con.execute(
            "INSERT INTO modele_defi VALUES (?,?,?,?,?,?,?,?,?)",
            (
                m["id"],
                m["type"],
                m["etape"],
                m["titre"],
                m["gabarit"],
                json.dumps(m["parametres"], ensure_ascii=False),
                m["difficulteMin"],
                m["deblocage"]["etapeMin"],
                json.dumps(m["deblocage"].get("conditions", []), ensure_ascii=False),
            ),
        )
    return {"modeles": len(modeles), "defis d'etape": sum(1 for m in modeles if m["type"] == "ETAPE")}
