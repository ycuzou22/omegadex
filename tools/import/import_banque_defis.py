"""Banque de défis par famille d'évolution : environ 200 défis pour chaque famille capturable.

Les défis sont générés à partir des données déjà importées dans la base jeu (espèces, évolutions,
rencontres, attaques, talents, étapes) et de `donnees/objets_rosa.json` (lieux des CT, pierres et
Méga-Gemmes). Rien n'est saisi à la main : changer un gabarit ici régénère toute la banque.

- Une famille est un groupe d'espèces reliées par l'évolution (Arcko, Massko, Jungko). Elle est
  identifiée par sa **racine**, la plus petite espèce de la famille qui n'est l'évolution d'aucune autre.
- Une famille est **capturable** si au moins une de ses espèces s'obtient dans Rubis Oméga sans échange
  (même règle que `DisponibiliteRencontre` dans l'app) ; ses autres membres s'obtiennent alors par
  évolution (sauf évolution par échange) ou par reproduction.
- Chaque défi vise **un membre** de la famille (`parametres.cible`) et n'est proposé qu'à partir de
  l'étape où ce membre peut être obtenu ; un défi qui demande un niveau, une CT, une pierre ou une
  Méga-Gemme attend en plus l'étape où ce niveau (sous le plafond) ou cet objet devient accessible.
- Les défis sont écrits dans la table `modele_defi`, avec des identifiants réservés :
  `100000 + racine × 1000 + n` (n de 1 à 999). L'app retrouve ainsi la banque d'une famille par une
  simple plage d'identifiants.

Usage autonome, sur la base déjà importée : `python3 tools/import/import_banque_defis.py`.
"""

from __future__ import annotations

import argparse
import collections
import json
import pathlib
import random
import re
import sqlite3
import sys

DOSSIER = pathlib.Path(__file__).resolve().parent
RACINE_DEPOT = DOSSIER.parent.parent
BASE = RACINE_DEPOT / "app" / "src" / "main" / "assets" / "database" / "omegadex_jeu.db"
OBJETS = DOSSIER / "donnees" / "objets_rosa.json"

ID_BANQUE = 100_000
TAILLE_PLAGE = 1_000
DEFIS_PAR_FAMILLE = 200

# Étapes clés (identifiants de la table `etape`), alignées sur `EtapesCles` de l'app.
CANNE, ECLATE_ROC, LUNETTES_SABLE, SURF, SUPER_CANNE = 2, 4, 5, 6, 6
MEGA_CANNE, DEVON_SCOPE, PLONGEE, APRES_GROUDON, APRES_LIGUE, APRES_DELTA = 7, 7, 8, 9, 11, 12
PENSION = 3  # Pension de la Route 117
CONCOURS = 3  # Concours Live de Vergazon
MEGA_BRACELET = 6  # remis après le badge de Norman
DONNEUR = 12  # Donneurs de capacités de l'Atoll de Combat (PCo)
DERNIERE_ETAPE = 12

SAPHIR_ALPHA = "Saphir Alpha uniquement"
RE_GROUDON = re.compile(r"après avoir (vaincu|capturé) Groudon", re.IGNORECASE)
RE_DELTA = re.compile(r"après l'Épisode Delta|Ligue une deuxième fois", re.IGNORECASE)

NOMS_TYPES = {
    "normal": "Normal", "feu": "Feu", "eau": "Eau", "plante": "Plante", "electrik": "Électrik",
    "glace": "Glace", "combat": "Combat", "poison": "Poison", "sol": "Sol", "vol": "Vol", "psy": "Psy",
    "insecte": "Insecte", "roche": "Roche", "spectre": "Spectre", "dragon": "Dragon",
    "tenebres": "Ténèbres", "acier": "Acier", "fee": "Fée",
}  # fmt: skip

# Table des types (6e génération) : types contre lesquels une attaque est super efficace.
SUPER_EFFICACE = {
    "normal": [], "feu": ["plante", "glace", "insecte", "acier"], "eau": ["feu", "sol", "roche"],
    "plante": ["eau", "sol", "roche"], "electrik": ["eau", "vol"],
    "glace": ["plante", "sol", "vol", "dragon"], "combat": ["normal", "glace", "roche", "tenebres", "acier"],
    "poison": ["plante", "fee"], "sol": ["feu", "electrik", "poison", "roche", "acier"],
    "vol": ["plante", "combat", "insecte"], "psy": ["combat", "poison"],
    "insecte": ["plante", "psy", "tenebres"], "roche": ["feu", "glace", "vol", "insecte"],
    "spectre": ["psy", "spectre"], "dragon": ["dragon"], "tenebres": ["psy", "spectre"],
    "acier": ["glace", "roche", "fee"], "fee": ["combat", "dragon", "tenebres"],
}  # fmt: skip

CATEGORIES_CONCOURS = ["Sang-Froid", "Beauté", "Grâce", "Intelligence", "Robustesse"]
NATURES = {  # nature qui augmente la statistique
    "attaque": ["Rigide", "Brave"], "defense": ["Assuré", "Malin"], "attaqueSpeciale": ["Modeste", "Discret"],
    "defenseSpeciale": ["Calme", "Prudent"], "vitesse": ["Timide", "Jovial"],
}  # fmt: skip
NOMS_STATS = {
    "pv": "PV", "attaque": "Attaque", "defense": "Défense", "attaqueSpeciale": "Attaque Spéciale",
    "defenseSpeciale": "Défense Spéciale", "vitesse": "Vitesse",
}  # fmt: skip

# Part de chaque catégorie dans la banque d'une espèce (redistribuée si une catégorie manque de défis).
POIDS = {
    "combat": 22, "attaque": 26, "type": 9, "talent": 6, "evolution": 8, "statistique": 7,
    "lieu": 7, "capture": 4, "concours": 8, "elevage": 4, "mega": 3,
}  # fmt: skip

N, D, H = "NORMAL", "DIFFICILE", "HARDCORE"


# --------------------------------------------------------------------------------------------- données


def lire_base(con: sqlite3.Connection) -> dict:
    """Lit dans la base jeu tout ce dont la banque a besoin."""
    con.row_factory = sqlite3.Row
    especes = {r["id"]: dict(r) for r in con.execute("SELECT * FROM espece")}
    etapes = [dict(r) for r in con.execute("SELECT * FROM etape ORDER BY id")]
    lieux = {r["nom"]: r["etapeDeblocageId"] for r in con.execute("SELECT nom, etapeDeblocageId FROM lieu")}
    evolutions = [dict(r) for r in con.execute("SELECT * FROM evolution ORDER BY id")]
    rencontres = [
        dict(r)
        for r in con.execute(
            "SELECT r.*, l.nom AS lieuNom, l.etapeDeblocageId AS lieuEtape FROM rencontre r "
            "JOIN lieu l ON l.id = r.lieuId ORDER BY r.id"
        )
    ]
    attaques = collections.defaultdict(list)
    for r in con.execute(
        "SELECT aa.especeId, aa.methode, aa.niveau, aa.numeroCt, a.nom, a.type, a.categorie, a.puissance "
        "FROM attaque_apprise aa JOIN attaque a ON a.id = aa.attaqueId ORDER BY aa.especeId, a.nom"
    ):
        attaques[r["especeId"]].append(dict(r))
    talents = collections.defaultdict(list)
    for r in con.execute(
        "SELECT et.especeId, t.nom, et.cache FROM espece_talent et JOIN talent t ON t.id = et.talentId "
        "ORDER BY et.especeId, et.cache, t.nom"
    ):
        talents[r["especeId"]].append((r["nom"], bool(r["cache"])))
    con.row_factory = None
    return {
        "especes": especes, "etapes": etapes, "lieux": lieux, "evolutions": evolutions,
        "rencontres": rencontres, "attaques": attaques, "talents": talents,
    }  # fmt: skip


def lire_objets(lieux: dict[str, int], fichier: pathlib.Path = OBJETS) -> dict[str, int]:
    """Étape où chaque objet (CT, pierre, Méga-Gemme…) devient accessible ; absent si son lieu est inconnu."""
    if not fichier.exists():
        return {}
    objets = json.loads(fichier.read_text(encoding="utf-8"))["objets"]
    etapes = {}
    for o in objets:
        etape = o.get("etape")
        if etape is None and o.get("lieu") in lieux:
            etape = lieux[o["lieu"]]
        if etape is not None:
            etapes[o["nom"]] = max(etape, etapes.get(o["nom"], 0)) if o["nom"] in etapes else etape
    return etapes


# ----------------------------------------------------------------------------------------- règles


def etape_de_la_rencontre(r: dict) -> int | None:
    """Port de `DisponibiliteRencontre.de` : None si l'espèce ne s'y attrape pas dans Rubis Oméga."""
    condition = r["condition"] or ""
    if r["methode"] == "ECHANGE_REQUIS" or SAPHIR_ALPHA.lower() in condition.lower():
        return None
    exigences = [r["lieuEtape"]]
    methode = r["methode"]
    if methode == "SURF":
        exigences.append(SURF)
    elif methode == "PLONGEE":
        exigences.append(PLONGEE)
    elif methode == "ROCHE_FRACASSEE":
        exigences.append(ECLATE_ROC)
    elif methode == "PECHE":
        bas = condition.lower()
        exigences.append(MEGA_CANNE if "méga canne" in bas else SUPER_CANNE if "super canne" in bas else CANNE)
    if RE_GROUDON.search(condition):
        exigences.append(APRES_GROUDON)
    if "plongée requise" in condition.lower():
        exigences.append(PLONGEE)
    if "devon scope" in condition.lower():
        exigences.append(DEVON_SCOPE)
    if "fossile ranimé" in condition.lower():
        exigences.append(LUNETTES_SABLE)
    if RE_DELTA.search(condition):
        exigences.append(APRES_DELTA)
    elif "après la ligue" in condition.lower():
        exigences.append(APRES_LIGUE)
    return max(exigences)


def niveau_de(valeur: str | None) -> int | None:
    m = re.match(r"\s*(\d+)", valeur or "")
    return int(m.group(1)) if m else None


class Regles:
    """Étapes où un niveau ou un objet devient accessible, selon les plafonds."""

    def __init__(self, etapes: list[dict], objets: dict[str, int], lieux: dict[str, int]):
        self.plafonds = [e["plafond"] for e in etapes]
        self.objets = objets
        self.lieux = lieux

    def etape_du_niveau(self, niveau: int) -> int | None:
        """Première étape à partir de laquelle le plafond ne descend plus sous [niveau]."""
        for etape in range(len(self.plafonds)):
            if min(self.plafonds[etape:]) >= niveau:
                return etape
        return None

    def etape_de_l_objet(self, texte: str | None) -> int | None:
        """Étape de l'objet cité au début de [texte] (« Pierre Ovale tenu de jour ») ; None si inconnu."""
        if not texte:
            return None
        noms = [n for n in self.objets if texte.startswith(n)]
        return self.objets[max(noms, key=len)] if noms else None

    def etape_de_l_evolution(self, ev: dict, depart: int) -> int | None:
        """Étape où l'évolution devient possible depuis une espèce obtenue à l'étape [depart]."""
        condition = ev["condition"]
        if condition == "ECHANGE":
            return None
        if condition == "NIVEAU":
            niveau = niveau_de(ev["valeur"])
            seuil = self.etape_du_niveau(niveau) if niveau else depart
            return None if seuil is None else max(depart, seuil)
        if condition in ("PIERRE", "OBJET"):
            etape = self.etape_de_l_objet(ev["valeur"])
            return max(depart, DERNIERE_ETAPE if etape is None else etape)
        if condition == "LIEU":
            return max(depart, self.lieux.get(ev["valeur"] or "", DERNIERE_ETAPE))
        return depart  # bonheur, attaque connue, beauté…


def familles(donnees: dict) -> dict[int, list[int]]:
    """Familles d'évolution, par racine : la plus petite espèce qui n'est l'évolution d'aucune autre."""
    parent: dict[int, int] = {}

    def chef(x: int) -> int:
        while parent.get(x, x) != x:
            x = parent[x]
        return x

    for ev in donnees["evolutions"]:
        a, b = chef(ev["especeSourceId"]), chef(ev["especeCibleId"])
        if a != b:
            parent[b] = a
    groupes = collections.defaultdict(list)
    for e in sorted(donnees["especes"]):
        groupes[chef(e)].append(e)
    cibles = {ev["especeCibleId"] for ev in donnees["evolutions"]}
    resultat = {}
    for membres in groupes.values():
        racines = [m for m in membres if m not in cibles] or membres
        resultat[min(racines)] = membres
    return resultat


def etapes_d_obtention(donnees: dict, regles: Regles) -> dict[int, int]:
    """Première étape où chaque espèce s'obtient dans Rubis Oméga ; absente si elle ne s'obtient pas."""
    etapes: dict[int, int] = {}
    for r in donnees["rencontres"]:
        etape = etape_de_la_rencontre(r)
        if etape is not None and etape < etapes.get(r["especeId"], 99):
            etapes[r["especeId"]] = etape
    change = True
    while change:  # évolutions et reproduction jusqu'à stabilité
        change = False
        for ev in donnees["evolutions"]:
            a, b = ev["especeSourceId"], ev["especeCibleId"]
            if a in etapes:
                etape = regles.etape_de_l_evolution(ev, etapes[a])
                if etape is not None and etape < etapes.get(b, 99):
                    etapes[b], change = etape, True
            if b in etapes and "Inconnu" not in donnees["especes"][b]["groupesOeufs"]:
                etape = max(etapes[b], PENSION)
                if etape < etapes.get(a, 99):
                    etapes[a], change = etape, True
    return etapes


def niveaux_d_obtention(donnees: dict, regles: Regles) -> dict[int, int]:
    """Plus bas niveau auquel on obtient chaque espèce sans reproduction (sauvage, cadeau ou évolution)."""
    niveaux: dict[int, int] = {}
    for r in donnees["rencontres"]:
        if r["niveauMin"] and etape_de_la_rencontre(r) is not None:
            niveaux[r["especeId"]] = min(r["niveauMin"], niveaux.get(r["especeId"], 100))
    change = True
    while change:
        change = False
        for ev in donnees["evolutions"]:
            a, b = ev["especeSourceId"], ev["especeCibleId"]
            if a in niveaux and regles.etape_de_l_evolution(ev, 0) is not None:
                niveau = max(niveaux[a], niveau_de(ev["valeur"]) or 0) if ev["condition"] == "NIVEAU" else niveaux[a]
                if niveau < niveaux.get(b, 100):
                    niveaux[b], change = niveau, True
    return niveaux


# ------------------------------------------------------------------------------------ gabarits


VOYELLES = "AEIOUYÉÈÊÀÂÎÔÛ"


def de(nom: str) -> str:
    """« de Massko », « d'Arcko »."""
    return f"d'{nom}" if nom[:1].upper() in VOYELLES else f"de {nom}"


def que(nom: str) -> str:
    """« que Massko », « qu'Arcko »."""
    return f"qu'{nom}" if nom[:1].upper() in VOYELLES else f"que {nom}"


class Pool:
    """Défis candidats d'une espèce, par catégorie."""

    def __init__(self, espece: dict, etape_obtention: int, niveau_obtention: int | None = None):
        self.espece = espece
        self.depart = etape_obtention
        self.niveau_obtention = niveau_obtention
        self.par_categorie: dict[str, list[dict]] = collections.defaultdict(list)

    def etape_sous_le_niveau(self, niveau: int) -> int | None:
        """Étape à partir de laquelle on peut avoir l'espèce sous le [niveau] : obtenue plus bas, sinon
        un œuf de la Pension (éclos au niveau 1) ; None si ni l'un ni l'autre n'est possible."""
        if self.niveau_obtention is None or self.niveau_obtention < niveau:
            return 0
        return None if "Inconnu" in self.espece["groupesOeufs"] else PENSION

    def ajouter(self, categorie, type_defi, difficulte, titre, texte, etape=0, conditions=(), **parametres):
        etape_min = max(self.depart, etape)
        if etape_min > DERNIERE_ETAPE:
            return
        self.par_categorie[categorie].append({
            "type": type_defi, "difficulteMin": difficulte, "titre": titre, "gabarit": texte,
            "etapeMin": etape_min, "conditions": list(conditions),
            "parametres": {"cible": self.espece["id"], "categorie": categorie, **parametres},
        })  # fmt: skip


def defis_de_combat(p: Pool) -> None:
    s = p.espece["nom"]
    e = "ESPECE"
    for diff, titre, texte, ecart in [
        (N, "En solo", f"Gagner un combat de dresseur avec {s} seul dans l'équipe", None),
        (N, "Sans K.O.", f"Battre {{adversaire}} avec {s} dans l'équipe, sans qu'il soit mis K.O.", None),
        (N, "Tête d'affiche", f"Gagner 5 combats de dresseurs d'affilée avec {s} en tête d'équipe", None),
        (N, "Ouverture", f"Mettre K.O. le premier Pokémon de {{adversaire}} avec {s}", None),
        (N, "Sans objet", f"Gagner 10 combats sauvages avec {s} sans utiliser d'objet", None),
        (N, "Duo", f"Gagner un Combat Duo avec {s} dans l'équipe", None),
        (N, "Trio de choc", f"Gagner 3 combats de dresseurs où {s} est le seul Pokémon à combattre", None),
        (N, "Sauvetage", f"Gagner un combat de dresseur où {s} met K.O. le dernier adversaire", None),
        (N, "Endurance", f"Gagner 20 combats sauvages avec {s} sans passer par un Centre Pokémon", None),
        (D, "Coup de grâce", f"Mettre K.O. le dernier Pokémon de {{adversaire}} avec {s}", None),
        (D, "Sans soin", f"Battre {{adversaire}} avec {s} sans utiliser d'objet de soin", None),
        (D, "Équipe réduite", f"Battre {{adversaire}} avec {s} et seulement deux autres Pokémon", None),
        (D, "Série", f"Gagner 10 combats de dresseurs d'affilée avec {s}, sans Centre Pokémon", None),
        (D, "Tenir bon", f"Battre {{adversaire}} sans {que(s)} ne passe sous la moitié de ses PV", None),
        (D, "Mains nues", f"Battre {{adversaire}} avec {s} sans objet tenu", None),
        (D, "Doublé", f"Mettre K.O. deux Pokémon de {{adversaire}} avec {s}", None),
        (D, "Sous-niveau", f"Battre {{adversaire}} avec {s} au niveau {{niveau}} au plus", 5),
        (D, "Intouchable", f"Gagner un combat de dresseur sans {que(s)} ne subisse de dégâts", None),
        (H, "Seul contre tous", f"Battre {{adversaire}} avec {s} seul dans l'équipe", None),
        (H, "Zéro perte", f"Battre {{adversaire}} avec {s}, sans qu'aucun Pokémon de l'équipe ne soit mis K.O.", None),
        (H, "Ascèse", f"Battre {{adversaire}} avec {s} seul, sans objet de soin", None),
        (H, "Grand écart", f"Battre {{adversaire}} avec {s} au niveau {{niveau}} au plus", 10),
        (H, "Rouleau compresseur", f"Mettre K.O. toute l'équipe de {{adversaire}} avec {s}, sans changer de Pokémon", None),
    ]:
        extra = {"ecart": ecart} if ecart is not None else {}
        p.ajouter("combat", e, diff, titre, texte, **extra)
    for nombre, diff in ((10, N), (25, N), (50, D)):
        p.ajouter("combat", e, diff, f"{nombre} victoires", f"Gagner {nombre} combats de dresseurs avec {s} dans l'équipe")
    for nombre, diff in ((10, N), (30, N), (100, D)):
        p.ajouter("combat", e, diff, f"{nombre} K.O.", f"Mettre K.O. {nombre} Pokémon sauvages avec {s}")
    for nombre, diff in ((3, N), (10, D)):
        p.ajouter("capture", e, diff, f"Rabatteur ×{nombre}",
                  f"Capturer {nombre} Pokémon sauvages après les avoir affaiblis avec {s}")  # fmt: skip
    p.ajouter("combat", e, N, "Combat Inversé", f"Remporter un Combat Inversé à Lavandia avec {s}", 3)
    p.ajouter("combat", e, D, "Inversé sans faute",
              f"Remporter un Combat Inversé à Lavandia avec {s}, sans qu'il soit mis K.O.", 3)  # fmt: skip
    p.ajouter("combat", e, N, "Revanches", f"Battre 5 dresseurs en revanche grâce au Pokématos avec {s}", 4)
    for serie, diff in ((7, N), (21, D), (50, H)):
        p.ajouter("combat", e, diff, f"Maison de Combat ×{serie}",
                  f"Enchaîner {serie} victoires à la Maison de Combat avec {s}", DONNEUR)  # fmt: skip
    p.ajouter("combat", e, D, "Combat Triple", f"Gagner un Combat Triple à la Maison de Combat avec {s}", DONNEUR)
    p.ajouter("combat", e, N, "Amitié", f"Atteindre l'affection maximale avec {s} à la Poké Récré")


def defis_d_attaques(p: Pool, attaques: list[dict], regles: Regles) -> None:
    s = p.espece["nom"]
    vues: set[tuple[str, str]] = set()
    for a in attaques:
        cle = (a["nom"], a["methode"])
        if cle in vues:
            continue
        vues.add(cle)
        nom, statut = a["nom"], a["categorie"] == "STATUT"
        etape = etape_de_l_attaque(a, regles)
        if etape is None:
            continue
        if a["methode"] == "NIVEAU":
            avant = p.etape_sous_le_niveau(a["niveau"])
            if a["niveau"] > 1 and avant is not None:
                etape = max(etape, avant)
                p.ajouter("attaque", "ESPECE", N, f"Apprendre {nom}",
                          f"Faire apprendre {nom} à {s} au niveau {a['niveau']} et la garder jusqu'à {{adversaire}}",
                          etape)  # fmt: skip
        elif a["methode"] == "CT_CS":
            ct = a["numeroCt"] or "CT"
            p.ajouter("attaque", "ESPECE", N, f"{ct} {nom}",
                      f"Enseigner la {ct} ({nom}) à {s} et gagner 3 combats en l'utilisant", etape)  # fmt: skip
        elif a["methode"] == "DONNEUR":
            p.ajouter("attaque", "ESPECE", N, f"Leçon : {nom}",
                      f"Faire apprendre {nom} à {s} par un Donneur de capacités et l'utiliser en combat", etape)  # fmt: skip
        elif a["methode"] == "REPRODUCTION":
            p.ajouter("elevage", "ESPECE", D, f"Capacité Œuf : {nom}",
                      f"Obtenir par la reproduction un {s} qui connaît {nom}", etape)  # fmt: skip
        if statut:
            p.ajouter("attaque", "ESPECE", N, f"Tactique : {nom}",
                      f"Gagner un combat de dresseur où {s} utilise {nom} dès le premier tour", etape)  # fmt: skip
            p.ajouter("attaque", "ESPECE", D, f"Préparation : {nom}",
                      f"Battre {{adversaire}} après {que(s)} a utilisé {nom} au premier tour", etape)  # fmt: skip
        else:
            p.ajouter("attaque", "ESPECE", N, f"Spécialité : {nom}",
                      f"Mettre K.O. 5 Pokémon sauvages avec {s} en n'utilisant que {nom}", etape)  # fmt: skip
            p.ajouter("attaque", "ESPECE", D, f"Frappe : {nom}",
                      f"Mettre K.O. un Pokémon de {{adversaire}} avec {nom} lancée par {s}", etape)  # fmt: skip
            p.ajouter("attaque", "ESPECE", H, f"Une seule arme : {nom}",
                      f"Battre {{adversaire}} avec {s} en n'utilisant que {nom}", etape)  # fmt: skip


def etape_de_l_attaque(a: dict, regles: Regles) -> int | None:
    """Étape où l'espèce peut connaître l'attaque [a], selon sa méthode d'apprentissage."""
    if a["methode"] == "NIVEAU":
        return regles.etape_du_niveau(max(a["niveau"], 1))
    if a["methode"] == "CT_CS":
        return regles.etape_de_l_objet(f"{a['numeroCt'] or 'CT'} ")
    if a["methode"] == "DONNEUR":
        return DONNEUR
    if a["methode"] == "REPRODUCTION":
        return PENSION
    return None


def defis_de_types(p: Pool, attaques: list[dict], regles: Regles) -> None:
    s, sp = p.espece["nom"], p.espece
    types = [t for t in (sp["type1"], sp["type2"]) if t]
    offensifs: dict[str, int] = {}  # type d'attaque -> première étape où l'espèce en connaît une
    for a in attaques:
        etape = etape_de_l_attaque(a, regles)
        if a["categorie"] != "STATUT" and etape is not None:
            offensifs[a["type"]] = min(etape, offensifs.get(a["type"], 99))
    for t in types:
        nom_t = NOMS_TYPES[t]
        if t in offensifs:
            p.ajouter("type", "ESPECE", D, f"Pur {nom_t}",
                      f"Battre {{adversaire}} avec {s} en n'utilisant que des attaques de type {nom_t}",
                      offensifs[t])  # fmt: skip
            for cible in SUPER_EFFICACE[t]:
                p.ajouter("type", "ESPECE", N, f"{nom_t} contre {NOMS_TYPES[cible]}",
                          f"Mettre K.O. 3 Pokémon de type {NOMS_TYPES[cible]} avec une attaque {nom_t} {de(s)}",
                          offensifs[t])  # fmt: skip
    faiblesses = sorted({x for x, cibles in SUPER_EFFICACE.items() for t in types if t in cibles})
    for x in faiblesses:
        p.ajouter("type", "ESPECE", D, f"Face au type {NOMS_TYPES[x]}",
                  f"Gagner un combat contre un Pokémon de type {NOMS_TYPES[x]} avec {s}, sans le changer")  # fmt: skip
    for x in sorted(offensifs.keys() - set(types)):
        p.ajouter("type", "ESPECE", N, f"Couverture {NOMS_TYPES[x]}",
                  f"Mettre K.O. un Pokémon de {{adversaire}} avec une attaque de type {NOMS_TYPES[x]} {de(s)}",
                  offensifs[x])  # fmt: skip
    for x, nom_x in NOMS_TYPES.items():
        p.ajouter("type", "ESPECE", N, f"Chasse au type {nom_x}", f"Mettre K.O. 5 Pokémon de type {nom_x} avec {s}")
        p.ajouter("capture", "ESPECE", N, f"Capture {nom_x}",
                  f"Capturer un Pokémon de type {nom_x} après l'avoir affaibli avec {s}")  # fmt: skip


def defis_de_talents(p: Pool, talents: list[tuple[str, bool]], navi: bool) -> None:
    s = p.espece["nom"]
    for nom, cache in talents:
        if cache:
            if navi:
                p.ajouter("talent", "NAVI_DEX", D, f"Talent caché : {nom}",
                          f"Trouver {s} avec son talent caché {nom} grâce au Navi-Dex", 0, ["TALENT_CACHE"])  # fmt: skip
            p.ajouter("talent", "ESPECE", H, f"Rareté : {nom}",
                      f"Battre {{adversaire}} avec un {s} au talent caché {nom}")  # fmt: skip
        else:
            p.ajouter("talent", "ESPECE", N, f"Talent : {nom}",
                      f"Gagner 5 combats de dresseurs avec {s} au talent {nom}")  # fmt: skip
            p.ajouter("talent", "ESPECE", D, f"Atout : {nom}",
                      f"Battre {{adversaire}} avec un {s} au talent {nom}")  # fmt: skip


def defis_d_evolution(p: Pool, evolutions: list[dict], etapes: dict[int, int], noms: dict[int, str],
                      regles: Regles) -> None:  # fmt: skip
    s = p.espece["nom"]
    for ev in evolutions:
        b = ev["especeCibleId"]
        etape = regles.etape_de_l_evolution(ev, p.depart)
        if etape is None or b not in etapes:
            continue
        nb = noms[b]
        p.ajouter("evolution", "EVOLUTION", N, f"Vers {nb}",
                  f"Faire évoluer {s} en {nb} avant d'affronter {{adversaire}}", etape)  # fmt: skip
        p.ajouter("evolution", "EVOLUTION", D, f"{nb} sans raccourci",
                  f"Faire évoluer {s} en {nb} sans utiliser de Super Bonbon", etape)  # fmt: skip
        p.ajouter("evolution", "EVOLUTION", H, f"{nb} invaincu",
                  f"Faire évoluer {s} en {nb} sans qu'il ait été mis K.O. une seule fois depuis son obtention",
                  etape)  # fmt: skip
        avant = p.etape_sous_le_niveau(niveau_de(ev["valeur"]) or 0) if ev["condition"] == "NIVEAU" else None
        if ev["condition"] == "NIVEAU" and niveau_de(ev["valeur"]) and avant is not None:
            niveau = niveau_de(ev["valeur"])
            p.ajouter("evolution", "EVOLUTION", N, f"{nb} pile à l'heure",
                      f"Faire évoluer {s} en {nb} au niveau {niveau} exactement", max(etape, avant))  # fmt: skip
        if ev["condition"] == "PIERRE" and ev["valeur"]:
            p.ajouter("evolution", "EVOLUTION", D, f"{nb} à la {ev['valeur']}",
                      f"Faire évoluer {s} en {nb} avec une {ev['valeur']} ramassée, sans l'acheter", etape)  # fmt: skip
        p.ajouter("evolution", "ESPECE", D, f"Rester {s}",
                  f"Battre {{adversaire}} avec {s} sans le faire évoluer", etape)  # fmt: skip
    if evolutions and p.depart <= 10:
        p.ajouter("evolution", "ESPECE", H, f"{s} pour toujours",
                  f"Atteindre la Ligue Pokémon avec {s} dans l'équipe, sans jamais le faire évoluer")  # fmt: skip


def defis_de_statistiques(p: Pool) -> None:
    sp, s = p.espece, p.espece["nom"]
    stats = {k: sp[k] for k in NOMS_STATS}
    meilleure = max(stats, key=lambda k: (stats[k], k))
    physique = sp["attaque"] >= sp["attaqueSpeciale"]
    p.ajouter("statistique", "ESPECE", D, "Force brute" if physique else "Puissance de l'esprit",
              f"Battre {{adversaire}} avec {s} en n'utilisant que des attaques "
              f"{'physiques' if physique else 'spéciales'}")  # fmt: skip
    p.ajouter("statistique", "ESPECE", N, f"Entraînement {NOMS_STATS[meilleure]}",
              f"Monter au maximum les EV de {s} en {NOMS_STATS[meilleure]} au Pokémon-Multi Entraînement")  # fmt: skip
    for nature in NATURES.get(meilleure, NATURES["attaque" if physique else "attaqueSpeciale"]):
        p.ajouter("statistique", "ESPECE", N, f"Nature {nature}",
                  f"Obtenir un {s} de nature {nature} et gagner 5 combats avec lui")  # fmt: skip
    if sp["vitesse"] >= 90:
        p.ajouter("statistique", "ESPECE", N, "Premier à frapper",
                  f"Gagner 5 combats de dresseurs où {s} attaque toujours en premier")  # fmt: skip
    if sp["pv"] + sp["defense"] + sp["defenseSpeciale"] >= 240:
        p.ajouter("statistique", "ESPECE", D, "Rempart",
                  f"Encaisser une attaque super efficace avec {s} sans être mis K.O., puis gagner le combat")  # fmt: skip
    p.ajouter("statistique", "ESPECE", H, "Sans entraînement",
              f"Battre {{adversaire}} avec {s} sans lui avoir donné de vitamine ni d'entraînement d'EV")  # fmt: skip


def defis_de_lieux(p: Pool, rencontres: list[dict]) -> None:
    s = p.espece["nom"]
    vus = set()
    for r in rencontres:
        etape = etape_de_la_rencontre(r)
        if etape is None or r["lieuNom"] in vus:
            continue
        vus.add(r["lieuNom"])
        lieu = r["lieuNom"]
        p.ajouter("lieu", "ESPECE", N, f"Retour à {lieu}",
                  f"Gagner 10 combats sauvages à {lieu} avec {s} en tête d'équipe", etape)  # fmt: skip
        p.ajouter("lieu", "ESPECE", D, f"Second {s}",
                  f"Capturer un second {s} à {lieu} du premier coup de Poké Ball", etape)  # fmt: skip
        if r["methode"] == "HORDE":
            p.ajouter("lieu", "ESPECE", D, f"Horde à {lieu}",
                      f"Vaincre une horde de {s} à {lieu} en un seul tour", etape)  # fmt: skip
        if r["methode"] == "NAVI_DEX":
            p.ajouter("lieu", "NAVI_DEX", H, f"Potentiel à {lieu}",
                      f"Trouver à {lieu} un {s} d'au moins 3 étoiles de potentiel grâce au Navi-Dex", etape)  # fmt: skip


def defis_d_elevage(p: Pool) -> None:
    sp, s = p.espece, p.espece["nom"]
    p.ajouter("elevage", "ESPECE", H, f"{s} chromatique", f"Obtenir un {s} chromatique et gagner un combat avec lui")
    p.ajouter("elevage", "ESPECE", N, "Surnom", f"Donner un surnom à {s} et gagner 10 combats avec lui")
    if "Inconnu" not in sp["groupesOeufs"]:
        p.ajouter("elevage", "ESPECE", D, f"Œuf {de(s)}", f"Faire éclore un œuf {de(s)} à la Pension", PENSION)
    ratio = sp["ratioMale"]
    if ratio is not None and 0 < ratio < 1:
        p.ajouter("elevage", "ESPECE", N, "Couple", f"Avoir un {s} mâle et un {s} femelle dans le PC")


def defis_de_concours(p: Pool) -> None:
    s = p.espece["nom"]
    for categorie in CATEGORIES_CONCOURS:
        for rang, diff in (("Normal", N), ("Super", N), ("Hyper", D), ("Master", H)):
            p.ajouter("concours", "CONCOURS", diff, f"{categorie} {rang}",
                      f"Remporter un Concours Live {categorie} de rang {rang} avec {s}", CONCOURS,
                      ["CONCOURS_OUVERTS"])  # fmt: skip
    p.ajouter("concours", "CONCOURS", D, "Idole",
              f"Remporter un Concours Live avec {s} après lui avoir donné 5 Pokéblocs", CONCOURS,
              ["CONCOURS_OUVERTS"])  # fmt: skip


def defis_mega(p: Pool, etape_gemme: int | None) -> None:
    sp, s = p.espece, p.espece["nom"]
    if not sp["aUneMega"] or etape_gemme is None:
        return
    etape = max(MEGA_BRACELET, etape_gemme)
    cond = ["MEGA_DISPONIBLE", "ESPECE_DANS_EQUIPE"]
    for diff, titre, texte in [
        (N, "Méga-Évolution", f"Gagner 5 combats de dresseurs en Méga-évoluant {s}"),
        (D, "Méga-victoire", f"Battre {{adversaire}} grâce à la Méga-Évolution {de(s)}"),
        (D, "Méga-sprint", f"Mettre K.O. deux Pokémon de {{adversaire}} avec Méga-{s}"),
        (H, "Méga-solo", f"Battre {{adversaire}} avec Méga-{s} seul dans l'équipe"),
        (H, "Méga-tardive", f"Battre {{adversaire}} en ne Méga-évoluant {s} qu'au dernier Pokémon adverse"),
    ]:
        p.ajouter("mega", "MEGA", diff, titre, texte, etape, cond)


def etape_de_la_gemme(espece: dict, regles: Regles) -> int | None:
    """Étape de la Méga-Gemme de l'espèce (nom de la gemme dérivé du nom de l'espèce)."""
    espece_propre = re.sub(r"[^a-zàâäéèêëîïôöûüç]", "", espece["nom"].lower())
    for nom, etape in regles.objets.items():
        radical = re.sub(r"[^a-zàâäéèêëîïôöûüç]", "", nom.lower())[:-3]
        if nom.endswith("ite") and len(radical) >= 4 and espece_propre.startswith(radical):
            return etape
    return None


# ------------------------------------------------------------------------------------- sélection


def selectionner(pools: list[Pool], total: int, graine: int) -> list[dict]:
    """Choisit [total] défis répartis entre les membres puis entre les catégories, sans doublon d'énoncé."""
    hasard = random.Random(graine)
    quotas = [total // len(pools) + (1 if i < total % len(pools) else 0) for i in range(len(pools))]
    choisis: list[dict] = []
    vus: set[str] = set()
    reste = 0
    for pool, quota in zip(pools, quotas, strict=True):
        quota += reste
        files = {}
        for cat, liste in sorted(pool.par_categorie.items()):
            liste = [d for d in liste if d["gabarit"] not in vus]
            hasard.shuffle(liste)
            files[cat] = collections.deque(liste)
        pris = 0
        credits = dict.fromkeys(files, 0.0)
        while pris < quota and any(files.values()):
            actives = [c for c in files if files[c]]
            poids = sum(POIDS[c] for c in actives)
            for c in actives:
                credits[c] += POIDS[c] / poids
            c = max(actives, key=lambda x: (credits[x], x))
            credits[c] -= 1
            defi = files[c].popleft()
            if defi["gabarit"] in vus:
                continue
            vus.add(defi["gabarit"])
            choisis.append(defi)
            pris += 1
        reste = quota - pris
    return choisis


def generer(donnees: dict, objets: dict[str, int]) -> list[dict]:
    """Toute la banque : liste de modèles de défis au format de la table `modele_defi`."""
    regles = Regles(donnees["etapes"], objets, donnees["lieux"])
    obtention = etapes_d_obtention(donnees, regles)
    niveaux = niveaux_d_obtention(donnees, regles)
    noms = {i: e["nom"] for i, e in donnees["especes"].items()}
    par_source = collections.defaultdict(list)
    for ev in donnees["evolutions"]:
        par_source[ev["especeSourceId"]].append(ev)
    rencontres = collections.defaultdict(list)
    for r in donnees["rencontres"]:
        rencontres[r["especeId"]].append(r)
    banque = []
    for racine, membres in sorted(familles(donnees).items()):
        obtenus = [m for m in membres if m in obtention]
        if not obtenus:
            continue
        pools = []
        for m in obtenus:
            sp = donnees["especes"][m]
            p = Pool(sp, obtention[m], niveaux.get(m))
            navi = any(r["methode"] == "NAVI_DEX" and etape_de_la_rencontre(r) is not None for r in rencontres[m])
            defis_de_combat(p)
            defis_d_attaques(p, donnees["attaques"][m], regles)
            defis_de_types(p, donnees["attaques"][m], regles)
            defis_de_talents(p, donnees["talents"][m], navi)
            defis_d_evolution(p, par_source[m], obtention, noms, regles)
            defis_de_statistiques(p)
            defis_de_lieux(p, rencontres[m])
            defis_d_elevage(p)
            defis_de_concours(p)
            defis_mega(p, etape_de_la_gemme(sp, regles))
            pools.append(p)
        for n, defi in enumerate(selectionner(pools, DEFIS_PAR_FAMILLE, graine=racine), 1):
            defi["id"] = ID_BANQUE + racine * TAILLE_PLAGE + n
            defi["parametres"] = {"famille": racine, **defi["parametres"]}
            banque.append(defi)
    return banque


# ------------------------------------------------------------------------------------- écriture


def valider(banque: list[dict]) -> None:
    """Mêmes contrôles que les modèles saisis à la main (import_defis.valider)."""
    import import_defis

    for position, d in enumerate(banque, 1):
        modele = {
            "id": d["id"], "type": d["type"], "etape": None, "titre": d["titre"], "gabarit": d["gabarit"],
            "parametres": d["parametres"], "difficulteMin": d["difficulteMin"],
            "deblocage": {"etapeMin": d["etapeMin"], "conditions": d["conditions"]},
        }  # fmt: skip
        import_defis.valider(modele, position)
        famille = d["parametres"]["famille"]
        if not ID_BANQUE + famille * TAILLE_PLAGE < d["id"] < ID_BANQUE + (famille + 1) * TAILLE_PLAGE:
            raise import_defis.ModeleInvalide(f"banque, défi {d['id']} : hors de la plage de sa famille {famille}")


def importer(con: sqlite3.Connection) -> dict[str, int]:
    donnees = lire_base(con)
    banque = generer(donnees, lire_objets(donnees["lieux"]))
    valider(banque)
    con.execute("DELETE FROM modele_defi WHERE id >= ?", (ID_BANQUE,))
    con.executemany(
        "INSERT INTO modele_defi VALUES (?,?,?,?,?,?,?,?,?)",
        [
            (
                d["id"], d["type"], None, d["titre"], d["gabarit"],
                json.dumps(d["parametres"], ensure_ascii=False, separators=(",", ":")),
                d["difficulteMin"], d["etapeMin"], json.dumps(d["conditions"], separators=(",", ":")),
            )  # fmt: skip
            for d in banque
        ],
    )
    nb_familles = len({d["parametres"]["famille"] for d in banque})
    return {"defis de la banque": len(banque), "familles": nb_familles}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--base", type=pathlib.Path, default=BASE)
    args = parser.parse_args()
    sys.path.insert(0, str(DOSSIER))
    con = sqlite3.connect(args.base)
    with con:
        resultat = importer(con)
    con.execute("VACUUM")
    con.close()
    print(", ".join(f"{v} {k}" for k, v in resultat.items()))


if __name__ == "__main__":
    main()
