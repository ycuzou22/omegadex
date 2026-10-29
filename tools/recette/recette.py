#!/usr/bin/env python3
"""Recette du MVP sur émulateur (T16.2 et T16.3) : pilote l'application installée par adb et UI Automator.

    python3 tools/recette/recette.py --profil pixel [--jusqu-a-la-ligue] [--sortie recette]

Chaque vérification est notée OK ou ÉCHEC dans <sortie>/<profil>/resultats.json et recette.md, avec une capture
d'écran par étape. Le script échoue (code 1) si une vérification échoue ou si l'application plante.
Aucune dépendance : seulement adb (dans le PATH) et la bibliothèque standard de Python.
"""

from __future__ import annotations

import argparse
import json
import pathlib
import re
import subprocess
import sys
import time
import unicodedata
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field

PAQUET = "com.omegadex.app"
ACTIVITE = f"{PAQUET}/.MainActivity"
RACINE = pathlib.Path(__file__).resolve().parents[2]
ETAPES = json.loads((RACINE / "tools/import/donnees/etapes_rosa.json").read_text(encoding="utf-8"))["etapes"]
ETAPE_LIGUE = 10
MOT_DE_PASSE = "rubis-omega"


# --- adb ---------------------------------------------------------------------------------------------------


def adb(*args: str, binaire: bool = False, delai: int = 60) -> str | bytes:
    sortie = subprocess.run(["adb", *args], capture_output=True, timeout=delai, check=False)
    return sortie.stdout if binaire else sortie.stdout.decode("utf-8", "replace")


def shell(commande: str) -> str:
    return adb("shell", commande)


@dataclass
class Noeud:
    texte: str
    description: str
    coche: bool
    cochable: bool
    classe: str
    focus: bool
    selectionne: bool
    x1: int
    y1: int
    x2: int
    y2: int

    @property
    def centre(self) -> tuple[int, int]:
        return (self.x1 + self.x2) // 2, (self.y1 + self.y2) // 2

    @property
    def libelle(self) -> str:
        return self.texte or self.description


def lire_ecran() -> list[Noeud]:
    """Arbre d'accessibilité de l'écran (uiautomator dump), aplati."""
    for _ in range(3):
        shell("uiautomator dump /sdcard/ecran.xml >/dev/null 2>&1")
        xml = adb("exec-out", "cat /sdcard/ecran.xml")
        if xml.strip().startswith("<?xml"):
            break
        time.sleep(1)
    else:
        return []
    noeuds = []
    for n in ET.fromstring(xml).iter("node"):
        b = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds", ""))
        if not b:
            continue
        noeuds.append(
            Noeud(
                n.get("text", ""),
                n.get("content-desc", ""),
                n.get("checked") == "true",
                n.get("checkable") == "true",
                n.get("class", ""),
                n.get("focused") == "true",
                n.get("selected") == "true",
                *map(int, b.groups()),
            )
        )
    return noeuds


def fermer_dialogues_systeme(ecran: list[Noeud]) -> bool:
    """Ferme une boîte « … isn't responding » d'une autre application (lanceur lent sur émulateur)."""
    alerte = next((n for n in ecran if "isn't responding" in n.texte or "ne répond pas" in n.texte), None)
    if not alerte or "Omégadex" in alerte.texte:
        return False
    attendre_bouton = next((n for n in ecran if n.texte in ("Wait", "Attendre")), None)
    if attendre_bouton:
        toucher(attendre_bouton, 1.5)
        return True
    return False


def desactiver_clavier_virtuel() -> list[str]:
    """Désactive les claviers virtuels : la saisie passe par `input text` (événements clavier matériel).

    Un clavier affiché masque la moitié de l'écran, et la touche Retour qui le fermerait quitte l'application
    s'il est déjà fermé : sans clavier virtuel, le script n'a jamais à le fermer.
    """
    # Avec le clavier matériel déclaré (hw.keyboard=yes), Android n'ouvre plus le clavier virtuel.
    shell("settings put secure show_ime_with_hard_keyboard 0")
    claviers = [ligne.strip() for ligne in shell("ime list -s").splitlines() if ligne.strip()]
    for clavier in claviers:
        shell(f"ime disable {clavier}")
    return claviers


def chercher(motif: str, ecran: list[Noeud] | None = None, exact: bool = False) -> Noeud | None:
    """Premier nœud dont le texte ou la description contient [motif] (ou lui est égal si [exact])."""
    if ecran is None:
        ecran = lire_ecran()
        if fermer_dialogues_systeme(ecran):
            ecran = lire_ecran()
    for n in ecran:
        for valeur in (n.texte, n.description):
            if valeur and (valeur == motif if exact else motif in valeur):
                return n
    return None


def attendre(motif: str, delai: float = 15, exact: bool = False) -> Noeud | None:
    fin = time.time() + delai
    while time.time() < fin:
        n = chercher(motif, exact=exact)
        if n:
            return n
        time.sleep(1)
    return None


def taille_ecran() -> tuple[int, int]:
    m = re.findall(r"(\d+)x(\d+)", shell("wm size"))
    return tuple(map(int, m[-1])) if m else (1080, 1920)


def toucher(n: Noeud | tuple[int, int], pause: float = 1.2) -> None:
    x, y = n.centre if isinstance(n, Noeud) else n
    shell(f"input tap {x} {y}")
    time.sleep(pause)


def faire_defiler(vers_le_bas: bool = True) -> None:
    largeur, hauteur = taille_ecran()
    # Zone centrale : loin de la barre des tâches de la tablette et des gestes système.
    haut, bas = int(hauteur * 0.3), int(hauteur * 0.62)
    debut, fin = (bas, haut) if vers_le_bas else (haut, bas)
    shell(f"input swipe {largeur // 2} {debut} {largeur // 2} {fin} 350")
    time.sleep(1)


def trouver_en_defilant(motif: str, essais: int = 12, exact: bool = False) -> Noeud | None:
    """Cherche vers le bas, puis vers le haut si l'élément est déjà passé."""
    for vers_le_bas in (True, False):
        for _ in range(essais):
            n = chercher(motif, exact=exact)
            if n:
                # Pas sous la barre des tâches de la tablette ni rogné par le bord de la liste.
                return rendre_visible(lambda: chercher(motif, exact=exact)) or n
            faire_defiler(vers_le_bas)
    return None


def remonter(essais: int = 8) -> None:
    for _ in range(essais):
        faire_defiler(vers_le_bas=False)


def entierement_visible(n: Noeud) -> bool:
    """Le nœud est loin des bords : ni sous la barre de navigation, ni rogné en haut de l'écran."""
    hauteur = taille_ecran()[1]
    return n.y1 >= hauteur * 0.08 and n.y2 <= hauteur * 0.82


def rendre_visible(trouver, essais: int = 4) -> Noeud | None:
    """Fait défiler jusqu'à ce que le nœud rendu par [trouver]() soit entièrement visible."""
    for _ in range(essais):
        n = trouver()
        if n is None or entierement_visible(n):
            return n
        faire_defiler(vers_le_bas=n.y2 > taille_ecran()[1] * 0.82)
    return trouver()


def champs_texte() -> list[Noeud]:
    return [n for n in lire_ecran() if n.classe.endswith("EditText")]


def saisir(rang: int, texte: str) -> bool:
    """Saisit [texte] dans le [rang]-ième champ de l'écran (0 = le premier), après l'avoir vidé."""
    for _ in range(3):
        champ = rendre_visible(lambda: next(iter(champs_texte()[rang:rang + 1]), None))
        if champ is None:
            return False
        toucher(champ, 0.8)
        champ = champs_texte()[rang]
        if not champ.focus:
            continue
        shell("input keyevent KEYCODE_MOVE_END")
        shell("input keyevent " + " ".join(["KEYCODE_DEL"] * 30))
        if texte:
            shell(f"input text '{texte}'")
        time.sleep(0.8)
        valeur = champs_texte()[rang].texte
        # Un champ mot de passe affiche des puces : on compare la longueur.
        if valeur == texte or (set(valeur) == {"•"} and len(valeur) == len(texte)) or (not texte and len(valeur) <= 30):
            return True
        print(f"saisie à reprendre dans le champ {rang} : {len(valeur)} caractères au lieu de {len(texte)}", flush=True)
    return False


def onglet(nom: str) -> bool:
    """Onglet de la barre de navigation : le nœud le plus bas de l'écran qui porte ce libellé."""
    for _ in range(3):
        candidats = [n for n in lire_ecran() if n.libelle == nom]
        if not candidats:
            time.sleep(1)
            continue
        toucher(max(candidats, key=lambda n: n.y1), 2.5)
        ecran = lire_ecran()
        # L'onglet actif est sélectionné (selectable) : son libellé est dans un nœud selected.
        if any(n.selectionne and n.y1 <= c.y1 <= n.y2 for n in ecran for c in ecran if c.libelle == nom):
            return True
    return False


ETAPE_AFFICHEE = re.compile(r"(?i)^étape (\d+) sur (\d+)$")


def etape_courante() -> int | None:
    """Numéro de l'étape en cours sur l'écran Aventure (« ÉTAPE 1 SUR 13 »)."""
    for n in lire_ecran():
        m = ETAPE_AFFICHEE.match(n.texte.strip())
        if m:
            return int(m.group(1))
    return None


def quete_cochee(quete: Noeud) -> bool:
    """La case à cocher (gemme) est un nœud cochable sur la même ligne que le texte de la quête."""
    y = quete.centre[1]
    return any(n.cochable and n.coche and n.y1 <= y <= n.y2 for n in lire_ecran())


def redemarrer_app() -> None:
    shell(f"am force-stop {PAQUET}")
    time.sleep(1)
    shell(f"am start -n {ACTIVITE}")
    time.sleep(6)


def passer_animations(motif_attendu: str, delai: float = 40) -> bool:
    """Attend la fin d'une célébration (étape validée, boîte à badges) en la touchant pour la passer."""
    largeur, hauteur = taille_ecran()
    fin = time.time() + delai
    while time.time() < fin:
        if chercher(motif_attendu):
            return True
        # Toucher le haut de l'écran passe l'animation ; au centre, la touche risquerait de cocher une
        # quête de l'étape suivante au moment où la célébration se ferme.
        shell(f"input tap {largeur // 2} {int(hauteur * 0.12)}")
        time.sleep(2.5)
    return False


# --- rapport -----------------------------------------------------------------------------------------------


@dataclass
class Recette:
    profil: str
    dossier: pathlib.Path
    resultats: list[dict] = field(default_factory=list)
    numero: int = 0
    pid: str = ""

    def capture(self, nom: str) -> str:
        self.numero += 1
        sans_accents = unicodedata.normalize("NFD", nom).encode("ascii", "ignore").decode()
        fichier = f"{self.numero:02d}-" + re.sub(r"[^a-z0-9]+", "-", sans_accents.lower()).strip("-")[:60] + ".png"
        (self.dossier / fichier).write_bytes(adb("exec-out", "screencap -p", binaire=True))
        return fichier

    def verifier(self, critere: str, nom: str, condition: object, detail: str = "", capture: bool = True) -> bool:
        ok = bool(condition)
        crash = self.plantage()
        if crash:
            ok, detail = False, f"{detail} · plantage : {crash}".strip(" ·")
        if not ok:
            # Ce que l'écran montre au moment de l'échec, pour le diagnostic sans ouvrir la capture.
            textes = [n.libelle for n in lire_ecran() if n.libelle][:25]
            detail = f"{detail} · écran : {' | '.join(textes)}".strip(" ·")[:900]
        image = self.capture(nom) if capture or not ok else ""
        self.resultats.append({"critere": critere, "verification": nom, "ok": ok, "detail": detail, "capture": image})
        print(f"{'OK    ' if ok else 'ÉCHEC '} [{critere}] {nom} {detail}", flush=True)
        if not ok:
            print(f"::error title=Recette {self.profil}::[{critere}] {nom} {detail}", flush=True)
        return ok

    def plantage(self) -> str:
        journal = adb("logcat", "-d", "-b", "crash")
        if PAQUET in journal and "FATAL EXCEPTION" in journal:
            ligne = next((l for l in journal.splitlines() if "Exception" in l and "FATAL" not in l), "FATAL EXCEPTION")
            return ligne.strip()[:300]
        ecran = lire_ecran()
        if any("Omégadex" in n.texte and ("isn't responding" in n.texte or "ne répond pas" in n.texte) for n in ecran):
            return "ANR d'Omégadex"
        if shell(f"pidof {PAQUET}").strip() == "":
            return "processus arrêté"
        return ""

    def ecrire(self) -> None:
        (self.dossier / "resultats.json").write_text(json.dumps(self.resultats, ensure_ascii=False, indent=1))
        echecs = [r for r in self.resultats if not r["ok"]]
        lignes = [
            f"# Recette · {self.profil}",
            "",
            f"Appareil : {appareil()}",
            "",
            f"**{len(self.resultats) - len(echecs)} / {len(self.resultats)} vérifications réussies**",
            "",
            "| Critère | Vérification | Résultat | Détail | Capture |",
            "| --- | --- | --- | --- | --- |",
        ]
        for r in self.resultats:
            etat = "OK" if r["ok"] else "**ÉCHEC**"
            lignes.append(f"| {r['critere']} | {r['verification']} | {etat} | {r['detail']} | {r['capture']} |")
        (self.dossier / "recette.md").write_text("\n".join(lignes) + "\n", encoding="utf-8")


def appareil() -> str:
    modele = shell("getprop ro.product.model").strip()
    version = shell("getprop ro.build.version.release").strip()
    api = shell("getprop ro.build.version.sdk").strip()
    largeur, hauteur = taille_ecran()
    densite = shell("wm density").strip().split(":")[-1].strip()
    return f"{modele}, Android {version} (API {api}), {largeur}×{hauteur} px, densité {densite}"


# --- scénarios ---------------------------------------------------------------------------------------------


def creer_compte(r: Recette, identifiant: str) -> bool:
    if chercher("Créer un compte", exact=True):
        toucher(chercher("Créer un compte", exact=True))
    attendre("Crée ton compte", 8)
    ok = all(saisir(rang, valeur) for rang, valeur in enumerate((identifiant, MOT_DE_PASSE, MOT_DE_PASSE)))
    bouton = trouver_en_defilant("Créer mon compte", 4, exact=True)
    if bouton:
        toucher(bouton, 4)
    return r.verifier("Comptes", f"inscription de {identifiant}", ok and attendre("Ton starter", 15))


def creer_run(r: Recette, starter: str = "Arcko") -> bool:
    attendre("Ton starter", 10)
    n = attendre(starter, 10, exact=True)
    if n:
        toucher(n)
    bouton = trouver_en_defilant("Commencer l'aventure", 6, exact=True)
    if bouton:
        toucher(bouton, 5)
    return r.verifier("Runs", f"création d'une run ({starter}, Normal)", attendre("Bonjour", 15))


def ouvrir_aventure() -> None:
    onglet("Aventure")
    remonter(3)


def etape_affichee(etape: dict) -> bool:
    remonter(3)
    return etape_courante() == etape["id"]


def valider_etape(r: Recette, etape: dict, suivante: dict, capturer: bool) -> bool:
    """Coche les quêtes principales encore ouvertes de [etape] ; tant qu'il en reste une, l'étape suivante
    ne doit pas apparaître (T08.3), puis elle doit apparaître juste après la dernière."""
    quetes = [q["description"] for q in etape["quetes"]]
    for i, quete in enumerate(quetes):
        n = rendre_visible(lambda q=quete: trouver_en_defilant(q, 10, exact=True))
        if not n:
            return r.verifier("Aventure", f"étape {etape['id']} : quête introuvable", False, quete)
        if quete_cochee(n):
            continue
        toucher(n, 1.5)
        restantes = [
            q for q in quetes[i + 1:] if not ((m := trouver_en_defilant(q, 6, exact=True)) and quete_cochee(m))
        ]
        if restantes:
            if etape_affichee(suivante):
                return r.verifier(
                    "Déblocage d'étape",
                    f"étape {suivante['id']} visible trop tôt",
                    False,
                    f"après la quête {i + 1}/{len(quetes)} de l'étape {etape['id']}",
                )
            remonter(2)
    attendu = f"SUR {len(ETAPES)}"
    fin = time.time() + 60
    vue = False
    while time.time() < fin and not vue:
        passer_animations(attendu, 10)
        remonter(2)
        vue = etape_courante() == suivante["id"]
    return r.verifier(
        "Déblocage d'étape",
        f"étape {etape['id']} validée → étape {suivante['id']} ({suivante['adversaire']})",
        vue,
        capture=capturer,
    )


def bande_statut(case: Noeud) -> tuple[int, int]:
    """Le « + » de la bande des rubis, en bas à droite de la case : une touche y fait avancer le statut."""
    largeur, hauteur = case.x2 - case.x1, case.y2 - case.y1
    return case.x2 - max(20, largeur // 8), case.y2 - max(16, hauteur // 7)


def revenir_de_la_fiche() -> bool:
    """Si une touche a ouvert la fiche Pokémon au lieu de changer le statut, revient au Pokédex."""
    if chercher("National #") and chercher("Retour"):
        toucher(chercher("Retour"), 2)
        return True
    return False


def tester_pokedex(r: Recette) -> None:
    onglet("Pokédex")
    r.verifier("Pokédex", "grille du Pokédex de Hoenn", attendre("Pokédex de Hoenn", 10))
    attendre("N°002", 10)
    case = rendre_visible(lambda: chercher("N°002"))
    if not case:
        r.verifier("Statut en 1 touche", "case de Massko (N°002)", False, "case introuvable")
        return
    avant = case.description
    toucher(bande_statut(case), 2)
    fiche_ouverte = revenir_de_la_fiche()
    apres = chercher("N°002")
    r.verifier(
        "Statut en 1 touche",
        "une touche sur la bande des rubis : Non vu → Vu",
        apres is not None and "Non vu" in avant and "Vu" in apres.description and "Non vu" not in apres.description,
        f"{avant} → {apres.description if apres else '?'}" + (" · la touche a ouvert la fiche" if fiche_ouverte else ""),
    )
    for _ in range(1):
        apres = rendre_visible(lambda: chercher("N°002"))
        if apres:
            toucher(bande_statut(apres), 2)
            revenir_de_la_fiche()
    capture = chercher("N°002")
    r.verifier("Statut en 1 touche", "deuxième touche : Vu → Capturé", capture and "Capturé" in capture.description,
               capture.description if capture else "")
    if champs_texte():
        saisir(0, "Gobou")
        time.sleep(1.5)
        ecran = lire_ecran()
        r.verifier(
            "Pokédex",
            "recherche « Gobou »",
            chercher("Gobou", ecran) and not chercher("N°002", ecran),
            "résultat : " + ", ".join(n.description.split(",")[0] for n in ecran if n.description.startswith("N°")),
        )
        saisir(0, "")
        time.sleep(1.5)
    # Fiche Pokémon : toucher la case ouvre la fiche en 6 onglets.
    case = attendre("N°002", 8)
    if case:
        toucher(((case.x1 + case.x2) // 2, case.y1 + (case.y2 - case.y1) // 3), 3)
    onglets = ["Infos", "Stats", "Évol.", "Attaq.", "Lieux", "Défis"]
    ecran = lire_ecran()
    presents = [o for o in onglets if chercher(o, ecran, exact=True)]
    r.verifier("Fiche complète", "fiche de Massko : 6 onglets", len(presents) == 6, ", ".join(presents))
    for o in onglets[1:]:
        n = chercher(o, exact=True)
        if n:
            toucher(n, 1.5)
            r.verifier("Fiche complète", f"onglet {o}", chercher("Retour") is not None)
    retour = chercher("Retour")
    if retour:
        toucher(retour, 2)


def tester_accueil(r: Recette, capturees_min: int) -> None:
    onglet("Accueil")
    remonter(3)
    jauge = attendre("Pokédex : ", 10)
    valeur = int(m.group(1)) if jauge and (m := re.search(r"Pokédex : (\d+)", jauge.description)) else -1
    r.verifier("Jauges", "la jauge Pokédex compte la capture", valeur >= capturees_min, jauge.description if jauge else "")


def tester_mode_avion(r: Recette) -> None:
    shell("cmd connectivity airplane-mode enable 2>/dev/null")
    shell("settings put global airplane_mode_on 1")
    shell("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true >/dev/null 2>&1")
    time.sleep(3)
    actif = shell("settings get global airplane_mode_on").strip() == "1"
    redemarrer_app()
    r.verifier("Mode avion", "redémarrage en mode avion", actif and attendre("Bonjour", 15), f"airplane_mode_on={int(actif)}")
    for nom in ("Aventure", "Pokédex", "Défis", "Équipe", "Accueil"):
        onglet(nom)
        r.verifier("Mode avion", f"onglet {nom} hors ligne", chercher(nom) is not None)
    shell("cmd connectivity airplane-mode disable 2>/dev/null")
    shell("settings put global airplane_mode_on 0")
    shell("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false >/dev/null 2>&1")


def changer_de_joueur() -> None:
    onglet("Accueil")
    remonter(3)
    n = attendre("Changer de joueur", 6)
    if n:
        toucher(n, 3)


def tester_comptes(r: Recette) -> None:
    changer_de_joueur()
    r.verifier("Comptes", "« Changer de joueur » ramène à la connexion", attendre("Se connecter", 10))
    creer_compte(r, "Flora")
    creer_run(r, "Gobou")
    ouvrir_aventure()
    r.verifier("Comptes isolés", "Flora démarre à l'étape 0", etape_affichee(ETAPES[0]))
    onglet("Pokédex")
    case = attendre("N°002", 8)
    r.verifier("Comptes isolés", "Massko n'est pas capturé chez Flora", case and "Non vu" in case.description,
               case.description if case else "")
    changer_de_joueur()
    yamao = attendre("Yamao", 8, exact=True)
    if yamao:
        toucher(yamao)
    attendre("Se connecter", 6)
    saisir(len(champs_texte()) - 1, "mauvais-mot")
    toucher(chercher("Se connecter", exact=True) or (0, 0), 3)
    r.verifier("Comptes", "mauvais mot de passe refusé", attendre("Identifiant ou mot de passe incorrect", 6))
    saisir(len(champs_texte()) - 1, MOT_DE_PASSE)
    toucher(chercher("Se connecter", exact=True) or (0, 0), 4)
    r.verifier("Comptes", "connexion de Yamao", attendre("Bonjour, Yamao", 12))
    ouvrir_aventure()
    r.verifier("Comptes isolés", "Yamao retrouve son étape 1", etape_affichee(ETAPES[1]))


def bouton_systeme(*libelles: str, delai: float = 12) -> Noeud | None:
    """Bouton d'une fenêtre système (sélecteur de fichiers), quelle que soit la langue ou la casse."""
    fin = time.time() + delai
    while time.time() < fin:
        ecran = lire_ecran()
        for n in ecran:
            if n.texte.strip().lower() in {l.lower() for l in libelles}:
                return n
        time.sleep(1)
    return None


def tester_sauvegarde(r: Recette) -> None:
    """Export puis import de la sauvegarde depuis Mes runs, avec le sélecteur de fichiers Android (T15)."""
    onglet("Accueil")
    remonter(3)
    # L'en-tête « Run <nom> · <difficulté> » de l'accueil ouvre Mes runs (action « Changer de run »).
    changer = attendre("Run Rubis Oméga", 6)
    if changer:
        toucher(changer, 3)
    exporter = trouver_en_defilant("Exporter ma progression", 8, exact=True)
    if not r.verifier("Export / import", "carte Sauvegarde dans Mes runs", exporter):
        return
    toucher(exporter, 4)
    enregistrer = bouton_systeme("Save", "Enregistrer")
    if enregistrer:
        toucher(enregistrer, 3)
    r.verifier("Export / import", "export dans un fichier JSON", attendre("Sauvegarde enregistrée", 12),
               "bouton du sélecteur introuvable" if not enregistrer else "")
    importer = trouver_en_defilant("Importer une sauvegarde", 6, exact=True)
    if importer:
        toucher(importer, 4)
    fichier = None
    fin = time.time() + 15
    while time.time() < fin and fichier is None:
        fichier = next((n for n in lire_ecran() if n.texte.startswith("omegadex-sauvegarde-")), None)
        time.sleep(1)
    if fichier:
        toucher(fichier, 4)
    r.verifier("Export / import", "import : la run déjà présente n'est pas dupliquée",
               attendre("déjà dans ce compte", 12), "fichier introuvable dans le sélecteur" if not fichier else "")
    retour = chercher("Retour", exact=True)
    if retour:
        toucher(retour, 2)


def scenario(r: Recette, jusqu_a_la_ligue: bool) -> None:
    shell(f"am start -n {ACTIVITE}")
    time.sleep(8)
    for _ in range(3):
        if not fermer_dialogues_systeme(lire_ecran()):
            break
    r.verifier("Lancement", "premier lancement", attendre("Créer un compte", 20) or attendre("Se connecter", 5))
    if not creer_compte(r, "Yamao") or not creer_run(r):
        return
    ouvrir_aventure()
    r.verifier("Runs", "la run s'ouvre sur l'étape 0", etape_affichee(ETAPES[0]))
    valider_etape(r, ETAPES[0], ETAPES[1], capturer=True)

    redemarrer_app()
    r.verifier("Session", "le joueur reste connecté après redémarrage", attendre("Bonjour, Yamao", 15))
    ouvrir_aventure()
    r.verifier("Aventure", "l'étape 1 survit au redémarrage", etape_affichee(ETAPES[1]))
    premiere = trouver_en_defilant(ETAPES[1]["quetes"][0]["description"], 8, exact=True)
    if premiere:
        toucher(premiere, 1.5)
        redemarrer_app()
        ouvrir_aventure()
        coche = trouver_en_defilant(ETAPES[1]["quetes"][0]["description"], 8, exact=True)
        r.verifier("Aventure", "une quête cochée survit au redémarrage", coche and quete_cochee(coche))

    tester_pokedex(r)
    tester_accueil(r, 1)
    for nom in ("Défis", "Équipe"):
        onglet(nom)
        r.verifier("Écrans", f"onglet {nom}", chercher(nom) is not None)
    tester_mode_avion(r)
    tester_comptes(r)
    tester_sauvegarde(r)

    if jusqu_a_la_ligue:
        run_jusqu_a_la_ligue(r)


def run_jusqu_a_la_ligue(r: Recette) -> None:
    """T16.3 : valide les quêtes principales des étapes 1 à 10 (Ligue) dans l'application."""
    ouvrir_aventure()
    for i in range(1, ETAPE_LIGUE + 1):
        etape, suivante = ETAPES[i], ETAPES[i + 1]
        remonter(4)
        if not valider_etape(r, etape, suivante, capturer=True):
            return
        remonter(4)
    onglet("Pokédex")
    r.verifier("Pokédex national", "le Pokédex national s'ouvre après la Ligue", attendre("Pokédex national", 10))
    onglet("Accueil")
    remonter(3)
    r.verifier("Run jusqu'à la Ligue", "accueil après la Ligue", attendre("Bonjour, Yamao", 8))


def main() -> int:
    args = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    args.add_argument("--profil", required=True)
    args.add_argument("--sortie", type=pathlib.Path, default=pathlib.Path("recette"))
    args.add_argument("--jusqu-a-la-ligue", action="store_true")
    a = args.parse_args()
    dossier = a.sortie / a.profil
    dossier.mkdir(parents=True, exist_ok=True)
    r = Recette(a.profil, dossier)
    adb("logcat", "-b", "crash", "-c")
    shell("settings put global hide_error_dialogs 1")  # pas de boîte « … ne répond pas » du lanceur
    print("Claviers virtuels désactivés :", desactiver_clavier_virtuel(), flush=True)
    try:
        scenario(r, a.jusqu_a_la_ligue)
    except Exception as erreur:  # le rapport doit être écrit même si le script lui-même échoue
        r.verifier("Script", "exception du script de recette", False, repr(erreur)[:300])
    finally:
        (dossier / "logcat.txt").write_text(adb("logcat", "-d", "-v", "time", f"--pid={shell('pidof ' + PAQUET).strip() or 0}"))
        (dossier / "crash.txt").write_text(adb("logcat", "-d", "-b", "crash"))
        r.ecrire()
    return 0 if all(x["ok"] for x in r.resultats) else 1


if __name__ == "__main__":
    sys.exit(main())
